package net.artifactgaming.carlbot.modules.danbooru.WebHandler;

import net.artifactgaming.carlbot.CarlBot;
import net.artifactgaming.carlbot.Utils;
import net.artifactgaming.carlbot.credentials.DanbooruLogin;
import net.artifactgaming.carlbot.modules.danbooru.Rating;
import net.artifactgaming.carlbot.modules.danbooru.WebHandler.DanbooruPostModel.DanbooruPost;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class Requestor {

    /**
     * How much results (latest) can it fetch at once.
     */
    private static final int REQUEST_RESULT_LIMITS = 3;

    private static final String DANBOORU_POSTS_SITE = "https://danbooru.donmai.us/posts.json";

    private final DanbooruLogin loginDetails;

    public Requestor(CarlBot carlBot){
        loginDetails = carlBot.getDanbooruLoginDetails();
    }

    public List<DanbooruPost> fetchLatestPosts(String tags) throws Exception {
        String resultString = getRequestResult(tags);

        JSONArray json = (JSONArray) JSONSerializer.toJSON(resultString);

        if (json.size() == 0){
            throw new EmptyResultException();
        }

        ArrayList<DanbooruPost> postsResults = new ArrayList<>();
        for (Object o : json) {
            JSONObject currentPostJson = (JSONObject) JSONSerializer.toJSON(o);

            String postId = String.valueOf(currentPostJson.getInt("id"));
            String fileUrl = currentPostJson.getString("large_file_url");
            Rating rating = Utils.toRating(currentPostJson.getString("rating"));

            postsResults.add(new DanbooruPost(postId, fileUrl, rating));
        }

        return postsResults;
    }

    private String getRequestResult(String tags) throws Exception {
        URI uriWithParams = Utils.appendUri(DANBOORU_POSTS_SITE, "login=" + loginDetails.getUsername());
        uriWithParams = Utils.appendUri(uriWithParams.toString(), "api_key=" + loginDetails.getApiKey());
        uriWithParams = Utils.appendUri(uriWithParams.toString(), "tags=" + tags);
        uriWithParams = Utils.appendUri(uriWithParams.toString(), "limit=" + REQUEST_RESULT_LIMITS);

        URL urlWithParams = uriWithParams.toURL();

        HttpsURLConnection connection = (HttpsURLConnection) urlWithParams.openConnection();
        connection.setRequestMethod("GET");

        BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder requestResult = new StringBuilder();

        String line;
        while ((line = rd.readLine()) != null) {
            requestResult.append(line);
        }

        return requestResult.toString();
    }
}
