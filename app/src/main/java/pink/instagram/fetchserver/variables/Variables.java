package pink.instagram.fetchserver.variables;

/**
 * Variables used in the context of the project
 *
 * Created by filiperodrigues on 20/11/15.
 */
public class Variables {

    // https://api.instagram.com/v1/tags/media/recent?client_id=bc24f77d7c304e93badf0c01073f6df9&count=20

    /**
     * Instagram related variables
     */
    public static final int NUMBER_IMAGES = 30;
    public static final String INSTAGRAM_URL = "https://api.instagram.com/v1/tags/";
    public static final String INSTAGRAM_URL_CLIENT = "/media/recent?client_id=";
    public static final String INSTAGRAM_COUNT = "&count=";
    public static final int LOAD_MORE_IMAGES_THRESHOLD = 5;
    public static final String DEFAULT_TAG = "caladowedding";

}
