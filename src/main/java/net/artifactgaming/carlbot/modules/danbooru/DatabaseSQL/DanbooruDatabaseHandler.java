package net.artifactgaming.carlbot.modules.danbooru.DatabaseSQL;

import net.artifactgaming.carlbot.Utils;
import net.artifactgaming.carlbot.modules.danbooru.Danbooru;
import net.artifactgaming.carlbot.modules.danbooru.DanbooruDataModel.DanbooruChannel;
import net.artifactgaming.carlbot.modules.danbooru.DanbooruDataModel.Rating;
import net.artifactgaming.carlbot.modules.persistence.Persistence;
import net.artifactgaming.carlbot.modules.persistence.PersistentModule;
import net.artifactgaming.carlbot.modules.persistence.Table;
import net.dv8tion.jda.core.entities.Guild;
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

    private List<DanbooruChannel> getGuildDanbooruChannels(Guild guild) throws SQLException {
        ArrayList<DanbooruChannel> danbooruChannels = new ArrayList<>();
        Table danbooruChannelTable = getDanbooruChannelTableInGuild(guild);

        ResultSet result = danbooruChannelTable.select().execute();

        while (result.next()){
            String channelID = result.getString(DanbooruChannel.CHANNEL_ID);
            String tags = result.getString(DanbooruChannel.TAGS);
            Rating minAcceptableRating = Utils.toRating(result.getString(DanbooruChannel.MIN_ACCEPTABLE_RATING));
            boolean active = result.getInt(DanbooruChannel.ACTIVE) == 1;

            DanbooruChannel danbooruChannel = new DanbooruChannel(channelID, tags, minAcceptableRating, active);
            danbooruChannels.add(danbooruChannel);
        }

        return danbooruChannels;
    }


    private Table getDanbooruChannelTableInGuild(Guild guild) throws SQLException {
        Table table = persistenceRef.getGuildTable(guild, persistentModuleRef);
        Table danbooruChannelTable = new Table(table, DANBOORU_CHANNEL_TABLE);

        if (!danbooruChannelTable.exists()) {
            danbooruChannelTable.create();

            danbooruChannelTable.alter().add()
                    .pushValue(DanbooruChannel.CHANNEL_ID + " varchar")
                    .pushValue(DanbooruChannel.TAGS + " varchar")
                    .pushValue(DanbooruChannel.MIN_ACCEPTABLE_RATING + " float")
                    .pushValue(DanbooruChannel.ACTIVE + " int")
                    .execute();
        }

        return danbooruChannelTable;
    }
}
