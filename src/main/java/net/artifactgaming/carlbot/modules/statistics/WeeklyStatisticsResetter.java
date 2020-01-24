package net.artifactgaming.carlbot.modules.statistics;

import net.artifactgaming.carlbot.modules.statistics.ChannelStatistic.LifetimeChannelStatistics;
import net.artifactgaming.carlbot.modules.statistics.ChannelStatistic.WeeklyChannelStatistics;
import net.artifactgaming.carlbot.modules.statistics.DatabaseSQL.StatisticsDatabaseHandler;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.ReadyEvent;
import org.slf4j.Logger;

import java.sql.SQLException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Use an instance of this object to reset overdue weekly statistics.
 */
public class WeeklyStatisticsResetter {

    /**
     * How many days must pass for a weekly statistic to reset.
     * Keep it to 7, unless for debug.
     */
    public final static int DAYS_TO_RESET = 7;

    // TODO: Possible refactor on the 'lifetimeChannelStatisticsToMergeList' ArrayList?

    /**
     * Create a list to merge with the actual database later.
     */
    private ArrayList<LifetimeChannelStatistics> lifetimeChannelStatisticsToMergeList = new ArrayList<>();

    private Logger logger;

    private JDA botInstance;

    private StatisticsDatabaseHandler statisticsDatabaseHandler;

    /**
     * Debug purposes.
     */
    private int noOfResetsDone = 0;

    WeeklyStatisticsResetter(JDA _botInstance, Logger _logger, StatisticsDatabaseHandler _statisticsDatabaseHandler){
        botInstance = _botInstance;
        logger = _logger;
        statisticsDatabaseHandler = _statisticsDatabaseHandler;
    }

    /**
     * Resets overdue weekly statistics.
     */
    public void resetOverdueStatistics(){
        List<Guild> guilds = botInstance.getGuilds();

        for (Guild guild: guilds) {
            tryResetOverdueWeeklyStatistics(guild);
            tryMergeLifetimeChannelStatistics(guild);
            lifetimeChannelStatisticsToMergeList.clear();
        }
        logger.debug("Reset " + noOfResetsDone + " channels because their weekly statistic data was outdated!");
    }

    private void tryMergeLifetimeChannelStatistics(Guild guild) {
        try {
            List<LifetimeChannelStatistics> lifetimeChannelStatisticsList = statisticsDatabaseHandler.getLifetimeGuildStatistics(guild);

            // Merge the data with the actual
            for (LifetimeChannelStatistics toMerge: lifetimeChannelStatisticsToMergeList) {
                boolean hasActualRow = false;
                for (LifetimeChannelStatistics actual: lifetimeChannelStatisticsList) {
                    if (toMerge.getChannelID().equals(actual.getChannelID())){
                        double mergedPercentage = actual.getPercentageOfTotalMessagesSent() + toMerge.getPercentageOfTotalMessagesSent();
                        double mergedPercentageOnImage = (actual.getPercentageOfMessagesContainImages() + toMerge.getPercentageOfMessagesContainImages()) / 2;

                        actual.setPercentageOfMessagesContainImages(mergedPercentageOnImage);
                        actual.setPercentageOfTotalMessagesSent(mergedPercentage);
                        actual.setChannelName(toMerge.getChannelName());
                        hasActualRow = true;
                        break;
                    }
                }

                if (!hasActualRow){
                    // It will reach here if there was no row for a lifetime data
                    // for this channel; We need to create one.
                    statisticsDatabaseHandler.insertNewChannelStatisticsIntoLifetimeStatisticsTable(guild, toMerge);
                    lifetimeChannelStatisticsList.add(toMerge);
                }
            }

            double totalPercentageOfMessagesSent = getTotalPercentageOfMessagesSent(lifetimeChannelStatisticsList);
            // Recalculate the merged result
            for (LifetimeChannelStatistics mergedResult: lifetimeChannelStatisticsList) {
                double finalPercentage = (mergedResult.getPercentageOfTotalMessagesSent() / totalPercentageOfMessagesSent) * 100;
                mergedResult.setPercentageOfTotalMessagesSent(finalPercentage);

                statisticsDatabaseHandler.updateLifetimeChannelStatistics(guild, mergedResult);
            }

        } catch (SQLException e){
            logger.error("Failed to fetch one of the guilds lifetime channel statistics due to: " + e.getMessage());
        }
    }

    private double getTotalPercentageOfMessagesSent(List<LifetimeChannelStatistics> lifetimeChannelStatisticsList){
        double result = 0;
        for (LifetimeChannelStatistics lifetimeChannelStatistics: lifetimeChannelStatisticsList) {
            result += lifetimeChannelStatistics.getPercentageOfTotalMessagesSent();
        }
        return result;
    }

    private void tryResetOverdueWeeklyStatistics(Guild guild){
        try {
            List<WeeklyChannelStatistics> weeklyChannelStatisticsList = statisticsDatabaseHandler.getWeeklyGuildStatistics(guild);

            long totalMessagesSent = getTotalMessagesSent(weeklyChannelStatisticsList);

            LocalDate currentLocalDate = Calendar.getInstance().getTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            for (WeeklyChannelStatistics weeklyChannelStatistics: weeklyChannelStatisticsList) {
                LocalDate startedTrackingLocalDate = weeklyChannelStatistics.getTrackedDate();

                Period dateDifference = Period.between(startedTrackingLocalDate, currentLocalDate);
                // If one week has passed since the date of tracking, reset
                if (dateDifference.getDays() >= DAYS_TO_RESET){
                    resetChannelWeeklyStatistics(guild, weeklyChannelStatistics, totalMessagesSent);
                }
            }
        } catch (SQLException e){
            logger.error("Failed to fetch one of the guilds weekly channel statistics due to: " + e.getMessage());
        } catch (ParseException e){
            logger.error("Failed to properly parse one of the date-formats in the weekly channel statistics: " + e.getMessage());
        }
    }

    private void resetChannelWeeklyStatistics(Guild guild, WeeklyChannelStatistics weeklyChannelStatistics, long totalMessagesSent) throws SQLException{
        // TODO: Maybe do something if the channel is now not-visible or deleted?

        // If no messages were sent onto this channel for this week, do nothing.
        if (weeklyChannelStatistics.getNoOfMessagesSent() > 0) {
            // To merge with the actual database later.
            LifetimeChannelStatistics lifetimeChannelStatisticsToMerge = new LifetimeChannelStatistics(weeklyChannelStatistics.getChannelID(), weeklyChannelStatistics.getChannelName());

            double percentOfMessagesSent = ((double) weeklyChannelStatistics.getNoOfMessagesSent() / totalMessagesSent) * 100;
            lifetimeChannelStatisticsToMerge.setPercentageOfTotalMessagesSent(percentOfMessagesSent);

            double percentOfMessagesHadImages = (weeklyChannelStatistics.getNoOfMessagesWithImage() / (double) weeklyChannelStatistics.getNoOfMessagesSent()) * 100;
            lifetimeChannelStatisticsToMerge.setPercentageOfMessagesContainImages(percentOfMessagesHadImages);

            lifetimeChannelStatisticsToMergeList.add(lifetimeChannelStatisticsToMerge);
        }

        weeklyChannelStatistics.reset();
        statisticsDatabaseHandler.updateWeeklyChannelStatistics(guild, weeklyChannelStatistics);

        // DEBUG!!!
        logger.debug("Reset channel weekly statistics :: " + weeklyChannelStatistics.getChannelName());
        ++noOfResetsDone;
    }

    private long getTotalMessagesSent(List<WeeklyChannelStatistics> allChannelStatistics){
        long totalMessagesSent = 0;
        for (WeeklyChannelStatistics weeklyChannelStatistics: allChannelStatistics) {
            totalMessagesSent += weeklyChannelStatistics.getNoOfMessagesSent();
        }

        return totalMessagesSent;
    }
}
