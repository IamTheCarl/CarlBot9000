import carlbot
import discord
from datetime import timedelta


class CommandParsing(carlbot.Module):
    def __init__(self):
        super().__init__()

    @staticmethod
    def get_name():
        return "command_parsing"

    @staticmethod
    def get_int_argument(args):
        name = args.pop(0)
        try:
            return int(args.pop(0))
        except ValueError:  # If the user gives bad input, catch this.
            raise Exception("Failed to interpret input for \"{input}\".".format(input=name))

    @staticmethod
    def get_time_delta(args):
        hour = 0
        day = 0
        week = 0
        year = 0

        for item in args:
            if item == 'hours':
                hour = CommandParsing.get_int_argument(args)

            elif item == 'days':
                day = CommandParsing.get_int_argument(args)

            elif item == 'weeks':
                week = CommandParsing.get_int_argument(args)

            elif item == 'years':
                year = CommandParsing.get_int_argument(args)

            else:
                raise Exception("Unknown time delta variable: {name}".format(name=item))

        return timedelta(hours=hour, days=day + year * 365, weeks=week)

    @staticmethod
    async def get_user(args, server):
        # Users are formatted <@#>
        a = args.pop(0)
        # Notice that we exclude roles.
        if a.startswith("<@") and not a.startswith("<@&"):
            offset = 2
            if a.startswith("<@!"):
                offset = 3
            return discord.utils.get(server.members, id=a[offset:-1])

    @staticmethod
    async def get_role(args, server):
        # Roles are formatted <@&#>
        a = args.pop(0)

        if a.startswith("<@&"):
            return discord.utils.get(server.roles, id=a[3:-1])
        pass

    @staticmethod
    async def get_user_or_role(args, server):
        # Users are formatted <@#>
        # Roles are formatted <@&#>
        a = args.pop(0)
        # Notice that we exclude roles.
        if a.startswith("<@"):
            if a.startswith("<@&"):  # Is a role.
                return discord.utils.get(server.roles, id=a[3:-1])
            else:  # Is a user.
                return discord.utils.get(server.members, id=a[2:-1])

carlbot.add_module(CommandParsing())
