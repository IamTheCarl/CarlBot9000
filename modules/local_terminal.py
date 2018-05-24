import carlbot
import asyncio
import sys
import shlex
import traceback
import os
import curses
import curses.ascii
import discord


# noinspection PyBroadException
class LocalTerminal(carlbot.Module):
    commands = {}

    def __init__(self):
        super().__init__()
        self.q = asyncio.Queue()
        carlbot.client.loop.add_reader(sys.stdin, self.on_get_terminal_input, self.q)

        self.current_server = None
        self.current_channel = None

        self._add_local_command("shutdown", self.shutdown)
        self._add_local_command("clear", self.clear_screen)

        self._add_local_command("whereami", self.where_am_i)
        self._add_local_command("lss", self.list_servers)
        self._add_local_command("lsc", self.list_channels)
        self._add_local_command("lsu", self.list_users)
        self._add_local_command("lsr", self.list_roles)
        self._add_local_command("joins", self.join_server)
        self._add_local_command("joinc", self.join_channel)
        self._add_local_command("joinp", self.join_private_channel)

        self._add_local_command("lsm", self.list_modules)
        self._add_local_command("lsam", self.list_avalible_modules)
        self._add_local_command("loadm", self.load_module)
        self._add_local_command("unloadm", self.unload_module)
        self._add_local_command("reloadm", self.reload_module)

        carlbot.main_window.resize(curses.LINES - 1, curses.COLS)
        self.command_window = curses.newwin(1, curses.COLS, curses.LINES - 1, 0)
        self.command_window.bkgd(' ', curses.color_pair(carlbot.COMMAND_COLOR))
        self.command_window.nodelay(True)

        self.chat_window = None

    @staticmethod
    def get_name():
        return "local_terminal"

    def _add_local_command(self, name, function):
        self.commands[name] = function

    def _remove_local_command(self, name):
        del self.commands[name]

    async def channel_response_hook(self, server, channel, message):
        if channel == self.current_channel:
            self.chat_window.addstr("{} [{}]: {}\n".format(message.author, message.timestamp.strftime("%I:%M:%S%p"), message.content))
            self.chat_window.refresh()

    def message_hooks(self):
        return [self.channel_response_hook]

    def on_module_load(self, module):
        func = getattr(module, "local_commands", None)
        if func:
            for name, command in func():
                self._add_local_command(name, command)

    def on_module_unload(self, module):
        func = getattr(module, "local_commands", None)
        if func:
            for name, command in func():
                self._remove_local_command(name)

    @staticmethod
    async def unknown_command(args):
        return "Unknown command \"{command}\"\nRemember, I'm a stupid bot. I'm sensitive to caps and spaces.".format(
            command=args[0])

    @staticmethod
    async def shutdown(args):
        args.pop()
        await carlbot.shutdown()

    @staticmethod
    async def clear_screen(args):
        args.pop()
        carlbot.main_window.clear()
        carlbot.main_window.refresh()

    async def where_am_i(self, args):
        args.pop()
        if self.current_server:
            print("Server: {} - {}".format(self.current_server.name, self.current_server.id))
            if self.current_channel:
                print("Channel: {} - {}".format(self.current_channel.name, self.current_channel.id))
        else:
            if self.current_channel:
                print("Private Channel: {} - {}".format(self.current_channel.name, self.current_channel.id))
            else:
                print("You are nowhere.")

    @staticmethod
    async def list_servers(args):
        args.pop()
        for s in carlbot.client.servers:
            print("{} : {}".format(s.name, s.id))

    async def list_channels(self, args):
        if self.current_server:
            args.pop()
            for c in self.current_server.channels:
                if c.type != discord.ChannelType.voice:
                    print("{} : {}".format(c.name, c.id))
        else:
            print("You must be in a server to list the avalible channels.")

    async def list_users(self, args):
        if self.current_server:
            args.pop()
            for s in self.current_server.members:
                print("{} : {}".format(s.name, s.id))
        else:
            print("You must be in a server to list users.")

    async def list_roles(self, args):
        if self.current_server:
            args.pop()
            for r in self.current_server.roles:
                print("{} : {}".format(r.name, r.id))
        else:
            print("You must be in a server to list roles.")

    async def join_server(self, args):
        server_id = args.pop()
        server = carlbot.client.get_server(server_id)

        if server:
            self.current_channel = None
            self.current_server = server
            print("Joined server: {} - {}".format(server.id, server.name))
        else:
            print("Failed to find a server by that id.")

    async def join_channel(self, args):

        if self.current_server:
            channel_id = args.pop()

            channel = self.current_server.get_channel(channel_id)

            if channel:
                if channel.type == discord.ChannelType.voice:
                    carlbot.term_print_error("Can not join a voice channel.")
                else:
                    self.current_channel = channel
                    print("Joined channel: {} - {}".format(channel.id, channel.name))
                    await self.channel_join()
            else:
                print("Failed to find a channel by that id in this server.")
        else:
            print("You must first join a server.")

    async def join_private_channel(self, args):
        if self.current_server:
            user_id = args.pop()
            member = self.current_server.get_member(user_id)
            if member:
                channel = await carlbot.client.start_private_message(member)
                if channel:
                    self.current_server = None
                    self.current_channel = channel
                    print("Joined private channel.")
                    await self.channel_join()
                else:
                    print("Failed to create private channel with that user.")
            else:
                print("Could not find a member by that id in this server.")
        else:
            print("You need to be in a server to start a private conversation.")

    @staticmethod
    async def list_modules(args):
        args.pop()
        for k in carlbot.modules.list():
            print(k)

    @staticmethod
    async def list_avalible_modules(args):
        args.pop()
        for f in os.listdir(carlbot.module_folder):
            print(f[:-3])

    @staticmethod
    async def unload_module(args):
        module_name = args.pop()
        worked = carlbot.unload_module(module_name)

        if worked:
            print("Module has been unloaded.")
        else:
            print("Failed to unload module.")

    @staticmethod
    async def load_module(args):
        module_name = args.pop()
        worked = carlbot.load_module(module_name)

        if worked:
            print("Module has been loaded.")
        else:
            print("Failed to load module. Does it not exist? Is it already loaded?")

    @staticmethod
    async def reload_module(args):
        module_name = args.pop()

        worked = carlbot.unload_module(module_name)

        if worked:
            print("Module has been unloaded.")
            worked = carlbot.load_module(module_name)

            if worked:
                print("Module has been loaded.")
            else:
                print("Failed to load module. Does it not exist? Is it already loaded?")
        else:
            print("Failed to unload module.")

    async def channel_join(self):
        print("All text you enter will now be written by Carl Bot to the channel you are in.")
        print("Press ESC to exit this channel.")

        carlbot.main_window.resize(curses.LINES - 11, curses.COLS)

        self.chat_window = curses.newwin(10, curses.COLS, curses.LINES - 11, 0)
        self.chat_window.bkgd(' ', curses.color_pair(carlbot.INFO_WINDOW_COLOR))
        self.chat_window.scrollok(True)

        async for m in carlbot.client.logs_from(self.current_channel, limit=5, reverse=True):
            self.chat_window.addstr(
                "{} [{}]: {}\n".format(m.author, m.timestamp.strftime("%I:%M:%S%p"), m.content))

        self.chat_window.refresh()

    def channel_leave(self):
        carlbot.main_window.resize(curses.LINES - 1, curses.COLS)
        self.chat_window = None

    async def get_command(self):
        try:
            command = ""

            c = None
            while c != ord('\n'):

                c = await asyncio.ensure_future(self.q.get())

                if c == 27:  # Special response to the escape key.
                    return None

                if c == 127:  # Backspace
                    command = command[:-1]
                    self.command_window.clear()
                    self.command_window.addstr(0, 0, command)
                    self.command_window.refresh()

                if c != -1 and (curses.ascii.isgraph(c) or c == ord(' ')):
                    c = chr(c)
                    command = "{}{}".format(command, c)

                    self.command_window.clear()
                    self.command_window.addstr(0, 0, command)
                    self.command_window.refresh()
            return command
        except GeneratorExit:  # This is just to keep things quiet on shutdown.
            pass
        finally:
            self.command_window.clear()
            self.command_window.refresh()

    def on_get_terminal_input(self, q):
        asyncio.ensure_future(q.put(self.command_window.getch()))

    async def terminal_loop(self):
        await carlbot.client.wait_until_ready()
        print("Local terminal started.")
        self.command_window.refresh()

        while not carlbot.client.is_closed and self.is_loaded:

            try:
                command_string = await self.get_command()

                if self.current_channel:
                    if not command_string:
                        self.current_channel = None
                        self.channel_leave()
                        print("You are no-longer in a channel.")
                    else:
                        await carlbot.client.send_message(self.current_channel, command_string)
                else:
                    if command_string:
                        parts = None

                        try:
                            parts = shlex.split(command_string)
                        except Exception as error:
                            print("Error parsing input: {error}".format(error=error))

                        if parts:
                            carlbot.term_print_info("{}".format(command_string))
                            command = self.commands.get(parts[0], self.unknown_command)

                            message_string = None

                            try:
                                message_string = await command(parts)
                            except:
                                traceback.print_exc()

                            if message_string and len(message_string) > 0:
                                print(message_string)
            except:
                traceback.print_exc()

    def background_task(self):
        return self.terminal_loop

    def cleanup(self):
        print("Terminal is no-longer available.")
        self.channel_leave()
        carlbot.main_window.resize(curses.LINES, curses.COLS)
        self.command_window.nodelay(1)

carlbot.add_module(LocalTerminal())
