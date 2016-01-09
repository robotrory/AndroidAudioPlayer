package com.smithyproductions.audioplayertest;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ProgressBar;

import com.smithyproductions.audioplayer.AudioPlayer;
import com.smithyproductions.audioplayer.AudioPlayerBuilder;
import com.smithyproductions.audioplayer.AudioTrack;
import com.smithyproductions.audioplayer.audioEngines.FadingAudioEngine;
import com.smithyproductions.audioplayer.audioEngines.PreloadingAudioEngine;
import com.smithyproductions.audioplayer.interfaces.ProgressListener;
import com.smithyproductions.audioplayer.playerEngines.MediaPlayerEngine;
import com.smithyproductions.audioplayer.trackProviders.PlaylistTrackProvider;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final int PROGRESS_MAX = 1000;
    private AudioPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        Button playButton = (Button) findViewById(R.id.play_button);
        Button pauseButton = (Button) findViewById(R.id.pause_button);
        Button resetButton = (Button) findViewById(R.id.reset_button);
        Button nextButton = (Button) findViewById(R.id.next_button);
        Button previousButton = (Button) findViewById(R.id.previous_button);

        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setMax(PROGRESS_MAX);

        List<AudioTrack> playlist = new ArrayList<>();
        playlist.add(AudioTrack.create("name","performer","http://smithyproductions.co.uk/infinitracks/tempTracks/UvLT1hJcnd.mp3"));
        playlist.add(AudioTrack.create("name2","performer2","http://smithyproductions.co.uk/infinitracks/tempTracks/VsObrsPlQN.mp3"));
        playlist.add(AudioTrack.create("name3","performer3","http://smithyproductions.co.uk/infinitracks/tempTracks/BkSaO7Bwhq.mp3"));
        playlist.add(AudioTrack.create("name4","performer4","http://smithyproductions.co.uk/infinitracks/tempTracks/1WKJCbjwBg.mp3"));
        playlist.add(AudioTrack.create("name4","performer4","http://smithyproductions.co.uk/infinitracks/tempTracks/2NJWVdQwWP.mp3"));
        playlist.add(AudioTrack.create("name4","performer4","http://smithyproductions.co.uk/infinitracks/tempTracks/1WKJCbjwBg.mp3"));
        playlist.add(AudioTrack.create("name4","performer4","http://smithyproductions.co.uk/infinitracks/tempTracks/yXmDdrPDXB.mp3"));
        playlist.add(AudioTrack.create("name4","performer4","http://smithyproductions.co.uk/infinitracks/tempTracks/ss1d47GGkp.mp3"));
        playlist.add(AudioTrack.create("name4","performer4","http://smithyproductions.co.uk/infinitracks/tempTracks/fQWahwjjC3.mp3"));
        playlist.add(AudioTrack.create("name4","performer4","http://smithyproductions.co.uk/infinitracks/tempTracks/Dltax3DKAO.mp3"));
        playlist.add(AudioTrack.create("name4","performer4","http://smithyproductions.co.uk/infinitracks/tempTracks/4atGM6W68q.mp3"));
        playlist.add(AudioTrack.create("name4","performer4","http://smithyproductions.co.uk/infinitracks/tempTracks/voanJbSUrn.mp3"));
        playlist.add(AudioTrack.create("name4","performer4","http://smithyproductions.co.uk/infinitracks/tempTracks/yXmDdrPDXB.mp3"));
        playlist.add(AudioTrack.create("name4","performer4","http://smithyproductions.co.uk/infinitracks/tempTracks/dHj37mQN4d.mp3"));

        player = new AudioPlayerBuilder()
                .setTrackProvider(new PlaylistTrackProvider(playlist))
                .setPlayerEngine(MediaPlayerEngine.class)
                .setAudioEngine(FadingAudioEngine.class)
                .build();

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.play();
            }
        });

        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.pause();
            }
        });

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.nextTrack();
            }
        });

        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.previousTrack();
            }
        });

        player.addProgressListener(new ProgressListener() {
            @Override
            public void onProgress(float progress) {
                progressBar.setProgress((int) (progress * PROGRESS_MAX));
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
}
