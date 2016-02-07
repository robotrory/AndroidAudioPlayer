package audioplayertest.e8tracks;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.smithyproductions.audioplayer.AudioTrack;
import com.smithyproductions.audioplayer.trackProviders.TrackProvider;
import audioplayertest.e8tracks.models.MixInfoResponse;
import audioplayertest.e8tracks.models.MixResponse;
import audioplayertest.e8tracks.models.MixSetResponse;
import audioplayertest.e8tracks.models.NextMixResponse;
import audioplayertest.e8tracks.models.TrackResponse;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.logging.HttpLoggingInterceptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * Created by rory on 09/01/16.
 */
public class MixSetTrackProvider extends TrackProvider {

    public static final String PLAY_TOKEN = "2735211";
    private final E8tracksService apiService;
    final ConcurrentHashMap<Integer, TrackCallback> requestCallbacks = new ConcurrentHashMap<>();

    @Nullable private MixResponse currentMix;
    @Nullable private MixResponse nextMix;
    private int currentTrackIndex;

    private boolean currentMixReachedEnd;

    private List<retrofit.Call> mixRequests = new ArrayList<>();
    private List<retrofit.Call> trackRequests = new ArrayList<>();
    private Map<Integer, List<AudioTrack>> trackListMap = new HashMap<>();
    private Map<Integer, Boolean> mixSkipMap = new HashMap<>();

    private Set<MixSetTrackProviderInterface> mixSetTrackProviderInterfaceSet = new HashSet<>();

    public interface MixSetTrackProviderInterface {

        void onMixChange(@Nullable MixResponse mixResponse);

    }
    public MixSetTrackProvider() {

        OkHttpClient client = new OkHttpClient();
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        client.interceptors().add(interceptor);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://8tracks.com")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(E8tracksService.class);

        apiService.mixInfo("rory", "mixname").enqueue(new Callback<MixInfoResponse>() {
            @Override
            public void onResponse(Response<MixInfoResponse> response, Retrofit retrofit) {

            }

            @Override
            public void onFailure(Throwable t) {

            }
        });
    }
    public void loadMix(final MixResponse mixResponse) {

        trackListMap.clear();

        for (MixSetTrackProviderInterface mixSetTrackProviderInterface : mixSetTrackProviderInterfaceSet) {
            mixSetTrackProviderInterface.onMixChange(currentMix);
        }

        Log.d("MixSetTrackProvider", "getting tracks for mix with id: " + mixResponse.id);
        final retrofit.Call<MixSetResponse> call = apiService.playMix(PLAY_TOKEN, mixResponse.id);
        if(!trackRequests.contains(call)) {
            trackRequests.add(call);
            call.enqueue(new Callback<MixSetResponse>() {
                @Override
                public void onResponse(Response<MixSetResponse> response, Retrofit retrofit) {

                    handleMixSetResponse(call, response, mixResponse);

                }

                @Override
                public void onFailure(Throwable t) {

                }
            });
        }
    }

    private void handleMixSetResponse(retrofit.Call networkCall, Response<MixSetResponse> response, MixResponse mixResponse) {
        if(response.body() != null && response.body().set != null) {
            List<AudioTrack> newTracks = getNewTracksFromResponse(response, mixResponse);

            List<AudioTrack> trackList = getMixTrackList(mixResponse);

            trackList.addAll(newTracks);

            trackListMap.put(mixResponse.id, trackList);

            mixSkipMap.put(mixResponse.id, response.body().set.skip_allowed);

            if(currentMix == null) {
                currentMix = mixResponse;
                Log.d("MixSetTrackProvider", "setting currentMix: "+currentMix);

                //gone from null to something, should notify
                for(TrackProviderListener trackProviderListener : trackProviderListenerSet) {
                    trackProviderListener.onTracksInvalidated();
                }

                for (MixSetTrackProviderInterface mixSetTrackProviderInterface : mixSetTrackProviderInterfaceSet) {
                    mixSetTrackProviderInterface.onMixChange(currentMix);
                }

                fetchNextMix(currentMix);

            } else if (nextMix != null && mixResponse.id.intValue() == nextMix.id.intValue()) {
                //not sure what these do??
//                Log.d("MixSetTrackProvider", "moving onto nextMix: "+nextMix);
//                swapToNextMix();
            } else if(nextMix == null) {
                nextMix = mixResponse;
                Log.d("MixSetTrackProvider", "setting nextMix: "+nextMix);
            }

            if (currentMix != null && mixResponse.id.intValue() == currentMix.id.intValue()) {
                currentMixReachedEnd = newTracks.size() <= 0;
            }

            List<AudioTrack> currentTrackList = getMixTrackList(currentMix);
            List<AudioTrack> nextTrackList = getMixTrackList(nextMix);

            Log.d("MixSetTrackProvider", "trackList: " + currentTrackList);
            Log.d("MixSetTrackProvider", "nextTrackList: " + nextTrackList);

            trackRequests.remove(networkCall);
            Iterator<Map.Entry<Integer, TrackCallback>> iterator = requestCallbacks.entrySet().iterator();

            synchronized (requestCallbacks) {
                while (iterator.hasNext()) {
                    Map.Entry<Integer, TrackCallback> entry = iterator.next();

                    final Integer pos = entry.getKey();
                    if (handleCallback(pos, entry.getValue(), currentTrackList, nextTrackList)) {
                        iterator.remove();
                    }
                }
            }
        } else {
            //can't play it
            Log.e("MixSetTrackProvider", "can't play mix, will try to get next: "+mixResponse);
            currentMixReachedEnd = true;
            fetchNextMix(mixResponse);
        }
    }

    private boolean handleCallback(final int n, final TrackCallback callback, final List<AudioTrack> currentTrackList, final List<AudioTrack> nextTrackList) {
        if (currentMix != null) {
            if (n < currentTrackList.size()) {
                //we have tracks for the current mix
                Log.d("MixSetTrackProvider", "handleCallback returning track");
                callback.onTrackRetrieved(currentTrackList.get(n));
                return true;
            } else if (!currentMixReachedEnd) {
                //we don't have this track, but our last request was successful
                //so we should try getting the next tracks
                Log.d("MixSetTrackProvider", "handleCallback fetching next track");
                getNextTracks(currentMix);
            } else if ((n - currentTrackList.size()) < nextTrackList.size()) {
                //we have tracks available in the next mix
                Log.d("MixSetTrackProvider", "handleCallback returning track from next mix");
                callback.onTrackRetrieved(nextTrackList.get(n - currentTrackList.size()));
                return true;
            } else if (nextMix == null) {
                Log.d("MixSetTrackProvider", "handleCallback fetching next mix");
                fetchNextMix(currentMix);
            } else {
                Log.d("MixSetTrackProvider", "handleCallback fetching next traks for next mix");
                getNextTracks(nextMix);
            }
        }
        return false;
    }

    public void goToNextMix() {
        if(nextMix != null) {
            swapToNextMix();

            for(TrackProviderListener trackProviderListener : trackProviderListenerSet) {
                trackProviderListener.onTracksInvalidated();
            }
        }
    }

    private void fetchNextMix(@NonNull final MixResponse currentMix) {
        final Call<NextMixResponse> mixCall = apiService.nextMix(PLAY_TOKEN, currentMix.id, "similar:"+currentMix.id);
        if(!mixRequests.contains(mixCall)) {
            mixRequests.add(mixCall);
            mixCall.enqueue(new Callback<NextMixResponse>() {
                @Override
                public void onResponse(Response<NextMixResponse> response, Retrofit retrofit) {

                    final MixResponse mixResponse = response.body().next_mix;
                    final retrofit.Call<MixSetResponse> trackCall = apiService.playMix(PLAY_TOKEN, mixResponse.id);
                    if (!trackRequests.contains(trackCall)) {
                        trackRequests.add(trackCall);
                        trackCall.enqueue(new Callback<MixSetResponse>() {
                            @Override
                            public void onResponse(Response<MixSetResponse> response, Retrofit retrofit) {

                                mixRequests.remove(mixCall);
                                handleMixSetResponse(trackCall, response, mixResponse);

                            }

                            @Override
                            public void onFailure(Throwable t) {

                            }
                        });
                    } else {
                        mixRequests.remove(mixCall);
                    }
                }

                @Override
                public void onFailure(Throwable t) {

                }
            });
        }
    }


    @Override
    public int getCurrentTrackIndex() {
        return currentTrackIndex;
    }

    @Override
    public void decrementTrackIndex() {
        //noop
    }

    @Override
    public void incrementTrackIndex() {
        currentTrackIndex++;
        onTrackChange();
    }

    public boolean canSkip () {
        if(currentMix != null && mixSkipMap.containsKey(currentMix.id)) {
            return mixSkipMap.get(currentMix.id);
        } else {
            return false;
        }
    }

    protected void onTrackChange() {

        if(currentTrackIndex >= getMixTrackList(currentMix).size()) {
            //if we've reached the end of the current mix and our current track index is in the next mix
            //then update our current and next mixes
            if(currentMixReachedEnd) {
                swapToNextMix();
            }
        }

    }
    private void swapToNextMix() {
        if(currentMix != null) {
            MixResponse lastMix = currentMix;
            Log.d("MixSetTrackProvider","currentTrackIndex: "+currentTrackIndex+" currentMix: "+currentMix.id+" getMixTrackList(currentMix).size(): "+getMixTrackList(currentMix).size());
            currentTrackIndex = 0;//-= getMixTrackList(currentMix).size();

            currentMix = nextMix;
            nextMix = null;

            if (currentMix != null) {
                fetchNextMix(currentMix);
            } else {
                throw new RuntimeException("HELP! We moved past the current mix but didn't have a next mix, so now we have no current mix!");
            }

            trackListMap.remove(lastMix.id);

            for (MixSetTrackProviderInterface mixSetTrackProviderInterface : mixSetTrackProviderInterfaceSet) {
                mixSetTrackProviderInterface.onMixChange(currentMix);
            }
        }
    }

    private List<AudioTrack> getNewTracksFromResponse(Response<MixSetResponse> response, MixResponse mixResponse) {
        List<AudioTrack> existingTracks = getMixTrackList(mixResponse);
        List<AudioTrack> addedTracks = new ArrayList<>();

        //at_end is true when we are given no tracks
        if (response.body() != null && response.body().set != null && !response.body().set.at_end) {
            TrackResponse track1 = response.body().set.track;
            TrackResponse track2 = response.body().set.next_track;


            boolean track1Exists = false;
            boolean track2Exists = false;

            for (AudioTrack track : existingTracks) {
                if (track1 != null && track.getId().intValue() == track1.id) {
                    track1Exists = true;
                }

                if (track2 != null && track.getId().intValue() == track2.id) {
                    track2Exists = true;
                }
            }

            if (track1 != null && !track1Exists) {
                final AudioTrack track = AudioTrack.create(track1.name, track1.performer, track1.track_file_stream_url, track1.id);
                Log.d("MixSetTrackProvider", "adding track: " + track);
                track.setArtworkUrl(mixResponse.cover_urls.max200);
                addedTracks.add(track);
            }

            if (track2 != null && !track2Exists) {
                final AudioTrack track = AudioTrack.create(track2.name, track2.performer, track2.track_file_stream_url, track2.id);
                Log.d("MixSetTrackProvider", "adding track: " + track);
                track.setArtworkUrl(mixResponse.cover_urls.max200);
                addedTracks.add(track);
            }

        }

        return addedTracks;
    }

    private List<AudioTrack> getMixTrackList(@Nullable MixResponse mixResponse) {
        List<AudioTrack> targetList;
        if (mixResponse != null && trackListMap.containsKey(mixResponse.id)) {
            targetList = trackListMap.get(mixResponse.id);
        } else {
            targetList = new ArrayList<>();
        }
        return targetList;
    }

    @Override
    public void requestNthTrack(int n, TrackCallback callback) {
        List<AudioTrack> currentTrackList = getMixTrackList(currentMix);
        List<AudioTrack> nextTrackList = getMixTrackList(nextMix);
        Log.d("MixSetTrackProvider", "request for "+n+"th track");

        if(currentMix == null) {
            callback.onError("No data");
        } else if(n < 0) {
            callback.onError("Can't go to previous track, we're at the first!");
        } else if(!handleCallback(n, callback, currentTrackList, nextTrackList)) {
            Log.d("MixSetTrackProvider", "couldn't handle callback, so we're saving it");
            requestCallbacks.put(n, callback);
        }
    }

    private void getNextTracks(@NonNull final MixResponse mixResponse) {
        final retrofit.Call<MixSetResponse> call = apiService.next(PLAY_TOKEN, mixResponse.id);
        if (!trackRequests.contains(call)) {
            trackRequests.add(call);
            call.enqueue(new Callback<MixSetResponse>() {
                @Override
                public void onResponse(Response<MixSetResponse> response, Retrofit retrofit) {

                    handleMixSetResponse(call, response, mixResponse);

                }

                @Override
                public void onFailure(Throwable t) {

                }
            });
        }
    }

    @Override
    public void cancelAllTrackRequests() {
        requestCallbacks.clear();
    }

    @Override
    public int getTrackCount() {
        if (currentMix != null) {
            return trackListMap.get(currentMix.id).size();
        } else {
            return 0;
        }
    }

    @Override
    public List<AudioTrack> getTrackList() {
        if (currentMix != null) {
            return trackListMap.get(currentMix.id);
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public void reset() {
        trackListMap.clear();
        requestCallbacks.clear();

        currentMix = null;
        nextMix = null;
        currentTrackIndex = 0;
        currentMixReachedEnd = false;

        Iterator<retrofit.Call> iterator = trackRequests.iterator();

        while(iterator.hasNext()) {
            iterator.next().cancel();
            iterator.remove();
        }

        for(TrackProviderListener trackProviderListener : trackProviderListenerSet) {
            trackProviderListener.onTracksInvalidated();
        }

        for(MixSetTrackProviderInterface mixSetTrackProviderInterface : mixSetTrackProviderInterfaceSet) {
            mixSetTrackProviderInterface.onMixChange(currentMix);
        }
    }

    public void addMixSetTrackProviderInterface(MixSetTrackProviderInterface mixSetTrackProviderInterface) {
        mixSetTrackProviderInterfaceSet.add(mixSetTrackProviderInterface);
    }
}
