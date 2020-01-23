package net.artifactgaming.carlbot.modules.quotes;


import net.artifactgaming.carlbot.*;
import net.artifactgaming.carlbot.Module;
import net.artifactgaming.carlbot.modules.authority.Authority;
import net.artifactgaming.carlbot.modules.authority.AuthorityManagement;
import net.artifactgaming.carlbot.modules.authority.AuthorityRequiring;
import net.artifactgaming.carlbot.modules.persistence.Persistence;
import net.artifactgaming.carlbot.modules.persistence.PersistentModule;
import net.artifactgaming.carlbot.modules.persistence.Table;
import net.artifactgaming.carlbot.modules.schedule.SchedulableCommand;
import net.artifactgaming.carlbot.modules.selfdocumentation.Documented;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.sf.json.*;

public class Quotes implements Module, AuthorityRequiring, PersistentModule, Documented {

    ///region SQL_TableNames
    /**
     * Used in the SQL Data table to represent the column name to store owner ID.
     */
    private static final String OWNER_ID = "owner";

    /**
     * Used in the SQL Data table to represent the column name to store the owner name.
     */
    private static final String OWNER_NAME = "owner_name";

    /**
     * Used in the SQL Data table to represent the column name to store the key of the quote.
     */
    private static final String QUOTE_KEY = "key";

    /**
     * Used in the SQL Data table to represent the column name to store the key of the quote.
     */
    private static final String QUOTE_CONTENT = "quote";

    ///endregion

    private AuthorityManagement authorityManagement;
    private Persistence persistence;

    private Logger logger = LoggerFactory.getLogger(Quotes.class);

    @Override
    public Authority[] getRequiredAuthority() {
        return new Authority[] { new QuoteAdmin(), new UseQuotes() };
    }

    private QuoteListMessageReactionListener quoteListMessageReactionListener;

    /**
     * Use "updateGuildTableWithQuoteByQuoteKey" instead.
     */
    @Deprecated
    private void replaceQuoteOnGuild(Guild guild, Quote quote) throws SQLException{
        ObjectResult<Quote> fetchOriginalQuoteResult = tryFetchQuoteFromGuildTableByQuoteKey(guild, quote.getKey());

        if (fetchOriginalQuoteResult.getResult()){
            Quote originalQuote = fetchOriginalQuoteResult.getObject();
            deleteQuoteFromGuildTableByQuoteKey(guild, originalQuote.getKey());
        }

        addQuoteToGuildTable(guild, quote);
    }

    private List<Quote> getAllQuotesFromGuild(Guild guild) throws SQLException {
        Table table = getQuoteTable(guild);
        ResultSet resultSet = table.select().execute();

        ArrayList<Quote> result = new ArrayList<>();
        while (resultSet.next()){
             result.add(new Quote(
                    resultSet.getString(OWNER_ID),
                    resultSet.getString(OWNER_NAME),
                    resultSet.getString(QUOTE_KEY),
                    resultSet.getString(QUOTE_CONTENT)
             ));
        }

        return result;
    }


    private void updateGuildTableWithQuoteByQuoteKey(Guild guild, Quote quote, String quoteKey) throws SQLException {
        Table table = getQuoteTable(guild);

        table.update()
                .set(OWNER_ID, quote.getOwnerID())
                .set(OWNER_NAME, quote.getOwnerName())
                .set(QUOTE_KEY, quote.getKey())
                .set(QUOTE_CONTENT, quote.getContent())
                .where(QUOTE_KEY, "=", quoteKey)
                .execute();
    }

    private ObjectResult<Quote> tryFetchQuoteFromGuildTableByQuoteKey(Guild guild, String quoteKey) throws SQLException {
        Table table = getQuoteTable(guild);

        ResultSet resultSet = table.select().where(QUOTE_KEY, "=", quoteKey).execute();

        if (resultSet.next()){
            Quote foundQuote = new Quote(
                    resultSet.getString(OWNER_ID),
                    resultSet.getString(OWNER_NAME),
                    resultSet.getString(QUOTE_KEY),
                    resultSet.getString(QUOTE_CONTENT)
            );

            return new ObjectResult<>(foundQuote);
        } else {
            return new ObjectResult<>(null);
        }
    }

    private void deleteQuoteFromGuildTableByQuoteKey(Guild guild, String quoteKey) throws SQLException {
        Table table = getQuoteTable(guild);

        table.delete()
                .where(QUOTE_KEY, "=", quoteKey)
                .execute();
    }

    private void addQuoteToGuildTable(Guild guild, Quote quoteToAdd) throws SQLException {
        Table table = getQuoteTable(guild);

        table.insert().set(OWNER_ID, quoteToAdd.getOwnerID())
                .set(OWNER_NAME, quoteToAdd.getOwnerName())
                .set(QUOTE_KEY, quoteToAdd.getKey())
                .set(QUOTE_CONTENT, quoteToAdd.getContent()).execute();
    }

    private boolean quoteKeyExistsOnGuildTable(Guild guild, String quoteKey) throws SQLException {
        Table table = getQuoteTable(guild);

        ResultSet resultSet = table.select().where(QUOTE_KEY, "=", quoteKey).execute();

        boolean quoteExists = false;

        if (resultSet.next()){
            quoteExists = true;
        }

        resultSet.close();

        return quoteExists;
    }

    private class ImportOldQuotesCommand implements Command, Documented, AuthorityRequiring {

        @Override
        public String getCallsign() {
            return "importNitori";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            ///region Local_Function

            BooleanSupplier messageFirstAttachmentIsText = () -> {
                Message.Attachment firstAttachment = event.getMessage().getAttachments().get(0);

                return firstAttachment.getUrl().endsWith(".txt");
            };

            BooleanSupplier messageHasNoTextAttachment = () -> {
                if (event.getMessage().getAttachments().size() == 0){
                    return true;
                } else return !messageFirstAttachmentIsText.getAsBoolean();
            };

            ///endregion

            if (messageHasNoTextAttachment.getAsBoolean()){
                event.getChannel().sendMessage("Please send a text file full of NitroBot quotes to import into this guild." + Utils.NEWLINE +  "(.txt extension)").queue();
                return;
            }

            Message importingMessage = event.getChannel().sendMessage("Importing quotes...").complete();
            ObjectResult<List<Quote>> getQuotesObjectResult = tryGetQuotesFromAttachment(event.getMessage().getAttachments().get(0));

            if (getQuotesObjectResult.getResult()){
                List<Quote> quotes = getQuotesObjectResult.getObject();

                // TODO: Allow the user to give an input to override all the quotes.
                addQuotesToGuild(event.getGuild(),quotes, false);

                importingMessage.editMessage("Imported " + quotes.size() + " quotes!").queue();
            } else {
                importingMessage.editMessage(getQuotesObjectResult.getResultMessage()).queue();
            }

        }

        private void addQuotesToGuild(Guild guild, List<Quote> quotes, boolean overrideQuote) throws SQLException {
            for (Quote quote : quotes){
                boolean quoteWithKeyExists = quoteKeyExistsOnGuildTable(guild, quote.getKey());

                if (!quoteWithKeyExists) {
                    addQuoteToGuildTable(guild, quote);
                } else if (overrideQuote){
                    updateGuildTableWithQuoteByQuoteKey(guild, quote, quote.getKey());
                }
            }
        }

        private ObjectResult<List<Quote>> tryGetQuotesFromAttachment(Message.Attachment attachment) throws Exception{
            ///region Local_Function

            Predicate<String> isEmptyOrWhitespaceOrNull = (testString) -> {
                if (testString == null) {
                    return true;
                }

                return testString.isEmpty() || testString.trim().isEmpty();
            };

            Predicate<String> isRemovedQuote = (stringLine) -> stringLine.matches("Quote \\d+ no longer exists(.*)");

            Predicate<String> notPartOfQuoteOrRemovedQuote = (stringLine) -> {
                if (isRemovedQuote.test(stringLine)){
                    return true;
                }

                // Informative stuff.
                if (stringLine.matches("^There are \\d+ quotes on this server with \\d+ removed quotes(.*)")){
                    return true;
                }

                return isEmptyOrWhitespaceOrNull.test(stringLine);
            };

            ///endregion

            ArrayList<Quote> quotes = new ArrayList<>();
            InputStream inputStream =  attachment.getInputStream();

            try (BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                Quote currentReadingQuote = new Quote();
                boolean fetchingOwnerData = false;

                String inputStr;
                while ((inputStr = streamReader.readLine()) != null) {
                    if (notPartOfQuoteOrRemovedQuote.test(inputStr)){
                        currentReadingQuote = new Quote();
                        fetchingOwnerData = false;
                        continue;
                    }

                    if (fetchingOwnerData){
                        // We do not need the creation date and the channel which this quote is created at.
                        String ownerDataString = inputStr
                                .replaceAll("Created \\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2} by ", Utils.STRING_EMPTY)
                                .replaceAll("in <#\\d+>", Utils.STRING_EMPTY);

                        ///region Setting_Owner_Name

                        String ownerName = ownerDataString.replaceAll(" (<@\\d+>) ", Utils.STRING_EMPTY);
                        // Ignore this quote if it does not have an owner name
                        if (isEmptyOrWhitespaceOrNull.test(ownerName)){
                            currentReadingQuote = new Quote();
                            fetchingOwnerData = false;
                            continue;
                        }
                        currentReadingQuote.setOwnerName(ownerName);

                        ///endregion

                        ///region Setting_Owner_ID

                        String ownerID = ownerDataString
                                .replaceAll("(.+<@)", Utils.STRING_EMPTY)
                                .replaceAll("> ", Utils.STRING_EMPTY);

                        // Ignore this quote if it does not have an owner ID
                        if (isEmptyOrWhitespaceOrNull.test(ownerID)){
                            currentReadingQuote = new Quote();
                            fetchingOwnerData = false;
                            continue;
                        }
                        currentReadingQuote.setOwnerID(ownerID);

                        ///endregion

                        quotes.add(currentReadingQuote);
                        currentReadingQuote = new Quote();
                        fetchingOwnerData = false;
                    } else {
                        currentReadingQuote = new Quote();

                        String quoteContent = inputStr.replaceAll("Quote \\d+: ", Utils.STRING_EMPTY);
                        currentReadingQuote.setContent(quoteContent);

                        String quoteKey = inputStr
                                .replaceAll("Quote ", Utils.STRING_EMPTY)
                                .replaceAll(": (.*)", Utils.STRING_EMPTY);

                        currentReadingQuote.setKey(quoteKey);

                        fetchingOwnerData = true;
                    }
                }

            } catch (Exception e) {
                return new ObjectResult<>(null, "File is either invalid or formatted wrongly.");
            }

            return new ObjectResult<>(quotes);
        }

        @Override
        public Module getParentModule() {
            return Quotes.this;
        }

        @Override
        public String getDocumentation() {
            return "Use this command to import old quotes. (NitroBot format)";
        }

        @Override
        public String getDocumentationCallsign() {
            return "importNitori";
        }

        @Override
        public Authority[] getRequiredAuthority() {
            return new Authority[] { new QuoteAdmin() };
        }
    }

    private class ImportCommand implements Command, AuthorityRequiring, Documented {
        @Override
        public String getCallsign() {
            return "import";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            ///region Local_Function

            BooleanSupplier messageFirstAttachmentIsJSON = () -> {
                Message.Attachment firstAttachment = event.getMessage().getAttachments().get(0);

                return firstAttachment.getUrl().endsWith(".json");
            };

            BooleanSupplier messageHasNoJsonAttachment = () -> {
                if (event.getMessage().getAttachments().size() == 0){
                    return true;
                } else return !messageFirstAttachmentIsJSON.getAsBoolean();
            };

            ///endregion

            if (messageHasNoJsonAttachment.getAsBoolean()){
                event.getChannel().sendMessage("Please send a JSON file full of quotes to import into this guild.").queue();
                return;
            }

            Message importingMessage = event.getChannel().sendMessage("Importing quotes...").complete();

            ObjectResult<List<Quote>> getQuotesObjectResult = tryGetQuotesFromAttachment(event.getMessage().getAttachments().get(0));

            if (getQuotesObjectResult.getResult()){
                List<Quote> quotes = getQuotesObjectResult.getObject();

                // TODO: Allow the user to give an input to override all the quotes.
                addQuotesToGuild(event.getGuild(),quotes, false);

                importingMessage.editMessage("Imported " + quotes.size() + " quotes!").queue();
            } else {
                importingMessage.editMessage(getQuotesObjectResult.getResultMessage()).queue();
            }
        }

        private void addQuotesToGuild(Guild guild, List<Quote> quotes, boolean overrideQuote) throws SQLException {
            for (Quote quote : quotes){
                boolean quoteWithKeyExists = quoteKeyExistsOnGuildTable(guild, quote.getKey());

                if (!quoteWithKeyExists) {
                    addQuoteToGuildTable(guild, quote);
                } else if (overrideQuote){
                    updateGuildTableWithQuoteByQuoteKey(guild, quote, quote.getKey());
                }
            }
        }

        private ObjectResult<List<Quote>> tryGetQuotesFromAttachment(Message.Attachment attachment) throws Exception {

            InputStream jsonAsInputString =  attachment.getInputStream();

            JSONArray quoteJsonArray;
            try (BufferedReader streamReader = new BufferedReader(new InputStreamReader(jsonAsInputString, StandardCharsets.UTF_8))) {
                String inputStr;
                StringBuilder responseStrBuilder = new StringBuilder();

                while ((inputStr = streamReader.readLine()) != null) {
                    responseStrBuilder.append(inputStr);
                }

                quoteJsonArray = JSONArray.fromObject(responseStrBuilder.toString());
            } catch (Exception e) {
                return new ObjectResult<List<Quote>>(null, "File is not JSON formatted.");
            }

            return new ObjectResult<List<Quote>>(jsonArrayToQuotesList(quoteJsonArray));
        }

        private List<Quote> jsonArrayToQuotesList(JSONArray jsonArray){
            ArrayList<Quote> quotes = new ArrayList<Quote>();

            for(Object o: jsonArray){
                if ( o instanceof JSONObject ) {
                    quotes.add(Quote.toQuoteObject((JSONObject) o));
                }
            }

            return quotes;
        }

        @Override
        public Authority[] getRequiredAuthority() {
            return new Authority[] { new QuoteAdmin() };
        }

        @Override
        public Module getParentModule() {
            return Quotes.this;
        }

        @Override
        public String getDocumentation() {
            return "import all the quotes into this guild from a JSON file.";
        }

        @Override
        public String getDocumentationCallsign() {
            return "import";
        }

    }

    private class ExportCommand implements Command, AuthorityRequiring, Documented {
        @Override
        public String getCallsign() {
            return "export";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {

            List<Quote> quotesList = getAllQuotesFromGuild(event.getGuild());

            if (quotesList.size() != 0){
                Message exportingQuoteMessage = event.getChannel().sendMessage("Exporting quotes, please wait...").complete();

                JSONArray quoteListAsJSON = new JSONArray();

                for (Quote quote : quotesList){
                    quoteListAsJSON.add(quote.toJsonObject());
                }

                String jsonFilePathName = "./" + event.getGuild().getId() + "_quotes.json";

                File exportedJsonFile = new File(jsonFilePathName);

                if (exportedJsonFile.createNewFile()){

                    Files.write(Paths.get(jsonFilePathName), quoteListAsJSON.toString().getBytes());

                    event.getChannel().sendFile(exportedJsonFile).queue();
                    exportingQuoteMessage.delete().queue();

                    exportedJsonFile.delete();
                } else {
                    exportingQuoteMessage.editMessage("ERROR: Already exporting quote!").queue();
                }
            } else {
                event.getChannel().sendMessage(
                        "There are no quotes in this guild!").queue();
            }
        }

        @Override
        public Authority[] getRequiredAuthority() {
            return new Authority[] { new QuoteAdmin() };
        }

        @Override
        public Module getParentModule() {
            return Quotes.this;
        }

        @Override
        public String getDocumentation() {
            return "Export all the quotes in this guild as a JSON file.";
        }

        @Override
        public String getDocumentationCallsign() {
            return "export";
        }

    }

    private class ListCommand implements Command, AuthorityRequiring, Documented {

        @Override
        public String getCallsign() {
            return "list";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {

            List<Quote> quotesList = getAllQuotesFromGuild(event.getGuild());

            Message quoteMessage = event.getChannel().sendMessage(
                    "Fetching quotes...").complete();

            if (quotesList.size() > QuotePage.maxQuotesPerListCount){

                QuoteListMessage quoteListMessage = new QuoteListMessage(quotesList, quoteMessage.getId(), quoteListMessageReactionListener);

                quoteListMessageReactionListener.addQuoteListMessageToListener(quoteListMessage);

                quoteMessage.editMessage("```" + quoteListMessage.getCurrentPageAsReadableDiscordString() + "```").queue();

                quoteMessage.addReaction( QuoteListMessageReactionListener.PREVIOUS_EMOTE_NAME).complete();
                quoteMessage.addReaction( QuoteListMessageReactionListener.NEXT_EMOTE_NAME).queue();
            } else if (quotesList.size() > 0) {
                quoteMessage.editMessage("```" + QuoteListMessage.getQuoteListAsReadableDiscordString(quotesList) + "```").queue();
            } else {
                quoteMessage.editMessage(
                        "There are no quotes in this guild!").queue();
            }
        }

        @Override
        public Module getParentModule() {
            return Quotes.this;
        }

        @Override
        public String getDocumentation() {
            return "Lists all the quotes in this guild.";
        }

        @Override
        public String getDocumentationCallsign() {
            return "list";
        }

        @Override
        public Authority[] getRequiredAuthority() {
            return new Authority[] { new UseQuotes() };
        }
    }

    private class AddCommand implements Command, AuthorityRequiring, Documented {

        @Override
        public String getCallsign() {
            return "add";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {

            ///region Local_Function

            Supplier<Quote> getQuoteDataFromMessage = () -> {
                User author = event.getMessage().getAuthor();
                return new Quote(author.getId(), author.getName(), tokens.get(0), Utils.makeStringSQLFriendly(tokens.get(1)));
            };

            ///endregion

            if (tokens.size() == 2) {
                Guild guild = event.getGuild();

                if (quoteKeyExistsOnGuildTable(guild, tokens.get(0))) {
                    event.getChannel().sendMessage("A quote already exists for this key. "
                            + "You can edit or remove the quote if you are the owner.").queue();
                } else {
                    Quote quoteToAdd = getQuoteDataFromMessage.get();

                    addQuoteToGuildTable(event.getGuild(), quoteToAdd);

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

                Guild guild = event.getGuild();

                ObjectResult<Quote> fetchingQuoteResult = tryFetchQuoteFromGuildTableByQuoteKey(guild, tokens.get(0));

                if (fetchingQuoteResult.getResult()) {

                    Quote quoteToDelete = fetchingQuoteResult.getObject();

                    // If user has permission to delete quotes
                    if (hasAuthorityOverQuote(event.getMember(), quoteToDelete)) {

                        deleteQuoteFromGuildTableByQuoteKey(guild, quoteToDelete.getKey());

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

    private class DeleteAllCommand implements Command, AuthorityRequiring, Documented {

        @Override
        public String getCallsign() {
            return "deleteAll";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            // TODO: Actual quote deletion command
            Table table = getQuoteTable(event.getGuild());

            table.delete().execute();

            event.getChannel().sendMessage("All quotes deleted!").queue();
        }

        @Override
        public Authority[] getRequiredAuthority() {
            return new Authority[] { new QuoteAdmin() };
        }

        @Override
        public Module getParentModule() {
            return Quotes.this;
        }

        @Override
        public String getDocumentation() {
            return "Deletes all quotes from this server. (WARN: This action is irreversible and mostly for test purposes!)";
        }

        @Override
        public String getDocumentationCallsign() {
            return "deleteAll";
        }
    }

    private class RandomCommand implements Command, AuthorityRequiring, Documented {

        @Override
        public String getCallsign() {
            return "random";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {

            ///region Local_Function

            Function<List<String>, String> pickQuoteFromQuotesList = (quotesList) -> {
                Random rand = new Random();

                int randIndex = rand.nextInt(quotesList.size());
                return quotesList.get(randIndex);
            };
            ///endregion


            List<String> quotesToChooseFrom = fetchAllQuotesContentFromGuild(event.getGuild());

            if (quotesToChooseFrom.size() > 0) {

                String quoteToShow = pickQuoteFromQuotesList.apply(quotesToChooseFrom);

                event.getChannel().sendMessage("\"" + quoteToShow + "\"").queue();
            } else {
                event.getChannel().sendMessage("This server doesn't have any quotes.").queue();
            }
        }

        private List<String> fetchAllQuotesContentFromGuild(Guild guild) throws SQLException {
            Table table = getQuoteTable(guild);
            ResultSet resultSet = table.select().execute();

            List<String> allQuotesContent = new ArrayList<String>();

            while (resultSet.next()){
                String quote = resultSet.getString(QUOTE_CONTENT);
                allQuotesContent.add(quote);
            }

            return allQuotesContent;
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

                ObjectResult<Quote> fetchQuoteResult = tryFetchQuoteFromGuildTableByQuoteKey(event.getGuild(), tokens.get(0));

                if (fetchQuoteResult.getResult()) {
                    Quote quoteInfoToShow = fetchQuoteResult.getObject();
                    String message = "Quote:\n```\n" + quoteInfoToShow.getContent() + "\n```\n";

                    boolean quoteUpdated = false;

                    User owner = event.getJDA().getUserById(quoteInfoToShow.getOwnerID());
                    if (owner == null) {
                        message += "Owner's account could not be found.\n"
                                 + "Name of the owner during creation of the quote: "
                                 + quoteInfoToShow.getOwnerName() + "\n";
                    } else if (!owner.getName().equals(quoteInfoToShow.getOwnerName())){
                        quoteUpdated = true;
                        quoteInfoToShow.setOwnerName(owner.getName());
                        message += "Owner: " + owner.getName();
                    } else {
                        message += "Owner: " + owner.getName();
                    }

                    event.getChannel().sendMessage(message).queue();

                    updateGuildTableWithQuoteByQuoteKey(event.getGuild(), quoteInfoToShow, quoteInfoToShow.getKey());
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

    private class RenameCommand implements Command, AuthorityRequiring, Documented {
        @Override
        public String getCallsign() {
            return "rename";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            if (tokens.size() == 2) {
                // Exit if another quote with the name to replace to exists.
                if (quoteKeyExistsOnGuildTable(event.getGuild(), tokens.get(1))){
                    event.getChannel().sendMessage("A quote with the new key name given already exists!").queue();
                    return;
                }

                ObjectResult<Quote> fetchQuoteResult = tryFetchQuoteFromGuildTableByQuoteKey(event.getGuild(), tokens.get(0));

                if (fetchQuoteResult.getResult()) {

                    Quote quoteToEdit = fetchQuoteResult.getObject();

                    if (hasAuthorityOverQuote(event.getMember(), quoteToEdit)) {
                        String messageToSend = Utils.STRING_EMPTY;

                        quoteToEdit.setKey(tokens.get(1));

                        updateGuildTableWithQuoteByQuoteKey(event.getGuild(), quoteToEdit, tokens.get(0));

                        messageToSend += "\n\n Quote Name Updated to be " + tokens.get(1);
                        event.getChannel().sendMessage(messageToSend).queue();

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
                        "Wrong number of arguments. Command should be:\n$>quote rename \"key\" \"new quote name\"").queue();
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
            return "Edit the name of a quote that currently exists.";
        }

        @Override
        public String getDocumentationCallsign() {
            return "rename";
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

                ObjectResult<Quote> fetchedQuoteResult = tryFetchQuoteFromGuildTableByQuoteKey(event.getGuild(), tokens.get(0));

                if (fetchedQuoteResult.getResult()) {

                    Quote quoteToEdit = fetchedQuoteResult.getObject();

                    if (hasAuthorityOverQuote(event.getMember(), quoteToEdit)) {
                        quoteToEdit.setContent(tokens.get(1));
                        updateGuildTableWithQuoteByQuoteKey(event.getGuild(), quoteToEdit, tokens.get(0));

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
                        "Wrong number of arguments. Command should be:\n$>quote edit \"key\" \"new quote content\"").queue();
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
            return "Edit the content of a quote that currently exists.";
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
                ObjectResult<Quote> fetchQuoteResult = tryFetchQuoteFromGuildTableByQuoteKey(event.getGuild(), tokens.get(0));

                if (fetchQuoteResult.getResult()) {

                    Quote quoteToEdit = fetchQuoteResult.getObject();

                    if (hasAuthorityOverQuote(event.getMember(), quoteToEdit)) {

                        Member newOwner = Utils.getMemberFromMessage(event, tokens.get(1));
                        if (newOwner != null) {

                            quoteToEdit.setOwnerID(newOwner.getUser().getId());
                            quoteToEdit.setOwnerName(newOwner.getUser().getName());
                            updateGuildTableWithQuoteByQuoteKey(event.getGuild(), quoteToEdit, tokens.get(0));
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
            return "giveaway";
        }
    }

    private class GetCommand implements Command, AuthorityRequiring, Documented, SchedulableCommand {

        @Override
        public String getCallsign() {
            return "get";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            FindQuoteByTokensAndSendToTextChannel(event.getTextChannel(), tokens);
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

        @Override
        public void invokeCommandAsSchedulable(TextChannel channel, List<String> tokens) {
            try {
                FindQuoteByTokensAndSendToTextChannel(channel, tokens);
            } catch (SQLException e){
                logger.error("Failed fetch quotes from guild," + channel.getGuild().getName() + ", error message: " + e.getMessage());
                channel.sendMessage("Error fetching quote from the server!").queue();
            }
        }

        private void FindQuoteByTokensAndSendToTextChannel(TextChannel channel, List<String> tokens) throws SQLException {
            Table table = getQuoteTable(channel.getGuild());

            if (!tokens.isEmpty()) {
                ObjectResult<Quote> fetchQuoteResult = tryFetchQuoteFromGuildTableByQuoteKey(channel.getGuild(), tokens.get(0));
                if (fetchQuoteResult.getResult()) {
                    Quote quoteToSend = fetchQuoteResult.getObject();

                    channel.sendMessage("[" + Utils.cleanMessage(quoteToSend.getContent()) + "]")
                            .queue();
                } else {
                    channel.sendMessage("Could not find a quote by that key.").queue();
                }
            } else {
                channel.sendMessage("You need to provide a key to find the quote you want.").queue();
            }

        }
    }

    private class QuoteCommand implements Command, AuthorityRequiring, Documented, CommandSet, SchedulableCommand {

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
            commands.addCommand(new RenameCommand());
            commands.addCommand(new GiveAwayCommand());
            commands.addCommand(new GetCommand());
            commands.addCommand(new ListCommand());

            commands.addCommand(new ExportCommand());
            commands.addCommand(new ImportCommand());
            commands.addCommand(new ImportOldQuotesCommand());
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

        @Override
        public void invokeCommandAsSchedulable(TextChannel channel, List<String> tokens) {
            commands.invokeCommandAsSchedulable(channel, tokens);
        }
    }

    private boolean hasAuthorityOverQuote(Member member, Quote quote) throws SQLException {
        return member.getUser().getId().equals(quote.getOwnerID()) || authorityManagement.checkHasAuthority(member, new QuoteAdmin());
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

        quoteListMessageReactionListener = new QuoteListMessageReactionListener();

        carlbot.addOnMessageReactionListener(quoteListMessageReactionListener);
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
