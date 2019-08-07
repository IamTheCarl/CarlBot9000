package net.artifactgaming.carlbot.modules.quotes;

class QuoteOwner {

    private String ownerID;

    private String ownerName;

    QuoteOwner(String ownerID, String ownerName) {
        this.ownerID = ownerID;
        this.ownerName = ownerName;
    }

    String getOwnerID() {
        return ownerID;
    }

    void setOwnerID(String ownerID) {
        this.ownerID = ownerID;
    }

    String getOwnerName() {
        return ownerName;
    }

    void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }
}
