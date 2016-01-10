package com.smithyproductions.audioplayer;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.novoda.merlin.MerlinsBeard;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;

/**
 * Created by rory on 14/01/16.
 */
public class PlayerKicker {
    private static PlayerKicker sKicker;

    private static final long DEFAULT_DELAY = 30 * 1000;
    private static final long MIN_GLOBAL_COUNT = 4;
    private final Handler handler;
    private final MerlinsBeard merlin;
    private HashMap<AudioTrack, Long> startTimes = new HashMap<>();
    private HashMap<AudioTrack, Long> timeoutTimes = new HashMap<>();
    private long mGlobalAverage = 0;
    private long mGlobalAverageCount = 0;


    public interface KickerInterface {
        void onPlayerShouldHaveLoaded(final AudioTrack audioTrack, boolean urlValid);
    }

    public static PlayerKicker obtainKicker(Context context) {
        if(sKicker == null) {
            sKicker = new PlayerKicker(context);
        }
        return sKicker;
    }

    private PlayerKicker(final Context context) {
        this.handler = new Handler();
        merlin = MerlinsBeard.from(context);
    }

    public void notifyPrepareStart(final AudioTrack audioTrack, final KickerInterface kickerInterface) {
        long startTime = System.currentTimeMillis();
        startTimes.put(audioTrack, startTime);

        final long timeoutForTrack = getTimeoutForTrack(audioTrack);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!startTimes.containsKey(audioTrack)) {
                    Log.d("PlayerKicker", "looks like this track loaded ok in "+timeoutForTrack+"ms: "+audioTrack);
                } else if (merlin.isConnected()) {
                    Log.e("PlayerKicker", "why hasn't this track loaded in "+timeoutForTrack+"ms?: "+audioTrack);
                    //something's wrong
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final boolean urlValid = isUrlValid(audioTrack);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    //may have changed during request
                                    if (startTimes.containsKey(audioTrack)) {
                                        startTimes.remove(audioTrack);
                                        kickerInterface.onPlayerShouldHaveLoaded(audioTrack, urlValid);
                                    }
                                }
                            });
                        }
                    }).start();

                } else {
                    startTimes.remove(audioTrack);
                    Log.e("PlayerKicker", "no internet, check connection!");
                }
            }
        }, timeoutForTrack);
    }

    private boolean isUrlValid(final AudioTrack audioTrack) {
        try {

            URL url = new URL(audioTrack.getUrl());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            int code = connection.getResponseCode();

            Log.d("PlayerKicker", "url checker returned code: "+String.valueOf(code));
            return code == 200;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private long getTimeoutForTrack(AudioTrack audioTrack) {
        if (timeoutTimes.containsKey(audioTrack)) {
            Log.d("PlayerKicker", "providing specific average callback time ("+timeoutTimes.get(audioTrack)+"ms)");
            return timeoutTimes.get(audioTrack);
        } else if (mGlobalAverageCount >= MIN_GLOBAL_COUNT) {
            final long returnVal = 3 * mGlobalAverage;
            Log.d("PlayerKicker", "providing global average callback time ("+ returnVal +"ms)");
            return returnVal;
        } else {
            Log.d("PlayerKicker", "providing default callback time ("+DEFAULT_DELAY+"ms), not enough data yet ("+(MIN_GLOBAL_COUNT-mGlobalAverageCount)+" remaining)");
            return DEFAULT_DELAY;
        }
    }

    public void notifyPrepareEnd(final AudioTrack audioTrack) {
        if (startTimes.containsKey(audioTrack)) {
            long totalTime = System.currentTimeMillis() - startTimes.get(audioTrack);
            startTimes.remove(audioTrack);

            Log.d("PlayerKicker", "updating average time based on prepared track time of " + totalTime + "ms");

            if(timeoutTimes.containsKey(audioTrack)) {
                timeoutTimes.put(audioTrack, (long) ((timeoutTimes.get(audioTrack) + totalTime) /2f));
            } else {
                timeoutTimes.put(audioTrack, totalTime);
            }

            updateGlobalAverage();
        } else {
            Log.e("PlayerKicker", "unexpected audiotrack provided in notifyPrepareEnd");
        }
    }

    private void updateGlobalAverage() {
        mGlobalAverageCount++;
        mGlobalAverage = 0;
        for(Long l : timeoutTimes.values()) {
            mGlobalAverage += (l / (float) mGlobalAverageCount);
        }
    }

}
