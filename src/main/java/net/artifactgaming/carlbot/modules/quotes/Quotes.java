package net.artifactgaming.carlbot.modules.quotes;

import net.artifactgaming.carlbot.*;
import net.artifactgaming.carlbot.Module;
import net.artifactgaming.carlbot.modules.authority.Authority;
import net.artifactgaming.carlbot.modules.authority.AuthorityManagement;
import net.artifactgaming.carlbot.modules.authority.AuthorityRequiring;
import net.artifactgaming.carlbot.modules.persistence.Persistence;
import net.artifactgaming.carlbot.modules.persistence.PersistentModule;
import net.artifactgaming.carlbot.modules.persistence.Table;
import net.artifactgaming.carlbot.modules.selfdocumentation.Documented;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

public class Quotes implements Module, AuthorityRequiring, PersistentModule, Documented {

    private AuthorityManagement authorityManagement;
    private Persistence persistence;

    private Logger logger = LoggerFactory.getLogger(Quotes.class);

    @Override
    public Authority[] getRequiredAuthority() {
        return new Authority[] { new QuoteAdmin(), new UseQuotes() };
    }

    private class AddCommand implements Command, AuthorityRequiring, Documented {

        @Override
        public String getCallsign() {
            return "add";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            if (tokens.size() == 2) {
                Table table = getQuoteTable(event.getGuild());

                // First we check if the quote already exists.
                ResultSet resultSet = table.select().where("key", "=",tokens.get(0)).execute();

                if (resultSet.next()) {
                    event.getChannel().sendMessage("A quote already exists for this key. "
                            + "You can edit or remove the quote if you are the owner.").queue();
                } else {
                    User author = event.getMessage().getAuthor();
                    table.insert().set("owner", author.getId())
                                  .set("owner_name", author.getName())
                                  .set("key", tokens.get(0))
                                  .set("quote", tokens.get(1)).execute();

                    event.getChannel().sendMessage("Quote added to database.").queue();
                }

            } else {
                event.getChannel().sendMessage(
                        "Wrong number of arguments. Command should be:\n$>quote add \"key\" \"quote\"").queue();
            }
        }

        @Override
        public Authority[] getRequiredAuthority() {
            return new Authority[] { new UseQuotes() };
        }

        @Override
        public Module getParentModule() {
            return Quotes.this;
        }

        @Override
        public String getDocumentation() {
            return "Adds a quote.";
        }

        @Override
        public String getDocumentationCallsign() {
            return "add";
        }
    }

    private class RemoveCommand implements Command, AuthorityRequiring, Documented {

        @Override
        public String getCallsign() {
            return "remove";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            if (tokens.size() == 1) {
                Table table = getQuoteTable(event.getGuild());

                // First we check if the quote already exists.
                ResultSet resultSet = table.select().where("key", "=", tokens.get(0)).execute();

                if (resultSet.next()) {
                    if (event.getAuthor().getId().equals(resultSet.getString("owner"))
                            || authorityManagement.checkHasAuthority(event.getMember(), new QuoteAdmin())) {

                        table.delete()
                                .where("key", "=", tokens.get(0))
                                .execute();

                        event.getChannel().sendMessage("Quote deleted.").queue();

                    } else {
                        event.getChannel().sendMessage(
                                "You must own this quote or be the quote admin to delete it.").queue();
                    }
                } else {
                    event.getChannel().sendMessage("Could not find quote by that key.").queue();
                }
            } else {
                event.getChannel()
                        .sendMessage("Wrong number of arguments. Command should be:\n$>quote remove \"key\".")
                        .queue();
            }
        }

        @Override
        public Authority[] getRequiredAuthority() {
            return new Authority[] { new UseQuotes() };
        }

        @Override
        public Module getParentModule() {
            return Quotes.this;
        }

        @Override
        public String getDocumentation() {
            return "Removes a quote.";
        }

        @Override
        public String getDocumentationCallsign() {
            return "remove";
        }
    }

    private class DeleteAllCommand implements Command, AuthorityRequiring {

        @Override
        public String getCallsign() {
            return "remove_all";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) {
            // TODO: Quote deletion command (Implement Documented afterwards)
            event.getChannel().sendMessage("Remove all command issued, but its not yet supported.").queue();
        }

        @Override
        public Authority[] getRequiredAuthority() {
            return new Authority[] { new UseQuotes() };
        }

        @Override
        public Module getParentModule() {
            return Quotes.this;
        }
    }

    private class RandomCommand implements Command, AuthorityRequiring, Documented {

        @Override
        public String getCallsign() {
            return "random";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {

            Table table = getQuoteTable(event.getGuild());
            ResultSet resultSet = table.select().column("quote").orderBy("RANDOM()").limit(1).execute();

            if (resultSet.next()) {
                event.getChannel().sendMessage("\"" + resultSet.getString("quote") + "\"").queue();
            } else {
                event.getChannel().sendMessage("This server doesn't have any quotes.").queue();
            }
        }

        @Override
        public Authority[] getRequiredAuthority() {
            return new Authority[] { new UseQuotes() };
        }

        @Override
        public Module getParentModule() {
            return Quotes.this;
        }

        @Override
        public String getDocumentation() {
            return "Fetches a random quote from the sea of quotes.";
        }

        @Override
        public String getDocumentationCallsign() {
            return "random";
        }
    }

    private class InfoCommand implements Command, AuthorityRequiring, Documented {

        @Override
        public String getCallsign() {
            return "info";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            if (tokens.size() == 1) {
                Table table = getQuoteTable(event.getGuild());
                ResultSet resultSet = table.select().where("key", "=",tokens.get(0))
                        .execute();

                if (resultSet.next()) {
                    String message = "Quote:\n```\n" + resultSet.getString("quote") + "\n```\n";

                    User owner = event.getJDA().getUserById(resultSet.getString("owner"));
                    if (owner == null) {
                        message += "Owner's account could not be found.\n"
                                 + "Name of the owner during creation of the quote: "
                                 + resultSet.getString("owner_name") + "\n";
                    } else {
                        message += "Owner: " + owner.getName();
                    }

                    event.getChannel().sendMessage(message).queue();
                } else {
                    event.getChannel().sendMessage("Could not find a quote by that key.").queue();
                }

            } else {
                event.getChannel().sendMessage("Wrong number of arguments. Need a quote key to find the quote.")
                        .queue();
            }
        }

        @Override
        public Authority[] getRequiredAuthority() {
            return new Authority[] { new UseQuotes() };
        }

        @Override
        public Module getParentModule() {
            return Quotes.this;
        }

        @Override
        public String getDocumentation() {
            return "Fetches the quote and the owner of it.";
        }

        @Override
        public String getDocumentationCallsign() {
            return "info";
        }
    }

    private class EditCommand implements Command, AuthorityRequiring, Documented {

        @Override
        public String getCallsign() {
            return "edit";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            if (tokens.size() == 2) {
                Table table = getQuoteTable(event.getGuild());

                // First we check if the quote already exists.
                ResultSet resultSet = table.select().where("key", "=",tokens.get(0))
                        .execute();

                if (resultSet.next()) {

                    if (event.getAuthor().getId().equals(resultSet.getString("owner"))
                            || authorityManagement.checkHasAuthority(event.getMember(), new QuoteAdmin())) {

                        table.update()
                                .set("quote", tokens.get(1))
                                .where("key","=", tokens.get(0))
                                .execute();

                        event.getChannel().sendMessage("Quote updated.").queue();

                    } else {
                        event.getChannel().sendMessage(
                                "You must own this quote or be the quote admin to edit it.").queue();
                    }
                } else {
                    event.getChannel().sendMessage("A quote for this key does not exist. "
                            + "You can make a new quote using the quote add command.").queue();

                }

            } else {
                event.getChannel().sendMessage(
                        "Wrong number of arguments. Command should be:\n$>quote edit \"key\" \"new quote\"").queue();
            }
        }

        @Override
        public Authority[] getRequiredAuthority() {
            return new Authority[] { new UseQuotes() };
        }

        @Override
        public Module getParentModule() {
            return Quotes.this;
        }

        @Override
        public String getDocumentation() {
            return "Edit a quote that currently exists.";
        }

        @Override
        public String getDocumentationCallsign() {
            return "edit";
        }
    }

    private class GiveAwayCommand implements Command, AuthorityRequiring, Documented {

        @Override
        public String getCallsign() {
            return "giveaway";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            if (tokens.size() == 2) {
                Table table = getQuoteTable(event.getGuild());

                // First we check if the quote already exists.
                ResultSet resultSet = table.select().where("key", "=",tokens.get(0))
                        .execute();

                if (resultSet.next()) {

                    if (event.getAuthor().getId().equals(resultSet.getString("owner"))
                            || authorityManagement.checkHasAuthority(event.getMember(), new QuoteAdmin())) {

                        Member newOwner = Utils.getMemberFromMessage(event, tokens.get(1));

                        if (newOwner != null) {
                            table.update()
                                    .set("owner", newOwner.getUser().getId())
                                    .where("key", "=", tokens.get(0))
                                    .execute();

                            event.getChannel().sendMessage("Quote owner updated.").queue();
                        } else {
                            event.getChannel().sendMessage(
                                    "Could not find the member you were trying to give the quote to.").queue();
                        }
                    } else {
                        event.getChannel().sendMessage(
                                "You must own this quote or be the quote admin to edit it.").queue();
                    }
                } else {
                    event.getChannel().sendMessage("A quote for this key does not exist. "
                            + "You can make a new quote using the quote add command.").queue();

                }

            } else {
                event.getChannel().sendMessage(
                        "Wrong number of arguments. Command should be:\n"
                           + "$>quote giveaway \"key\" <Other user you wish to give this quote to>").queue();
            }
        }

        @Override
        public Authority[] getRequiredAuthority() {
            return new Authority[] { new UseQuotes() };
        }

        @Override
        public Module getParentModule() {
            return Quotes.this;
        }

        @Override
        public String getDocumentation() {
            return "Set the owner of this quote to another person.";
        }

        @Override
        public String getDocumentationCallsign() {
            return "give away";
        }
    }

    private class GetCommand implements Command, AuthorityRequiring, Documented {

        @Override
        public String getCallsign() {
            return "get";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            Table table = getQuoteTable(event.getGuild());

            if (!tokens.isEmpty()) {
                ResultSet resultSet = table.select().where("key", "=", tokens.get(0))
                        .execute();

                if (resultSet.next()) {
                    String quote = resultSet.getString("quote");

                    event.getChannel().sendMessage("[" + Utils.cleanMessage(event.getAuthor(), quote) + "]")
                            .queue();
                } else {
                    event.getChannel().sendMessage("Could not find a quote by that key.").queue();
                }
            } else {
                event.getChannel().sendMessage("You need to provide a key to find the quote you want.").queue();
            }
        }

        @Override
        public Authority[] getRequiredAuthority() {
            return new Authority[] { new UseQuotes() };
        }

        @Override
        public Module getParentModule() {
            return Quotes.this;
        }

        @Override
        public String getDocumentation() {
            return "Fetches a quote.";
        }

        @Override
        public String getDocumentationCallsign() {
            return "get";
        }
    }

    private class QuoteCommand implements Command, AuthorityRequiring, Documented, CommandSet {

        private CommandHandler commands;

        QuoteCommand(CarlBot carlbot) {
            commands = new CommandHandler(carlbot);

            commands.setSubName(this.getCallsign());
            commands.addCommand(new AddCommand());
            commands.addCommand(new RemoveCommand());
            commands.addCommand(new DeleteAllCommand());
            commands.addCommand(new RandomCommand());
            commands.addCommand(new InfoCommand());
            commands.addCommand(new EditCommand());
            commands.addCommand(new GiveAwayCommand());
            commands.addCommand(new GetCommand());
        }

        @Override
        public String getCallsign() {
            return "quote";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) {
            commands.runCommand(event, rawString, tokens);
        }

        @Override
        public Authority[] getRequiredAuthority() {
            return new Authority[] { new UseQuotes() };
        }

        @Override
        public Module getParentModule() {
            return Quotes.this;
        }

        @Override
        public String getDocumentation() {
            return "This module allows you to add, remove and access quotes";
        }

        @Override
        public String getDocumentationCallsign() {
            return "quote";
        }

        public Collection<Command> getCommands() {
            return commands.getCommands();
        }
    }

    private Table getQuoteTable(Guild guild) throws SQLException {
        Table table = persistence.getGuildTable(guild, this);
        Table quoteTable = new Table(table, "quotes");

        if (!quoteTable.exists()) {
            quoteTable.create();

            quoteTable.alter().add()
                    .pushValue("owner varchar").pushValue("owner_name varchar")
                    .pushValue("key varchar").pushValue("quote varchar")
                    .execute();
        }

        return quoteTable;
    }

    @Override
    public void setup(CarlBot carlbot) {
        // Get the authority module.
        authorityManagement = (AuthorityManagement) carlbot.getModule(AuthorityManagement.class);

        if (authorityManagement == null) {
            logger.error("Authority module is not loaded.");
            carlbot.crash();
        }

        // Get the persistence module.
        persistence = (Persistence) carlbot.getModule(Persistence.class);

        if (persistence == null) {
            logger.error("Persistence module is not loaded.");
            carlbot.crash();
        }
    }

    @Override
    public String getDocumentation() {
        return "This module allows you to add, remove and access quotes";
    }

    @Override
    public String getDocumentationCallsign() {
        return "quote";
    }

    @Override
    public Command[] getCommands(CarlBot carlbot) {
        return new Command[] { new QuoteCommand(carlbot) };
    }
}
