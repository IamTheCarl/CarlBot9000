import carlbot
import datetime
import requests
import tempfile
import re
import random


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
                ("import_nitori", self.import_nitori)]

    @staticmethod
    def authorities():
        return ["quote_admin", "infinity_gauntlet_of_quotes", "quote_scrub"]

    async def quote(self, args, server, channel, message):
        name = args.pop(0)  # Remove command name.

        if await carlbot.modules.authority\
                .check_authority(server.id, message.author, "quote_scrub", admin_override=False):
            return "You have the `quote_scrub` authority, meaning that the admins of this server have banned you from "\
                   "using quotes.\nIt was probably your own fault and you deserved it."

        if len(args) < 1:
            return "Usage: $>{} add|edit|remove|delete_all|setup|random|<quote#>"\
                   "\nPlease see Carl Bot Wiki for more details.".format(name)

        data = carlbot.modules.persistence.get_server_data(self, server.id)
        quotes = data.get("quotes", None)

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
                if len(args) == 1:
                    index = len(quotes)
                    quotes[index] = {
                        "text": args.pop(0),
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

        net_source = requests.get(url)
        with tempfile.SpooledTemporaryFile() as source:
            source.write(bytes(net_source.text, "utf-8"))
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

carlbot.add_module(Quotes())
