package net.artifactgaming.carlbot.modules.statistics.DatabaseSQL;

import net.artifactgaming.carlbot.Utils;
import net.artifactgaming.carlbot.modules.persistence.Persistence;
import net.artifactgaming.carlbot.modules.persistence.PersistentModule;
import net.artifactgaming.carlbot.modules.persistence.Table;
import net.artifactgaming.carlbot.modules.statistics.*;
import net.artifactgaming.carlbot.modules.statistics.ChannelStatistic.LifetimeChannelStatistics;
import net.artifactgaming.carlbot.modules.statistics.ChannelStatistic.WeeklyChannelStatistics;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import org.graalvm.compiler.api.replacements.Snippet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class StatisticsDatabaseHandler {

    private Logger logger = LoggerFactory.getLogger(Statistics.class);

    private Persistence persistenceRef;

    private PersistentModule persistentModuleRef;

    private WeeklyDatabaseHandler weeklyDatabaseHandler;
    private LifetimeDatabaseHandler lifetimeDatabaseHandler;

    public StatisticsDatabaseHandler(Persistence _persistenceRef, PersistentModule _persistentModuleRef) {
        persistenceRef = _persistenceRef;
        persistentModuleRef = _persistentModuleRef;

        weeklyDatabaseHandler = new WeeklyDatabaseHandler();
        lifetimeDatabaseHandler = new LifetimeDatabaseHandler();
    }

    private class LifetimeDatabaseHandler {
        ///region Table names
        private static final String LIFETIME_STATISTICS_TABLE = "LIFETIME_STATISTICS";
        ///endregion

        private List<LifetimeChannelStatistics> getLifetimeGuildStatistics(Guild guild) throws SQLException {
            ArrayList<LifetimeChannelStatistics> lifetimeChannelStatisticsList = new ArrayList<>();
            Table lifetimeStatisticsTable = getLifetimeStatisticsTableInGuild(guild);

            ResultSet result = lifetimeStatisticsTable.select().execute();

            while (result.next()){
                String channelID = result.getString(LifetimeChannelStatistics.CHANNEL_ID);
                String channelName = result.getString(LifetimeChannelStatistics.CHANNEL_NAME);
                double percentageMessagesSent = result.getDouble(LifetimeChannelStatistics.PERCENT_OF_MESSAGES_SENT);
                double percentageMessageSentContainImage = result.getDouble(LifetimeChannelStatistics.PERCENT_OF_MESSAGES_WITH_IMAGE);

                LifetimeChannelStatistics lifetimeChannelStatistics = new LifetimeChannelStatistics(channelID, channelName, percentageMessagesSent, percentageMessageSentContainImage);
                lifetimeChannelStatisticsList.add(lifetimeChannelStatistics);
            }

            return lifetimeChannelStatisticsList;
        }

        private LifetimeChannelStatistics getLifetimeChannelStatistics(Guild guild, TextChannel channel) throws SQLException {
            Table lifetimeStatisticsTable = getLifetimeStatisticsTableInGuild(guild);

            ResultSet result = lifetimeStatisticsTable.select()
                    .where(LifetimeChannelStatistics.CHANNEL_ID, "=", channel.getId())
                    .execute();

            LifetimeChannelStatistics lifetimeChannelStatistics;
            if (result.next()){
                String channelID = result.getString(LifetimeChannelStatistics.CHANNEL_ID);
                String channelName = result.getString(LifetimeChannelStatistics.CHANNEL_NAME);
                double percentageMessagesSent = result.getDouble(LifetimeChannelStatistics.PERCENT_OF_MESSAGES_SENT);
                double percentageMessageSentContainImage = result.getDouble(LifetimeChannelStatistics.PERCENT_OF_MESSAGES_WITH_IMAGE);

                lifetimeChannelStatistics = new LifetimeChannelStatistics(channelID, channelName, percentageMessagesSent, percentageMessageSentContainImage);
            } else {
                lifetimeChannelStatistics = new LifetimeChannelStatistics(channel.getId(), channel.getName());

                insertNewChannelIntoLifetimeStatisticsTable(channel);
            }

            return lifetimeChannelStatistics;
        }

        private void updateLifetimeChannelStatistics(Guild guild, LifetimeChannelStatistics lifetimeChannelStatistics) throws SQLException {
            Table lifetimeStatisticsTable = getLifetimeStatisticsTableInGuild(guild);

            lifetimeStatisticsTable.update()
                    .where(LifetimeChannelStatistics.CHANNEL_ID, "=", lifetimeChannelStatistics.getChannelID())
                    .set(LifetimeChannelStatistics.CHANNEL_NAME, lifetimeChannelStatistics.getChannelName())
                    .set(LifetimeChannelStatistics.PERCENT_OF_MESSAGES_SENT, String.valueOf(lifetimeChannelStatistics.getPercentageOfTotalMessagesSent()))
                    .set(LifetimeChannelStatistics.PERCENT_OF_MESSAGES_WITH_IMAGE, String.valueOf(lifetimeChannelStatistics.getPercentageOfMessagesContainImages()))
                    .execute();
        }

        private void insertNewChannelIntoLifetimeStatisticsTable(TextChannel channel) throws SQLException{
            Table lifetimeStatisticsTable = getLifetimeStatisticsTableInGuild(channel.getGuild());

            lifetimeStatisticsTable.insert()
                    .set(LifetimeChannelStatistics.CHANNEL_ID, channel.getId())
                    .set(LifetimeChannelStatistics.CHANNEL_NAME, channel.getName())
                    .set(LifetimeChannelStatistics.PERCENT_OF_MESSAGES_SENT, "0")
                    .set(LifetimeChannelStatistics.PERCENT_OF_MESSAGES_WITH_IMAGE, "0")
                    .execute();
        }

        private void insertNewChannelStatisticsIntoLifetimeStatisticsTable(Guild guild, LifetimeChannelStatistics lifetimeChannelStatistics) throws SQLException {
            Table lifetimeStatisticsTable = getLifetimeStatisticsTableInGuild(guild);

            lifetimeStatisticsTable.insert()
                    .set(LifetimeChannelStatistics.CHANNEL_ID, lifetimeChannelStatistics.getChannelID())
                    .set(LifetimeChannelStatistics.CHANNEL_NAME, lifetimeChannelStatistics.getChannelName())
                    .set(LifetimeChannelStatistics.PERCENT_OF_MESSAGES_SENT, String.valueOf(lifetimeChannelStatistics.getPercentageOfTotalMessagesSent()))
                    .set(LifetimeChannelStatistics.PERCENT_OF_MESSAGES_WITH_IMAGE, String.valueOf(lifetimeChannelStatistics.getPercentageOfMessagesContainImages()))
                    .execute();
        }

        private Table getLifetimeStatisticsTableInGuild(Guild guild) throws SQLException {
            Table table = persistenceRef.getGuildTable(guild, persistentModuleRef);
            Table lifetimeStatisticsTable = new Table(table, LIFETIME_STATISTICS_TABLE);

            if (!lifetimeStatisticsTable.exists()) {
                lifetimeStatisticsTable.create();

                lifetimeStatisticsTable.alter().add()
                        .pushValue(LifetimeChannelStatistics.CHANNEL_ID + " varchar")
                        .pushValue(LifetimeChannelStatistics.CHANNEL_NAME + " varchar")
                        .pushValue(LifetimeChannelStatistics.PERCENT_OF_MESSAGES_SENT + " float")
                        .pushValue(LifetimeChannelStatistics.PERCENT_OF_MESSAGES_WITH_IMAGE + " float")
                        .execute();
            }

            return lifetimeStatisticsTable;
        }

        private void deleteLifetimeStatisticsChannel(Guild guild, String channelID) throws SQLException {
            Table table = persistenceRef.getGuildTable(guild, persistentModuleRef);

            table.delete()
                    .where(LifetimeChannelStatistics.CHANNEL_ID, "=", channelID)
                    .execute();
        }
    }

    private class WeeklyDatabaseHandler {

        ///region Table names
        private static final String WEEKLY_STATISTICS_TABLE = "WEEKLY_STATISTICS";
        ///endregion

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

                // Convert the date string as a 'LocalDate' type.
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Utils.GLOBAL_DATE_FORMAT_PATTERN);
                LocalDate trackedDate = LocalDate.parse(trackedDateString, formatter);

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
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Utils.GLOBAL_DATE_FORMAT_PATTERN);
                LocalDate trackedDate = LocalDate.parse(trackedDateString, formatter);

                weeklyChannelStatistics = new WeeklyChannelStatistics(channelID, channelName, noOfMessagesSent, noOfMessagesSentWithImage, trackedDate);
            } else {
                insertNewTextChannelIntoWeeklyStatisticsTable(guild, channel);

                weeklyChannelStatistics = new WeeklyChannelStatistics(channel.getId(), channel.getName());
            }

            return weeklyChannelStatistics;
        }

        private void updateWeeklyChannelStatistics(Guild guild, WeeklyChannelStatistics weeklyChannelStatistics) throws SQLException {
            Table weeklyStatisticsTable = getWeeklyStatisticsTableInGuild(guild);

            DateTimeFormatter  dateFormatter = DateTimeFormatter .ofPattern(Utils.GLOBAL_DATE_FORMAT_PATTERN);
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
            DateFormat dateFormatter = new SimpleDateFormat(Utils.GLOBAL_DATE_FORMAT_PATTERN);

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

        private void deleteWeeklyStatisticsChannel(Guild guild, String channelID) throws SQLException {
            Table table = persistenceRef.getGuildTable(guild, persistentModuleRef);

            table.delete()
                    .where(WeeklyChannelStatistics.CHANNEL_ID, "=", channelID)
                    .execute();
        }
    }

    public List<WeeklyChannelStatistics> getWeeklyGuildStatistics(@Snippet.NonNullParameter Guild guild) throws SQLException, ParseException {
        return weeklyDatabaseHandler.getWeeklyGuildStatistics(guild);
    }

    public WeeklyChannelStatistics getWeeklyChannelStatistics(@Snippet.NonNullParameter Guild guild, @Snippet.NonNullParameter TextChannel channel) throws SQLException, ParseException {
        return weeklyDatabaseHandler.getWeeklyChannelStatistics(guild, channel);
    }

    public void updateWeeklyChannelStatistics(@Snippet.NonNullParameter Guild guild, @Snippet.NonNullParameter WeeklyChannelStatistics weeklyChannelStatistics) throws SQLException {
        weeklyDatabaseHandler.updateWeeklyChannelStatistics(guild, weeklyChannelStatistics);
    }

    public void deleteWeeklyChannelStatistics(@Snippet.NonNullParameter Guild guild, @Snippet.NonNullParameter String channelID) throws SQLException {
        weeklyDatabaseHandler.deleteWeeklyStatisticsChannel(guild, channelID);
    }

    public List<LifetimeChannelStatistics> getLifetimeGuildStatistics(@Snippet.NonNullParameter Guild guild) throws SQLException {
        return lifetimeDatabaseHandler.getLifetimeGuildStatistics(guild);
    }

    public LifetimeChannelStatistics getLifetimeChannelStatistics(@Snippet.NonNullParameter Guild guild, @Snippet.NonNullParameter TextChannel channel) throws SQLException {
        return lifetimeDatabaseHandler.getLifetimeChannelStatistics(guild, channel);
    }

    public void updateLifetimeChannelStatistics(@Snippet.NonNullParameter Guild guild, @Snippet.NonNullParameter LifetimeChannelStatistics lifetimeChannelStatistics) throws SQLException {
        lifetimeDatabaseHandler.updateLifetimeChannelStatistics(guild, lifetimeChannelStatistics);
    }

    public void deleteLifetimeChannelStatistics(@Snippet.NonNullParameter Guild guild, @Snippet.NonNullParameter String channelID) throws SQLException {
        lifetimeDatabaseHandler.deleteLifetimeStatisticsChannel(guild, channelID);
    }

    public void insertNewChannelStatisticsIntoLifetimeStatisticsTable(@Snippet.NonNullParameter Guild guild, @Snippet.NonNullParameter LifetimeChannelStatistics lifetimeChannelStatistics) throws SQLException {
        lifetimeDatabaseHandler.insertNewChannelStatisticsIntoLifetimeStatisticsTable(guild, lifetimeChannelStatistics);
    }
}
