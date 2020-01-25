package net.artifactgaming.carlbot.credentials;

public class DanbooruLogin {
    private final String username;

    private final String apiKey;

    public DanbooruLogin(String username, String apiKey) {
        this.username = username;
        this.apiKey = apiKey;
    }

    public String getUsername() {
        return username;
    }

    public String getApiKey() {
        return apiKey;
    }
}
