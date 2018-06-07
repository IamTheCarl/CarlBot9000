import carlbot
import random


class Pelt(carlbot.Module):
    pelt_items = [
        "\U0001F95C",  # Peanut.
        "\U0001F45E",  # Man's Shoe.
        "\U0001F45F",  # Athletic Shoe.
        "\U0001F58D",  # Crayon
        "\U0001F954",  # Potato
        "\U0001F4D8",  # Blue Book
        "\U0001F4B5",  # Money
        "\U0001F32D"   # Hotdog
    ]

    def __init__(self):
        super().__init__()

    @staticmethod
    def get_name():
        return "pelt"

    @staticmethod
    def dependency_list():
        return ["command_parsing", "persistence", "authority"]

    def public_commands(self):
        return [("pelt", self.pelt)]

    def message_hooks(self):
        return [self.on_message]

    @staticmethod
    def authorities():
        return ["pelt_anyone", "pelt_scrub"]

    async def pelt(self, args, server, channel, message):

        args.pop(0)  # Remove the command name.

        if len(args) < 1:
            await carlbot.client.send_message(channel, "You need to supply a target to pelt.")
            return

        target = await carlbot.modules.command_parsing.get_user(args, server)

        if await carlbot.modules.authority.check_authority(server.id, message.author, "pelt_anyone")\
            or (target.id == message.author.id
                and not await carlbot.modules.authority.check_authority(
                        server.id, target, "pelt_scrub", admin_override=False)):

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
        else:
            await carlbot.client.send_message(channel, "You do not have authority to pelt this person.")

    async def on_message(self, server, channel, message):
        data = carlbot.modules.persistence.get_server_data(self, server.id)
        targets = data.get("targets", None)
        if not targets:
            targets = []
            data["targets"] = targets

        if message.author.id in targets:
            item = self.pelt_items[max(random.randint(-100, len(self.pelt_items) - 1), 0)]
            await carlbot.client.add_reaction(message, item)

carlbot.add_module(Pelt())
