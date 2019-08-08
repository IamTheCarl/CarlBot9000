package net.artifactgaming.carlbot.modules.quotes;

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

    public QuoteListMessage(ArrayList<Quote> quotesList, String messageID) {

        ///region Local_Function

        Runnable createQuotePagesFromQuoteLists = () -> {
            quotesListAsPages = new ArrayList<ArrayList<Quote>>();
            quotesListAsPages.add(new ArrayList<>());

            for (int temp = 0, currPageIndex = 0, i = 0; i < quotesList.size(); ++i, ++temp){
                quotesListAsPages.get(currPageIndex).add(quotesList.get(i));

                if (temp >= maxQuotesPerListCount){
                    temp = 0;
                    ++currPageIndex;
                    quotesListAsPages.add(new ArrayList<>());
                }
            }
        };

        ///endregion

        createQuotePagesFromQuoteLists.run();
        currentlyShownPageIndex = 0;
        this.messageID = messageID;
    }

    public String getMessageID() {
        return messageID;
    }

    public List<Quote> getCurrentPage(){
        return quotesListAsPages.get(currentlyShownPageIndex);
    }

    public List<Quote> getNextPage(){
        ++currentlyShownPageIndex;
        clampCurrentlyShownPageIndexToQuotePageSize();

        return quotesListAsPages.get(currentlyShownPageIndex);
    }

    public List<Quote> getPreviousPage(){
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
}
