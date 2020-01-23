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

/**
 * Handles the data on settings of how this bot collect statistics on the target server.
 */
public class SettingsDatabaseHandler {

    private Logger logger = LoggerFactory.getLogger(Statistics.class);

    ///region Table Names
    private static final String STATISTIC_SETTINGS_TABLE = "STATISTIC_SETTINGS";
    ///endregion

    ///region Columns Names
    private static final String SETTINGS_IDENTIFIER_COLUMN = "SETTINGS_NAME";

    private static final String SETTINGS_VALUE_COLUMN = "SETTINGS_VALUE";
    ///endregion

    private Persistence persistenceRef;

    private PersistentModule persistentModuleRef;

    public SettingsDatabaseHandler(Persistence _persistenceRef, PersistentModule _persistentModuleRef){
        persistenceRef = _persistenceRef;
        persistentModuleRef = _persistentModuleRef;
    }

    public StatisticsSettings getStatisticSettingsInGuild(Guild guild) throws SQLException {
        //TODO: Possible refactor?
        Table settingsTable = getSettingsTableInGuild(guild);
        StatisticsSettings result = new StatisticsSettings();

        // Set the 'isEnabled' state of the settings
        ResultSet isEnabledResult = settingsTable.select().where(SETTINGS_IDENTIFIER_COLUMN, "=", StatisticsSettings.IS_ENABLED).execute();
        if (isEnabledResult.next()){
            result.setEnabledByString(isEnabledResult.getString(SETTINGS_VALUE_COLUMN));
        } else {
            // Had no value; Insert with default value.
            settingsTable
                    .insert()
                    .set(SETTINGS_IDENTIFIER_COLUMN, StatisticsSettings.IS_ENABLED)
                    .set(SETTINGS_VALUE_COLUMN, result.isEnabledToString())
                    .execute();
        }

        return result;
    }

    public void updateStatisticSettingsInGuild(Guild guild, StatisticsSettings updatedSettings) throws SQLException {
        // TODO: Possible refactor?
        Table settingsTable = getSettingsTableInGuild(guild);

        // Update the 'isEnabled' property.
        settingsTable.update()
                .set(SETTINGS_VALUE_COLUMN, updatedSettings.isEnabledToString())
                .where(SETTINGS_IDENTIFIER_COLUMN, "=", StatisticsSettings.IS_ENABLED)
                .execute();
    }

    private Table getSettingsTableInGuild(Guild guild) throws SQLException {
        Table table = persistenceRef.getGuildTable(guild, persistentModuleRef);
        Table settingsTable = new Table(table, STATISTIC_SETTINGS_TABLE);

        if (!settingsTable.exists()) {
            settingsTable.create();

            settingsTable.alter().add()
                    .pushValue(SETTINGS_IDENTIFIER_COLUMN + " varchar")
                    .pushValue(SETTINGS_VALUE_COLUMN + " varchar")
                    .execute();
        }

        return settingsTable;
    }
}
