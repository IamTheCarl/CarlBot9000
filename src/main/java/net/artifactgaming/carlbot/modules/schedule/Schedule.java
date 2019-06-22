package net.artifactgaming.carlbot.modules.schedule;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import  java.time.temporal.ChronoUnit;

public class Schedule {

    private String ownerID;

    private String scheduleMessage;

    private LocalDateTime scheduleDateTime;

    public Schedule(String ownerID, LocalDateTime scheduleDateTime, String scheduleMessage) {
        this.ownerID = ownerID;
        this.scheduleMessage = scheduleMessage;
        this.scheduleDateTime = scheduleDateTime;
    }

    public String getOwnerID() {
        return ownerID;
    }

    public String getScheduleMessage() {
        return scheduleMessage;
    }

    public LocalDateTime getScheduleDateTime() {
        return scheduleDateTime;
    }

    public String getScheduleDateTimeAsSQLFormat(){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return scheduleDateTime.format(formatter);
    }

    public long getTimeInSecondsToScheduleReached(){
        LocalDateTime now = LocalDateTime.now();

        return ChronoUnit.SECONDS.between(now, scheduleDateTime);
    }

    public long getTimeInMilisecondsToScheduleReached(){
        LocalDateTime now = LocalDateTime.now();

        return ChronoUnit.MILLIS.between(now, scheduleDateTime);
    }

    public LocalDateTime getDateTimeToScheduleReached(){
        LocalDateTime now = LocalDateTime.now();

        long secondsToScheduleReached= getTimeInSecondsToScheduleReached();
        LocalDateTime dateTimeToScheduleReached = now.plusSeconds(secondsToScheduleReached);

        return dateTimeToScheduleReached;
    }
}
