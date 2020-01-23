package net.artifactgaming.carlbot.modules.statistics.ChannelStatistic;

public abstract class ChannelStatistics {
    ///region SQL Column Names
    public final static String CHANNEL_NAME = "CHANNEL_NAME";

    public final static String CHANNEL_ID = "CHANNEL_ID";
    ///endregion

    private String channelID;

    private String channelName;

    ChannelStatistics(String _channelID, String _channelName) {
        channelName = _channelName;
        channelID = _channelID;
    }

    ///region Getter/Setter
    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getChannelID() {
        return channelID;
    }

    public void setChannelID(String channelID) {
        this.channelID = channelID;
    }
    ///endregion
}
