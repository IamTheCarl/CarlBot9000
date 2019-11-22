package net.artifactgaming.carlbot.modules.statistics.DatabaseSQL;

import net.artifactgaming.carlbot.CarlBot;
import net.artifactgaming.carlbot.modules.persistence.Persistence;
import net.artifactgaming.carlbot.modules.persistence.PersistentModule;
import net.artifactgaming.carlbot.modules.persistence.Table;
import net.artifactgaming.carlbot.modules.statistics.Statistics;
import net.artifactgaming.carlbot.modules.statistics.StatisticsSettings;
import net.artifactgaming.carlbot.modules.statistics.WeeklyChannelStatistics;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Invite;
import net.dv8tion.jda.core.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class StatisticsDatabaseHandler {

    ///region Table names

    private static final String WEEKLY_STATISTICS_TABLE = "WEEKLY_STATISTICS";

    ///endregion

    private Logger logger = LoggerFactory.getLogger(Statistics.class);

    private Persistence persistenceRef;

    private PersistentModule persistentModuleRef;

    private WeeklyDatabaseHandler weeklyDatabaseHandler;

    public StatisticsDatabaseHandler(Persistence _persistenceRef, PersistentModule _persistentModuleRef) {
        persistenceRef = _persistenceRef;
        persistentModuleRef = _persistentModuleRef;

        weeklyDatabaseHandler = new WeeklyDatabaseHandler();
    }

    private class WeeklyDatabaseHandler {

        private List<WeeklyChannelStatistics> getWeeklyGuildStatistics(Guild guild) throws SQLException, ParseException {
            ArrayList<WeeklyChannelStatistics> weeklyChannelStatisticsList = new ArrayList<>();
            Table weeklyStatisticsTable = getWeeklyStatisticsTableInGuild(guild);

            ResultSet result = weeklyStatisticsTable.select().execute();

            while (result.next()){
                String channelID = result.getString(WeeklyChannelStatistics.CHANNEL_ID);
                String channelName = result.getString(WeeklyChannelStatistics.CHANNEL_NAME);
                int noOfMessagesSent = result.getInt(WeeklyChannelStatistics.NO_OF_MESSAGES_SENT);
                int noOfMessagesSentWithImage = result.getInt(WeeklyChannelStatistics.NO_OF_MESSAGES_WITH_IMAGE);
                String trackedDateString = result.getString(WeeklyChannelStatistics.TRACKED_DATE);

                // Convert the date string as a 'Date' type.
                DateFormat dateFormatter = new SimpleDateFormat(WeeklyChannelStatistics.DATE_FORMAT_PATTERN);
                Date trackedDate = dateFormatter.parse(trackedDateString);

                WeeklyChannelStatistics weeklyChannelStatistics = new WeeklyChannelStatistics(channelID, channelName, noOfMessagesSent, noOfMessagesSentWithImage, trackedDate);
                weeklyChannelStatisticsList.add(weeklyChannelStatistics);
            }

            return weeklyChannelStatisticsList;
        }

        private WeeklyChannelStatistics getWeeklyChannelStatistics(Guild guild, TextChannel channel) throws SQLException, ParseException {
            Table weeklyStatisticsTable = getWeeklyStatisticsTableInGuild(guild);

            ResultSet result = weeklyStatisticsTable.select().where(WeeklyChannelStatistics.CHANNEL_ID, "=", channel.getId()).execute();

            WeeklyChannelStatistics weeklyChannelStatistics;

            if (result.next()){
                String channelID = result.getString(WeeklyChannelStatistics.CHANNEL_ID);
                String channelName = result.getString(WeeklyChannelStatistics.CHANNEL_NAME);
                int noOfMessagesSent = result.getInt(WeeklyChannelStatistics.NO_OF_MESSAGES_SENT);
                int noOfMessagesSentWithImage = result.getInt(WeeklyChannelStatistics.NO_OF_MESSAGES_WITH_IMAGE);
                String trackedDateString = result.getString(WeeklyChannelStatistics.TRACKED_DATE);

                // Convert the date string as a 'Date' type.
                DateFormat dateFormatter = new SimpleDateFormat(WeeklyChannelStatistics.DATE_FORMAT_PATTERN);
                Date trackedDate = dateFormatter.parse(trackedDateString);

                weeklyChannelStatistics = new WeeklyChannelStatistics(channelID, channelName, noOfMessagesSent, noOfMessagesSentWithImage, trackedDate);
            } else {
                insertNewTextChannelIntoWeeklyStatisticsTable(guild, channel);

                weeklyChannelStatistics = new WeeklyChannelStatistics(channel.getId(), channel.getName());
            }

            return weeklyChannelStatistics;
        }

        private void updateWeeklyChannelStatistics(Guild guild, TextChannel channel, WeeklyChannelStatistics weeklyChannelStatistics) throws SQLException {
            Table weeklyStatisticsTable = getWeeklyStatisticsTableInGuild(guild);

            DateFormat dateFormatter = new SimpleDateFormat(WeeklyChannelStatistics.DATE_FORMAT_PATTERN);
            String dateAsString = dateFormatter.format(weeklyChannelStatistics.getTrackedDate());

            weeklyStatisticsTable.update()
                    .where(WeeklyChannelStatistics.CHANNEL_ID, "=", weeklyChannelStatistics.getChannelID())
                    .set(WeeklyChannelStatistics.CHANNEL_NAME, weeklyChannelStatistics.getChannelName())
                    .set(WeeklyChannelStatistics.NO_OF_MESSAGES_SENT, Integer.toString(weeklyChannelStatistics.getNoOfMessagesSent()))
                    .set(WeeklyChannelStatistics.NO_OF_MESSAGES_WITH_IMAGE, Integer.toString(weeklyChannelStatistics.getNoOfMessagesWithImage()))
                    .set(WeeklyChannelStatistics.TRACKED_DATE, dateAsString)
                    .execute();
        }

        private void insertNewTextChannelIntoWeeklyStatisticsTable(Guild guild, TextChannel channel) throws SQLException {
            Table weeklyStatisticsTable = getWeeklyStatisticsTableInGuild(guild);

            Date currentDate = Calendar.getInstance().getTime();
            DateFormat dateFormatter = new SimpleDateFormat(WeeklyChannelStatistics.DATE_FORMAT_PATTERN);

            weeklyStatisticsTable.insert()
                    .set(WeeklyChannelStatistics.CHANNEL_ID, channel.getId())
                    .set(WeeklyChannelStatistics.CHANNEL_NAME, channel.getName())
                    .set(WeeklyChannelStatistics.NO_OF_MESSAGES_SENT, "0")
                    .set(WeeklyChannelStatistics.NO_OF_MESSAGES_WITH_IMAGE, "0")
                    .set(WeeklyChannelStatistics.TRACKED_DATE, dateFormatter.format(currentDate))
                    .execute();
        }

        private Table getWeeklyStatisticsTableInGuild(Guild guild) throws SQLException {
            Table table = persistenceRef.getGuildTable(guild, persistentModuleRef);
            Table weeklyStatisticsTable = new Table(table, WEEKLY_STATISTICS_TABLE);

            if (!weeklyStatisticsTable.exists()) {
                weeklyStatisticsTable.create();

                weeklyStatisticsTable.alter().add()
                        .pushValue(WeeklyChannelStatistics.CHANNEL_ID + " varchar")
                        .pushValue(WeeklyChannelStatistics.CHANNEL_NAME + " varchar")
                        .pushValue(WeeklyChannelStatistics.NO_OF_MESSAGES_SENT + " int")
                        .pushValue(WeeklyChannelStatistics.NO_OF_MESSAGES_WITH_IMAGE + " int")
                        .pushValue(WeeklyChannelStatistics.TRACKED_DATE + " varchar")
                        .execute();
            }

            return weeklyStatisticsTable;
        }
    }

    public List<WeeklyChannelStatistics> getWeeklyGuildStatistics(Guild guild) throws SQLException, ParseException {
        return weeklyDatabaseHandler.getWeeklyGuildStatistics(guild);
    }

    public WeeklyChannelStatistics getWeeklyChannelStatistics(Guild guild, TextChannel channel) throws SQLException, ParseException {
        return weeklyDatabaseHandler.getWeeklyChannelStatistics(guild, channel);
    }

    public void updateWeeklyChannelStatistics(Guild guild, TextChannel channel, WeeklyChannelStatistics weeklyChannelStatistics) throws SQLException {
        weeklyDatabaseHandler.updateWeeklyChannelStatistics(guild, channel, weeklyChannelStatistics);
    }
}
