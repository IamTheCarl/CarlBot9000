package net.artifactgaming.carlbot.modules.statistics;

import net.artifactgaming.carlbot.listeners.MessageReader;
import net.artifactgaming.carlbot.modules.statistics.DatabaseSQL.StatisticsDatabaseHandler;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class MessageStatisticCollector implements MessageReader {

    private Logger logger = LoggerFactory.getLogger(Statistics.class);

    private StatisticsDatabaseHandler databaseHandler;

    MessageStatisticCollector(StatisticsDatabaseHandler statisticsDatabaseHandler){
        databaseHandler = statisticsDatabaseHandler;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (messageFromBotOrNonGuild(event)){
            return;
        }
        try {
            StatisticsSettings statsSettings = databaseHandler.getStatisticSettingsInGuild(event.getGuild());

            if (statsSettings.isEnabled()){
                event.getTextChannel().sendMessage("OK").queue(); // DEBUG!!!
            }

        } catch (SQLException e){
            logger.error(e.getMessage());
        }
    }

    private boolean messageFromBotOrNonGuild(MessageReceivedEvent event){
        return event.getAuthor().isBot() || event.getGuild() == null;
    }
}
