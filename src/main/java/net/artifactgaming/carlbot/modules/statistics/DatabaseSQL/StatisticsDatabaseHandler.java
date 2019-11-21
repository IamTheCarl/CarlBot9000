package net.artifactgaming.carlbot.modules.statistics.DatabaseSQL;

import net.artifactgaming.carlbot.CarlBot;
import net.artifactgaming.carlbot.modules.persistence.Persistence;
import net.artifactgaming.carlbot.modules.persistence.PersistentModule;
import net.artifactgaming.carlbot.modules.persistence.Table;
import net.artifactgaming.carlbot.modules.statistics.Statistics;
import net.artifactgaming.carlbot.modules.statistics.StatisticsSettings;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Invite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;

public class StatisticsDatabaseHandler {

    private Logger logger = LoggerFactory.getLogger(Statistics.class);

    private Persistence persistenceRef;

    private PersistentModule persistentModuleRef;

    public StatisticsDatabaseHandler(Persistence _persistenceRef, PersistentModule _persistentModuleRef) {
        persistenceRef = _persistenceRef;
        persistentModuleRef = _persistentModuleRef;
    }
}
