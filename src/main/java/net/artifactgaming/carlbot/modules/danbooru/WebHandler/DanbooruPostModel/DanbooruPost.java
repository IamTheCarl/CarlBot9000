package net.artifactgaming.carlbot.modules.danbooru.WebHandler.DanbooruPostModel;

import net.artifactgaming.carlbot.modules.danbooru.Rating;

public class DanbooruPost {
    private String id;

    private String fileUrl;

    private Rating rating;

    public DanbooruPost(String id, String fileUrl, Rating rating) {
        this.id = id;
        this.fileUrl = fileUrl;
        this.rating = rating;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public Rating getRating() {
        return rating;
    }

    public void setRating(Rating rating) {
        this.rating = rating;
    }
}
