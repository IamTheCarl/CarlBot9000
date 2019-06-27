package net.artifactgaming.carlbot.modules.schedule;

public class Schedule {
    private String userID;
    private String guildID;
    private String channelID;

    private String commandRawString;

    private OnScheduleInterval onScheduleInterval;

    private int intervalHours;

    public Schedule(String userID, String guildID, String channelID, String commandRawString, int intervalHours) {
        this.userID = userID;
        this.guildID = guildID;
        this.channelID = channelID;
        this.commandRawString = commandRawString;
        this.intervalHours = intervalHours;
    }

    public void setOnScheduleIntervalListener(OnScheduleInterval onScheduleInterval){
        this.onScheduleInterval = onScheduleInterval;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getGuildID() {
        return guildID;
    }

    public void setGuildID(String guildID) {
        this.guildID = guildID;
    }

    public String getChannelID() {
        return channelID;
    }

    public void setChannelID(String channelID) {
        this.channelID = channelID;
    }

    public String getCommandRawString() {
        return commandRawString;
    }

    public void setCommandRawString(String commandRawString) {
        this.commandRawString = commandRawString;
    }

    public int getIntervalHours() {
        return intervalHours;
    }

    public void setIntervalHours(int intervalHours) {
        this.intervalHours = intervalHours;
    }
}
