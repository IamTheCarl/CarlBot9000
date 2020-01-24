package net.artifactgaming.carlbot.modules.danbooru.DanbooruDataModel;

public class DanbooruChannel {
    ///region SQL Column Names
    public final static String CHANNEL_ID = "CHANNEL_ID";

    public final static String TAGS = "TAGS";

    public final static String MIN_ACCEPTABLE_RATING = "MIN_ACCEPTABLE_RATING";

    public final static String ACTIVE = "ACTIVE";
    ///endregion


    private String channelID;
    private String tags;
    private Rating minAcceptableRating;

    /**
     * True if we should actively fetch images from danbooru to this channel.
     */
    private boolean active;

    public DanbooruChannel(String channelID, String tags, Rating minAcceptableRating, boolean active) {
        this.channelID = channelID;
        this.tags = tags;
        this.minAcceptableRating = minAcceptableRating;
        this.active = active;
    }

    public DanbooruChannel(String channelID) {
        this.channelID = channelID;
        this.tags = "";
        this.minAcceptableRating = Rating.SAFE;
        this.active = false;
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
        this.tags = tags;
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
}
