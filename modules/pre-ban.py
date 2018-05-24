import carlbot


class PreBan(carlbot.Module):
    def __int__(self):
        super().__init__()

    @staticmethod
    def get_name():
        return "pre-ban"

    @staticmethod
    def dependency_list():
        return ["authority"]

    @staticmethod
    def permission_list():
        return ["pre-ban"]

carlbot.add_module(PreBan())
