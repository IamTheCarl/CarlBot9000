import carlbot


class PreBan(carlbot.Module):
    def __int__(self):
        super().__init__()

    @staticmethod
    def dependency_list():
        return ["authority"]

    @staticmethod
    def permission_list():
        return ["pre-ban"]

carlbot.add_module("pre-ban", PreBan())
