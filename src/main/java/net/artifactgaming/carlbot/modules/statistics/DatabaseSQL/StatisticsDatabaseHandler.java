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

public class StatisticsDatabaseHandler implements PersistentModule {

    private static Logger logger = LoggerFactory.getLogger(Statistics.class);

    ///region Columns names for settings
    private static final String SETTINGS_IDENTIFIER = "SETTINGS_NAME";

    private static final String SETTINGS_VALUE = "SETTINGS_VALUE";
    ///endregion

    private Persistence persistence;

    public StatisticsDatabaseHandler(CarlBot carlBot){
        // Get the persistence module.
        persistence = (Persistence) carlBot.getModule(Persistence.class);

        if (persistence == null) {
            logger.error("Persistence module is not loaded.");
            carlBot.crash();
        }
    }

    // TODO: Possible refactor? (Could split into 2 database handler. 1 for settings and one for main)

    ///Region Settings_Database

    public StatisticsSettings getStatisticSettingsInGuild(Guild guild) throws SQLException {
        //TODO: Possible refactor?
        Table settingsTable = getSettingsTableInGuild(guild);
        StatisticsSettings result = new StatisticsSettings();

        // Set the 'isEnabled' state of the settings
        ResultSet isEnabledResult = settingsTable.select().where(SETTINGS_IDENTIFIER, "=", StatisticsSettings.IS_ENABLED).execute();
        if (isEnabledResult.next()){
            result.setEnabledByString(isEnabledResult.getString(SETTINGS_VALUE));
        }

        return result;
    }

    public void updateStatisticSettingsInGuild(Guild guild, StatisticsSettings updatedSettings) throws SQLException {
        // TODO: Possible refactor?
        Table settingsTable = getSettingsTableInGuild(guild);

        // Update the 'isEnabled' property.
        settingsTable.update()
                .set(SETTINGS_VALUE, updatedSettings.isEnabledToString())
                .where(SETTINGS_IDENTIFIER, "=", StatisticsSettings.IS_ENABLED)
                .execute();
    }

    private Table getSettingsTableInGuild(Guild guild) throws SQLException {
        Table table = persistence.getGuildTable(guild, this);
        Table settingsTable = new Table(table, "statisticSettings");

        if (!settingsTable.exists()) {
            settingsTable.create();

            settingsTable.alter().add()
                    .pushValue(SETTINGS_IDENTIFIER + " varchar").pushValue(SETTINGS_VALUE + " varchar")
                    .execute();
        }

        return settingsTable;
    }

    ///Endregion

    private Table getGuildTable(Guild guild) throws SQLException {
        Table table = persistence.getGuildTable(guild, this);
        Table quoteTable = new Table(table, "quotes");

        if (!quoteTable.exists()) {
            quoteTable.create();

            quoteTable.alter().add()
                    .pushValue("owner varchar").pushValue("owner_name varchar")
                    .pushValue("key varchar").pushValue("quote varchar")
                    .execute();
        }

        return quoteTable;
    }
}
