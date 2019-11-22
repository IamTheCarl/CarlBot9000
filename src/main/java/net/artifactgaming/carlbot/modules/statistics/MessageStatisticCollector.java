package net.artifactgaming.carlbot.modules.statistics;

import net.artifactgaming.carlbot.listeners.MessageReader;
import net.artifactgaming.carlbot.modules.statistics.DatabaseSQL.SettingsDatabaseHandler;
import net.artifactgaming.carlbot.modules.statistics.DatabaseSQL.StatisticsDatabaseHandler;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import org.h2.engine.Setting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class MessageStatisticCollector implements MessageReader {

    private Logger logger = LoggerFactory.getLogger(Statistics.class);

    private SettingsDatabaseHandler settingsDatabaseHandler;

    private StatisticsDatabaseHandler statisticsDatabaseHandler;

    MessageStatisticCollector(SettingsDatabaseHandler _settingsDatabaseHandler, StatisticsDatabaseHandler _statisticsDatabaseHandler){
        settingsDatabaseHandler = _settingsDatabaseHandler;
        statisticsDatabaseHandler = _statisticsDatabaseHandler;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (messageFromBotOrNonGuild(event)){
            return;
        }
        try {
            StatisticsSettings statsSettings = settingsDatabaseHandler.getStatisticSettingsInGuild(event.getGuild());

            if (statsSettings.isEnabled()){

            }

        } catch (SQLException e){
            logger.error(e.getMessage());
        }
    }

    private boolean messageFromBotOrNonGuild(MessageReceivedEvent event){
        return event.getAuthor().isBot() || event.getGuild() == null;
    }
}
