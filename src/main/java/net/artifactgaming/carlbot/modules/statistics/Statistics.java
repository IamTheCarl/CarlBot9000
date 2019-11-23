package net.artifactgaming.carlbot.modules.statistics;

import net.artifactgaming.carlbot.*;
import net.artifactgaming.carlbot.Module;
import net.artifactgaming.carlbot.modules.authority.Authority;
import net.artifactgaming.carlbot.modules.authority.AuthorityManagement;
import net.artifactgaming.carlbot.modules.authority.AuthorityRequiring;
import net.artifactgaming.carlbot.modules.persistence.Persistence;
import net.artifactgaming.carlbot.modules.persistence.PersistentModule;
import net.artifactgaming.carlbot.modules.selfdocumentation.Documented;
import net.artifactgaming.carlbot.modules.statistics.DatabaseSQL.SettingsDatabaseHandler;
import net.artifactgaming.carlbot.modules.statistics.DatabaseSQL.StatisticsDatabaseHandler;
import net.artifactgaming.carlbot.modules.statistics.authority.ToggleStatistics;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.graalvm.compiler.lir.sparc.SPARCTailDelayedLIRInstruction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.nio.ch.Util;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

public class Statistics implements Module, Documented, PersistentModule {

    private SettingsDatabaseHandler settingsDatabaseHandler;

    private StatisticsDatabaseHandler statisticsDatabaseHandler;

    private MessageStatisticCollector messageStatisticCollector;

    private Persistence persistence;

    private Logger logger = LoggerFactory.getLogger(Statistics.class);

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

        carlbot.addOnMessageReceivedListener(messageStatisticCollector);
    }

    @Override
    public Command[] getCommands(CarlBot carlbot) {
        return new Command[] {new StatisticsCommand(carlbot)};
    }

    private class ShowWeeklyStaisticsCommand implements Command, Documented {

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
        }

        private String getReadableStatisticsResult(List<WeeklyChannelStatistics> weeklyChannelStatisticsList){
            long totalMessagesSent = getTotalMessagesSent(weeklyChannelStatisticsList);

            StringBuilder statisticsResult = new StringBuilder();
            statisticsResult.append("```md" + Utils.NEWLINE);

            for (WeeklyChannelStatistics weeklyChannelStatistics: weeklyChannelStatisticsList) {
                double percentageOfMessagesInGuild = ((double) weeklyChannelStatistics.getNoOfMessagesSent() / (double) totalMessagesSent) * 100;
                statisticsResult.append(weeklyChannelStatistics.getChannelName()).append(" (").append(percentageOfMessagesInGuild).append("%)").append(Utils.NEWLINE);

                statisticsResult.append("======" + Utils.NEWLINE);

                statisticsResult.append(weeklyChannelStatistics.getNoOfMessagesSent()).append(" messages were sent.").append(Utils.NEWLINE);

                statisticsResult.append(weeklyChannelStatistics.getNoOfMessagesWithImage()).append(" of those messages contained images.").append(Utils.NEWLINE);
            }

            statisticsResult.append("```" + Utils.NEWLINE);
            statisticsResult.append("**NOTE**: Channels that I do not have access to is not tracked!");

            return statisticsResult.toString();
        }

        private long getTotalMessagesSent(List<WeeklyChannelStatistics> allChannelStatistics){
            long totalMessagesSent = 0;
            for (WeeklyChannelStatistics weeklyChannelStatistics: allChannelStatistics) {
                totalMessagesSent += weeklyChannelStatistics.getNoOfMessagesSent();
            }

            return totalMessagesSent;
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
            commands.addCommand(new ShowWeeklyStaisticsCommand());
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

    private boolean guildHasStatisticsEnabled(Guild guild) throws SQLException {
        StatisticsSettings statsSettings = settingsDatabaseHandler.getStatisticSettingsInGuild(guild);
        return statsSettings.isEnabled();
    }

    ///endregion
}
