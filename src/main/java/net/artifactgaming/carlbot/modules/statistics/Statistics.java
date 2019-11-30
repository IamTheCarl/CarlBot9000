package net.artifactgaming.carlbot.modules.statistics;

import net.artifactgaming.carlbot.*;
import net.artifactgaming.carlbot.Module;
import net.artifactgaming.carlbot.listeners.OnCarlBotReady;
import net.artifactgaming.carlbot.modules.authority.Authority;
import net.artifactgaming.carlbot.modules.authority.AuthorityRequiring;
import net.artifactgaming.carlbot.modules.persistence.Persistence;
import net.artifactgaming.carlbot.modules.persistence.PersistentModule;
import net.artifactgaming.carlbot.modules.selfdocumentation.Documented;
import net.artifactgaming.carlbot.modules.statistics.ChannelStatistic.LifetimeChannelStatistics;
import net.artifactgaming.carlbot.modules.statistics.ChannelStatistic.WeeklyChannelStatistics;
import net.artifactgaming.carlbot.modules.statistics.DatabaseSQL.SettingsDatabaseHandler;
import net.artifactgaming.carlbot.modules.statistics.DatabaseSQL.StatisticsDatabaseHandler;
import net.artifactgaming.carlbot.modules.statistics.authority.ToggleStatistics;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Statistics implements Module, Documented, PersistentModule {

    /**
     * How many days must pass for a weekly statistic to reset.
     * Keep it to 7, unless for debug.
     */
    private final static int DAYS_TO_RESET = 0;

    private SettingsDatabaseHandler settingsDatabaseHandler;

    private StatisticsDatabaseHandler statisticsDatabaseHandler;

    private MessageStatisticCollector messageStatisticCollector;

    private Persistence persistence;

    private Logger logger = LoggerFactory.getLogger(Statistics.class);

    public Statistics(){
        CarlBot.addOnCarlbotReadyListener(new ResetOverdueWeeklyStatisticsOnCarlBotReady());
    }

    @Override
    public String getDocumentation() {
        return "Commands relating to collecting and displaying statistics on your server!";
    }

    @Override
    public String getDocumentationCallsign() {
        return "stats";
    }

    @Override
    public void setup(CarlBot carlbot) {
        // Get the persistence module.
        persistence = (Persistence) carlbot.getModule(Persistence.class);

        if (persistence == null) {
            logger.error("Persistence module is not loaded.");
            carlbot.crash();
        }

        settingsDatabaseHandler = new SettingsDatabaseHandler(persistence, this);
        statisticsDatabaseHandler = new StatisticsDatabaseHandler(persistence, this);

        messageStatisticCollector = new MessageStatisticCollector(settingsDatabaseHandler, statisticsDatabaseHandler);

        // TODO: Setup a repeating timer to constantly check if weekly statistics need resetting.

        carlbot.addOnMessageReceivedListener(messageStatisticCollector);
    }

    private class ResetOverdueWeeklyStatisticsOnCarlBotReady implements OnCarlBotReady {

        // TODO: Possible refactor on the 'lifetimeChannelStatisticsToMergeList' ArrayList?

        /**
         * Create a list to merge with the actual database later.
         */
        private ArrayList<LifetimeChannelStatistics> lifetimeChannelStatisticsToMergeList = new ArrayList<>();

        /**
         * Debug purposes.
         */
        int noOfResetsDone = 0;

        @Override
        public void onCarlBotReady(ReadyEvent event) {
            List<Guild> guilds = event.getJDA().getGuilds();

            for (Guild guild: guilds) {
                tryResetOverdueWeeklyStatistics(guild);
                tryMergeLifetimeChannelStatistics(guild);
                lifetimeChannelStatisticsToMergeList.clear();
            }
            logger.debug("Reset " + noOfResetsDone + " channels because their weekly statistic data was outdated!");
        }

        private void tryMergeLifetimeChannelStatistics(Guild guild) {
            try {
                List<LifetimeChannelStatistics> lifetimeChannelStatisticsList = statisticsDatabaseHandler.getLifetimeGuildStatistics(guild);

                // Merge the data with the actual
                for (LifetimeChannelStatistics toMerge: lifetimeChannelStatisticsToMergeList) {
                    boolean hasActualRow = false;
                    for (LifetimeChannelStatistics actual: lifetimeChannelStatisticsList) {
                        if (toMerge.getChannelID().equals(actual.getChannelID())){
                            double mergedPercentage = actual.getPercentageOfTotalMessagesSent() + toMerge.getPercentageOfTotalMessagesSent();
                            double mergedPercentageOnImage = (actual.getPercentageOfMessagesContainImages() + toMerge.getPercentageOfMessagesContainImages()) / 2;

                            actual.setPercentageOfMessagesContainImages(mergedPercentageOnImage);
                            actual.setPercentageOfTotalMessagesSent(mergedPercentage);
                            actual.setChannelName(toMerge.getChannelName());
                            hasActualRow = true;
                            break;
                        }
                    }

                    if (!hasActualRow){
                        // It will reach here if there was no row for a lifetime data
                        // for this channel; We need to create one.
                        statisticsDatabaseHandler.insertNewChannelStatisticsIntoLifetimeStatisticsTable(guild, toMerge);
                        lifetimeChannelStatisticsList.add(toMerge);
                    }
                }
                
                double totalPercentageOfMessagesSent = getTotalPercentageOfMessagesSent(lifetimeChannelStatisticsList);
                // Recalculate the merged result
                for (LifetimeChannelStatistics mergedResult: lifetimeChannelStatisticsList) {
                    double finalPercentage = (mergedResult.getPercentageOfTotalMessagesSent() / totalPercentageOfMessagesSent) * 100;
                    mergedResult.setPercentageOfTotalMessagesSent(finalPercentage);

                    statisticsDatabaseHandler.updateLifetimeChannelStatistics(guild, mergedResult);
                }

            } catch (SQLException e){
                logger.error("Failed to fetch one of the guilds lifetime channel statistics due to: " + e.getMessage());
            }
        }
        
        private double getTotalPercentageOfMessagesSent(List<LifetimeChannelStatistics> lifetimeChannelStatisticsList){
            double result = 0;
            for (LifetimeChannelStatistics lifetimeChannelStatistics: lifetimeChannelStatisticsList) {
                result += lifetimeChannelStatistics.getPercentageOfTotalMessagesSent();
            }
            return result;
        }

        private void tryResetOverdueWeeklyStatistics(Guild guild){
            try {
                List<WeeklyChannelStatistics> weeklyChannelStatisticsList = statisticsDatabaseHandler.getWeeklyGuildStatistics(guild);

                long totalMessagesSent = getTotalMessagesSent(weeklyChannelStatisticsList);

                LocalDate currentLocalDate = Calendar.getInstance().getTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

                for (WeeklyChannelStatistics weeklyChannelStatistics: weeklyChannelStatisticsList) {
                    LocalDate startedTrackingLocalDate = weeklyChannelStatistics.getTrackedDate();

                    Period dateDifference = Period.between(startedTrackingLocalDate, currentLocalDate);
                    // If one week has passed since the date of tracking, reset
                    if (dateDifference.getDays() >= DAYS_TO_RESET){
                        resetChannelWeeklyStatistics(guild, weeklyChannelStatistics, totalMessagesSent);
                    }
                }
            } catch (SQLException e){
                logger.error("Failed to fetch one of the guilds weekly channel statistics due to: " + e.getMessage());
            } catch (ParseException e){
                logger.error("Failed to properly parse one of the date-formats in the weekly channel statistics: " + e.getMessage());
            }
        }

        private void resetChannelWeeklyStatistics(Guild guild, WeeklyChannelStatistics weeklyChannelStatistics, long totalMessagesSent) throws SQLException{
            // TODO: Maybe do something if the channel is now not-visible or deleted?

            // If no messages were sent onto this channel for this week, do nothing.
            if (weeklyChannelStatistics.getNoOfMessagesSent() > 0) {
                // To merge with the actual database later.
                LifetimeChannelStatistics lifetimeChannelStatisticsToMerge = new LifetimeChannelStatistics(weeklyChannelStatistics.getChannelID(), weeklyChannelStatistics.getChannelName());

                double percentOfMessagesSent = ((double) weeklyChannelStatistics.getNoOfMessagesSent() / totalMessagesSent) * 100;
                lifetimeChannelStatisticsToMerge.setPercentageOfTotalMessagesSent(percentOfMessagesSent);

                double percentOfMessagesHadImages = (weeklyChannelStatistics.getNoOfMessagesWithImage() / (double) weeklyChannelStatistics.getNoOfMessagesSent()) * 100;
                lifetimeChannelStatisticsToMerge.setPercentageOfMessagesContainImages(percentOfMessagesHadImages);

                lifetimeChannelStatisticsToMergeList.add(lifetimeChannelStatisticsToMerge);
            }

            weeklyChannelStatistics.reset();
            statisticsDatabaseHandler.updateWeeklyChannelStatistics(guild, weeklyChannelStatistics);

            // DEBUG!!!
            logger.debug("Reset channel weekly statistics :: " + weeklyChannelStatistics.getChannelName());
            ++noOfResetsDone;
        }
    }

    @Override
    public Command[] getCommands(CarlBot carlbot) {
        return new Command[] {new StatisticsCommand(carlbot)};
    }

    private class ShowLifetimeStatisticsCommand implements Command, Documented{

        @Override
        public String getCallsign() {
            return "lifetime";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            if (event.getGuild() == null){
                event.getTextChannel().sendMessage("Statistics command can only be invoked in a server!").queue();
                return;
            }

            if (!guildHasStatisticsEnabled(event.getGuild())){
                event.getTextChannel().sendMessage("This server does not have statistics enabled!").queue();
                return;
            }

            List<LifetimeChannelStatistics> lifetimeChannelStatistics = statisticsDatabaseHandler.getLifetimeGuildStatistics(event.getGuild());

            String asReadableStatistics = getReadableStatistics(lifetimeChannelStatistics);
            event.getTextChannel().sendMessage(asReadableStatistics).queue();
        }

        private String getReadableStatistics(List<LifetimeChannelStatistics> lifetimeChannelStatisticsList){
            if (lifetimeChannelStatisticsList.size() <= 0){
                return "None of the channels' weekly statistics have been tracked into the lifetime database yet!" + Utils.NEWLINE
                        + "Try again after a week."
                        + Utils.NEWLINE
                        + "**NOTE**: Channels that I do not have access to is not tracked!";
            }

            // TODO: Like the weekly statistics, separate into multiple pages if the list is too long.
            StringBuilder readableStatistics = new StringBuilder();
            readableStatistics.append("```md");
            for (LifetimeChannelStatistics channelStatistics : lifetimeChannelStatisticsList){
                readableStatistics.append(Utils.NEWLINE);
                readableStatistics.append(channelStatistics.getChannelName()).append(" (").append(String.format("%.1f", channelStatistics.getPercentageOfTotalMessagesSent())).append("%)").append(Utils.NEWLINE);
                readableStatistics.append("====").append(Utils.NEWLINE);
                readableStatistics.append(String.format("%.1f", channelStatistics.getPercentageOfMessagesContainImages())).append("% contains images.").append(Utils.NEWLINE);
            }
            readableStatistics.append("```**NOTE**: Channels that I do not have access to is not tracked!");

            return readableStatistics.toString();
        }

        @Override
        public Module getParentModule() {
            return Statistics.this;
        }

        @Override
        public String getDocumentation() {
            return "Show lifetime statistics of all the channels.";
        }

        @Override
        public String getDocumentationCallsign() {
            return "lifetime";
        }
    }

    private class ShowWeeklyStatisticsCommand implements Command, Documented {

        @Override
        public String getCallsign() {
            return "weekly";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            if (event.getGuild() == null){
                event.getTextChannel().sendMessage("Statistics command can only be invoked in a server!").queue();
                return;
            }

            if (!guildHasStatisticsEnabled(event.getGuild())){
                event.getTextChannel().sendMessage("This server does not have statistics enabled!").queue();
                return;
            }

            List<WeeklyChannelStatistics> weeklyChannelStatisticsList = statisticsDatabaseHandler.getWeeklyGuildStatistics(event.getGuild());

            String readableStatisticResult = getReadableStatisticsResult(weeklyChannelStatisticsList);

            event.getTextChannel().sendMessage(readableStatisticResult).queue();
            event.getTextChannel().sendMessage("L: " + readableStatisticResult.length()).queue();
        }

        private String getReadableStatisticsResult(List<WeeklyChannelStatistics> weeklyChannelStatisticsList) {
            long totalMessagesSent = getTotalMessagesSent(weeklyChannelStatisticsList);

            if (totalMessagesSent <= 0){
                return "No messages have been sent in any channels since I started tracking for this week!"
                        + Utils.NEWLINE
                        + "**NOTE**: Channels that I do not have access to is not tracked!";
            }

            // TODO: Optimise for word-count (2000 word limit); Break into multiple pages?

            StringBuilder statisticsResult = new StringBuilder();
            statisticsResult.append("```md" + Utils.NEWLINE);

            for (WeeklyChannelStatistics weeklyChannelStatistics: weeklyChannelStatisticsList) {
                double percentageOfMessagesInGuild = ((double) weeklyChannelStatistics.getNoOfMessagesSent() / (double) totalMessagesSent) * 100;
                statisticsResult.append(weeklyChannelStatistics.getChannelName()).append(" (").append(String.format("%.1f", percentageOfMessagesInGuild)).append("%)").append(Utils.NEWLINE);

                statisticsResult.append("====" + Utils.NEWLINE);

                statisticsResult.append(weeklyChannelStatistics.getNoOfMessagesSent()).append(" messages were sent.").append(Utils.NEWLINE);

                if (weeklyChannelStatistics.getNoOfMessagesSent() > 0) {
                    double percentageOfMessagesWereImage = ((double) weeklyChannelStatistics.getNoOfMessagesWithImage() / (double) weeklyChannelStatistics.getNoOfMessagesSent()) * 100;
                    statisticsResult.append(weeklyChannelStatistics.getNoOfMessagesWithImage()).append(" messages had images.").append(" (").append(String.format("%.1f",percentageOfMessagesWereImage)).append("%)").append(Utils.NEWLINE);
                }

                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(Utils.GLOBAL_DATE_FORMAT_PATTERN);
                String resetDateString = dateFormatter.format(weeklyChannelStatistics.getTrackedDate().plusDays(DAYS_TO_RESET));

                statisticsResult.append("> Next reset at: ").append(resetDateString).append(Utils.NEWLINE);

                statisticsResult.append(Utils.NEWLINE);
            }

            statisticsResult.append("```" + Utils.NEWLINE);
            statisticsResult.append("**NOTE**: Channels that I do not have access to is not tracked!");

            return statisticsResult.toString();
        }

        @Override
        public Module getParentModule() {
            return Statistics.this;
        }

        @Override
        public String getDocumentation() {
            return "Shows this week's statistics for all the channels.";
        }

        @Override
        public String getDocumentationCallsign() {
            return "week";
        }
    }

    private class ToggleStatisticsCommand implements  Command, AuthorityRequiring, Documented {

        @Override
        public String getCallsign() {
            return "toggle";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) throws Exception {
            if (event.getGuild() == null){
                event.getTextChannel().sendMessage("Statistic tracking can only be enabled in servers!").queue();
                return;
            }

            StatisticsSettings statisticsSettings = settingsDatabaseHandler.getStatisticSettingsInGuild(event.getGuild());
            statisticsSettings.setEnabled(!statisticsSettings.isEnabled());

            settingsDatabaseHandler.updateStatisticSettingsInGuild(event.getGuild(), statisticsSettings);

            if (statisticsSettings.isEnabled()){
                event.getTextChannel().sendMessage("Statistic tracking for this server is now enabled!").queue();
            } else {
                event.getTextChannel().sendMessage("Statistic tracking for this server is now disabled!").queue();
            }
        }

        @Override
        public Module getParentModule() {
            return Statistics.this;
        }

        @Override
        public Authority[] getRequiredAuthority() {
            return new Authority[] { new ToggleStatistics() };
        }

        @Override
        public String getDocumentation() {
            return "Use this command to enable/disable this bot to collect data about your server.";
        }

        @Override
        public String getDocumentationCallsign() {
            return "toggle";
        }
    }

    private class StatisticsCommand implements Command, Documented, CommandSet {

        private CommandHandler commands;

        StatisticsCommand(CarlBot carlbot) {
            commands = new CommandHandler(carlbot);

            commands.addCommand(new ToggleStatisticsCommand());
            commands.addCommand(new ShowWeeklyStatisticsCommand());
            commands.addCommand(new ShowLifetimeStatisticsCommand());
        }

        @Override
        public String getCallsign() {
            return "stats";
        }

        @Override
        public void runCommand(MessageReceivedEvent event, String rawString, List<String> tokens) {
            commands.runCommand(event, rawString, tokens);
        }

        @Override
        public Module getParentModule() {
            return Statistics.this;
        }

        @Override
        public String getDocumentation() {
            return "This module allows access the statistics of your server, and toggle collection of data in your server.";
        }

        @Override
        public String getDocumentationCallsign() {
            return "stats";
        }

        public Collection<Command> getCommands() {
            return commands.getCommands();
        }
    }

    ///region Utils

    private long getTotalMessagesSent(List<WeeklyChannelStatistics> allChannelStatistics){
        long totalMessagesSent = 0;
        for (WeeklyChannelStatistics weeklyChannelStatistics: allChannelStatistics) {
            totalMessagesSent += weeklyChannelStatistics.getNoOfMessagesSent();
        }

        return totalMessagesSent;
    }

    private boolean guildHasStatisticsEnabled(Guild guild) throws SQLException {
        StatisticsSettings statsSettings = settingsDatabaseHandler.getStatisticSettingsInGuild(guild);
        return statsSettings.isEnabled();
    }

    ///endregion
}
