package net.artifactgaming.carlbot.modules.danbooru.DanbooruDataModel;

import net.artifactgaming.carlbot.Utils;
import net.artifactgaming.carlbot.modules.danbooru.Rating;

public class DanbooruChannel {
    ///region SQL Column Names
    public final static String CHANNEL_ID = "CHANNEL_ID";

    public final static String TAGS = "TAGS";

    public final static String MIN_ACCEPTABLE_RATING = "MIN_ACCEPTABLE_RATING";

    public final static String ACTIVE = "ACTIVE";

    public final static String LAST_IMAGE_SENT_ID = "LAST_IMAGE_SENT";

    public final static String BANNED_TAGS = "BANNED_TAGS";
    ///endregion

    private final static String DEFAULT_BANNED_TAGS = "69 top-down_bottom-up  girl_on_top straddling cowgirl_position legs_over_head center_opening topless bottomless zenra nude dress_lift downblouse nipples upskirt cleavage_reach erection molestation head_between_breasts";


    private String channelID;
    private String tags;

    private Rating minAcceptableRating;

    /**
     * True if we should actively fetch images from danbooru to this channel.
     */
    private boolean active;

    private String lastImageSentID;

    private String bannedTags;

    public DanbooruChannel(String channelID, String tags, Rating minAcceptableRating, boolean active, String lastImageSentID, String bannedTags) {
        this.channelID = channelID;
        this.tags = tags.trim();
        this.minAcceptableRating = minAcceptableRating;
        this.active = active;
        this.lastImageSentID = lastImageSentID;
        this.bannedTags = bannedTags;
    }

    public DanbooruChannel(String channelID) {
        this.channelID = channelID;
        this.tags = Utils.STRING_EMPTY;
        this.minAcceptableRating = Rating.SAFE;
        this.active = false;
        lastImageSentID = Utils.STRING_EMPTY;
        bannedTags = DEFAULT_BANNED_TAGS;
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

    public String getBannedTags() {
        return bannedTags;
    }

    public void setBannedTags(String bannedTags) {
        this.bannedTags = bannedTags;
    }
}
