package net.artifactgaming.carlbot.modules.schedule;

import net.dv8tion.jda.core.entities.TextChannel;

import java.nio.channels.Channel;
import java.util.Timer;
import java.util.TimerTask;

public class Schedule {

    private final static long ONE_HOUR_IN_MILISECONDS = 3600000;

    private String key;
    private String userID;
    private String guildID;
    private String channelID;

    private String commandRawString;

    private OnScheduleInterval onScheduleInterval;

    private int intervalHours;

    private Timer scheduleTimer;

    /*
    The channel this schedule is binded to.
     */
    private TextChannel bindedChannel;

    public Schedule(String key, String userID, String guildID, String channelID, String commandRawString, TextChannel bindedChannel, int intervalHours) {
        this.key = key;
        this.userID = userID;
        this.guildID = guildID;
        this.channelID = channelID;
        this.commandRawString = commandRawString;
        this.intervalHours = intervalHours;
        this.bindedChannel = bindedChannel;
        setupScheduleTimer();
    }

    public Schedule(String key, String userID, String guildID, String channelID, String commandRawString, int intervalHours) {
        this.key = key;
        this.userID = userID;
        this.guildID = guildID;
        this.channelID = channelID;
        this.commandRawString = commandRawString;
        this.intervalHours = intervalHours;
        this.bindedChannel = null;
        setupScheduleTimer();
    }

    public Schedule(String key, String userID, String guildID, String channelID, String commandRawString, int intervalHours, TextChannel bindedChannel, boolean startTimer) {
        this.key = key;
        this.userID = userID;
        this.guildID = guildID;
        this.channelID = channelID;
        this.commandRawString = commandRawString;
        this.intervalHours = intervalHours;
        this.bindedChannel = bindedChannel;

        if (startTimer){
            setupScheduleTimer();
        }
    }

    public Schedule(String key, String userID, String guildID, String channelID, String commandRawString, int intervalHours, boolean startTimer) {
        this.key = key;
        this.userID = userID;
        this.guildID = guildID;
        this.channelID = channelID;
        this.commandRawString = commandRawString;
        this.intervalHours = intervalHours;
        this.bindedChannel = null;

        if (startTimer){
            setupScheduleTimer();
        }
    }

    public void startScheduleTimer(){
        if (scheduleTimer == null){
            setupScheduleTimer();
        }
    }

    private void setupScheduleTimer(){
        scheduleTimer = new Timer();

        long delayAndPeriod = ONE_HOUR_IN_MILISECONDS * intervalHours;

        scheduleTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (onScheduleInterval != null){
                    onScheduleInterval.onScheduleIntervalCallback(Schedule.this);
                }
            }
        }, delayAndPeriod, delayAndPeriod);
    }

    public void setOnScheduleIntervalListener(OnScheduleInterval onScheduleInterval){
        this.onScheduleInterval = onScheduleInterval;
    }

    public void stopScheduleTimer(){
        if (scheduleTimer != null) {
            scheduleTimer.cancel();
            scheduleTimer.purge();
        }
    }

    public String getKey(){return key;}

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

    public void setBindedChannel(TextChannel bindedChannel){
        this.bindedChannel = bindedChannel;
    }

    public TextChannel getBindedChannel(){
        return bindedChannel;
    }
}
