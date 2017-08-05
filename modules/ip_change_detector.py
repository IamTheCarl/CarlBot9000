import carlbot
import os
import urllib.request
import json
import socket
import discord


class IpChangeDetector(carlbot.Module):
    def __init__(self):
        super().__init__()
        self.notification_channels = []

    @staticmethod
    def dependency_list():
        return ["local_terminal"]

    async def post_dependency_resolve(self):
        await self.load()

    def local_commands(self):
        return [("ipNotifyAdd", self.add_channel_to_notify),
                ("ipNotifyRemove", self.remove_channel_to_notify),
                ("ipNotifyBroadcast", self.command_line_broadcast)]

    async def add_channel_to_notify(self, args):

        if len(args) < 4:
            carlbot.term_print_error("Not enough arguments.")
            return

        role = args.pop()
        channel = args.pop()
        server = args.pop()

        self._add_channel_to_notify(server, channel, role)

        self.save()

    async def remove_channel_to_notify(self, args):

        if len(args) < 4:
            carlbot.term_print_error("Not enough arguments.")
            return

        role = args.pop()
        channel = args.pop()
        server = args.pop()

        server = carlbot.client.get_server(server)
        if not server:
            carlbot.term_print_error("Failed to find server.")
            return

        role = discord.utils.find(lambda r: r.id == role, server.roles)
        if role not in server.roles:
            carlbot.term_print_error("Failed to find role in server.")
            return

        channel = server.get_channel(channel)
        if not channel:
            carlbot.term_print_error("Failed to find channel in server.")
            return

        carlbot.term_print("Removed notification channel:\nServer: {}\nChannel: {}\nRole: {}"
                           .format(server.name, channel.name, role.name))

        notification_set = (server, channel, role)

        self.notification_channels.remove(notification_set)
        self.save()

        if notification_set in self.notification_channels:
            carlbot.term_print_warning(
                "[Warning] This channel was in the list of channels to be notified multiple times.\n"
                "It will still receive notifications of potential ip change.")

    async def command_line_broadcast(self, args):
        args.pop()
        await self.notify_of_connect()

    async def _add_channel_to_notify(self, server, channel, role):
        server = carlbot.client.get_server(server)
        if not server:
            carlbot.term_print_error("Failed to find server.")
            return

        role = discord.utils.find(lambda r: r.id == role, server.roles)
        if role not in server.roles:
            carlbot.term_print_error("Failed to find role in server.")
            return

        channel = server.get_channel(channel)
        if not channel:
            carlbot.term_print_error("Failed to find channel in server.")
            return

        print("Added notification channel:\n"
              "Server: {} - {}\n"
              "Channel: {} - {}\n"
              "Role: {} - {}"
              .format(server.name, server.id,
                      channel.name, channel.id,
                      role.name, role.id))

        notification_set = (server, channel, role)

        if notification_set in self.notification_channels:
            carlbot.term_print_warning(
                "[Warning] This channel is already set to receive notifications of potential ip change.\n"
                "This will result in multiple notifications being sent to this channel at once.")

        self.notification_channels.append(notification_set)

    @staticmethod
    def get_external_ip():
        r = urllib.request.urlopen("https://api.ipify.org?format=json").read()
        data = json.loads(r.decode("utf-8"))
        return data["ip"]

    async def notify_of_connect(self):
        host = os.uname()[1]
        ip = self.get_external_ip()

        net_address = socket.gethostbyname("artifactgaming.net")
        com_address = socket.gethostbyname("artifactgaming.com")

        message = "Re-connection detected.\n" \
                  "Host: {host}\n" \
                  "External IP address: {ip}\n" \
                  "Domain \"artifactgaming.net\" resolves to: {net_address}\n" \
                  "Domain \"artifactgaming.com\" resolves to: {com_address}\n" \
                  "If any of these IP addresses do not match, there is a problem." \
            .format(host=host, ip=ip, net_address=net_address, com_address=com_address)

        for server, channel, role in self.notification_channels:
            await carlbot.client.send_message(channel, "{}\n{}".format(role.mention, message))

    def connection_hooks(self):
        return[self.notify_of_connect]

    async def load(self):
        await carlbot.client.wait_until_ready()

        if os.path.exists("database/ip_change_detection/channels_to_notify.json"):
            carlbot.term_print_event("Loading ip change config.")
            with open("database/ip_change_detection/channels_to_notify.json", "r") as channels_to_notify:
                data = json.load(channels_to_notify)
                for server, channel, role in data:
                    await self._add_channel_to_notify(server, channel, role)
            carlbot.term_print_event("Done.")
        else:
            print("There were no channels to notify data to load.")

    def save(self):

        data = []

        for server, channel, role in self.notification_channels:
            data.append((server.id, channel.id, role.id))

        if not os.path.exists("database/ip_change_detection"):
            os.makedirs("database/ip_change_detection")

        with open("database/ip_change_detection/channels_to_notify.json", "w") as channels_to_notify:
            json.dump(data, channels_to_notify)

carlbot.add_module("ip_change_detector", IpChangeDetector())
