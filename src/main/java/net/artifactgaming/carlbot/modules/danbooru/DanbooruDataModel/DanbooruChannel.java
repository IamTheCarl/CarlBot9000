package net.artifactgaming.carlbot.modules.danbooru.DanbooruDataModel;

import net.artifactgaming.carlbot.Utils;

public class DanbooruChannel {
    ///region SQL Column Names
    public final static String CHANNEL_ID = "CHANNEL_ID";

    public final static String TAGS = "TAGS";

    public final static String MIN_ACCEPTABLE_RATING = "MIN_ACCEPTABLE_RATING";

    public final static String ACTIVE = "ACTIVE";

    public final static String LAST_IMAGE_SENT_ID = "LAST_IMAGE_SENT";
    ///endregion


    private String channelID;
    private String tags;
    private Rating minAcceptableRating;

    /**
     * True if we should actively fetch images from danbooru to this channel.
     */
    private boolean active;

    private String lastImageSentID;

    public DanbooruChannel(String channelID, String tags, Rating minAcceptableRating, boolean active, String lastImageSentID) {
        this.channelID = channelID;
        this.tags = tags.trim();
        this.minAcceptableRating = minAcceptableRating;
        this.active = active;
        this.lastImageSentID = lastImageSentID;
    }

    public DanbooruChannel(String channelID) {
        this.channelID = channelID;
        this.tags = Utils.STRING_EMPTY;
        this.minAcceptableRating = Rating.SAFE;
        this.active = false;
        lastImageSentID = Utils.STRING_EMPTY;
    }

    public String getChannelID() {
        return channelID;
    }

    public void setChannelID(String channelID) {
        this.channelID = channelID;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags.trim();
    }

    public Rating getMinAcceptableRating() {
        return minAcceptableRating;
    }

    public void setMinAcceptableRating(Rating minAcceptableRating) {
        this.minAcceptableRating = minAcceptableRating;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Also used to check if this channel is configured.
     */
    public boolean emptyTag(){
        return tags.isEmpty();
    }

    public String getLastImageSentID() {
        return lastImageSentID;
    }

    public void setLastImageSentID(String lastImageSentID) {
        this.lastImageSentID = lastImageSentID;
    }
}
