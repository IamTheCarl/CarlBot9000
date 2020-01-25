package net.artifactgaming.carlbot.modules.danbooru.WebHandler;

import net.artifactgaming.carlbot.CarlBot;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Requestor {

    /**
     * How much results (latest) can it fetch at once.
     */
    private static final int REQUEST_RESULT_LIMITS = 3;

    private static final String DANBOORU_POSTS_SITE = "https://danbooru.donmai.us/posts.json";



    public Requestor(CarlBot carlBot){

    }

    private String GetRequestResult(String tags) throws Exception {
        URL urlWithParams = new URL(DANBOORU_POSTS_SITE + "?limit=" + REQUEST_RESULT_LIMITS + "&tags=" + tags);

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
