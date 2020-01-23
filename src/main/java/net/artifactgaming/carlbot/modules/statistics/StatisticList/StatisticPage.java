package net.artifactgaming.carlbot.modules.statistics.StatisticList;

import java.security.InvalidParameterException;

public class StatisticPage {
    /**
     * Max number of characters allowed in a page
     */
    public static final int maxCharacterCountInPage = 1500;

    private String pageOutput;

    public StatisticPage(String pageContent){
        // TODO: Possible refector?
        if (pageContent.length() >= maxCharacterCountInPage){
            throw new InvalidParameterException("pageContent should be no more than " + maxCharacterCountInPage + " length in characters!");
        }

        pageOutput = pageContent;
    }

    public String getPageContent(){
        return pageOutput;
    }
}
