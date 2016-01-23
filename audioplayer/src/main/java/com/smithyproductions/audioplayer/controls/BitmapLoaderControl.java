package com.smithyproductions.audioplayer.controls;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;
import android.util.Log;

import com.smithyproductions.audioplayer.AudioTrack;
import com.smithyproductions.audioplayer.interfaces.ControlInterface;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by rory on 23/01/16.
 */
public class BitmapLoaderControl extends ControlAdapter {

    private static BitmapLoaderControl sInstance;

    @Nullable
    public Bitmap getCurrentBitmap() {
        return currentBitmap;
    }

    @Nullable
    public AudioTrack getcurrentTrack() {
        return currentTrack;
    }

    public interface BitmapLoaderInterface {

        void onAudioTrackBitmapReady(AudioTrack track);

    }
    private Set<BitmapLoaderInterface> interfaceSet = new HashSet<>();
    private
    @Nullable
    AudioTrack currentTrack;

    private @Nullable Bitmap currentBitmap;

    private BitmapLoaderControl () {

    }

    public static BitmapLoaderControl getInstance() {
        if(sInstance == null) {
            sInstance = new BitmapLoaderControl();
        }
        return sInstance;
    }

    @Override
    public void onTrackChange(@Nullable AudioTrack track) {
        final AudioTrack oldTrack = currentTrack;
        currentTrack = track;
        if (oldTrack != null && currentTrack != null && oldTrack.getArtworkUrl() != null && oldTrack.getArtworkUrl().equals(currentTrack.getArtworkUrl())) {
            //same
            Log.d("BitmapLoaderControl", "New track has same bitmap");
        } else if (currentTrack != null && currentTrack.getArtworkUrl() != null){
            //different, fetch
            currentBitmap = null;
            final AudioTrack targetTrack = currentTrack;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Bitmap bitmap = getBitmapFromURL(targetTrack.getArtworkUrl());
                    Log.d("BitmapLoaderControl", "loaded bitmap for "+targetTrack);
                    if(targetTrack.equals(currentTrack)) {
                        currentBitmap = bitmap;

                        for(BitmapLoaderInterface bitmapLoaderInterface : interfaceSet) {
                            bitmapLoaderInterface.onAudioTrackBitmapReady(targetTrack);
                        }
                    }


                }
            }).start();

        } else {
            //this track doesn't have any artwork
            currentBitmap = null;
            Log.d("BitmapLoaderControl", "This track has no artwork");
        }
    }

    public static Bitmap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            // Log exception
            return null;
        }
    }

    public void attachBitmapLoaderInterface(final BitmapLoaderInterface bitmapLoaderInterface) {
        interfaceSet.add(bitmapLoaderInterface);
    }

    public void detachBitmapLoaderInterface(final BitmapLoaderInterface bitmapLoaderInterface) {
        interfaceSet.remove(bitmapLoaderInterface);
    }
}
