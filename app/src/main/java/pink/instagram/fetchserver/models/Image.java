package pink.instagram.fetchserver.models;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Represents an image with some parameters that are relevant for this project
 *
 * Created by filiperodrigues on 20/11/15.
 */
public class Image {
    private String attribution = null;
    private String[] tags = null;
    private Location location = null;
    private URLs images = null;
    private Caption caption = null;

    private String id = "-1";

    public URLs getURLs(){
        return images;
    }
    public Caption getCaption(){return caption;}

    public class Caption{
        private String created_time = null;
        private String text = null;
        private From from = null;

        public From getFrom(){
            return from;
        }
        public String getComment(){return text;}
        public String getDate(){
            try{
                DateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
                Date netDate = (new Date(Long.parseLong(created_time)*1000L));
                return sdf.format(netDate);
            }
            catch(Exception ex){
                return "xx";
            }
        }

        public class From{
            private String id = null;
            private String username = null;
            private String full_name = null;

            public String getUsername(){
                return username;
            }
            public String getFullName(){
                return full_name;
            }
        }
    }

    public class URLs{
        private LowRes low_resolution = null;
        private Thumbnail thumbnail = null;
        private StandardResolution standard_resolution = null;

        public LowRes getLowRes(){
            return low_resolution;
        }
        public Thumbnail getThumbnail(){
            return thumbnail;
        }
        public StandardResolution getStandardResolution(){
            return standard_resolution;
        }

        public class LowRes{
            private String url = null;
            private String width = null;
            private String height = null;

            public String getURL(){
                return url;
            }
        }

        public class Thumbnail{
            String url = null;
            String width = null;
            String height = null;

            public String getURL(){
                return url;
            }
        }

        public class StandardResolution{
            String url = null;
            String width = null;
            String height = null;

            public String getURL(){
                return url;
            }
        }
    }

    public String getTagsString(){
        StringBuilder builder = new StringBuilder();
        for(String s : tags) {
            builder.append(s + " ");
        }
        return builder.toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
