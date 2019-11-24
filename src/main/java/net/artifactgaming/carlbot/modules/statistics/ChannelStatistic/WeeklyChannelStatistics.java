package net.artifactgaming.carlbot.modules.statistics.ChannelStatistic;

import java.util.Calendar;
import java.util.Date;

public class WeeklyChannelStatistics extends ChannelStatistics {

    ///region SQL Column Names

    public final static String NO_OF_MESSAGES_SENT = "NO_OF_MESSAGES_SENT";

    public final static String NO_OF_MESSAGES_WITH_IMAGE = "NO_OF_MESSAGES_WITH_IMAGE";

    public final static String TRACKED_DATE = "TRACKED_DATE";
    ///endregion

    /**
     * Total number of messages sent into this channel.
     */
    private int noOfMessagesSent;

    /**
     * Total number of messages sent into this channel, that had images in them.
     */
    private int noOfMessagesWithImage;

    /**
     * At which date did this channel began it's weekly tracking?
     * (So that we can know when to reset the week's statistics)
     * TODO: Use 'LocalDate' instead; Much better.
     */
    private Date trackedDate;

    public WeeklyChannelStatistics(String _channelID, String _channelName){
        super(_channelID, _channelName);
        noOfMessagesSent = 0;
        noOfMessagesWithImage = 0;
        trackedDate = Calendar.getInstance().getTime();
    }

    public WeeklyChannelStatistics(String _channelID, String _channelName, int _noOfMessagesSent, int _noOfMessagesWithImage, Date _trackedDate) {
        super(_channelID, _channelName);
        noOfMessagesSent = _noOfMessagesSent;
        noOfMessagesWithImage = _noOfMessagesWithImage;
        trackedDate = _trackedDate;
    }

    ///region Getter/Setter

    public int getNoOfMessagesSent() {
        return noOfMessagesSent;
    }

    public void setNoOfMessagesSent(int noOfMessagesSent) {
        this.noOfMessagesSent = noOfMessagesSent;
    }

    public int getNoOfMessagesWithImage() {
        return noOfMessagesWithImage;
    }

    public void setNoOfMessagesWithImage(int noOfMessagesWithImage) {
        this.noOfMessagesWithImage = noOfMessagesWithImage;
    }

    public Date getTrackedDate() {
        return trackedDate;
    }

    public void setTrackedDate(Date trackedDate) {
        this.trackedDate = trackedDate;
    }

    ///endregion

    //region Utils

    public void incrementNoOfMessagesSent(){
        ++noOfMessagesSent;
    }

    public void incrementNoOfMessagesSentWithImage(){
        ++noOfMessagesWithImage;
    }

    public void reset(){
        noOfMessagesSent = 0;
        noOfMessagesWithImage = 0;
        trackedDate = Calendar.getInstance().getTime();
    }

    //endregion
}
