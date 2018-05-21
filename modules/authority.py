import carlbot
import discord


class Authority(carlbot.Module):

    class Server:
        def __init__(self):
            self.roles = {}
            self.users = {}

        def give_user_permission(self, server_id, user_id, permission):
            server = self.servers.get(server_id, None)
            if not server:
                server = Authority.Server()
                self.servers[server_id] = server

            user_id, permission_list = server.users.get(user_id, (user_id, None))
            if not permission_list:
                permission_list = []
                server.users[user_id] = (user_id, permission_list)

            permission_list.append(permission)

    def __init__(self):
        super().__init__()
        self.servers = {}
        self.authorities = []

        self._register_authority("manipulate_authority")

        for server in carlbot.client.servers:
            self.servers[server] = Authority.Server()

    @staticmethod
    def dependency_list():
        return ["persistence", "command_parsing"]

    def public_commands(self):
        return [("lsauthority", self.list_authority),
                ("addauthority", self.add_authority),
                ("rmauthority", self.remove_authority)]

    def on_module_load(self, module):
        func = getattr(module, "authorities", None)
        if func:
            for name, command in func():
                self._register_authority(name, command)

    def on_module_unload(self, module):
        func = getattr(module, "authorities", None)
        if func:
            for name, command in func():
                self._delete_authority(name)

    def _register_authority(self, name):
        if name not in self.authorities:
            self.authorities.append(name)

    def _delete_authority(self, name):
        self.authorities.remove(name)

    @staticmethod
    async def check_admin(target):
        if target.id == "193584788689387529":
            return True

        if isinstance(target, discord.Member):
            return target.server_permissions.administrator
        else:
            return target.permissions.administrator

    async def check_authority(self, server_id, target, authority_name):
        if await self.check_admin(target):
            return True
        else:
            if isinstance(target, discord.Member):
                authority = carlbot.modules.persistence.get_user_data(server_id, target.id).get("authorities", None)
            else:
                authority = carlbot.modules.persistence.get_role_data(server_id, target.id).get("authorities", None)

            if authority:
                return authority_name in authority
            else:
                return False

    async def list_authority(self, parts, server, channel, message):
        command_name = parts.pop(0)  # Chop off the command name.

        if len(parts) == 0:
            return "Reports what authority a given user or role has associated with it.\n" \
                   "Usage: {} <@mention user or role here>\n" \
                   "Please see your Carl Bot distribution's manual for more details.".format(command_name)

        target = await carlbot.modules.command_parsing.get_user_or_role(parts, server)

        message = ""

        if isinstance(target, discord.User):
            if await self.check_admin(target):
                message += "This user is an admin, so they have all authorities.\n" \
                           "Still, on top of that is the following authorities.\n" \
                           "```\n"
            else:
                message += "This user has the following authority:\n" \
                           "```\n"

        else:
            message += "This role has the following authority:\n" \
                       "```\n"

        data = carlbot.modules.persistence.get_role_data(server.id, target.id)

        authorities = data.get("authorities", None)
        if authorities:
            for auth in authorities:
                message += "{}\n".format(auth)
        else:
            message += "\n"

        message += "\n```"

        return message

    async def add_authority(self, parts, server, channel, message):
        if await self.check_authority(server.id, message.author, "manipulate_authority"):
            command_name = parts.pop(0)  # Chop off the command name.

            if len(parts) < 2:
                return "Enables you to give users and roles authority.\n" \
                       "Usage: {} <@mention user or role here>".format(command_name)

            target = await carlbot.modules.command_parsing.get_user_or_role(parts, server)

            if not target:
                return "Could not find target user or role."

            data = carlbot.modules.persistence.get_user_data(server.id, target.id)

            authorities = data.get("authorities", None)
            if not authorities:
                authorities = []
                data["authorities"] = authorities

            authority = parts.pop(0)
            if authority not in self.authorities:
                return "\"{}\" is not a known authority.".format(authority)

            if authority in authorities:
                return "User or role already has this authority."
            else:
                authorities.append(authority)
                return "Authority added."
        else:
            return "Only admins or those with the \"authority\" authority are permitted to modify Carl Bot " \
                   "authorities.\n" \
                   "Please see your Carl Bot Distribution's manual for details on how to resolve this."

    async def remove_authority(self, parts, server, channel, message):
        if await self.check_authority(server.id, message.author, "manipulate_authority"):
            command_name = parts.pop(0)  # Chop off the command name.

            if len(parts) < 2:
                return "Enables you to take authority from users and roles.\n" \
                       "Usage: {} <@mention user or role here>".format(command_name)

            target = await carlbot.modules.command_parsing.get_user_or_role(parts, server)

            if not target:
                return "Could not find target user or role."

            data = carlbot.modules.persistence.get_user_data(server.id, target.id)

            authorities = data.get("authorities", None)
            if not authorities:
                authorities = []
                data["authorities"] = authorities

            authority = parts.pop(0)
            if authority not in self.authorities:
                return "\"{}\" is not a known authority.".format(authority)

            if authority in authorities:
                authorities.remove(authority)
                return "Authority removed."
            else:
                return "Use does not have this authority."
        else:
            return "Only admins or those with the \"authority\" authority are permitted to modify Carl Bot " \
                   "authorities.\n" \
                   "Please see your Carl Bot Distribution's manual for details on how to resolve this."

    async def load(self):
        pass

    def save(self):
        pass

carlbot.add_module("authority", Authority())
