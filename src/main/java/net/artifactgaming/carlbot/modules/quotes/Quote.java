package net.artifactgaming.carlbot.modules.quotes;

import net.sf.json.*;

public class Quote {

    private QuoteOwner quoteOwner;
    private String key;

    private String content;

    Quote(String ownerID, String ownerName, String key, String content) {
        this.quoteOwner = new QuoteOwner(ownerID, ownerName);
        this.key = key;
        this.content = content;
    }

    Quote(QuoteOwner quoteOwner, String key, String content){
        this.quoteOwner = quoteOwner;
        this.key = key;
        this.content = content;
    }

    String getOwnerID() {
        return quoteOwner.getOwnerID();
    }

    void setOwnerID(String ownerID) {
        quoteOwner.setOwnerID(ownerID);
    }

    String getOwnerName() {
        return quoteOwner.getOwnerName();
    }

    void setOwnerName(String ownerName) {
        quoteOwner.setOwnerName(ownerName);
    }

    String getKey() {
        return key;
    }

    void setKey(String key) {
        this.key = key;
    }

    String getContent() {
        return content;
    }

    void setContent(String content) {
        this.content = content;

    }

    String toJsonString(){
        return toJsonObject().toString();
    }

    JSONObject toJsonObject(){
        JSONObject result = new JSONObject();

        result.put("key", key);
        result.put("content", content);
        result.put("ownerID", quoteOwner.getOwnerID());
        result.put("ownerName", quoteOwner.getOwnerName());

        return result;
    }

    static Quote toQuoteObject(JSONObject jsonObject){
        String quoteKey = jsonObject.getString("key");
        String quoteContent = jsonObject.getString("content");

        String quoteOwnerID = jsonObject.getString("ownerID");
        String quoteOwnerName = jsonObject.getString("ownerName");

        QuoteOwner quoteOwner = new QuoteOwner(quoteOwnerID, quoteOwnerName);

        return new Quote(quoteOwner, quoteKey, quoteContent);
    }
}
