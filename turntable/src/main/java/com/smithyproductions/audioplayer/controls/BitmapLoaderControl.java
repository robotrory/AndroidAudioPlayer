package com.smithyproductions.audioplayer.controls;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import com.smithyproductions.audioplayer.AudioTrack;

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

    public boolean hasBitmapForTrack(@Nullable AudioTrack audioTrack) {
        return currentTrack != null && audioTrack != null && currentTrack.getArtworkUrl() != null && currentTrack.getArtworkUrl().equals(audioTrack.getArtworkUrl());
    }

    public interface BitmapLoaderInterface {

        void onCurrentAudioTrackBitmapReady();

    }

    private Set<BitmapLoaderInterface> interfaceSet = new HashSet<>();
    private
    @Nullable
    AudioTrack currentTrack;

    private
    @Nullable
    Bitmap currentBitmap;

    private BitmapLoaderControl() {

    }

    public static BitmapLoaderControl getInstance() {
        if (sInstance == null) {
            sInstance = new BitmapLoaderControl();
        }
        return sInstance;
    }

    @Override
    public void onTrackChange(@Nullable AudioTrack track) {
        if (hasBitmapForTrack(track)) {
            //same
            Log.d("BitmapLoaderControl", "New track has same bitmap");
        } else if (track != null && track.getArtworkUrl() != null) {
            //different, fetch
            currentBitmap = null;
            final AudioTrack targetTrack = track;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final Bitmap bitmap = getBitmapFromURL(targetTrack.getArtworkUrl());
                    Log.d("BitmapLoaderControl", "loaded bitmap for " + targetTrack);

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (targetTrack.equals(currentTrack)) {
                                currentBitmap = bitmap;

                                for (BitmapLoaderInterface bitmapLoaderInterface : interfaceSet) {
                                    bitmapLoaderInterface.onCurrentAudioTrackBitmapReady();
                                }
                            }
                        }
                    });


                }
            }).start();

        } else {
            //this track doesn't have any artwork
            currentBitmap = null;
            Log.d("BitmapLoaderControl", "This track has no artwork");
        }
        currentTrack = track;
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

        if (currentTrack != null && currentBitmap != null) {
            bitmapLoaderInterface.onCurrentAudioTrackBitmapReady();
        }
    }

    public void detachBitmapLoaderInterface(final BitmapLoaderInterface bitmapLoaderInterface) {
        interfaceSet.remove(bitmapLoaderInterface);
    }
}
