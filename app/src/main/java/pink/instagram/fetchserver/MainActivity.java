package pink.instagram.fetchserver;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

import pink.instagram.fetchserver.instagram.ImageResponse;
import pink.instagram.fetchserver.instagram.InstagramAPI;
import pink.instagram.fetchserver.models.Image;
import pink.instagram.fetchserver.variables.Variables;

/**
 * Created by filiperodrigues on 20/11/15.
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Represents a response from Instagram API
     */
    private Vector<ImageResponse> instagramImageResponse = null;
    private int paginationIndex = 0;
    private MainActivity activity;

    private static Vector<String> uploadedImageIDs = null; // TODO: save and load from preferences onStop/onResume
    public static int imageNumber = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activity = this;

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        instagramImageResponse = new Vector<>();
        uploadedImageIDs = new Vector<>();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Fetching Instragram Server", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                new GetImagesInstagram().execute(Variables.DEFAULT_TAG);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * AsyncTask used to access Instagram API and load first images
     */
    private class GetImagesInstagram extends AsyncTask<String, Void, ImageResponse> {
        @Override
        protected ImageResponse doInBackground(String... tag) {
            return InstagramAPI.getImages(tag[0]);
        }

        // Save data and fill structures
        @Override
        protected void onPostExecute(ImageResponse result) {
            instagramImageResponse.clear();
            paginationIndex = 0;

            instagramImageResponse.add(result);

            if(result.getMeta().getCode() == 200) {
            //    Log.d("NEXT URL:", instagramImageResponse.get(paginationIndex).getPagination().getNextURL());
                Log.d("NUMBER OF IMAGES", instagramImageResponse.get(paginationIndex).getImages().length + "");

                loadImagesAndSaveToDisk(instagramImageResponse.get(paginationIndex).getImages());

                if(!getUploadedImageIDs().contains( // If last item is not added, get next page
                        instagramImageResponse.get(paginationIndex).getImages()[instagramImageResponse.get(paginationIndex).getImages().length-1]) &&
                        instagramImageResponse.get(paginationIndex).getPagination().getNextURL() != null){

                    new GetNextImagesInstagram().execute(instagramImageResponse.get(paginationIndex).getPagination().getNextURL());
                }

            } else {
                Log.d("NULL", "FRAG");
                // TODO: deal with it
            }
        }

    }

    /**
     * AsyncTask used to access Instagram API and load next images
     */
    private class GetNextImagesInstagram extends AsyncTask<String, Void, ImageResponse> {
        @Override
        protected ImageResponse doInBackground(String... url) {
            return InstagramAPI.getNextImages(url[0]);
        }

        // Save data and fill structures
        @Override
        protected void onPostExecute(ImageResponse result) {
            paginationIndex++;

            instagramImageResponse.add(result);

            if(result.getMeta().getCode() == 200){

                loadImagesAndSaveToDisk(instagramImageResponse.get(paginationIndex).getImages());

                if(!getUploadedImageIDs().contains( // If last item is not added, get next page
                        instagramImageResponse.get(paginationIndex).getImages()[instagramImageResponse.get(paginationIndex).getImages().length-1])){

                    new GetNextImagesInstagram().execute(instagramImageResponse.get(paginationIndex).getPagination().getNextURL());
                }

            }
            else {
                Log.d("NULL", "FRAG");
                // TODO: deal with it
            }
        }
    }

    public void loadImagesAndSaveToDisk(final Image[] images){

        new DownloadFilesAndSaveToCardTask(images, 0).execute(0);

     /*   if(imageNumber == (images.length-1)){ // If we reached the end of page, load next images
            // Todo save images from another page
        }
        */
    }

    private class DownloadFilesAndSaveToCardTask extends AsyncTask<Integer, Integer, Bitmap> {

        private Image[] images = null;
        private int index = 0;

        public DownloadFilesAndSaveToCardTask(Image[] images, int index){
            this.images = images;
            this.index = index;
        }

        @Override
        protected Bitmap doInBackground(Integer... index) {
            if(MainActivity.getUploadedImageIDs().contains(images[index[0]].getId())){
                this.cancel(true);
            }
            else {
                try {
                    URL url = new URL(images[index[0]].getURLs().getStandardResolution().getURL());
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    Bitmap myBitmap = BitmapFactory.decodeStream(input);
                    return myBitmap;
                } catch (IOException e) {
                    // Log exception
                    return null;
                }
            }

            return null;
        }

        protected void onPostExecute(Bitmap result) {

            try {
                // Save Photo to sdcard
                File file = new File(Environment.getExternalStorageDirectory().getPath() + "/scsporto/saved" + imageNumber + ".jpg");

                file.createNewFile();
                FileOutputStream ostream = new FileOutputStream(file);
                result.compress(Bitmap.CompressFormat.JPEG, 100, ostream);
                ostream.close();

                MainActivity.getUploadedImageIDs().add(images[index].getId());
                Log.e("Image saved", "With SUCCESS at " + file.getAbsolutePath());

                imageNumber++;
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            if(result != null && index < (images.length-1)){
                index++;
                new DownloadFilesAndSaveToCardTask(images, index).execute(index);
            }
        }
    }



    public static Vector<String> getUploadedImageIDs() {
        return uploadedImageIDs;
    }

    public static void setUploadedImageIDs(Vector<String> uploadedImageIDs) {
        MainActivity.uploadedImageIDs = uploadedImageIDs;
    }
}
