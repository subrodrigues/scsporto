package pink.instagram.fetchserver.utils;

import android.content.Context;
import android.util.Log;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.FirebaseException;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;

/**
 *
 * Created by Jorge on 23/11/2015.
 *
 **/
public class FirebaseUtil {
    private static final String FIREBASE_ROOT_URL = "https://gdgsummit2015.firebaseio.com";
    private static final boolean SHOW_FIREBASE_LOG = true;
    private static final String TAG = "scsporto.firebase";
    private static FirebaseUtil singleton;
    private Firebase summitFirebase;

    /**
     * lets keep this private, shall we?
     * */
    private FirebaseUtil(){
        summitFirebase = new Firebase(FIREBASE_ROOT_URL);
    }

    public static FirebaseUtil getInstance(Context applicationContext) {
        if(singleton == null) {
            Firebase.setAndroidContext(applicationContext.getApplicationContext());
            singleton = new FirebaseUtil();
        }

        return singleton;
    }

    public void addUserphoto(final String username, final String photoUrl) {

        final String escapedUsername = username.replaceAll("[^a-zA-Z0-9]+","");

        summitFirebase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                SummitUserFirebaseUserModel userModel = null;
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    DataSnapshot userSnapshot = dataSnapshot.child(escapedUsername);
                    try {
                        userModel = userSnapshot.getValue(SummitUserFirebaseUserModel.class);
                    } catch (FirebaseException exception) {
                        if (SHOW_FIREBASE_LOG) {
                            Log.i(TAG, "onDataChange wrong data type", exception);
                        }
                    }
                }
                if (userModel == null) {
                    userModel = new SummitUserFirebaseUserModel();
                    userModel.username = escapedUsername;
                    userModel.numberOfPhotos = 0;
                    userModel.photosURLs = new ArrayList<>();
                }

                userModel.numberOfPhotos++;

                ArrayList<String> newURLsList = new ArrayList<>();

                newURLsList.addAll(userModel.getPhotosURLs());
                newURLsList.add(photoUrl);
                userModel.photosURLs = newURLsList;

                summitFirebase.child("users").child(escapedUsername).setValue(userModel);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    public static class SummitUserFirebaseUserModel {
        private String username;
        private int numberOfPhotos;
        private ArrayList<String> photosURLs;

        public SummitUserFirebaseUserModel() {

        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username.replaceAll("[^a-zA-Z0-9]+","");
        }

        public int getNumberOfPhotos() {
            return numberOfPhotos;
        }

        public void setNumberOfPhotos(int numberOfPhotos) {
            this.numberOfPhotos = numberOfPhotos;
        }

        public ArrayList<String> getPhotosURLs() {
            return photosURLs;
        }

        public void setPhotosURLs(ArrayList<String> photosURLs) {
            this.photosURLs = photosURLs;
        }
    }
}
