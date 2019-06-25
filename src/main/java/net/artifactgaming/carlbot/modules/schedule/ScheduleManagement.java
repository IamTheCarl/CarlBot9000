package net.artifactgaming.carlbot.modules.schedule;

import net.artifactgaming.carlbot.*;
import net.artifactgaming.carlbot.modules.persistence.InsertBuilder;
import net.artifactgaming.carlbot.modules.persistence.Persistence;
import net.artifactgaming.carlbot.modules.persistence.PersistentModule;
import net.artifactgaming.carlbot.modules.persistence.Table;
import net.artifactgaming.carlbot.modules.quotes.Quotes;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import java.util.concurrent.*;
import java.util.function.Function;

public class ScheduleManagement implements PersistentModule, Module {

    private Persistence persistence;
    private Logger logger = LoggerFactory.getLogger(Quotes.class);

    private List<ScheduleTimer> scheduleTimers;

    private class ScheduleCommands implements Command, CommandSet {
        private CommandHandler commands;

        ScheduleCommands(CarlBot carlbot) {
            commands = new CommandHandler(carlbot);

            commands.addCommand(new AddCommand());
            commands.addCommand(new GetCommand());
            commands.addCommand(new RemoveCommand());
        }

        @Override
        public String getCallsign() {
            return "schedule";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) {
            commands.runCommand(event, rawString, tokens);
        }

        @Override
        public Module getParentModule() {
            return ScheduleManagement.this;
        }

        public Collection<Command> getCommands() {
            return commands.getCommands();
        }

    }

    private class RemoveCommand implements Command {

        @Override
        public String getCallsign() {
            return "remove";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            ObjectResult<Schedule> currentSchedule = tryGetScheduleInTableByOwnerID(event.getAuthor().getId());

            if (currentSchedule.getResult()){
                deleteScheduleInTable(currentSchedule.getObject());
                removeTimedScheduleBySchedule(currentSchedule.getObject());

                event.getChannel().sendMessage("Your current schedule has been removed.")
                        .queue();
            } else {
                event.getChannel().sendMessage("You had no schedule set.")
                        .queue();
            }
        }

        @Override
        public Module getParentModule() {
            return ScheduleManagement.this;
        }
    }

    private class GetCommand implements Command {

        @Override
        public String getCallsign() {
            return "get";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            ObjectResult<Schedule> currentScheduleResult = tryGetScheduleInTableByOwnerID(event.getAuthor().getId());

            if (currentScheduleResult.getResult()){
                Schedule currentSchedule = currentScheduleResult.getObject();

                String dateTimeToScheduleReachedInString = getDateTimeToScheduleReachedInString(currentSchedule);

                String scheduleMessage = getScheduleMessageDetail(currentSchedule);

                String messageToSend = "You have a schedule set at " +
                        currentSchedule.getScheduleDateTimeAsSQLFormat() +
                        "\n" + scheduleMessage +
                        "\n\nYou will be alerted of your schedule in " + dateTimeToScheduleReachedInString;


                event.getChannel().sendMessage(messageToSend)
                        .queue();
            } else {
                event.getChannel().sendMessage("You currently do not have any schedule set.")
                        .queue();
            }
        }

        private String getScheduleMessageDetail(Schedule schedule){
            String scheduleMessage;

            if (schedule.getScheduleMessage().isEmpty()){
                scheduleMessage= "You have no schedule message set.";
            } else {
                scheduleMessage = "Your schedule message is `" + schedule.getScheduleMessage() + "`";
            }

            return scheduleMessage;
        }

        private String getDateTimeToScheduleReachedInString(Schedule schedule){
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return schedule.getDateTimeToScheduleReached().format(formatter);
        }

        @Override
        public Module getParentModule() {
            return ScheduleManagement.this;
        }
    }

    private class AddCommand implements Command {

        @Override
        public String getCallsign() {
            return "add";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {

            ObjectResult<Schedule> searchedOldScheduleResult = tryGetScheduleInTableByOwnerID(event.getAuthor().getId());

            // Remove any previous schedule that the user has done.
            if (searchedOldScheduleResult.getResult()){
                deleteScheduleInTable(searchedOldScheduleResult.getObject());
            }

            ObjectResult<Schedule> scheduleFormedResult = tryGetScheduleFromTokens(event.getAuthor().getId(), tokens);

            if (scheduleFormedResult.getResult()){

                Schedule newSchedule = scheduleFormedResult.getObject();

                insertScheduleToTable(newSchedule);
                AddScheduleToTimedSchedules(newSchedule);

                String messageToSend = Utils.STRING_EMPTY;

                // If we have removed any previous schedule that the user has done
                if (searchedOldScheduleResult.getResult()) {
                    messageToSend += "Your previous schedule has been replaced with your new schedule!\n";
                } else {
                    messageToSend += "We have added the new schedule for you!";
                }

                event.getChannel().sendMessage(messageToSend)
                        .queue();
            } else {
                event.getChannel().sendMessage(scheduleFormedResult.getResultMessage())
                        .queue();
            }
        }

        private ObjectResult<Schedule> tryGetScheduleFromTokens(String ownerID, List<String> tokens){
            if (tokens.size() >= 1) {
                ObjectResult<Integer> parsingToIntResult = Utils.tryParseInteger(tokens.get(0));

                if (!parsingToIntResult.getResult()) {
                    return new ObjectResult<Schedule>(null, "Wrong type of arguments. The first argument should be a minute.");
                }

                LocalDateTime scheduleDateTime = getDateTimeByMinutesFromNow(parsingToIntResult.getObject());

                String scheduleMessage = getScheduleMessageFromTokens(tokens);

                Schedule schedule = new Schedule(ownerID, scheduleDateTime, scheduleMessage);

                return new ObjectResult<>(schedule);
            } else {
                return new ObjectResult<Schedule>(null, "Wrong number of arguments. Give a minute to trigger the schedule, and optionally a message with it.");
            }
        }

        private String getScheduleMessageFromTokens(List<String> tokens){
            String scheduleMessage = Utils.STRING_EMPTY;

            // If a message is given
            if (tokens.size() > 1){
                for (int i = 1; i < tokens.size(); ++i){
                    scheduleMessage += tokens.get(i) + " ";
                }
            }

            return scheduleMessage;
        }

        private LocalDateTime getDateTimeByMinutesFromNow(int minute){
            LocalDateTime now = LocalDateTime.now();
            return now.plusMinutes(minute);
        }

        @Override
        public Module getParentModule() {
            return ScheduleManagement.this;
        }
    }

    //region SQL_Related

    private ObjectResult<Schedule> tryGetScheduleInTableByOwnerID(String ownerID) throws SQLException {
        Table scheduleTable = getScheduleTable();

        ResultSet resultSet = scheduleTable.select()
                .where("owner_id", "=", ownerID)
                .execute();

        Schedule fetchedSchedule;

        if (resultSet.next()){
            LocalDateTime dateTime = resultSet.getTimestamp("schedule_time").toLocalDateTime();
            String scheduleMessage = resultSet.getString("schedule_message");

            fetchedSchedule = new Schedule(ownerID, dateTime,scheduleMessage);
        } else {
            fetchedSchedule = null;
        }

        return new ObjectResult<>(fetchedSchedule);
    }

    private void deleteScheduleInTable(Schedule scheduleToDelete) throws SQLException {
        Table scheduleTable = getScheduleTable();

        scheduleTable.delete()
                .where("owner_id", "=", scheduleToDelete.getOwnerID())
                .execute();
    }

    private void insertScheduleToTable(Schedule scheduleToAdd) throws SQLException {
        Table scheduleTable = getScheduleTable();

        scheduleTable.insert()
                .set("owner_id", scheduleToAdd.getOwnerID())
                .set("schedule_message", scheduleToAdd.getScheduleMessage())
                .set("schedule_time", scheduleToAdd.getScheduleDateTimeAsSQLFormat())
                .execute();
    }

    private Table getScheduleTable() throws SQLException {
        Table table = persistence.getUsersTable();
        Table schedulesTable = new Table(table, "schedules");

        if (!schedulesTable.exists()) {
            schedulesTable.create();

            schedulesTable.alter().add()
                    .pushValue("owner_id varchar")
                    .pushValue("schedule_message varchar")
                    .pushValue("schedule_time datetime")
                    .execute();
        }

        return schedulesTable;
    }

    private Schedule[] getAllSchedules() throws SQLException {
        Table scheduleTable = getScheduleTable();

        ResultSet resultSet = scheduleTable.select()
                .execute();

        Schedule[] schedules = new Schedule[resultSet.getFetchSize()];

        int i = 0;
        while (resultSet.next()){

            String ownerID = resultSet.getString("owner_id");
            LocalDateTime dateTime = resultSet.getTimestamp("schedule_time").toLocalDateTime();
            String scheduleMessage = resultSet.getString("schedule_message");

            schedules[i] = new Schedule(ownerID, dateTime, scheduleMessage);

            ++i;
        }

        return schedules;
    }

    //endregion

    private void removeTimedScheduleBySchedule(Schedule schedule){
        ObjectResult<ScheduleTimer> timedScheduleResult = tryFindTimedScheduleBySchedule(schedule);

        if (timedScheduleResult.getResult()){
            ScheduleTimer scheduleTimerToRemove = timedScheduleResult.getObject();
            scheduleTimerToRemove.cancelScheduleTimer();
            scheduleTimers.remove(scheduleTimerToRemove);
        }
    }

    private ObjectResult<ScheduleTimer> tryFindTimedScheduleBySchedule(Schedule schedule){
        for (ScheduleTimer scheduleTimer : scheduleTimers){
            if (scheduleTimer.getSchedule().getOwnerID().equals(schedule.getOwnerID())){
                return new ObjectResult<>(scheduleTimer);
            }
        }

        return new ObjectResult<>(null);
    }

    private void AddScheduleToTimedSchedules(Schedule schedule){
        ScheduleTimer newScheduleTimer = new ScheduleTimer(schedule);
        newScheduleTimer.registerOnScheduleTimerReachedEvent(new ScheduleTimerReachedEventHandler());
        scheduleTimers.add(newScheduleTimer);
    }

    @Override
    public void setup(CarlBot carlbot) {

        if (!TryLoadPersistenceModule(carlbot)) {
            logger.error("Persistence module is not loaded.");
            carlbot.crash();
        }

        scheduleTimers = new ArrayList<ScheduleTimer>();

        try {
            TimeAllSchedulesFromTable();
        } catch (SQLException e){
            logger.error("Failure in getting all schedules");
            carlbot.crash();
        }
    }

    private void TimeAllSchedulesFromTable() throws SQLException{
        for (Schedule schedule : getAllSchedules()) {
            AddScheduleToTimedSchedules(schedule);
        }
    }

    private boolean TryLoadPersistenceModule(CarlBot carlbot){
        persistence = (Persistence) carlbot.getModule(Persistence.class);

        return persistence != null;
    }


    @Override
    public Command[] getCommands(CarlBot carlbot) {
        return new Command[] { new ScheduleCommands(carlbot) };
    }

    private class ScheduleTimerReachedEventHandler implements OnScheduleTimerReachedEvent {

        @Override
        public void onScheduleTimerReachedEvent(ScheduleTimer scheduleTimer) {
            scheduleTimers.remove(scheduleTimer);
        }
    }
}
