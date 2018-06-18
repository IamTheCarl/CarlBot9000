import carlbot
import datetime
import json
import tempfile
import re
import random
import discord


class Quotes(carlbot.Module):
    def __init__(self):
        super().__init__()

    @staticmethod
    def get_name():
        return "quotes"

    @staticmethod
    def dependency_list():
        return ["command_parsing", "persistence", "authority"]

    def public_commands(self):
        return [("quote", self.quote),
                ("import_nitori", self.import_nitori),
                ("export_quotes", self.export_quotes)]

    @staticmethod
    def authorities():
        return ["quote_admin", "infinity_gauntlet_of_quotes", "quote_scrub"]

    async def quote(self, args, server, channel, message):
        name = args.pop(0)  # Remove command name.

        # User is a pleb without perms to quote.
        if await carlbot.modules.authority\
                .check_authority(server.id, message.author, "quote_scrub", admin_override=False):
            return "You have the `quote_scrub` authority, meaning that the admins of this server have banned you from "\
                   "using quotes.\nIt was probably your own fault and you deserved it."

        # No argument was given.
        if len(args) < 1:
            return "Usage: $>{} add|edit|remove|delete_all|setup|random|<quote#>"\
                   "\nPlease see Carl Bot Wiki for more details.".format(name)

        # Fetch data from teh server, all the quotes.
        data = carlbot.modules.persistence.get_server_data(self, server.id)
        quotes = data.get("quotes", None)

        # If only one argument was given.
        if len(args) == 1:
            pass

            index_string = args.pop(0)

            if index_string == "setup":
                if await carlbot.modules.authority.check_authority(server.id, message.author, "quote_admin"):
                    if quotes is None:
                        data["quotes"] = {}
                        return "Quote system is now setup. " \
                               "Note that you no longer can import quotes from a Nitori file.\n" \
                               "If you wish to undo this action so you can import quotes from RoboNitori, use the" \
                               "`delete_all` command."
                    else:
                        return "Quote system is already setup."
                else:
                    return "You need the `quote_admin` authority to do this."

            if index_string == "delete_all":
                if await carlbot.modules\
                        .authority.\
                        check_authority(server.id, message.author, "infinity_gauntlet_of_quotes", admin_override=False):
                    carlbot.modules.authority.remove_authority(server.id, message.author, "infinity_gauntlet_of_quotes")

                    if quotes is not None:
                        data["quotes"] = None

                        return "I don't feel so good... all quotes deleted from server.\n"\
                               "You will need to go through setup again to re-enabled quotes."
                    else:
                        return "Quote system was never setup."

                else:
                    return "You need the authority `infinity_gauntlet_of_quotes` to do this. Being admin does not "\
                           "qualify for this authority."

            if quotes is None:
                return "Quote system has not been setup for this server.\n"\
                       "Have someone with the quote_admin authority do it."

            if index_string == "random":
                text = None
                index = 0

                while text is None:
                    index = random.randint(0, len(quotes) - 1)
                    text = quotes[index]["text"]

                # Put a bullet in front to not trigger commands.
                return "\U00002022 {}".format(text)

            try:
                index = int(index_string)
            except ValueError:
                return "Failed to read quote number."

            quote = quotes.get(index, None)

            if quote is None:
                return "No such quote."
            else:
                text = quote["text"]

                if text is None:
                    return "This quote was deleted."
                else:
                    # Put a bullet in front to not trigger commands.
                    return "\U00002022 {}".format(text)
        else:
            # add, edit, remove, or delete_all quotes
            mode = args.pop(0)

            if quotes is None:
                return "Quote system is not yet setup."

            if mode == "add":

                quote_msg = args.pop(0)
                # If this quote mentions everybody!
                if message.mention_everyone:
                    # Place all occurrences of '@everyone' in code-wraps
                    # to prevent @everyone mentions when using this quote.
                    quote_msg = quote_msg.replace("@everyone", "`@everyone`")

                if len(args) == 1:
                    index = len(quotes)
                    quotes[index] = {
                        "text": quote_msg,
                        "owner": message.author.id,
                        "channel": channel.id,
                        "datetime": datetime.datetime.now()
                    }
                    return "Added quote number {}".format(index)
                else:
                    return "Error: Unexpected number of arguments.\n" \
                           "Should be `$>quote add \"your quote here\""

            if mode == "remove":
                if len(args) == 1:
                    index = int(args.pop(0))
                    quote = quotes.get(index, None)

                    if quote is None:
                        return "This quote does not exist."
                    else:
                        owner = quote["owner"]

                        if owner == message.author.id or\
                                await carlbot.modules\
                                .authority.check_authority(server.id, message.author, "quote_admin"):

                            quote["text"] = None

                            return "Quote removed."
                        else:
                            return "Only the owner of this quote or someone with the `quote_admin` "\
                                   "authority can delete this quote."
                else:
                    return "Error: Unexpected number of arguments.\n" \
                           "Should be `$>quote remove [quote number here]"

            if mode == "edit":
                if len(args) == 2:
                    index = int(args.pop(0))
                    new_text = args.pop(0)

                    quote = quotes.get(index, None)

                    if quote is None:
                        return "This quote does not exist."
                    else:
                        owner = quote["owner"]

                        if owner == message.author.id or\
                                await carlbot.modules.authority\
                                .check_authority(server.id, message.author, "quote_admin"):

                            quote["text"] = new_text

                            return "Quote changed."
                        else:
                            return "Only the owner of this quote or someone with the `quote_admin` "\
                                   "authority can edit this quote."
                else:
                    return "Error: Unexpected number of arguments.\n" \
                           "Should be `$>quote edit [quote number here] "\
                           "[new text for quote here (best if put between \" marks)]"

            if mode == "info":
                if len(args) == 1:
                    index = int(args.pop(0))

                    quote = quotes.get(index, None)

                    if quote is None:
                        return "This quote does not exist."
                    else:
                        owner = str(discord.utils.get(server.members, id=quote["owner"]))
                        channel = str(discord.utils.get(server.channels, id=quote["channel"]))
                        date = str(quote["datetime"])
                        text = quote["text"]
                        if text is None:
                            text = "Message Deleted."

                        return "```Text: {}\nOwner: {}\nChannel Created in: {}\nDate of creation: {}```"\
                            .format(text, owner, channel, date)

                else:
                    return "Error: Unexpected number of arguments.\n" \
                           "Should be `$>quote info [quote number here]"

            if mode == "chown":
                if len(args) == 2:
                    index = int(args.pop(0))
                    new_owner = await carlbot.modules.command_parsing.get_user(args, server)

                    quote = quotes.get(index, None)

                    if quote is None:
                        return "This quote does not exist."
                    else:
                        owner = quote["owner"]

                        if owner == message.author.id or\
                                await carlbot.modules.authority\
                                .check_authority(server.id, message.author, "quote_admin"):

                            quote["owner"] = new_owner.id

                            return "Owner changed."
                        else:
                            return "Only the owner of this quote or someone with the `quote_admin` "\
                                   "authority can change the owner of this quote."
                else:
                    return "Error: Unexpected number of arguments.\n" \
                           "Should be `$>quote edit [quote number here] "\
                           "[new text for quote here (best if put between \" marks)]"

            return "Unknown action: {}".format(mode)

    async def import_nitori(self, args, server, channel, message):
        args.pop(0)

        data = carlbot.modules.persistence.get_server_data(self, server.id)
        quotes = data.get("quotes", None)

        if quotes is not None:
            return "Quote system has already been setup. Use `$>quote delete_all` to delete all quotes. "\
                   "Then you can use this command."

        if not await carlbot.modules.authority.check_authority(server.id, message.author, "quote_admin"):
            return "You need the `quote_admin` authority to do this."

        attachments = message.attachments
        if len(attachments) == 0:
            return "You must send the Nitori quotes file as an attachment to the message this command was sent in."

        url = attachments[0]["url"]

        with tempfile.SpooledTemporaryFile() as source:
            async with carlbot.aiohttp_session.get(url) as net_source:
                source.write(bytes(await net_source.text(), "utf-8"))
            source.seek(0)

            quotes = {
                0: {
                    "text": "Quotes imported from Robo Nitori. RIP Robo Nitori. Thank you Googie for a wonderful bot.",
                    "datetime": datetime.datetime.now(),
                    "owner": "",
                    "channel": ""
                }
            }

            data["quotes"] = quotes

            quote = {}
            number = 0

            for line in source:
                line = line.decode("utf-8")
                if line.startswith("Quote"):
                    start = line.find(":")
                    num_start = re.search("\d", line).start()

                    quote["text"] = line[start + 2:-1]

                    if start == -1:
                        start = line.find(" ", num_start)
                        quote["text"] = None

                    number = int(line[num_start:start])

                    if quote["text"] is None:
                        quotes[number] = quote
                        quote = {}
                    continue

                if line.startswith("Created"):
                    date_start = re.search("\d", line).start()
                    date_end = line.find(" ", line.find(" ", date_start) + 1)

                    quote["datetime"] = datetime.datetime.strptime(line[date_start:date_end], "%Y-%m-%d %H:%M:%S")

                    owner_start = line.find("<@") + 2
                    quote["owner"] = line[owner_start:owner_start + 18]

                    channel_start = line.find("<#") + 2
                    quote["channel"] = line[channel_start:channel_start + 18]

                    quotes[number] = quote
                    quote = {}

        return "Done."

    async def export_quotes(self, args, server, channel, message):
        data = carlbot.modules.persistence.get_server_data(self, server.id)
        quotes = data.get("quotes", None)

        await carlbot.client.send_message(channel, "Working...")

        if quotes:
            with tempfile.NamedTemporaryFile() as export:

                data = []

                for key, quote in quotes.items():
                    human_owner_name = discord.utils.get(server.members, id=quote.get("owner", None))
                    if human_owner_name:
                        human_owner_name = human_owner_name.name
                    else:
                        human_owner_name = None
                                                                                                

                    data.append({
                        "number": key,
                        "datetime": str(quote.get("datetime", None)),
                        "text": {
                            "machine": quote.get("text", None),
                            "human": await carlbot.modules.command_parsing.clean_message(server, quote.get("text", None))
                        },
                        "owner": {
                            "machine": quote.get("owner", None),
                            "human": human_owner_name
                        }
                    })
                export.write(bytes(json.dumps(data, indent=4, sort_keys=True), "utf-8"))
                export.flush()

                await carlbot.client.send_file(channel,
                                               export.name,
                                               content="{} Done.".format(message.author.mention),
                                               filename="QuotesFor-{}-Written-{}.json"\
                                               .format(server.name, datetime.datetime.now()))
        else:
            return "Quotes are not setup."

carlbot.add_module(Quotes())
