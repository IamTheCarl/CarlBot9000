import discord
import shlex
import os
import sys
import traceback
import curses
import sqlalchemy as sql

from botToken import BotToken

module_folder = "./modules"
call_sign = "$>"

client = discord.Client()
_bot_globals = locals()
main_window = None

ERROR_COLOR = 1
WARNING_COLOR = 2
EVENT_COLOR = 3
INFO_COLOR = 4
COMMAND_COLOR = 5
INFO_WINDOW_COLOR = 6

_LOG = None


# def term_print(text):
#     _LOG.write("{}\n".format(text))
#
#     main_window.addstr("{}\n".format(text))
#     main_window.refresh()


def term_print_error(text):
    _LOG.write("{}\n".format(text))

    main_window.addstr("{}\n".format(text), curses.color_pair(ERROR_COLOR))
    main_window.refresh()


def term_print_warning(text):
    _LOG.write("{}\n".format(text))

    main_window.addstr("{}\n".format(text), curses.color_pair(WARNING_COLOR))
    main_window.refresh()


def term_print_event(text):
    _LOG.write("{}\n".format(text))

    main_window.addstr("{}\n".format(text), curses.color_pair(EVENT_COLOR))
    main_window.refresh()


def term_print_info(text):
    _LOG.write("{}\n".format(text))

    main_window.addstr("{}\n".format(text), curses.color_pair(INFO_COLOR))
    main_window.refresh()


class Module:
    def __init__(self):
        self.is_loaded = False

    @staticmethod
    def dependency_list():
        return []

    @staticmethod
    async def post_dependency_resolve():
        pass

    @staticmethod
    def public_commands():
        return []

    @staticmethod
    def private_commands():
        return []

    def background_task(self):
        return None

    @staticmethod
    def connection_hooks():
        return []

    @staticmethod
    def message_hooks():
        return []

    @staticmethod
    def message_delete_hooks():
        return []

    @staticmethod
    def message_edit_hooks():
        return []

    @staticmethod
    def reaction_add_hooks():
        return []

    @staticmethod
    def reaction_remove_hooks():
        return []

    @staticmethod
    def reaction_clear_hooks():
        return []

    @staticmethod
    def channel_delete_hooks():
        return []

    @staticmethod
    def channel_update_hooks():
        return []

    @staticmethod
    def member_join_hooks():
        return []

    @staticmethod
    def member_update_hooks():
        return []

    @staticmethod
    def server_join_hooks():
        return []

    @staticmethod
    def server_remove_hooks():
        return []

    @staticmethod
    def server_update_hooks():
        return []

    @staticmethod
    def server_role_creation_hooks():
        return []

    @staticmethod
    def server_role_deletion_hooks():
        return []

    @staticmethod
    def server_role_update_hooks():
        return []

    @staticmethod
    def server_emojis_update_hooks():
        return []

    @staticmethod
    def server_avalible_hooks():
        return []

    @staticmethod
    def server_unavailable_hooks():
        return []

    @staticmethod
    def voice_state_update_hooks():
        return []

    @staticmethod
    def member_ban_hooks():
        return []

    @staticmethod
    def member_pardon_hooks():
        return []

    @staticmethod
    def on_typing_hooks():
        return []

    @staticmethod
    def on_group_join_hooks():
        return []

    @staticmethod
    def on_module_load(module):
        pass

    @staticmethod
    def on_module_unload(module):
        pass

    def cleanup(self):
        pass

    def save(self):
        pass


def add_message_hook(function):
    message_hooks.append(function)

async def unknown_command(args, server, channel, message):
    return "Unknown command \"{command}\"\n" \
           "Remember, I'm a stupid bot. I'm sensitive to caps and spaces.".format(command=args[0])


async def shutdown():
    for m in _modules.values():
        m.is_loaded = False
        m.save()
        m.cleanup()

    await client.close()


_modules = {}
modules = None

public_commands = {}
private_commands = {}
connect_hooks = []
message_hooks = []
message_delete_hooks = []
message_edit_hooks = []
reaction_add_hooks = []
reaction_remove_hooks = []
reaction_clear_hooks = []
channel_delete_hooks = []
channel_update_hooks = []
member_join_hooks = []
member_update_hooks = []
server_join_hooks = []
server_remove_hooks = []
server_update_hooks = []
server_role_creation_hooks = []
server_role_deletion_hooks = []
server_role_update_hooks = []
server_emojis_update_hooks = []
server_avalible_hooks = []
server_unavailable_hooks = []
voice_state_update_hooks = []
member_ban_hooks = []
member_pardon_hooks = []
on_typing_hooks = []
group_join_hooks = []


def add_module(name, module):

    module.is_loaded = True

    for dep in module.dependency_list():
        load_module(dep)

    client.loop.create_task(module.post_dependency_resolve())

    background_task = module.background_task()
    if background_task:
        client.loop.create_task(background_task())

    for command_name, command in module.public_commands():
        public_commands[command_name] = command

    for command_name, command in module.private_commands():
        private_commands[command_name] = command

    for connect_hook in module.connection_hooks():
        connect_hooks.append(connect_hook)

    for message_hook in module.message_hooks():
        message_hooks.append(message_hook)

    for message_delete_hook in module.message_delete_hooks():
        message_delete_hooks.append(message_delete_hook)

    for message_edit_hook in module.message_edit_hooks():
        message_edit_hooks.append(message_edit_hook)

    for message_add_hook in module.reaction_add_hooks():
        reaction_add_hooks.append(message_add_hook)

    for reaction_remove_hook in module.reaction_add_hooks():
        reaction_remove_hooks.append(reaction_remove_hook)

    for reaction_clear_hook in module.reaction_clear_hooks():
        reaction_clear_hooks.append(reaction_clear_hook)

    for channel_delete_hook in module.channel_delete_hooks():
        channel_delete_hooks.append(channel_delete_hook)

    for channel_update_hook in module.channel_update_hooks():
        channel_update_hooks.append(channel_update_hook)

    for member_join_hook in module.member_join_hooks():
        member_join_hooks.append(member_join_hook)

    for member_update_hook in module.member_update_hooks():
        member_update_hooks.append(member_update_hook)

    for server_join_hook in module.server_join_hooks():
        server_join_hooks.append(server_join_hook)

    for server_remove_hook in module.server_remove_hooks():
        server_remove_hooks.append(server_remove_hook)

    for server_update_hook in module.server_update_hooks():
        server_update_hooks.append(server_update_hook)

    for server_role_creation_hook in module.server_role_creation_hooks():
        server_role_creation_hooks.append(server_role_creation_hook)

    for server_role_creation_hook in module.server_role_creation_hooks():
        server_role_deletion_hooks.append(server_role_creation_hook)

    for server_role_update_hook in module.server_role_update_hooks():
        server_role_update_hooks.append(server_role_update_hook)

    for server_emojis_update_hook in module.server_emojis_update_hooks():
        server_emojis_update_hooks.append(server_emojis_update_hook)

    for server_avalible_hook in module.server_avalible_hooks():
        server_avalible_hooks.append(server_avalible_hook)

    for server_unavailable_hook in module.server_unavailable_hooks():
        server_unavailable_hooks.append(server_unavailable_hook)

    for voice_state_update_hook in module.voice_state_update_hooks():
        voice_state_update_hooks.append(voice_state_update_hook)

    for member_ban_hook in module.member_ban_hooks():
        member_ban_hooks.append(member_ban_hook)

    for member_pardon_hook in module.member_pardon_hooks():
        member_pardon_hooks.append(member_pardon_hook)

    for on_typing_hook in module.on_typing_hooks():
        on_typing_hooks.append(on_typing_hook)

    for group_join_hook in module.on_group_join_hooks():
        group_join_hooks.append(group_join_hook)

    for m in _modules.values():
        m.on_module_load(module)

    _modules[name] = module


def unload_module(name):
    module = _modules.get(name, None)

    if module:
        del _modules[name]
        module.is_loaded = False

        for command_name, command in module.public_commands():
            del public_commands[command_name]

        for command_name, command in module.private_commands():
            del private_commands[command_name]

        for connect_hook in module.connection_hooks():
            connect_hooks.remove(connect_hook)

        for message_hook in module.message_hooks():
            message_hooks.remove(message_hook)

        for message_delete_hook in module.message_delete_hooks():
            message_delete_hooks.remove(message_delete_hook)

        for message_edit_hook in module.message_edit_hooks():
            message_edit_hooks.remove(message_edit_hook)

        for message_add_hook in module.reaction_add_hooks():
            reaction_add_hooks.remove(message_add_hook)

        for reaction_remove_hook in module.reaction_add_hooks():
            reaction_remove_hooks.remove(reaction_remove_hook)

        for reaction_clear_hook in module.reaction_clear_hooks():
            reaction_clear_hooks.remove(reaction_clear_hook)

        for channel_delete_hook in module.channel_delete_hooks():
            channel_delete_hooks.remove(channel_delete_hook)

        for channel_update_hook in module.channel_update_hooks():
            channel_update_hooks.remove(channel_update_hook)

        for member_join_hook in module.member_join_hooks():
            member_join_hooks.remove(member_join_hook)

        for member_update_hook in module.member_update_hooks():
            member_update_hooks.remove(member_update_hook)

        for server_join_hook in module.server_join_hooks():
            server_join_hooks.remove(server_join_hook)

        for server_remove_hook in module.server_remove_hooks():
            server_remove_hooks.remove(server_remove_hook)

        for server_update_hook in module.server_update_hooks():
            server_update_hooks.remove(server_update_hook)

        for server_role_creation_hook in module.server_role_creation_hooks():
            server_role_creation_hooks.remove(server_role_creation_hook)

        for server_role_creation_hook in module.server_role_creation_hooks():
            server_role_deletion_hooks.remove(server_role_creation_hook)

        for server_role_update_hook in module.server_role_update_hooks():
            server_role_update_hooks.remove(server_role_update_hook)

        for server_emojis_update_hook in module.server_emojis_update_hooks():
            server_emojis_update_hooks.remove(server_emojis_update_hook)

        for server_avalible_hook in module.server_avalible_hooks():
            server_avalible_hooks.remove(server_avalible_hook)

        for server_unavailable_hook in module.server_unavailable_hooks():
            server_unavailable_hooks.remove(server_unavailable_hook)

        for voice_state_update_hook in module.voice_state_update_hooks():
            voice_state_update_hooks.remove(voice_state_update_hook)

        for member_ban_hook in module.member_ban_hooks():
            member_ban_hooks.remove(member_ban_hook)

        for member_pardon_hook in module.member_pardon_hooks():
            member_pardon_hooks.remove(member_pardon_hook)

        for on_typing_hook in module.on_typing_hooks():
            on_typing_hooks.remove(on_typing_hook)

        for group_join_hook in module.on_group_join_hooks():
            group_join_hooks.remove(group_join_hook)

        for m in _modules.values():
            m.on_module_unload(module)

        module.save()
        module.cleanup()

        return True
    else:
        return False


def load_module(name):
    if _modules.get(name, None):
        return False

    print("Loading: {}".format(name))
    name = "{}.py".format(name)

    file = os.path.join(module_folder, name)

    if not os.path.exists(file):
        return False

    data = ""
    with open(file, "r") as input_file:
        for line in input_file:
            data += line

    # Disable the warning about too broad of an exception catch. We want to catch all exceptions here.
    # noinspection PyBroadException
    try:
        exec(compile(data, file, 'exec'), globals())
    except:
        term_print_error("Error while loading module:")
        term_print_error(traceback.format_exc())

    return True


def load_modules():
    print("Loading all modules...")

    # We do not want the modules we are loading to double import the Carl Bot API.
    # To prevent that, we take all the functions and variables we've created and pack them into an object.
    class CarlBot:
        def __init__(self):
            self.__dict__ = _bot_globals

    class Modules:
        def __init__(self):
            self.__dict__ = _modules

        @staticmethod
        def list():
            return _modules

    global modules
    modules = Modules()

    # Then we create that object and load it as the Carl Bot API.
    # Because it is already loaded as an API in the modules, it will not load again when the features request Python
    # to import carlbot.
    sys.modules['carlbot'] = CarlBot()

    with open("enabled_modules.txt", "r") as mods_file:
        mod_names = mods_file.readlines()
        mod_names = [x.strip() for x in mod_names]

    for mod in mod_names:  # List all the contents of the directory.
        if mod.startswith("#"):  # ignore comments.
            continue

        load_module(mod)

    print("Done.")


# noinspection PyBroadException
@client.event
async def on_ready():
    term_print_event("Bot logged in as:")
    term_print_event(client.user.name)

    for f in connect_hooks:
        try:
            await f()
        except:
            traceback.print_exc()


@client.event
async def on_message(message):

    channel = message.channel
    server = None
    if not channel.is_private:
        server = channel.server

    for hook in message_hooks:
        await hook(server, message.channel, message)

    if message.content.startswith(call_sign):
        # We got a command.
        parts = None

        try:
            parts = shlex.split(message.content[len(call_sign):])
        except Exception as error:
            await client.send_message(message.channel, "Error parsing input: {error}".format(error=error))

        if parts:
            if channel.is_private:
                command = private_commands.get(parts[0], unknown_command)
            else:
                command = public_commands.get(parts[0], unknown_command)

            try:
                message_string = await command(parts, server, message.channel, message)
            except Exception as error:
                message_string = "Error while executing command:\n`{}`".format(error)
                term_print_error(traceback.format_exc())

            if message_string and len(message_string) > 0:
                await client.send_message(message.channel, message_string)


db_engine = sql.create_engine("sqlite:///database.db", echo=True)
db_engine.connect()


def main(std_screen):
    curses.init_pair(ERROR_COLOR, curses.COLOR_RED, curses.COLOR_BLACK)
    curses.init_pair(WARNING_COLOR, curses.COLOR_YELLOW, curses.COLOR_BLACK)
    curses.init_pair(INFO_COLOR, curses.COLOR_GREEN, curses.COLOR_BLACK)
    curses.init_pair(EVENT_COLOR, curses.COLOR_MAGENTA, curses.COLOR_BLACK)
    curses.init_pair(COMMAND_COLOR, curses.COLOR_BLACK, curses.COLOR_GREEN)
    curses.init_pair(INFO_WINDOW_COLOR, curses.COLOR_WHITE, curses.COLOR_BLUE)

    curses.curs_set(True)

    global main_window
    main_window = std_screen
    main_window.scrollok(True)

    class StdOutWrapper:
        @staticmethod
        def write(text):
            _LOG.write(text)

            main_window.addstr(text)
            main_window.refresh()

    sys.stdout = StdOutWrapper()

    class StdErrWrapper:
        @staticmethod
        def write(text):
            _LOG.write(text)

            main_window.addstr(text, curses.color_pair(ERROR_COLOR))
            main_window.refresh()

    sys.stderr = StdErrWrapper()

    global _LOG
    with open("log.log", "w") as _LOG:
        load_modules()
        print("Starting Carl Bot...")
        client.run(BotToken.token)

if __name__ == "__main__":
    curses.wrapper(main)
