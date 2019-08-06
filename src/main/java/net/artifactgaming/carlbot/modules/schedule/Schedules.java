package net.artifactgaming.carlbot.modules.schedule;

import net.artifactgaming.carlbot.*;
import net.artifactgaming.carlbot.Module;
import net.artifactgaming.carlbot.modules.authority.Authority;
import net.artifactgaming.carlbot.modules.authority.AuthorityManagement;
import net.artifactgaming.carlbot.modules.authority.AuthorityRequiring;
import net.artifactgaming.carlbot.modules.persistence.Persistence;
import net.artifactgaming.carlbot.modules.persistence.PersistentModule;
import net.artifactgaming.carlbot.modules.persistence.Table;
import net.artifactgaming.carlbot.modules.selfdocumentation.Documented;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class Schedules implements Module, AuthorityRequiring, PersistentModule, Documented {

   private AuthorityManagement authorityManagement;
    private Persistence persistence;

    private Logger logger = LoggerFactory.getLogger(Schedules.class);

    private CarlBot carlBot;
    private Map<String, SchedulableCommand> schedulableModules = new HashMap<>();

    private ArrayList<Schedule> schedules;

    public Schedules() {
        CarlBot.addOnCarlbotReadyListener(new OnCarlBotReadyEvent());
    }

    @Override
    public void setup(CarlBot carlbot) {
        carlBot = carlbot;

        //Get the authority module.
        authorityManagement = (AuthorityManagement) carlbot.getModule(AuthorityManagement.class);

        if (authorityManagement == null) {
            logger.error("Authority module is not loaded.");
            carlbot.crash();
        }

        // Get the persistence module.
        persistence = (Persistence) carlbot.getModule(Persistence.class);

        if (persistence == null) {
            logger.error("Persistence module is not loaded.");
            carlbot.crash();
        }

        loadAllSchedulableCommandsIntoHashMap();
    }

    private void loadAllSchedulableCommandsIntoHashMap() {
        for (Module module : carlBot.getModules()) {
            if (module instanceof SchedulableCommand) {
                SchedulableCommand schedulableCommand = (SchedulableCommand) module;
                schedulableModules.put(schedulableCommand.getCallsign(), schedulableCommand);
            }

            for (Command command : module.getCommands(carlBot)) {
                if (command instanceof SchedulableCommand){
                    SchedulableCommand schedulableCommand = (SchedulableCommand) command;
                    schedulableModules.put(schedulableCommand.getCallsign(), schedulableCommand);
                }
            }
        }
    }


    private Table getScheduleTable(Guild guild) throws SQLException {
        Table table = persistence.getGuildTable(guild, this);
        Table scheduleTable = new Table(table, "schedules");

        if (!scheduleTable.exists()) {
            scheduleTable.create();

            scheduleTable.alter().add()
                    .pushValue("key varchar")
                    .pushValue("owner_ID varchar")
                    .pushValue("guild_ID varchar")
                    .pushValue("channel_ID varchar")
                    .pushValue("command_rawString varchar")
                    .pushValue("intervalHours varchar")
                    .execute();
        }

        return scheduleTable;
    }

    private ArrayList<Schedule> getSchedulesFromTable(Guild guild) throws SQLException {
        Table scheduleTable = getScheduleTable(guild);

        ResultSet resultSet = scheduleTable.select().execute();

        ArrayList<Schedule> fetchedSchedules = new ArrayList<>();

        // Add all schedules from the guild into the array.
        while (resultSet.next()) {
            String key = resultSet.getString("key");
            String ownerID = resultSet.getString("owner_ID");
            String guildID = resultSet.getString("guild_ID");
            String channelID = resultSet.getString("channel_ID");
            String commandRawString = resultSet.getString("command_rawString");
            int interval = Integer.parseInt(resultSet.getString("intervalHours"));

            Schedule temp = new Schedule(key, ownerID, guildID, channelID, commandRawString, interval, false);
            temp.setOnScheduleIntervalListener(new OnScheduleIntervalReached());

            fetchedSchedules.add(temp);
        }

        return fetchedSchedules;
    }

    private void addScheduleToTable(Guild guild, Schedule schedule) throws SQLException {
        Table scheduleTable = getScheduleTable(guild);

        scheduleTable.insert()
                .set("key", schedule.getKey())
                .set("owner_ID", schedule.getUserID())
                .set("guild_ID", schedule.getGuildID())
                .set("channel_ID", schedule.getChannelID())
                .set("command_rawString", schedule.getCommandRawString())
                .set("intervalHours", Integer.toString(schedule.getIntervalHours()))
                .execute();
    }

    private void removeScheduleFromTable(Guild guild, Schedule schedule) throws SQLException {
        Table scheduleTable = getScheduleTable(guild);

        scheduleTable.delete()
                .where("key", "=",schedule.getKey())
                .execute();
    }

    private class RemoveScheduleCommand implements  Command, Documented {

        @Override
        public String getCallsign() {
            return "remove";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            if (tokens.size() == 1){
                removeScheduleByKey(tokens.get(0), event);
            } else {
                event.getChannel().sendMessage("Wrong number of argument, usage: \n >$schedule remove \"key\"");
            }
        }

        private void removeScheduleByKey(String key, MessageReceivedEvent event) throws SQLException {
            List<Schedule> schedulesInGuild = filterSchedulesByGuildID(event.getGuild().getId());

            ObjectResult<Schedule> scheduleObjectResult = tryGetScheduleByKeyFromList(key, schedulesInGuild);

            if (scheduleObjectResult.getResult()){
                Schedule scheduleToRemove = scheduleObjectResult.getObject();

                scheduleToRemove.stopScheduleTimer();
                removeScheduleFromTable(event.getGuild(), scheduleToRemove);
                schedules.remove(scheduleToRemove);

                event.getChannel().sendMessage("Schedule of key \"" + key + "\" has been successfully removed.").queue();
            } else {
                event.getChannel().sendMessage("Schedule of key \"" + key + "\" could not be found.").queue();
            }
        }

        @Override
        public Module getParentModule() {
            return Schedules.this;
        }

        @Override
        public String getDocumentation() {
            return "Remove a schedule in this guild based on the schedule key.";
        }

        @Override
        public String getDocumentationCallsign() {
            return "remove";
        }
    }

    private class GetScheduleCommand implements Command, Documented {

        @Override
        public String getCallsign() {
            return "get";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            ArrayList<Schedule> guildSchedules = getSchedulesFromTable(event.getGuild());

            if (tokens.size() == 0) {
                printAllSchedulesInGuild(event, guildSchedules);
            } else if (tokens.size() == 1) {
                printScheduleInGuildByKey(tokens.get(0), event, guildSchedules);
            } else {
                event.getChannel().sendMessage("Wrong number of argument, usage:\n $>schedule get `optional_key_to_give`").queue();
            }
        }

        private void printScheduleInGuildByKey(String key, MessageReceivedEvent event, ArrayList<Schedule> guildSchedules){
            ObjectResult<Schedule> scheduleObjectResult = tryGetScheduleByKeyFromList(key, guildSchedules);

            if (scheduleObjectResult.getResult()){
                Schedule scheduleToPrint = scheduleObjectResult.getObject();

                String channelName = event.getGuild().getTextChannelById(scheduleToPrint.getChannelID()).getName();
                String ownerID = event.getGuild().getMemberById(scheduleToPrint.getUserID()).getNickname();

                String readableScheduleAsString = "KEY: `" + scheduleToPrint.getKey() + "`; In Channel `" + channelName + "` made by `" + ownerID +  "` with schedule command of: \"" + scheduleToPrint.getCommandRawString() + "\"";

                event.getChannel().sendMessage(readableScheduleAsString).queue();
            } else {
                event.getChannel().sendMessage("Schedule with key \"" + key + "\" is not found!").queue();
            }
        }

        private void printAllSchedulesInGuild(MessageReceivedEvent event, ArrayList<Schedule> guildSchedules) {
            String schedulesAsReadableString = schedulesToReadableString(guildSchedules, event.getGuild());

            event.getChannel().sendMessage(schedulesAsReadableString).queue();
        }

        private String schedulesToReadableString(List<Schedule> schedules, Guild guild) {
            if (schedules.size() == 0){
                return "There are no schedules in this server!";
            }

            String guildName = guild.getName();

            String readableString = "Schedules in " + guildName + "\n```";

            for (Schedule schedule : schedules) {
                String channelName = guild.getTextChannelById(schedule.getChannelID()).getName();
                String ownerID = guild.getMemberById(schedule.getUserID()).getNickname();

                readableString += "KEY: " + schedule.getKey() + "; In Channel " + channelName + " made by " + ownerID + " with schedule command of: \"" + schedule.getCommandRawString() + "\"";
            }

            readableString += "```";

            return readableString;
        }

        @Override
        public Module getParentModule() {
            return Schedules.this;
        }

        @Override
        public String getDocumentation() {
            return "Fetches all the currently scheduled commands in this server.";
        }

        @Override
        public String getDocumentationCallsign() {
            return "get";
        }
    }

    private class AddScheduleCommand implements Command, AuthorityRequiring, Documented {
        @Override
        public String getCallsign() {
            return "add";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            if (tokens.size() < 3) {
                event.getChannel().sendMessage("Wrong number of arguments. Command should be:\n$>schedule add \"key\" \"hour\" \"commandToInvoke\"").queue();
                return;
            }

            if (event.getGuild() == null) {
                event.getChannel().sendMessage("This command can only be invoked in a server.").queue();
                return;
            }

            if (scheduleKeyExistsInGuild(tokens.get(0), event.getGuild())){
                event.getChannel().sendMessage("Schedule with key \"" + tokens.get(0) + "\" already exists.");
                return;
            }

            ObjectResult<SchedulableCommand> schedulableCommandObjectResult = tryGetSchedulableCommandFromTokens(tokens);

            if (schedulableCommandObjectResult.getResult()) {
                SchedulableCommand commandToSchedule = schedulableCommandObjectResult.getObject();

                if (!hasAuthorityToScheduleCommand(event.getMember(), commandToSchedule)){
                    event.getChannel().sendMessage("You do not have authority to schedule the command!").queue();
                    return;
                }

                ObjectResult<Schedule> scheduleObjectResult = tryGetScheduleFromRanCommand(event, rawString, tokens);

                if (scheduleObjectResult.getResult()) {
                    Schedule newSchedule = scheduleObjectResult.getObject();
                    newSchedule.setOnScheduleIntervalListener(new OnScheduleIntervalReached());
                    newSchedule.setBindedChannel(event.getTextChannel());

                    addScheduleToTable(event.getGuild(), newSchedule);
                    schedules.add(newSchedule);
                    event.getChannel().sendMessage("Schedule for the command added successfully").queue();
                } else {
                    event.getChannel().sendMessage(scheduleObjectResult.getResultMessage()).queue();
                }
            } else {
                event.getChannel().sendMessage(schedulableCommandObjectResult.getResultMessage()).queue();
            }
        }

        private boolean hasAuthorityToScheduleCommand(Member member, SchedulableCommand command) throws SQLException {

            boolean hasAuthorityToScheduleCommand = true;
            if (command instanceof AuthorityRequiring){
                Authority[] authoritiesRequiredToScheduleCommand = ((AuthorityRequiring) command).getRequiredAuthority();

                hasAuthorityToScheduleCommand = authorityManagement.checkHasAuthorities(member, authoritiesRequiredToScheduleCommand);
            }
            return hasAuthorityToScheduleCommand;
        }

        private boolean scheduleKeyExistsInGuild(String key, Guild guild) throws SQLException {
            List<Schedule> schedulesInGuild = getSchedulesFromTable(guild);

            return tryGetScheduleByKeyFromList(key, schedulesInGuild).getResult();
        }

        private ObjectResult<Schedule> tryGetScheduleFromRanCommand(MessageReceivedEvent event, String rawString, List<String> tokens) {
            try {
                // TODO: Maybe trim the raw string so that it won't include the call-sign?
                Schedule newSchedule = new Schedule(tokens.get(0), event.getAuthor().getId(), event.getGuild().getId(), event.getChannel().getId(), rawString, Integer.parseInt(tokens.get(1)));
                return new ObjectResult<>(newSchedule);
            } catch (IndexOutOfBoundsException e) {
                return new ObjectResult<>(null, "Wrong number of arguments. Command should be:\n$>schedule add \"hour\" \"commandToInvoke\"");
            } catch (NumberFormatException e) {
                return new ObjectResult<>(null, "Argument is of wrong type.  Command should be:\n$>schedule add \"hour\" \"commandToInvoke\" \nWhere \"Hour\" is a number.");
            }
        }

        @Override
        public Authority[] getRequiredAuthority() {
            return new Authority[]{new UseSchedules()};
        }

        @Override
        public Module getParentModule() {
            return Schedules.this;
        }

        @Override
        public String getDocumentation() {
            return "Adds a new scheduled command to this server.";
        }

        @Override
        public String getDocumentationCallsign() {
            return "add";
        }
    }

    private class ScheduleCommands implements Command, AuthorityRequiring, CommandSet {

        private CommandHandler commands;

        ScheduleCommands(CarlBot carlbot) {
            commands = new CommandHandler(carlbot);

            commands.setSubName(this.getCallsign());
            commands.addCommand(new AddScheduleCommand());
            commands.addCommand(new GetScheduleCommand());
            commands.addCommand(new RemoveScheduleCommand());
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
        public Authority[] getRequiredAuthority() {

            return new Authority[]{new UseSchedules()};
        }

        @Override
        public Module getParentModule() {
            return Schedules.this;
        }

        public Collection<Command> getCommands() {
            return commands.getCommands();
        }
    }

    @Override
    public Command[] getCommands(CarlBot carlbot) {
        return new Command[]{new ScheduleCommands(carlbot)};
    }

    @Override
    public Authority[] getRequiredAuthority() {
        return new Authority[]{new UseSchedules()};
    }

    private class OnScheduleIntervalReached implements OnScheduleInterval {
        @Override
        public void onScheduleIntervalCallback(Schedule schedule) {
            // Cut off the callsign.
            String substring = schedule.getCommandRawString().substring(Utils.CALLSIGN.length());
            List<String> tokens = ShellSplitter.shellSplit(substring);

            // TODO: Refactor finding of the command to invoke
            // Remove the "Schedule Add 'key' 'time'" at the front.
            tokens.remove(0);
            tokens.remove(0);
            tokens.remove(0);
            tokens.remove(0);

            ObjectResult<SchedulableCommand> schedulableCommandObjectResult = tryGetSchedulableCommandFromTokens(tokens);

            if (schedulableCommandObjectResult.getResult()){
                SchedulableCommand commandToInvoke = schedulableCommandObjectResult.getObject();
                tokens.remove(commandToInvoke.getCallsign());

                commandToInvoke.InvokeCommand(schedule.getBindedChannel(), tokens);
            } else {
                logger.error("Schedulable Command not found for schedule object: " + schedule.toString());
            }
        }

    }

    private class OnCarlBotReadyEvent implements OnCarlBotReady {

        @Override
        public void OnCarlBotReady(ReadyEvent event) {
            schedules = new ArrayList<Schedule>();

            List<Guild> guilds = event.getJDA().getGuilds();
            try {
                loadAllSchedulesFromGuildsFromDatabase(guilds);
            } catch (SQLException e) {
                logger.error("Failed to load schedules from guilds.");
            }

        }

        private void loadAllSchedulesFromGuildsFromDatabase(List<Guild> guilds) throws SQLException {
            for (Guild guild : guilds) {
                List<Schedule> schedulesInGuild = getSchedulesFromTable(guild);

                // Find and set the text channel for each respective schedule.
                for (Schedule schedule : schedulesInGuild){
                    schedule.setBindedChannel(guild.getTextChannelById(schedule.getChannelID()));
                }

                schedules.addAll(schedulesInGuild);
            }

            for (Schedule schedule : schedules) {
                schedule.startScheduleTimer();
            }
        }
    }

    @Override
    public String getDocumentation() {
        return "Module that is related to scheduling commands in a server.";
    }

    @Override
    public String getDocumentationCallsign() {
        return "schedule";
    }

    private ObjectResult<Schedule> tryGetScheduleByKeyFromList(String key, List<Schedule> schedules) {
        Schedule foundSchedule = null;

        for (Schedule schedule : schedules){
            if (schedule.getKey().equals(key)){
                foundSchedule = schedule;
            }
        }

        return new ObjectResult<>(foundSchedule);
    }

    private List<Schedule> filterSchedulesByGuildID(String guildID){

        ArrayList<Schedule> filteredSchedules = new ArrayList<>();

        for (Schedule schedule : schedules){
            if (schedule.getGuildID().equals(guildID)){
                filteredSchedules.add(schedule);
            }
        }

        return filteredSchedules;
    }

    private ObjectResult<SchedulableCommand> tryGetSchedulableCommandFromTokens(List<String> tokens) {
        try {
            SchedulableCommand commandToSchedule = null;

            String token = tokens.get(0);
            SchedulableCommand temp = schedulableModules.get(token);

            if (temp instanceof CommandSet) {
                if (3 == tokens.size() - 1) {
                    return new ObjectResult<>(null, "Modules can not be scheduled.");
                }

                CommandSet tempCommandSet = (CommandSet) temp;

                boolean commandToScheduleFound = false;
                for (Command commandInCommandSet : tempCommandSet.getCommands()) {
                    // If this command can be scheduled, and it matches the call-sign.
                    if (commandInCommandSet instanceof SchedulableCommand) {
                        if (commandInCommandSet.getCallsign().equals(tokens.get(1))) {
                            commandToSchedule = (SchedulableCommand) commandInCommandSet;
                            commandToScheduleFound = true;
                        }
                    }

                    if (commandToScheduleFound) {
                        break;
                    }
                }
            } else {
                commandToSchedule = temp;
            }

            String resultMessage = Utils.STRING_EMPTY;
            if (commandToSchedule == null) {
                resultMessage = "Either the command could not be scheduled, or the command could not be found.";
            }
            return new ObjectResult<>(commandToSchedule, resultMessage);
        } catch (IndexOutOfBoundsException e) {
            // TODO: Finish error message for the user.
            return new ObjectResult<>(null, "Wrong number of arguments. ");
        }
    }
}
