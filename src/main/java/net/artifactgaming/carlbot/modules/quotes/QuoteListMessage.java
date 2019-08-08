package net.artifactgaming.carlbot.modules.quotes;

import net.artifactgaming.carlbot.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a discord message sent by CarlBot in order to display the list of quotes.
 */
public class QuoteListMessage {

    /**
     * How many quotes it can show in a 'page'
     */
    private static final int maxQuotesPerListCount = 5;

    private ArrayList<ArrayList<Quote>> quotesListAsPages;

    private int currentlyShownPageIndex;

    private String messageID;

    QuoteListMessage(List<Quote> quotesList, String messageID) {

        ///region Local_Function

        Runnable createQuotePagesFromQuoteLists = () -> {
            quotesListAsPages = new ArrayList<ArrayList<Quote>>();
            quotesListAsPages.add(new ArrayList<>());

            for (int temp = 0, currPageIndex = 0, i = 0; i < quotesList.size(); ++i, ++temp){
                if (temp >= maxQuotesPerListCount){
                    temp = 0;
                    ++currPageIndex;
                    quotesListAsPages.add(new ArrayList<>());
                }

                quotesListAsPages.get(currPageIndex).add(quotesList.get(i));
            }
        };

        ///endregion

        createQuotePagesFromQuoteLists.run();
        currentlyShownPageIndex = 0;
        this.messageID = messageID;
    }

    String getMessageID() {
        return messageID;
    }

    String getCurrentPageAsReadableDiscordString(){
        return getQuotePageAsReadableDiscordString(quotesListAsPages.get(currentlyShownPageIndex));
    }

    List<Quote> getCurrentPage(){
        return quotesListAsPages.get(currentlyShownPageIndex);
    }

    List<Quote> getNextPage(){
        ++currentlyShownPageIndex;
        clampCurrentlyShownPageIndexToQuotePageSize();

        return quotesListAsPages.get(currentlyShownPageIndex);
    }

    List<Quote> getPreviousPage(){
        --currentlyShownPageIndex;
        clampCurrentlyShownPageIndexToQuotePageSize();

        return quotesListAsPages.get(currentlyShownPageIndex);
    }

    private void clampCurrentlyShownPageIndexToQuotePageSize(){
        if (quotesListAsPages.size() <= currentlyShownPageIndex){
            currentlyShownPageIndex = 0;
        } else if (currentlyShownPageIndex < 0){
            currentlyShownPageIndex = quotesListAsPages.size() - 1;
        }
    }

    static String getQuotePageAsReadableDiscordString(List<Quote> quotePage){
        StringBuilder readableStr = new StringBuilder(Utils.STRING_EMPTY);

        for (Quote quote : quotePage) {
            // QuoteKey:: content of quote
            // owner_id, aka Name of owner

            // TODO: Formatting
            readableStr.append(quote.getKey()).append(":: ").append(quote.getContent()).append(Utils.NEWLINE);
            readableStr.append(quote.getOwnerID()).append(", aka ").append(quote.getOwnerName()).append(Utils.NEWLINE);
            readableStr.append(Utils.NEWLINE);
        }

        return readableStr.toString();
    }
}
