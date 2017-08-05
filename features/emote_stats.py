import carlbot
import discord
from datetime import datetime


async def emote_stats(args, server, channel, message):
    args.pop(0)  # Remove the command name.

    if len(args) == 0:
        return "```Usage:\n{callsign}reactionStats [time delta]\n" \
               "Type \"{callsign}help timedelta\" for more information on how to enter a time delta.```"

    await carlbot.client.send_message(message.channel, "Working...")

    delta = carlbot.get_time_delta(args)

    target = datetime.now() - delta

    total_reactions = 0

    class Stat:
        def __init__(self, callsign, name):
            self.callsign = callsign
            self.name = name
            self.reaction_count = 1

    emote_map = {}

    for search_channel in server.channels:

        if search_channel.type != discord.enums.ChannelType.voice:

            async for m in carlbot.client.logs_from(search_channel, limit=99999999999999999, before=None, after=target):
                for r in m.reactions:
                    if r.custom_emoji:
                        reaction_count = r.count

                        stat = emote_map.get(r.emoji, None)
                        if stat:
                            stat.reaction_count += 1
                        else:
                            stat = Stat("<:{}:{}>".format(r.emoji.name, r.emoji.id), r.emoji.name)
                            emote_map[r.emoji] = stat

                        total_reactions += reaction_count

    # Sort it.
    reactions = []

    for emote, stat in emote_map.items():
        reactions.append(stat)

    reactions = sorted(reactions, key=lambda reaction: reaction.reaction_count, reverse=True)

    message_string = "`{total} custom reaction emotes have been used since {target}.`\n"\
                     .format(target=target, total=total_reactions)

    for stat in reactions:
        message_string += "{callsign} - {count} %{num:.2f}\n"\
                            .format(callsign=stat.callsign, count=stat.reaction_count,
                                    num=(stat.reaction_count / total_reactions) * 100)

    await carlbot.client.send_message(message.channel, "{} done.".format(message.author.mention))

    return message_string


carlbot.add_command('emoteStats', emote_stats)
