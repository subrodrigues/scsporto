package pink.instagram.fetchserver.instagram;

import android.util.Log;

import com.google.gson.Gson;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.InputStream;

import pink.instagram.fetchserver.utils.Utils;
import pink.instagram.fetchserver.variables.APIKeys;
import pink.instagram.fetchserver.variables.Variables;

/**
 * Class that holds the methods to query the Instagram API
 *
 * Created by filiperodrigues on 20/11/15.
 */
public class InstagramAPI {
    private String format = "json";

    /**
     * Query the Instagram server for information
     * @param tag . String to fetch server
     * @return JSONObject with the server answer, null if some error occurs
     */
    public static JSONObject fetchInstagramServer(String tag, String url) {
        try {
            HttpClient client = new DefaultHttpClient();
            HttpGet get = null;

            if(url == null) { // Fetch without URL. The first query.
                get = new HttpGet(Variables.INSTAGRAM_URL + tag + Variables.INSTAGRAM_URL_CLIENT
                        + APIKeys.instagramClientID + Variables.INSTAGRAM_COUNT + Variables.NUMBER_IMAGES);
            }
            else{ // Get next images
                get = new HttpGet(url);
            }

            Log.d("Instagram", "URL: " + get.getURI().toString());

            HttpResponse response = client.execute(get);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                String result = Utils.convertStreamToString(instream);
                instream.close();
                return new JSONObject(result);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("FetchInformation.fetch", e.getLocalizedMessage());
            // TODO throw an action to app when it fails to fetch the server
        }
        return null;
    }

    public static ImageResponse getImages(String tag) {
        Gson gson = new Gson();

        JSONObject response = fetchInstagramServer(tag, null);
        Log.d("RESPONSE", response.toString());

        ImageResponse res = gson.fromJson(response.toString(), ImageResponse.class);

        return res;
    }

    public static ImageResponse getNextImages(String url) {
        Gson gson = new Gson();

        JSONObject response = fetchInstagramServer(null, url);
        Log.d("RESPONSE", response.toString());

        ImageResponse res = gson.fromJson(response.toString(), ImageResponse.class);

        return res;
    }

}
