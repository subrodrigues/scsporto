package pink.instagram.fetchserver.instagram;


import pink.instagram.fetchserver.models.Image;

/**
 * Represents a response from Instagram API
 *
 * Created by filiperodrigues on 20/11/15.
 */
public class ImageResponse {
    private Pagination pagination = null;
    private Image[] data = null;
    private Meta meta = null;

    public class Pagination{
        String next_url = null;
        String next_max_id = null;

        public String getNextURL() {
            return next_url;
        }
    }

    public class Meta{
        private int code = -1;

        public int getCode(){
            return code;
        }
    }

    public Image[] getImages(){
        return data;
    }

    public Pagination getPagination(){
        return pagination;
    }

    public Meta getMeta(){
        return meta;
    }
}
