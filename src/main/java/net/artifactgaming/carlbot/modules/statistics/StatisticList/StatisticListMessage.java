package net.artifactgaming.carlbot.modules.statistics.StatisticList;

import net.artifactgaming.carlbot.modules.statistics.ChannelStatistic.LifetimeChannelStatistics;
import net.artifactgaming.carlbot.modules.statistics.ChannelStatistic.WeeklyChannelStatistics;
import net.artifactgaming.carlbot.modules.statistics.StatisticListMessageReactionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Represents a discord message sent by CarlBot to display the list of statistics.
 */
public class StatisticListMessage {
    /**
     * How many seconds spent idling before this message stops reacting to reactions.
     */
    private static final int secondsToWaitToIdle = 30;

    /**
     * The object that is handling this message.
     */
    private StatisticListMessageReactionListener thisHandler;

    private ArrayList<StatisticPage> statisticPages;

    private int currentlyShownPageIndex;

    private String messageID;

    /**
     * Used to callback to stop handling reactions when the timer is up.
     */
    private Timer idleTimer;


    public StatisticListMessage(ArrayList<StatisticPage> statisticsToShow, StatisticListMessageReactionListener thisHandler, String messageID) {
        this.thisHandler = thisHandler;
        this.messageID = messageID;

        statisticPages = statisticsToShow;
    }


    private void setupInactiveTimer(){
        idleTimer = new Timer();

        long delayAndPeriod = secondsToWaitToIdle * 1000;

        idleTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                thisHandler.handleMessageLeftIdle(StatisticListMessage.this);
            }
        }, delayAndPeriod);
    }

    public String getCurrentPage(){
        return statisticPages.get(currentlyShownPageIndex).getPageContent();
    }

    public String getNextPage(){
        ++currentlyShownPageIndex;
        clampCurrentlyShownPageIndexToStatisticPageSize();

        resetIdleTimer();

        return statisticPages.get(currentlyShownPageIndex).getPageContent();
    }

    public String getPreviousPage(){
        --currentlyShownPageIndex;
        clampCurrentlyShownPageIndexToStatisticPageSize();

        resetIdleTimer();

        return statisticPages.get(currentlyShownPageIndex).getPageContent();
    }

    public void cancelAndPurgeIdleTimer(){
        idleTimer.cancel();
        idleTimer.purge();
    }

    private void resetIdleTimer(){
        cancelAndPurgeIdleTimer();
        setupInactiveTimer();
    }

    private void clampCurrentlyShownPageIndexToStatisticPageSize(){
        if (statisticPages.size() <= currentlyShownPageIndex){
            currentlyShownPageIndex = 0;
        } else if (currentlyShownPageIndex < 0){
            currentlyShownPageIndex = statisticPages.size() - 1;
        }
    }

    public String getMessageID() {
        return messageID;
    }
}
