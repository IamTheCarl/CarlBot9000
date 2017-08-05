import carlbot

async def clear(args, server, channel, message):
    def should_delete(m):
        return m.author == carlbot.client.user or m.content.startswith(carlbot.call_sign)

    await carlbot.client.purge_from(channel, limit=20, check=should_delete)


carlbot.add_command('clear', clear)