import carlbot
import asyncio

async def count_down(args, server, channel, message):
    args.pop(0)  # Remove the command name from the arguments list.

    if len(args) < 3:
        await carlbot.client.send_message(message.channel, "Bad Syntax.")
        return

    try:
        target = int(args.pop(0))
    except ValueError:  # If the user gives bad input, catch this.
        raise Exception("Provided countdown target is not a number.")

    count_text = args.pop(0)
    finish_text = args.pop(0)

    await carlbot.client.delete_message(message)

    message = await carlbot.client.send_message(message.channel, "{} {}".format(count_text, target))
    await asyncio.sleep(1)

    for i in range(target - 1, 0, -1):
        await carlbot.client.edit_message(message, "{} {}".format(count_text, i))
        await asyncio.sleep(1)

    await carlbot.client.edit_message(message, finish_text)

carlbot.add_command('countDown', count_down)