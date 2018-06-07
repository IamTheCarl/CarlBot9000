import carlbot


class UrMum(carlbot.Module):
    emojis = [
        "\U0001F1F3",  # N
        "\U0001F1F4",  # O
        "\U0001F1FA",  # U
    ]

    def __init__(self):
        super().__init__()

    @staticmethod
    def get_name():
        return "ur_mum"

    @staticmethod
    def dependency_list():
        return ["command_parsing"]

    def message_hooks(self):
        return [self.on_message]


    async def on_message(self, server, channel, message):
        if message.content.startswith("ur mum"):
            for e in self.emojis:
                await carlbot.client.add_reaction(message, e)


carlbot.add_module(UrMum())
