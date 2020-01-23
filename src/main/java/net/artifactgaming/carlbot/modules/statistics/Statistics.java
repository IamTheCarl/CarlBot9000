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
import net.artifactgaming.carlbot.modules.statistics.StatisticList.StatisticListMessage;
import net.artifactgaming.carlbot.modules.statistics.StatisticList.StatisticPage;
import net.artifactgaming.carlbot.modules.statistics.authority.ToggleStatistics;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;

public class Statistics implements Module, Documented, PersistentModule {

    private SettingsDatabaseHandler settingsDatabaseHandler;

    private StatisticsDatabaseHandler statisticsDatabaseHandler;

    private MessageStatisticCollector messageStatisticCollector;

    private StatisticListMessageReactionListener statisticListMessageReactionListener;

    private Persistence persistence;

    private Logger logger = LoggerFactory.getLogger(Statistics.class);

    private WeeklyStatisticsResetter weeklyStatisticsResetter;

    private Timer weeklyResetTimer;

    public Statistics(){
        CarlBot.addOnCarlbotReadyListener(new SetupWeeklyStatisticsResetter());
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

        statisticListMessageReactionListener = new StatisticListMessageReactionListener();
        carlbot.addOnMessageReactionListener(statisticListMessageReactionListener);


        // TODO: Setup a repeating timer to constantly check if weekly statistics need resetting.

        carlbot.addOnMessageReceivedListener(messageStatisticCollector);
    }

    private class SetupWeeklyStatisticsResetter implements OnCarlBotReady {

        @Override
        public void onCarlBotReady(ReadyEvent event) {
            weeklyStatisticsResetter = new WeeklyStatisticsResetter(event.getJDA(), logger, statisticsDatabaseHandler);
            weeklyResetTimer = new Timer();

            weeklyResetTimer.scheduleAtFixedRate(
                    new TimerTask() {
                        @Override
                        public void run() {
                            weeklyStatisticsResetter.resetOverdueStatistics();
                        }
                    },
                    0,
                    86400000 //24 Hour in miliseconds
            );
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

            Message statisticMessage = event.getChannel().sendMessage(
                    "Fetching statistics...").complete();

            List<LifetimeChannelStatistics> lifetimeChannelStatistics = statisticsDatabaseHandler.getLifetimeGuildStatistics(event.getGuild());

            ArrayList<StatisticPage> statisticPages = separateByPages(lifetimeChannelStatistics);

            if (statisticPages.size() == 1){
                statisticMessage.editMessage(statisticPages.get(0).getPageContent()).queue();
            } else if (statisticPages.size() == 0){
                statisticMessage.editMessage("There is no statistics to show.. yet." + Utils.NEWLINE + "**NOTE**: Channels that I do not have access to is not tracked!").queue();
            } else {
                StatisticListMessage statisticListMessage = new StatisticListMessage(statisticPages, statisticListMessageReactionListener, statisticMessage.getId());

                statisticListMessageReactionListener.addStatisticListMessageToListener(statisticListMessage);

                statisticMessage.editMessage(statisticListMessage.getCurrentPage()).queue();

                statisticMessage.addReaction(StatisticListMessageReactionListener.PREVIOUS_EMOTE_NAME).complete();
                statisticMessage.addReaction(StatisticListMessageReactionListener.NEXT_EMOTE_NAME).queue();
            }
        }

        private ArrayList<StatisticPage> separateByPages(List<LifetimeChannelStatistics> lifetimeChannelStatisticsList){
            ///region Local_Function

            Supplier<StringBuilder> createNewPageTemplate = () -> {
                StringBuilder template = new StringBuilder();
                template.append("```md" + Utils.NEWLINE);
                return template;
            };

            ///endregion

            // TODO: Possible refecctor? (Too dependent on string length)
            ArrayList<StatisticPage> result = new ArrayList<>();

            StringBuilder stringBuilder = createNewPageTemplate.get();

            for (LifetimeChannelStatistics statistic: lifetimeChannelStatisticsList) {
                String toAppend = toReadableString(statistic);

                // If appending it will exceed character count.
                if (stringBuilder.length() + toAppend.length() >= StatisticPage.maxCharacterCountInPage){
                    // End this page, and create new page instead.
                    stringBuilder.append("```**NOTE**: Channels that I do not have access to is not tracked!");
                    result.add(new StatisticPage(stringBuilder.toString()));

                    stringBuilder = createNewPageTemplate.get();
                }

                stringBuilder.append(toAppend);
            }

            // Last page has statistic
            if (stringBuilder.length() > 10){
                stringBuilder.append("```**NOTE**: Channels that I do not have access to is not tracked!");
                result.add(new StatisticPage(stringBuilder.toString()));
            }

            return result;
        }

        private String toReadableString(LifetimeChannelStatistics channelStatistics){
            return Utils.NEWLINE +
                    channelStatistics.getChannelName() + " (" + String.format("%.1f", channelStatistics.getPercentageOfTotalMessagesSent()) + "%)" + Utils.NEWLINE +
                    "====" + Utils.NEWLINE +
                    String.format("%.1f", channelStatistics.getPercentageOfMessagesContainImages()) + "% contains images." + Utils.NEWLINE;
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

            Message statisticMessage = event.getChannel().sendMessage(
                    "Fetching statistics...").complete();

            List<WeeklyChannelStatistics> weeklyChannelStatistics = statisticsDatabaseHandler.getWeeklyGuildStatistics(event.getGuild());

            ArrayList<StatisticPage> statisticPages = separateByPages(weeklyChannelStatistics);

            if (statisticPages.size() == 1){
                statisticMessage.editMessage(statisticPages.get(0).getPageContent()).queue();
            } else if (statisticPages.size() == 0){
                statisticMessage.editMessage("There is no statistics to show for this week.. yet." + Utils.NEWLINE + "**NOTE**: Channels that I do not have access to is not tracked!").queue();
            } else {
                StatisticListMessage statisticListMessage = new StatisticListMessage(statisticPages, statisticListMessageReactionListener, statisticMessage.getId());

                statisticListMessageReactionListener.addStatisticListMessageToListener(statisticListMessage);

                statisticMessage.editMessage(statisticListMessage.getCurrentPage()).queue();

                statisticMessage.addReaction(StatisticListMessageReactionListener.PREVIOUS_EMOTE_NAME).complete();
                statisticMessage.addReaction(StatisticListMessageReactionListener.NEXT_EMOTE_NAME).queue();
            }
        }

        private ArrayList<StatisticPage> separateByPages(List<WeeklyChannelStatistics> weeklyChannelStatisticsList){
            ///region Local_Function

            Supplier<StringBuilder> createNewPageTemplate = () -> {
                StringBuilder template = new StringBuilder();
                template.append("```md" + Utils.NEWLINE);
                return template;
            };

            ///endregion

            // TODO: Possible refecctor? (Too dependent on string length)
            ArrayList<StatisticPage> result = new ArrayList<>();

            StringBuilder stringBuilder = createNewPageTemplate.get();

            long totalMessagesSent = getTotalMessagesSent(weeklyChannelStatisticsList);

            if (totalMessagesSent == 0){
                return result;
            }

            for (WeeklyChannelStatistics statistic: weeklyChannelStatisticsList) {
                String toAppend = toReadableString(totalMessagesSent, statistic);

                // If appending it will exceed character count.
                if (stringBuilder.length() + toAppend.length() >= StatisticPage.maxCharacterCountInPage){
                    // End this page, and create new page instead.
                    stringBuilder.append("```**NOTE**: Channels that I do not have access to is not tracked!");
                    result.add(new StatisticPage(stringBuilder.toString()));

                    stringBuilder = createNewPageTemplate.get();
                }

                stringBuilder.append(toAppend);
            }

            // Last page has statistic
            if (stringBuilder.length() > 10){
                stringBuilder.append("```**NOTE**: Channels that I do not have access to is not tracked!");
                result.add(new StatisticPage(stringBuilder.toString()));
            }

            return result;
        }

        private String toReadableString(long totalMessagesSent, WeeklyChannelStatistics weeklyChannelStatistics){
            StringBuilder statisticsResult = new StringBuilder();

            double percentageOfMessagesInGuild = ((double) weeklyChannelStatistics.getNoOfMessagesSent() / (double) totalMessagesSent) * 100;
            statisticsResult.append(weeklyChannelStatistics.getChannelName()).append(" (").append(String.format("%.1f", percentageOfMessagesInGuild)).append("%)").append(Utils.NEWLINE);

            statisticsResult.append("====" + Utils.NEWLINE);

            statisticsResult.append(weeklyChannelStatistics.getNoOfMessagesSent()).append(" messages were sent.").append(Utils.NEWLINE);

            if (weeklyChannelStatistics.getNoOfMessagesSent() > 0) {
                double percentageOfMessagesWereImage = ((double) weeklyChannelStatistics.getNoOfMessagesWithImage() / (double) weeklyChannelStatistics.getNoOfMessagesSent()) * 100;
                statisticsResult.append(weeklyChannelStatistics.getNoOfMessagesWithImage()).append(" messages had images.").append(" (").append(String.format("%.1f",percentageOfMessagesWereImage)).append("%)").append(Utils.NEWLINE);
            }

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(Utils.GLOBAL_DATE_FORMAT_PATTERN);
            String resetDateString = dateFormatter.format(weeklyChannelStatistics.getTrackedDate().plusDays(WeeklyStatisticsResetter.DAYS_TO_RESET));

            statisticsResult.append("> Next reset at: ").append(resetDateString).append(Utils.NEWLINE);

            statisticsResult.append(Utils.NEWLINE);

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
