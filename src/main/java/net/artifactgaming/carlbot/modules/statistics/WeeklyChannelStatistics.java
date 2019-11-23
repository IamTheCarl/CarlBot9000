package net.artifactgaming.carlbot.modules.statistics;

import java.util.Calendar;
import java.util.Date;

public class WeeklyChannelStatistics {
    public final static String DATE_FORMAT_PATTERN = "MM/dd/yyyy HH:mm:ss";

    ///region SQL Column Names

    public final static String CHANNEL_NAME = "CHANNEL_NAME";

    public final static String CHANNEL_ID = "CHANNEL_ID";

    public final static String NO_OF_MESSAGES_SENT = "NO_OF_MESSAGES_SENT";

    public final static String NO_OF_MESSAGES_WITH_IMAGE = "NO_OF_MESSAGES_WITH_IMAGE";

    public final static String TRACKED_DATE = "TRACKED_DATE";
    ///endregion

    private String channelID;

    private String channelName;

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
     */
    private Date trackedDate;

    public WeeklyChannelStatistics(String _channelID, String _channelName){
        channelID = _channelID;
        channelName = _channelName;
        noOfMessagesSent = 0;
        noOfMessagesWithImage = 0;
        trackedDate = Calendar.getInstance().getTime();
    }

    public WeeklyChannelStatistics(String _channelID, String _channelName, int _noOfMessagesSent, int _noOfMessagesWithImage, Date _trackedDate) {
        channelID = _channelID;
        channelName = _channelName;
        noOfMessagesSent = _noOfMessagesSent;
        noOfMessagesWithImage = _noOfMessagesWithImage;
        trackedDate = _trackedDate;
    }

    ///region Getter/Setter

    public String getChannelID() {
        return channelID;
    }

    public void setChannelID(String channelID) {
        this.channelID = channelID;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

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

    void incrementNoOfMessagesSent(){
        ++noOfMessagesSent;
    }

    void incrementNoOfMessagesSentWithImage(){
        ++noOfMessagesWithImage;
    }
}
