import carlbot
import discord


class Pelt(carlbot.Module):
    def __init__(self):
        super().__init__()

    @staticmethod
    def get_name():
        return "pelt"

    @staticmethod
    def dependency_list():
        return ["command_parsing", "persistence"]

    def public_commands(self):
        return [("pelt", self.pelt)]

    def message_hooks(self):
        return [self.on_message]

    async def pelt(self, args, server, channel, message):

        args.pop(0)  # Remove the command name.
        target = await carlbot.modules.command_parsing.get_user(args, server)

        if message.author.server_permissions.administrator or target == message.author:
            data = carlbot.modules.persistence.get_server_data(self, server.id)
            targets = data.get("targets", None)
            if not targets:
                targets = []
                data["targets"] = targets
            
            if target.id in targets:
                targets.remove(target.id)
                await carlbot.client.send_message(channel, "I will pelt no more.")
            else:
                targets.append(target.id)
                await carlbot.client.send_message(channel, "I will pelt.")

    async def on_message(self, server, channel, message):
        data = carlbot.modules.persistence.get_server_data(self, server.id)
        targets = data.get("targets", None)
        if not targets:
            targets = []
            data["targets"] = targets

        if message.author.id in targets:
            await carlbot.client.add_reaction(message, "\U0001F95C")

carlbot.add_module(Pelt())
