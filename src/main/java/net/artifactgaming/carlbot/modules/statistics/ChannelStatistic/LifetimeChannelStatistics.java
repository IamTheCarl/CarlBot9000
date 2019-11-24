package net.artifactgaming.carlbot.modules.statistics.ChannelStatistic;

public class LifetimeChannelStatistics extends ChannelStatistics {
    ///region SQL Column Names

    public final static String PERCENT_OF_MESSAGES_SENT = "PERCENT_OF_MESSAGES_SENT";

    public final static String PERCENT_OF_MESSAGES_WITH_IMAGE = "PERCENT_OF_MESSAGES_WITH_IMAGE";
    ///endregion

    /**
     * How many percent of messages were sent in this channel in the entire server.
     */
    private double percentageOfTotalMessagesSent;

    /**
     * How many messages sent into this channel contained images.
     */
    private double percentageOfMessagesContainImages;

    public LifetimeChannelStatistics(String _channelID, String _channelName, double _percentageOfTotalMessagesSent, double _percentageOfMessagesContainImages) {
        super(_channelID,_channelName);
        percentageOfTotalMessagesSent = _percentageOfTotalMessagesSent;
        percentageOfMessagesContainImages = _percentageOfMessagesContainImages;
    }

    public LifetimeChannelStatistics(String _channelID, String _channelName) {
        super(_channelID,_channelName);
        percentageOfTotalMessagesSent = 0;
        percentageOfMessagesContainImages = 0;
    }

    ///region Getter/Setter
    public double getPercentageOfTotalMessagesSent() {
        return percentageOfTotalMessagesSent;
    }

    public void setPercentageOfTotalMessagesSent(double percentageOfTotalMessagesSent) {
        this.percentageOfTotalMessagesSent = percentageOfTotalMessagesSent;
    }

    public double getPercentageOfMessagesContainImages() {
        return percentageOfMessagesContainImages;
    }

    public void setPercentageOfMessagesContainImages(double percentageOfMessagesContainImages) {
        this.percentageOfMessagesContainImages = percentageOfMessagesContainImages;
    }
    ///endregion
}
