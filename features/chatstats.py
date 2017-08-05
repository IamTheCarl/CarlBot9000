import carlbot
import discord

async def chat_stats(args, server, channel, message):
    args.pop(0)  # Remove the command name.

    if len(args) == 0:
        return "```Usage:\n" \
               "{callsign}chatStats [time delta]\n" \
               "Type \"{callsign}help timedelta\" for more information on how to enter a time delta.```"\
                .format(callsign=carlbot.call_sign)

    await carlbot.client.send_message(message.channel, "Working...")
    message_string = '```'

    delta = carlbot.get_time_delta(args)

    target = carlbot.datetime.now() - delta

    total_messages = 0

    class ChannelStat:
        def __init__(self, name, messages):
            self.name = name
            self.messages = messages

    channels = []

    for search_channel in server.channels:

        if search_channel.type != discord.enums.ChannelType.voice:

            log = 0

            async for m in carlbot.client.logs_from(search_channel, limit=99999999999999999, before=None, after=target):
                log += 1

            channel = ChannelStat(search_channel.name, log)
            channels.append(channel)

            total_messages += log

    # Sort it.
    channels = sorted(channels, key=lambda channel: channel.messages, reverse=True)

    message_string += "Total Messages since {target:%c}: {count}\n".format(target=target, count=total_messages)

    for stat in channels:
        message_string += "{channel_name}: %{num:.2f} - {count} messages.\n".format(channel_name=stat.name, count=stat.messages,
                                                            num=(stat.messages / total_messages) * 100)

    message_string += '```'

    await carlbot.client.send_message(message.channel, "{} done.".format(message.author.mention))

    return message_string


carlbot.add_command('chatStats', chat_stats)