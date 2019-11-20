package net.artifactgaming.carlbot.modules.statistics;

import net.artifactgaming.carlbot.*;
import net.artifactgaming.carlbot.Module;
import net.artifactgaming.carlbot.modules.authority.Authority;
import net.artifactgaming.carlbot.modules.authority.AuthorityManagement;
import net.artifactgaming.carlbot.modules.authority.AuthorityRequiring;
import net.artifactgaming.carlbot.modules.persistence.Persistence;
import net.artifactgaming.carlbot.modules.persistence.PersistentModule;
import net.artifactgaming.carlbot.modules.selfdocumentation.Documented;
import net.artifactgaming.carlbot.modules.statistics.DatabaseSQL.StatisticsDatabaseHandler;
import net.artifactgaming.carlbot.modules.statistics.authority.ToggleStatistics;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

public class Statistics implements Module, Documented {

    private StatisticsDatabaseHandler databaseHandler;

    private MessageStatisticCollector messageStatisticCollector;

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
        databaseHandler = new StatisticsDatabaseHandler(carlbot);

        messageStatisticCollector = new MessageStatisticCollector(databaseHandler);

        carlbot.addOnMessageReceivedListener(messageStatisticCollector);
    }

    @Override
    public Command[] getCommands(CarlBot carlbot) {
        return new Command[] {new StatisticsCommand(carlbot)};
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

            StatisticsSettings statisticsSettings = databaseHandler.getStatisticSettingsInGuild(event.getGuild());
            statisticsSettings.setEnabled(!statisticsSettings.isEnabled());

            databaseHandler.updateStatisticSettingsInGuild(event.getGuild(), statisticsSettings);

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
}
