package pink.instagram.fetchserver;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.MimeTypeMap;
import android.widget.Button;

import com.google.android.gms.common.AccountPicker;
import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.media.MediaFileSource;
import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.data.photos.AlbumFeed;
import com.google.gdata.data.photos.GphotoEntry;
import com.google.gdata.data.photos.GphotoFeed;
import com.google.gdata.data.photos.PhotoEntry;
import com.google.gdata.data.photos.UserFeed;
import com.google.gdata.util.ServiceException;
import com.google.gdata.util.ServiceForbiddenException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.logging.Logger;

import pink.instagram.fetchserver.instagram.ImageResponse;
import pink.instagram.fetchserver.instagram.InstagramAPI;
import pink.instagram.fetchserver.models.Image;
import pink.instagram.fetchserver.tinydb.TinyDB;
import pink.instagram.fetchserver.variables.Variables;

/**
 * Created by filiperodrigues on 20/11/15.
 */
public class MainActivity extends AppCompatActivity {

    public static final int INSTAGRAM_QUERY_PERIOD = 30000;
    /**
     * Represents a response from Instagram API
     */
    private Vector<ImageResponse> instagramImageResponse = null;
    private int paginationIndex = 0;

    // TODO: change uploadedImageIDs and imageNumber for a sharedpreferences data thing
    private static Vector<String> uploadedImageIDs = null; // TODO: save and load from preferences onStop/onResume
    public static int imageNumber = 0;

    private Timer timer = null;
    private boolean isRunning = false;

    private File folder = null;

    /**
     * Picasa things
     */
    private final int PICK_ACCOUNT_REQUEST = 1;
    private final int REQUEST_AUTHENTICATE = 2;
    private static final String API_PREFIX
            = "https://picasaweb.google.com/data/feed/api/user/";
    private static final String ALBUM_PREFIX = "/albumid/";

    private static final String ALBUM_TO_UPLOAD = "Album de Testes";
    private AlbumEntry album = null;

    PicasawebService picasaService;
    Button selectAccount;
    AccountManager am;
    Account[] list;
    String selectedAccountName;
    Account selectedAccount;

    //DB
    TinyDB tinydb = null;

    /**
     * Picasa things
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        folder = new File(Environment.getExternalStorageDirectory().getPath() + "/scsporto");

        tinydb = new TinyDB(this);

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

                if (!isRunning) {
                    TimerTask task = new TimerTask() {
                        public void run() {
                            new GetImagesInstagram().execute(Variables.DEFAULT_TAG);
                            isRunning = true;
                        }
                    };
                    timer = new Timer();
                    timer.schedule(task, 500, INSTAGRAM_QUERY_PERIOD);
                } else {
                    isRunning = false;
                    timer.cancel();
                    Snackbar.make(view, "Instragram Server Stopped", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }

     /*           final Handler handler = new Handler();
                Runnable runnable = new Runnable() {

                    @Override
                    public void run() {
                        try{
                            new GetImagesInstagram().execute(Variables.DEFAULT_TAG);
                            //do your code here
                            //also call the same runnable
                            handler.postDelayed(this, 300000);
                        }
                        catch (Exception e) {
                            // TODO: handle exception
                        }
                    }
                };
                handler.postDelayed(runnable, 500);
                */
            }
        });


        // Picasa things
        am = (AccountManager) getSystemService(ACCOUNT_SERVICE);

        selectAccount = (Button) findViewById(R.id.selectAccount);
        selectAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                list = am.getAccounts();
                Log.e("Got {} accounts", list.length + "");
                for (Account a : list) {
                    Log.e("{} {}", a.name + " " + a.type);
                }

                Intent intent = AccountPicker.newChooseAccountIntent(null, null, new String[]{"com.google"},
                        false, null, null, null, null);
                startActivityForResult(intent, PICK_ACCOUNT_REQUEST);
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

            /*    if(!getUploadedImageIDs().contains( // If last item is not added, get next page
                        instagramImageResponse.get(paginationIndex).getImages()[instagramImageResponse.get(paginationIndex).getImages().length-1]) &&
                        instagramImageResponse.get(paginationIndex).getPagination().getNextURL() != null){
               */
                // If last item is not added, get next page
                if(tinydb.getString(instagramImageResponse.get(paginationIndex).getImages()[instagramImageResponse.get(paginationIndex).getImages().length-1].getId()).isEmpty() &&
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

           //     if(!getUploadedImageIDs().contains( // If last item is not added, get next page
           //             instagramImageResponse.get(paginationIndex).getImages()[instagramImageResponse.get(paginationIndex).getImages().length-1])){

                // If last item is not added, get next page
                    if(tinydb.getString(instagramImageResponse.get(paginationIndex).getImages()[instagramImageResponse.get(paginationIndex).getImages().length-1].getId()).isEmpty()){

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
        //    if(MainActivity.getUploadedImageIDs().contains(images[index[0]].getId())){
            if(!tinydb.getString(images[index[0]].getId()).isEmpty()){
                this.cancel(true);
            }
            else {
                try {
                    Log.e("IMAGE URL", images[index[0]].getURLs().getStandardResolution().getURL());

                    URL url = new URL(images[index[0]].getURLs().getStandardResolution().getURL());
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    Bitmap myBitmap = BitmapFactory.decodeStream(input);

                    try {
                        // Save Photo to sdcard
                        //    File folder = new File(Environment.getExternalStorageDirectory().getPath() + "/scsporto");
                        boolean success = true;
                        if (!folder.exists()) {
                            success = folder.mkdir();
                        }
                        if (success) {
                            // Do something on success
                            File file = new File(folder.getAbsolutePath() +"/saved" + imageNumber + ".jpg");
                            file.createNewFile();
                            FileOutputStream ostream = new FileOutputStream(file);
                            myBitmap.compress(Bitmap.CompressFormat.JPEG, 100, ostream);
                            ostream.close();

                            uploadAndDeletePhoto(album, file);

                            //   MainActivity.getUploadedImageIDs().add(images[index].getId());
                            tinydb.putString(images[this.index].getId(), images[this.index].getId());

                            Log.e("Image saved", "With SUCCESS at " + file.getAbsolutePath());

                            imageNumber++;
                        } else {
                            // TODO: deal with folder creation error
                        }

                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }

                    return myBitmap;
                } catch (IOException e) {
                    // Log exception
                    return null;
                }
            }

            return null;
        }

        protected void onPostExecute(Bitmap result) {


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


    /**
     * Picasa Things
     */
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {

        switch(requestCode) {
            case PICK_ACCOUNT_REQUEST:
                if (resultCode == RESULT_OK) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                //    LOG.debug("Selected Account {}", accountName);
                    selectedAccount =  null;
                    for (Account a:list) {
                        if (a.name.equals(accountName)) {
                            selectedAccount = a;
                            break;
                        }
                    }
                    selectedAccountName = accountName;

                    am.getAuthToken(
                            selectedAccount,                     // Account retrieved using getAccountsByType()
                            "lh2",            // Auth scope
                            null,                        // Authenticator-specific options
                            this,                           // Your activity
                            new OnTokenAcquired(),          // Callback called when a token is successfully acquired
                            null);    // Callback called if an error occ
                }
                break;
            case REQUEST_AUTHENTICATE:
                if (resultCode == RESULT_OK) {
                    am.getAuthToken(
                            selectedAccount,                     // Account retrieved using getAccountsByType()
                            "lh2",            // Auth scope
                            null,                        // Authenticator-specific options
                            this,                           // Your activity
                            new OnTokenAcquired(),          // Callback called when a token is successfully acquired
                            null);    // Callback called if an error occ
                }
                break;
        }
    }

    public <T extends GphotoFeed> T getFeed(String feedHref,
                                            Class<T> feedClass) throws IOException, ServiceException {
        Log.e("Feed", "Get Feed URL: " + feedHref);
        return picasaService.getFeed(new URL(feedHref), feedClass);
    }

    public List<AlbumEntry> getAlbums(String userId) throws IOException,
            ServiceException {

        String albumUrl = API_PREFIX + userId;
        UserFeed userFeed = getFeed(albumUrl, UserFeed.class);

        List<GphotoEntry> entries = userFeed.getEntries();
        List<AlbumEntry> albums = new ArrayList<AlbumEntry>();
        for (GphotoEntry entry : entries) {
            AlbumEntry ae = new AlbumEntry(entry);
            Log.e("Album name {}",ae.getName());
            albums.add(ae);
        }

        return albums;
    }

    public List<PhotoEntry> getPhotos(String userId, AlbumEntry album) throws IOException,
            ServiceException{
        AlbumFeed feed = album.getFeed();
        List<PhotoEntry> photos = new ArrayList<PhotoEntry>();
        for (GphotoEntry entry : feed.getEntries()) {
            PhotoEntry pe = new PhotoEntry(entry);
            photos.add(pe);
        }
        Log.e("Album {} has {} photos", album.getName()+ photos.size()+"");
        return photos;
    }

    public PhotoEntry uploadPhoto(String userId, String albumId, File photoFile) throws IOException, ServiceException{

        String albumUrl = API_PREFIX + userId + ALBUM_PREFIX + albumId;

        String mimeType = getMimeTypeFromFile(photoFile);

        MediaFileSource myMedia = new MediaFileSource(photoFile, mimeType);
        return picasaService.insert(new URL(albumUrl), PhotoEntry.class, myMedia);
    }

    private class OnTokenAcquired implements AccountManagerCallback<Bundle> {

        @Override
        public void run(AccountManagerFuture<Bundle> result) {
            try {
                Bundle b = result.getResult();

                if (b.containsKey(AccountManager.KEY_INTENT)) {
                    Intent intent = b.getParcelable(AccountManager.KEY_INTENT);
                    int flags = intent.getFlags();
                    intent.setFlags(flags);
                    flags &= ~Intent.FLAG_ACTIVITY_NEW_TASK;
                    startActivityForResult(intent, REQUEST_AUTHENTICATE);
                    return;
                }

                if (b.containsKey(AccountManager.KEY_AUTHTOKEN)) {
                    final String authToken = b.getString(AccountManager.KEY_AUTHTOKEN);
                    Log.e("Auth token {}", authToken);
                    picasaService = new PicasawebService("pictureframe");
                    picasaService.setUserToken(authToken);

                    new AsyncTask<Void, Void, Bitmap>() {
                        @Override
                        protected Bitmap doInBackground(Void... voids) {
                            List<AlbumEntry> albums = null;
                            try {

                                albums = getAlbums(selectedAccountName);
                                Log.e("Got {} albums", albums.size()+"");
                                for (AlbumEntry myAlbum : albums) {
                                    Log.e("Album {} ", myAlbum.getTitle().getPlainText());

                                    if(myAlbum.getTitle().getPlainText().equals(ALBUM_TO_UPLOAD)) {
                                        album = myAlbum;

                                        ArrayList<File> localPhotos = getListFiles(folder);

                                        for (File photoFile : localPhotos) {
                                            if(!isPictureMimeType(photoFile)) continue;

                                            try {
                                                uploadAndDeletePhoto(myAlbum, photoFile);
                                            } catch (Exception ex) {
                                                ex.printStackTrace();
                                            }
                                        }
                                    }
                                }

                                /*AlbumEntry album = albums.get(0);

                                List<PhotoEntry> photos = getPhotos(selectedAccountName, album);
                                PhotoEntry photo = photos.get(0);

                                URL photoUrl = new URL(photo.getMediaContents().get(0).getUrl());
                                Bitmap bmp = BitmapFactory.decodeStream(photoUrl.openConnection().getInputStream());
                                return bmp;*/
                                return null;
                            } catch (ServiceForbiddenException e) {
                                Log.e("ERROR", "Token expired, invalidating");
                                am.invalidateAuthToken("com.google", authToken);
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (ServiceException e) {
                                e.printStackTrace();
                            }
                            return null;
                        }
                        protected void onPostExecute(Bitmap result) {
                            //picture.setImageBitmap(result);

                        }
                    }.execute(null, null, null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void uploadAndDeletePhoto(AlbumEntry myAlbum, File photoFile) throws IOException, ServiceException {
        PhotoEntry entry = uploadPhoto(selectedAccountName, myAlbum.getGphotoId(), photoFile);
        boolean isDeleted = photoFile.delete();
        Log.e("Uploaded {} ", entry.getId() + " and original file deleted (" + isDeleted + ")");
    }

    private static String getMimeTypeFromFile(File file){
        String type = null;
        Uri uri = Uri.fromFile(file);
        String extension = MimeTypeMap.getFileExtensionFromUrl(uri.getPath());
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }

        return type;
    }

    private boolean isPictureMimeType(File file) {
        String type = getMimeTypeFromFile(file);
        return type != null && type.contains("image");
    }

    private ArrayList<File> getListFiles(File parentDir) {

        ArrayList<File> inFiles = new ArrayList<>();

        File[] files = parentDir.listFiles();

        for (File file : files) {

            if (file.isDirectory()) {

                inFiles.addAll(getListFiles(file));
            }
            else {
                inFiles.add(file);
            }
        }

        return inFiles;
    }

}
