package net.artifactgaming.carlbot.modules.quotes;

import net.artifactgaming.carlbot.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Represents a discord message sent by CarlBot in order to display the list of quotes.
 */
public class QuoteListMessage {

    /**
     * How many quotes it can show in a 'page'
     */
    private static final int maxQuotesPerListCount = 5;

    /**
     * How many seconds spent idling before this message stops reacting to reactions.
     */
    private static final int secondsToWaitToIdle = 30;

    /**
     * The object that is handling this message.
     */
    private QuoteListMessageReactionListener thisHandler;

    private ArrayList<ArrayList<Quote>> quotesListAsPages;

    private int currentlyShownPageIndex;

    private String messageID;

    /**
     * Used to callback to stop handling reactions when the timer is up.
     */
    private Timer idleTimer;

    QuoteListMessage(List<Quote> quotesList, String messageID, QuoteListMessageReactionListener thisHandler) {

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

        this.thisHandler = thisHandler;

        setupInactiveTimer();
    }

    private void setupInactiveTimer(){
        idleTimer = new Timer();

        long delayAndPeriod = secondsToWaitToIdle * 1000;

        idleTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                thisHandler.handleMessageLeftIdle(QuoteListMessage.this);
            }
        }, delayAndPeriod);
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

        resetIdleTimer();

        return quotesListAsPages.get(currentlyShownPageIndex);
    }

    List<Quote> getPreviousPage(){
        --currentlyShownPageIndex;
        clampCurrentlyShownPageIndexToQuotePageSize();

        resetIdleTimer();

        return quotesListAsPages.get(currentlyShownPageIndex);
    }

    void cancelAndPurgeIdleTimer(){
        idleTimer.cancel();
        idleTimer.purge();
    }

    private void resetIdleTimer(){
        cancelAndPurgeIdleTimer();
        setupInactiveTimer();
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
            // TODO: Formatting
            readableStr.append("KEY: "+ quote.getKey()).append(", CONTENT: ").append(quote.getContent()).append(Utils.NEWLINE);
            readableStr.append("OWNER_ID: "+ quote.getOwnerID()).append(", OWNER_NAME: ").append(quote.getOwnerName()).append(Utils.NEWLINE);
            readableStr.append(Utils.NEWLINE);
        }

        return readableStr.toString();
    }
}
