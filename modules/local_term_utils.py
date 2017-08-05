import carlbot


class LocalTermUtils(carlbot.Module):
    def __init__(self):
        super().__init__()

    def local_commands(self):
        return [("reconnect", self.reconnect)]

    @staticmethod
    async def reconnect(parts):
        print("Reconnecting...")
        # carlbot.client.connect()
        print("Done.")

carlbot.add_module("local_term_utils", LocalTermUtils())
