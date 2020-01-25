package net.artifactgaming.carlbot.modules.danbooru.DatabaseSQL;

import net.artifactgaming.carlbot.Utils;
import net.artifactgaming.carlbot.modules.danbooru.Danbooru;
import net.artifactgaming.carlbot.modules.danbooru.DanbooruDataModel.DanbooruChannel;
import net.artifactgaming.carlbot.modules.danbooru.Rating;
import net.artifactgaming.carlbot.modules.persistence.Persistence;
import net.artifactgaming.carlbot.modules.persistence.PersistentModule;
import net.artifactgaming.carlbot.modules.persistence.Table;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DanbooruDatabaseHandler {

    private static final String DANBOORU_CHANNEL_TABLE = "DANBOORU_CHANNEL";

    private Logger logger = LoggerFactory.getLogger(Danbooru.class);

    private Persistence persistenceRef;

    private PersistentModule persistentModuleRef;

    public DanbooruDatabaseHandler(Persistence _persistenceRef, PersistentModule _persistentModuleRef) {
        persistenceRef = _persistenceRef;
        persistentModuleRef = _persistentModuleRef;
    }

    public List<DanbooruChannel> getGuildDanbooruChannels(Guild guild) throws SQLException {
        ArrayList<DanbooruChannel> danbooruChannels = new ArrayList<>();
        Table danbooruChannelTable = getDanbooruChannelTableInGuild(guild);

        ResultSet result = danbooruChannelTable.select().execute();

        while (result.next()){
            String channelID = result.getString(DanbooruChannel.CHANNEL_ID);
            String tags = result.getString(DanbooruChannel.TAGS);
            Rating minAcceptableRating = Utils.toRating(result.getString(DanbooruChannel.MIN_ACCEPTABLE_RATING));
            boolean active = result.getInt(DanbooruChannel.ACTIVE) == 1;
            String lastImageSentID = result.getString(DanbooruChannel.LAST_IMAGE_SENT_ID);

            DanbooruChannel danbooruChannel = new DanbooruChannel(channelID, tags, minAcceptableRating, active, lastImageSentID);
            danbooruChannels.add(danbooruChannel);
        }

        return danbooruChannels;
    }

    public DanbooruChannel getDanbooruChannel(Guild guild, TextChannel channel) throws SQLException {
        Table danbooruChannelTable = getDanbooruChannelTableInGuild(guild);

        ResultSet result = danbooruChannelTable.select()
                .where(DanbooruChannel.CHANNEL_ID, "=", channel.getId())
                .execute();

        DanbooruChannel danbooruChannel;
        if (result.next()){
            String channelID = result.getString(DanbooruChannel.CHANNEL_ID);
            String tags = result.getString(DanbooruChannel.TAGS);
            Rating minAcceptableRating = Utils.toRating(result.getString(DanbooruChannel.MIN_ACCEPTABLE_RATING));
            boolean active = result.getInt(DanbooruChannel.ACTIVE) == 1;
            String lastImageSentID = result.getString(DanbooruChannel.LAST_IMAGE_SENT_ID);

            danbooruChannel = new DanbooruChannel(channelID, tags, minAcceptableRating, active, lastImageSentID);
        } else {
            danbooruChannel = new DanbooruChannel(channel.getId());

            insertNewDanbooruChannel(channel);
        }

        return danbooruChannel;
    }

    public void updateDanbooruChannel(Guild guild, DanbooruChannel danbooruChannel) throws SQLException {
        Table danbooruChannelTable = getDanbooruChannelTableInGuild(guild);

        danbooruChannelTable.update()
                .where(DanbooruChannel.CHANNEL_ID, "=", danbooruChannel.getChannelID())
                .set(DanbooruChannel.TAGS, danbooruChannel.getTags())
                .set(DanbooruChannel.MIN_ACCEPTABLE_RATING, Utils.fromRating(danbooruChannel.getMinAcceptableRating()))
                .set(DanbooruChannel.ACTIVE, danbooruChannel.isActive() ? "1" : "0")
                .set(DanbooruChannel.LAST_IMAGE_SENT_ID, danbooruChannel.getLastImageSentID())
                .execute();
    }

    private void insertNewDanbooruChannel(TextChannel channel) throws SQLException{
        Table danbooruChannelTable = getDanbooruChannelTableInGuild(channel.getGuild());

        danbooruChannelTable.insert()
                .set(DanbooruChannel.CHANNEL_ID, channel.getId())
                .set(DanbooruChannel.TAGS, Utils.STRING_EMPTY)
                .set(DanbooruChannel.MIN_ACCEPTABLE_RATING, "S")
                .set(DanbooruChannel.ACTIVE, "0")
                .set(DanbooruChannel.LAST_IMAGE_SENT_ID, Utils.STRING_EMPTY)
                .execute();
    }

    private Table getDanbooruChannelTableInGuild(Guild guild) throws SQLException {
        Table table = persistenceRef.getGuildTable(guild, persistentModuleRef);
        Table danbooruChannelTable = new Table(table, DANBOORU_CHANNEL_TABLE);

        if (!danbooruChannelTable.exists()) {
            danbooruChannelTable.create();

            danbooruChannelTable.alter().add()
                    .pushValue(DanbooruChannel.CHANNEL_ID + " varchar")
                    .pushValue(DanbooruChannel.TAGS + " varchar")
                    .pushValue(DanbooruChannel.MIN_ACCEPTABLE_RATING + " varchar")
                    .pushValue(DanbooruChannel.ACTIVE + " int")
                    .pushValue(DanbooruChannel.LAST_IMAGE_SENT_ID + " varchar")
                    .execute();
        }

        return danbooruChannelTable;
    }
}
