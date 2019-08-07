package net.artifactgaming.carlbot.modules.quotes;

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

    public String getOwnerID() {
        return quoteOwner.getOwnerID();
    }

    void setOwnerID(String ownerID) {
        quoteOwner.setOwnerID(ownerID);
    }

    public String getOwnerName() {
        return quoteOwner.getOwnerName();
    }

    void setOwnerName(String ownerName) {
        quoteOwner.setOwnerName(ownerName);
    }

    public String getKey() {
        return key;
    }

    void setKey(String key) {
        this.key = key;
    }

    public String getContent() {
        return content;
    }

    void setContent(String content) {
        this.content = content;
    }
}
