import carlbot
import discord


class Pelt(carlbot.Module):
    def __init__(self):
        super().__init__()
        self.targets = []

    @staticmethod
    def dependency_list():
        return ["command_parsing"]

    def public_commands(self):
        return [("pelt", self.pelt)]

    def message_hooks(self):
        return [self.on_message]

    async def pelt(self, args, server, channel, message):

        args.pop(0)  # Remove the command name.
        target = await carlbot.modules.command_parsing.get_user(args, server)

        if message.author.server_permissions.administrator or target == message.author:
            
            if target.id in self.targets:
                self.targets.remove(target.id)
                await carlbot.client.send_message(channel, "I will pelt no more.")
            else:
                self.targets.append(target.id)
                await carlbot.client.send_message(channel, "I will pelt.")

    async def on_message(self, server, channel, message):
        if message.author.id in self.targets:
            await carlbot.client.add_reaction(message, "\U0001F95C")

carlbot.add_module("pelt", Pelt())
