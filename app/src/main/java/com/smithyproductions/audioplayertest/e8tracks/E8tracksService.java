package com.smithyproductions.audioplayertest.e8tracks;

import com.smithyproductions.audioplayertest.e8tracks.models.MixInfoResponse;
import com.smithyproductions.audioplayertest.e8tracks.models.MixSetResponse;
import com.smithyproductions.audioplayertest.e8tracks.models.NextMixResponse;

import retrofit.Call;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;


/**
 * Created by rory on 09/01/16.
 */
public interface E8tracksService {
    @GET("sets/{play_token}/play.json?api_version=3&api_key=3dda11b4bd06c6ff75677d0f60b7c83794642c8d&include=next_track")
    Call<MixSetResponse> playMix(@Path("play_token") String playToken, @Query("mix_id") Integer mixId);

    @GET("sets/{play_token}/next.json?api_version=3&api_key=3dda11b4bd06c6ff75677d0f60b7c83794642c8d&include=next_track")
    Call<MixSetResponse> next(@Path("play_token") String playToken, @Query("mix_id") Integer mixId);

    @GET("sets/{play_token}/skip.json?api_version=3&api_key=3dda11b4bd06c6ff75677d0f60b7c83794642c8d&include=next_track")
    Call<MixSetResponse> skip(@Path("play_token") String playToken, @Query("mix_id") String mixId);

    @GET("{user}/{mix_name}.jsonh")
    Call<MixInfoResponse> mixInfo(@Path("user") String user, @Path("mix_name") String mixName);

    @GET("sets/{play_token}/next_mix.json?api_version=3&api_key=3dda11b4bd06c6ff75677d0f60b7c83794642c8d&include=next_track")
    Call<NextMixResponse> nextMix(@Path("play_token") String playToken, @Query("mix_id") Integer mixId, @Query("smart_id") String smartId);

}
