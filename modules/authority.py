import carlbot
import discord
import os
from pathlib import Path


class Authority(carlbot.Module):

    def __init__(self):
        super().__init__()
        self.authorities = []

        if not os.path.exists("owners.txt"):
            Path("owners.txt").touch()

        with open("owners.txt", "r") as owners_file:
            self.owners = owners_file.readlines()
            self.owners = [x.strip() for x in self.owners]

        self._register_authority("manipulate_authority")

    @staticmethod
    def get_name():
        return "authority"

    @staticmethod
    def dependency_list():
        return ["persistence", "command_parsing"]

    def public_commands(self):
        return [("lsauth", self.list_authority),
                ("addauth", self.add_authority_command),
                ("rmauth", self.remove_authority_command)]

    def on_module_load(self, module):
        print("Regestering authority for module: {}".format(module.get_name()))
        func = getattr(module, "authorities", None)
        if func:
            for name in func():
                self._register_authority(name)

    def on_module_unload(self, module):
        func = getattr(module, "authorities", None)
        if func:
            for name in func():
                self._delete_authority(name)

    def _register_authority(self, name):
        if name not in self.authorities:
            self.authorities.append(name)

    def _delete_authority(self, name):
        self.authorities.remove(name)

    async def check_admin(self, target):
        if target.id in self.owners:
            return True

        if isinstance(target, discord.Member):
            return target.server_permissions.administrator
        else:
            return target.permissions.administrator

    async def check_authority(self, server_id, target, authority_name, admin_override=True):
        if admin_override and await self.check_admin(target):
            return True
        else:

            if isinstance(target, discord.Role):
                authority = carlbot.modules.persistence.get_role_data(self, server_id, target.id).get("authorities", None)
            else:
                authority = carlbot.modules.persistence.get_user_data(self, server_id, target.id).get("authorities", None)

            if authority:
                return authority_name in authority
            else:
                return False

    async def list_authority(self, parts, server, channel, message):
        command_name = parts.pop(0)  # Chop off the command name.

        if len(parts) == 0:
            message = "Reports what authority a given user or role has associated with it.\n" \
                      "Usage: {} <@mention user or role here>\n" \
                      "Please see your Carl Bot distribution's manual for more details.\n\n".format(command_name)

            message += "Authorities avalible for assignment:\n```"

            for a in self.authorities:
                message += "{}\n".format(a)

            message += "```"

            return message

        target = await carlbot.modules.command_parsing.get_user_or_role(parts, server)

        message = ""

        if isinstance(target, discord.Role):
            message += "This role has the following authority:\n" \
                       "```\n"

            data = carlbot.modules.persistence.get_role_data(self, server.id, target.id)
        else:
            if await self.check_admin(target):
                message += "This user is an admin, so they have all authorities.\n" \
                           "Still, on top of that is the following authorities.\n" \
                           "```\n"
            else:
                message += "This user has the following authority:\n" \
                           "```\n"

            data = carlbot.modules.persistence.get_user_data(self, server.id, target.id)

        authorities = data.get("authorities", None)

        if authorities:
            for auth in authorities:
                message += "{}\n".format(auth)
        else:
            message += "\n"

        message += "\n```"

        return message

    auth_return_codes = {
        0: "Authority Added",
        1: "User or role already has this authority.",
        2: "`{auth}` is not a known authority.",
        3: "Authority removed.",
        4: "User does not have this authority."
    }

    async def add_authority_command(self, parts, server, channel, message):
        if await self.check_authority(server.id, message.author, "manipulate_authority"):
            command_name = parts.pop(0)  # Chop off the command name.

            if len(parts) < 2:
                return "Enables you to give users and roles authority.\n" \
                       "Usage: {} <@mention user or role here>".format(command_name)

            target = await carlbot.modules.command_parsing.get_user_or_role(parts, server)
            authority = parts.pop(0)

            if not target:
                return "Could not find target user or role."

            result = self.add_authority(server.id, target, authority)
            return self.auth_return_codes[result].format(auth=authority)
        else:
            return "Only admins or those with the \"authority\" authority are permitted to modify Carl Bot " \
                   "authorities.\n" \
                   "Please see your Carl Bot Distribution's manual for details on how to resolve this."

    async def remove_authority_command(self, parts, server, channel, message):
        if await self.check_authority(server.id, message.author, "manipulate_authority"):
            command_name = parts.pop(0)  # Chop off the command name.

            if len(parts) < 2:
                return "Enables you to take authority from users and roles.\n" \
                       "Usage: {} <@mention user or role here>".format(command_name)

            target = await carlbot.modules.command_parsing.get_user_or_role(parts, server)
            authority = parts.pop(0)

            if not target:
                return "Could not find target user or role."

            result = self.remove_authority(server.id, target, authority)
            return self.auth_return_codes[result].format(auth=authority)
        else:
            return "Only admins or those with the \"authority\" authority are permitted to modify Carl Bot " \
                   "authorities.\n" \
                   "Please see your Carl Bot Distribution's manual for details on how to resolve this."

    def add_authority(self, server_id, target, authority):
        if isinstance(target, discord.Role):
            data = carlbot.modules.persistence.get_role_data(self, server_id, target.id)
        else:
            data = carlbot.modules.persistence.get_user_data(self, server_id, target.id)

        authorities = data.get("authorities", None)
        if not authorities:
            authorities = []
            data["authorities"] = authorities

        if authority not in self.authorities:
            return 2

        if authority in authorities:
            return 1
        else:
            authorities.append(authority)
            return 0

    def remove_authority(self, server_id, target, authority):
        if isinstance(target, discord.Role):
            data = carlbot.modules.persistence.get_role_data(self, server_id, target.id)
        else:
            data = carlbot.modules.persistence.get_user_data(self, server_id, target.id)

        authorities = data.get("authorities", None)
        if not authorities:
            authorities = []
            data["authorities"] = authorities

        if authority not in self.authorities:
            return 2

        if authority in authorities:
            authorities.remove(authority)
            return 3
        else:
            return 4

    async def load(self):
        pass

    def save(self):
        pass

carlbot.add_module(Authority())
