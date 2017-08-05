import carlbot
import os
import json


class Persistence(carlbot.Module):

    class Server:
        def __init__(self, data=None):
            self.roles = {}
            self.users = {}
            self.data = {}

            if data:
                self.roles = data["roles"]
                self.users = data["users"]
                self.data = data["data"]

        def save(self, server_id, file):
            data = {"server_id": server_id,
                    "roles": self.roles,
                    "users": self.users,
                    "data":  self.data}

            json.dump(data, file)

    def __init__(self):
        super().__init__()
        self.servers = {}
        self.load()

    def local_commands(self):
        return [("save", self.save_command)]

    async def save_command(self, parts):
        print("Saving all data...")
        self.save()
        print("Saved.")

    def _get_server(self, server_id):
        server = self.servers.get(server_id, None)
        if not server:
            server = Persistence.Server()
            self.servers[server_id] = server

        return server

    def get_user_data(self, server_id, user_id):
        server = self._get_server(server_id)
        user = server.users.get(user_id, None)
        if not user:
            user = {}
            server.users[user_id] = user

        return user

    def get_role_data(self, server_id, role_id):
        server = self._get_server(server_id)
        role = server.users.get(role_id, None)
        if not role:
            role = {}
            server.roles[role_id] = role

        return role

    def get_server_data(self, server_id):
        server = self._get_server(server_id)
        return server.data

    def save(self):
        if not os.path.exists("database/persistence"):
            os.makedirs("database/persistence")

        for server_id, server in self.servers.items():
            with open("database/persistence/{}.json".format(server_id), "w") as file:
                server.save(server_id, file)

    def load(self):
        for filename in os.listdir("database/persistence/"):
            with open(os.path.join("database/persistence/", filename), "r") as file:
                data = json.load(file)
                self.servers[data["server_id"]] = Persistence.Server(data)


carlbot.add_module("persistence", Persistence())
