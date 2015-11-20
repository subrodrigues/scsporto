package pink.instagram.fetchserver.models;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.util.Log;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;

import pink.instagram.fetchserver.MainActivity;

/**
 * Created by filiperodrigues on 20/11/15.
 */
@Deprecated
public class CustomPicassoTarget implements Target {

    private int imageNumber = 0;

    public CustomPicassoTarget(int imageNumber) {
        this.imageNumber = imageNumber;
    }

    @Override
        public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {

        Log.e("Image loaded", "SUCCESS");

        new Thread(new Runnable() {
                @Override
                public void run() {

                    File file = new File(
                            Environment.getExternalStorageDirectory().getPath()
                                    + "/saved" + imageNumber + ".jpg");
                    try {
                        file.createNewFile();
                        FileOutputStream ostream = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, ostream);
                        ostream.close();

                        Log.e("Image saved", "With SUCCESS at " + file.getAbsolutePath());

                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            Log.e("ERROR", "LOADING");
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {}
}
