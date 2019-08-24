package net.artifactgaming.carlbot.modules.quotes;

import net.artifactgaming.carlbot.Utils;

import java.util.ArrayList;
import java.util.List;

class QuotePage {

    /**
     * How many quotes it can show in a page
     */
    static final int maxQuotesPerListCount = 15;

    /**
     * Max number of characters allowed in a page
     */
    private static final int maxCharacterCountInPage = 2000;

    ArrayList<Quote> quotesInPage;

    private int currentCharacterCount;

    QuotePage(){
        quotesInPage = new ArrayList<>();
        currentCharacterCount = 0;
    }

    /**
     * Try to add a quote to this page without exceeding the character count limit or quotes limit.
     * @param quoteToAdd The quote to add to this page.
     * @return True if the quote was added. (Aka adding this quote did not exceed the character count limit or quotes limit)
     */
    boolean tryAddQuoteToPage(Quote quoteToAdd){
        int quoteCharCount = getTotalCharacterCountFromQuote(quoteToAdd);

        if (quoteCharCount + currentCharacterCount >= maxCharacterCountInPage || quotesInPage.size() + 1 > maxQuotesPerListCount){
            return false;
        } else {
            currentCharacterCount += quoteCharCount;
            quotesInPage.add(quoteToAdd);
            return true;
        }
    }

    private static int getTotalCharacterCountFromQuote(Quote quote){
        StringBuilder readableStr = new StringBuilder(Utils.STRING_EMPTY);
        readableStr.append("KEY: "+ quote.getKey()).append(", CONTENT: ").append(quote.getContent()).append(Utils.NEWLINE);
        readableStr.append("OWNER_ID: "+ quote.getOwnerID()).append(", OWNER_NAME: ").append(quote.getOwnerName()).append(Utils.NEWLINE);
        readableStr.append(Utils.NEWLINE);

        return readableStr.length();
    }

    String getAsReadableDiscordString(){
        StringBuilder readableStr = new StringBuilder(Utils.STRING_EMPTY);

        for (Quote quote : quotesInPage) {
            // TODO: Formatting
            readableStr.append("KEY: "+ quote.getKey()).append(", CONTENT: ").append(quote.getContent()).append(Utils.NEWLINE);
            readableStr.append("OWNER_ID: "+ quote.getOwnerID()).append(", OWNER_NAME: ").append(quote.getOwnerName()).append(Utils.NEWLINE);
            readableStr.append(Utils.NEWLINE);
        }

        return readableStr.toString();
    }
}
