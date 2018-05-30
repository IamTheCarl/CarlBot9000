import carlbot
import datetime


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
        name = args.pop(0) # Remove command name.

        if len(args) < 1:
            return "Usage: $>{} add|edit|remove|delete_all|setup|<quote#>\nPlease see Carl Bot Wiki for more details.".format(name)

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

            try:
                index = int(index_string)
            except ValueError:
                return "Failed to read quote number."

            if quotes is None:
                return "Quote system has not been setup for this server.\n"\
                       "Have someone with the quote_admin authority do it."

            quote = quotes.get(index, None)

            if quote is None:
                return "No such quote."
            else:
                text = quote["text"]

                if text is None:
                    return "This quote was deleted."
                else:
                    return text
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

    def import_nitori(self, args, server, channel, message):
        pass

carlbot.add_module(Quotes())
