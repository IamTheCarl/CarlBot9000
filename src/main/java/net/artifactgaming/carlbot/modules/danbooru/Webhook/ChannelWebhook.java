package net.artifactgaming.carlbot.modules.danbooru.Webhook;

import net.artifactgaming.carlbot.Utils;
import net.artifactgaming.carlbot.modules.danbooru.Danbooru;
import net.artifactgaming.carlbot.modules.danbooru.DanbooruDataModel.DanbooruChannel;
import net.artifactgaming.carlbot.modules.danbooru.DatabaseSQL.DanbooruDatabaseHandler;
import net.artifactgaming.carlbot.modules.danbooru.WebHandler.DanbooruPostModel.DanbooruPost;
import net.artifactgaming.carlbot.modules.danbooru.WebHandler.EmptyResultException;
import net.artifactgaming.carlbot.modules.danbooru.WebHandler.Requester;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class ChannelWebhook {

    private Logger logger = LoggerFactory.getLogger(Danbooru.class);

    private static final int ONE_MINUTE_IN_MILISECONDS = 60000;

    private static Requester danbooruRequesterRef;

    private static DanbooruDatabaseHandler danbooruDatabaseHandlerRef;

    private TextChannel bindedChannel;
    private String channelID;

    private Danbooru danbooruRef;

    private Timer webhookTriggerTimer;

    public ChannelWebhook(TextChannel bindedChannel, Danbooru danbooruRef){
        this.bindedChannel = bindedChannel;
        this.danbooruRef = danbooruRef;
        
        channelID = bindedChannel.getId();

        setupTriggerTimer();
    }

    private void setupTriggerTimer(){
        webhookTriggerTimer = new Timer();

        webhookTriggerTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateChannelWithNewPost();
            }
        }, ONE_MINUTE_IN_MILISECONDS, ONE_MINUTE_IN_MILISECONDS);
    }

    private void updateChannelWithNewPost() {
        Guild targetGuild = bindedChannel.getGuild();

        if (targetGuild == null){
            garbageCollect();
            return;
        }

        try {
            DanbooruChannel targetChannel = danbooruDatabaseHandlerRef.getDanbooruChannel(targetGuild, bindedChannel);

            if (!targetChannel.isActive()){
                garbageCollect();
                return;
            }

            List<DanbooruPost> danbooruPosts = danbooruRequesterRef.fetchLatestPosts(targetChannel.getTags());

            boolean loggedNewPost = false;
            String bannedTags = targetChannel.getBannedTags().trim();

            for (DanbooruPost post : danbooruPosts){
                if (post.getId().equals(targetChannel.getLastImageSentID())){
                    break;
                }

                if (!bannedTags.isEmpty()){
                    if (Utils.containsBannedTags(post.getTags(), bannedTags)){
                        continue;
                    }
                }

                if (Utils.isWithinMinRating(post.getRating(), targetChannel.getMinAcceptableRating())){
                    bindedChannel.sendMessage(post.getFileUrl()).queue();

                    if (!loggedNewPost){
                        loggedNewPost = true;
                        targetChannel.setLastImageSentID(post.getId());
                    }
                }
            }

            danbooruDatabaseHandlerRef.updateDanbooruChannel(targetGuild, targetChannel);
        } catch (SQLException e){
            logger.error("Error trying to fetch danbooru channel for " + channelID);
        } catch (EmptyResultException e){
            bindedChannel.sendMessage("No danbooru posts were found by the set tag for the webhook. Please check and update the tag accordingly!").queue();
            disableWebhook();
        } catch (Exception ignored){

        }
    }

    private void disableWebhook(){
        Guild targetGuild = bindedChannel.getGuild();

        if (targetGuild == null){
            garbageCollect();
            return;
        }

        try {
            DanbooruChannel targetChannel = danbooruDatabaseHandlerRef.getDanbooruChannel(targetGuild, bindedChannel);
            targetChannel.setActive(false);
            danbooruDatabaseHandlerRef.updateDanbooruChannel(targetGuild, targetChannel);
        } catch (SQLException e){
            logger.error("Error trying to fetch danbooru channel for " + channelID);
        }
    }

    public void stopTriggerTimer(){
        if (webhookTriggerTimer != null){
            webhookTriggerTimer.cancel();
            webhookTriggerTimer.purge();
        }
    }

    public void garbageCollect(){
        stopTriggerTimer();
        danbooruRef.removeChannelWebhook(this);
    }

    public String getChannelID(){
        return channelID;
    }

    public static void setReference(Requester requester, DanbooruDatabaseHandler danbooruDatabaseHandler){
        danbooruRequesterRef = requester;
        danbooruDatabaseHandlerRef = danbooruDatabaseHandler;
    }
}
