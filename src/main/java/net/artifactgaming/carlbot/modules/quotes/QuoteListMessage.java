package net.artifactgaming.carlbot.modules.quotes;

import net.artifactgaming.carlbot.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Represents a discord message sent by CarlBot in order to display the list of quotes.
 */
class QuoteListMessage {

    /**
     * How many seconds spent idling before this message stops reacting to reactions.
     */
    private static final int secondsToWaitToIdle = 30;

    /**
     * The object that is handling this message.
     */
    private QuoteListMessageReactionListener thisHandler;

    private ArrayList<QuotePage> quotePages;

    private int currentlyShownPageIndex;

    private String messageID;

    /**
     * Used to callback to stop handling reactions when the timer is up.
     */
    private Timer idleTimer;

    QuoteListMessage(List<Quote> quotesList, String messageID, QuoteListMessageReactionListener thisHandler) {

        ///region Local_Function

        Runnable createQuotePagesFromQuoteLists = () -> {
            quotePages = new ArrayList<>();
            quotePages.add(new QuotePage());

            for (int currPageIndex = 0, i = 0; i < quotesList.size(); ++i){
                if (!quotePages.get(currPageIndex).tryAddQuoteToPage(quotesList.get(i))){
                    ++currPageIndex;
                    quotePages.add(new QuotePage());

                    quotePages.get(currPageIndex).tryAddQuoteToPage(quotesList.get(i));
                }
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
        return quotePages.get(currentlyShownPageIndex).getAsReadableDiscordString();
    }

    List<Quote> getCurrentPage(){
        return quotePages.get(currentlyShownPageIndex).quotesInPage;
    }

    List<Quote> getNextPage(){
        ++currentlyShownPageIndex;
        clampCurrentlyShownPageIndexToQuotePageSize();

        resetIdleTimer();

        return quotePages.get(currentlyShownPageIndex).quotesInPage;
    }

    List<Quote> getPreviousPage(){
        --currentlyShownPageIndex;
        clampCurrentlyShownPageIndexToQuotePageSize();

        resetIdleTimer();

        return quotePages.get(currentlyShownPageIndex).quotesInPage;
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
        if (quotePages.size() <= currentlyShownPageIndex){
            currentlyShownPageIndex = 0;
        } else if (currentlyShownPageIndex < 0){
            currentlyShownPageIndex = quotePages.size() - 1;
        }
    }

    static String getQuoteListAsReadableDiscordString(List<Quote> quotePage){
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
