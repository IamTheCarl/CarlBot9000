import carlbot


class LocalTermUtils(carlbot.Module):
    def __init__(self):
        super().__init__()

    @staticmethod
    def get_name():
        return "local_term_utils"

    def local_commands(self):
        return [("reconnect", self.reconnect)]

    @staticmethod
    async def reconnect(parts):
        print("Reconnecting...")
        # carlbot.client.connect()
        print("Done.")

carlbot.add_module(LocalTermUtils())
