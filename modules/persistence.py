import carlbot
import os
import pickle


class Persistence(carlbot.Module):

    class Server:
        def __init__(self, server_id):
            self.server_id = server_id
            self.roles = {}
            self.users = {}
            self.data = {}

    def __init__(self):
        super().__init__()
        self.servers = {}
        self.load()

    @staticmethod
    def get_name():
        return "persistence"

    def local_commands(self):
        return [("save", self.save_command)]

    async def save_command(self, parts):
        print("Saving all data...")
        self.save()
        print("Saved.")

    def _get_server(self, server_id):
        server = self.servers.get(server_id, None)
        if not server:
            server = Persistence.Server(server_id)
            self.servers[server_id] = server

        return server

    def get_user_data(self, module, server_id, user_id):
        server = self._get_server(server_id)
        user = server.users.get(user_id, None)
        if not user:
            user = {}
            server.users[user_id] = user

        mod_data = user.get(module.get_name(), None)
        if not mod_data:
            mod_data = {}
            user[module.get_name()] = mod_data

        return mod_data

    def get_role_data(self, module, server_id, role_id):
        server = self._get_server(server_id)
        role = server.roles.get(role_id, None)
        if not role:
            role = {}
            server.roles[role_id] = role

        mod_data = role.get(module.get_name(), None)
        if not mod_data:
            mod_data = {}
            role[module.get_name()] = mod_data

        return mod_data

    def get_server_data(self, module, server_id):
        server = self._get_server(server_id)

        mod_data = server.data.get(module.get_name(), None)
        if not mod_data:
            mod_data = {}
            server.data[module.get_name()] = mod_data

        return mod_data

    def save(self):
        if not os.path.exists("database/server_persistence"):
            os.makedirs("database/server_persistence")

        for server_id, server in self.servers.items():
            with open("database/server_persistence/{}.pkl".format(server_id), "wb") as file:
                pickle.dump(server, file, pickle.HIGHEST_PROTOCOL)

    def load(self):
        if not os.path.exists("database/server_persistence"):
            os.makedirs("database/server_persistence")

        for filename in os.listdir("database/server_persistence/"):
            with open(os.path.join("database/server_persistence/", filename), "rb") as file:
                server = pickle.load(file)
                print("Loaded server data for server: {}".format(server.server_id))
                self.servers[server.server_id] = server


carlbot.add_module(Persistence())
