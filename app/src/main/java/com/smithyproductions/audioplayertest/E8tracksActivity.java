package com.smithyproductions.audioplayertest;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.smithyproductions.audioplayer.AudioPlayer;
import com.smithyproductions.audioplayer.AudioPlayerBuilder;
import com.smithyproductions.audioplayer.AudioTrack;
import com.smithyproductions.audioplayer.audioEngines.FadingAudioEngine;
import com.smithyproductions.audioplayer.controls.MediaSessionControl;
import com.smithyproductions.audioplayer.controls.NotificationControl;
import com.smithyproductions.audioplayer.controls.ControlAdapter;
import com.smithyproductions.audioplayer.playerEngines.MediaPlayerEngine;
import com.smithyproductions.audioplayertest.e8tracks.E8tracksService;
import com.smithyproductions.audioplayertest.e8tracks.MixSetTrackProvider;
import com.smithyproductions.audioplayertest.e8tracks.models.MixInfoResponse;
import com.smithyproductions.audioplayertest.e8tracks.models.MixResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

public class E8tracksActivity extends AppCompatActivity {

    public static final int PROGRESS_MAX = 1000;
    private AudioPlayer player;
    private TextView performerTextView;
    private TextView titleTextView;
    private Button playPauseButton;
    private Button nextButton;
    private Button previousButton;
    private ProgressBar progressBar;
    MixSetTrackProvider mixSetTrackProvider;
    private String nextMixWebPath;
    private Button loadMixButton;
    private Button resetButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("");

        playPauseButton = (Button) findViewById(R.id.play_pause_button);
        nextButton = (Button) findViewById(R.id.next_button);
        previousButton = (Button) findViewById(R.id.previous_button);
        loadMixButton = (Button) findViewById(R.id.load_mix_button);
        resetButton = (Button) findViewById(R.id.reset_button);

        performerTextView = (TextView) findViewById(R.id.performer_textview);
        titleTextView = (TextView) findViewById(R.id.title_textview);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setMax(PROGRESS_MAX);

        mixSetTrackProvider = new MixSetTrackProvider();

        mixSetTrackProvider.addMixSetTrackProviderInterface(new MixSetTrackProvider.MixSetTrackProviderInterface() {
            @Override
            public void onMixChange(@Nullable MixResponse mixResponse) {
                if(mixResponse != null) {
                    setTitle(mixResponse.name);
                } else {
                    setTitle("");
                }
            }
        });

        Intent intent = getIntent();
        handleIntent(intent);

        AudioPlayer audioPlayer = AudioPlayer.getPlayer();

        audioPlayer.setTrackProvider(mixSetTrackProvider);

        setAudioPlayer(audioPlayer);

        loadMixButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playMixFromUrl("http://8tracks.com/midnight_citizen/fifty-anthems-of-2015.jsonh");
            }
        });

    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        String type = intent.getType();
        String mixUrl = null;

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                List<String> links = new ArrayList<String>();
                Matcher m = Patterns.WEB_URL.matcher(sharedText);
                while (m.find()) {
                    String url = m.group();
                    links.add(url);
                }

                if (links.size() > 0) {
                    mixUrl = links.get(0);
                }

            }
        }

        if(mixUrl == null) {
//            mixUrl = "http://8tracks.com/midnight_citizen/fifty-anthems-of-2015.jsonh";
        }


        if (mixUrl != null) {
            playMixFromUrl(mixUrl);
        } else {
            Toast.makeText(this, "Couldn't extract 8tracks url from intent", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    public void playMixFromUrl(String mixUrl) {

        Log.d("E8tracksActivity", "getting mix from url: " + mixUrl);

        String[] dotParts = mixUrl.split("\\.");
        String[] slashParts = null;

        for (int i = dotParts.length - 1; i >= 0; i--) {
            if(dotParts[i].contains("/")) {
                slashParts = dotParts[i].split("/");
                break;
            }
        }

        if(slashParts != null && slashParts.length >= 2) {

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://8tracks.com")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            E8tracksService service = retrofit.create(E8tracksService.class);

            service.mixInfo(slashParts[slashParts.length - 2], slashParts[slashParts.length - 1]).enqueue(new Callback<MixInfoResponse>() {
                @Override
                public void onResponse(Response<MixInfoResponse> response, Retrofit retrofit) {
                    mixSetTrackProvider.reset();
                    mixSetTrackProvider.loadMix(response.body().mix);
                }

                @Override
                public void onFailure(Throwable t) {

                }
            });
        }
    }

    protected void setAudioPlayer(AudioPlayer audioPlayer) {
        this.player = audioPlayer;



        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPlayPauseUI(player.isAutoPlay());
                if (player.isAutoPlay()) {
                    player.pause();
                } else {
                    player.play();
                }

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

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.stop();
            }
        });

    }

    final ControlAdapter controlInterface = new ControlAdapter() {
        @Override
        public void onTrackChange(AudioTrack track) {
            setTrackTextUI(track);
        }

        @Override
        public void onAutoPlayChange(boolean autoplay) {
            setPlayPauseUI(autoplay);
        }

        @Override
        public void onDataChange(boolean hasData) {
            playPauseButton.setEnabled(hasData);
            previousButton.setEnabled(hasData);
            nextButton.setEnabled(hasData);
        }

        @Override
        public void onProgressChange(float progress) {
            progressBar.setProgress((int) (progress * PROGRESS_MAX));
        }
    };


    private void setPlayPauseUI(boolean autoPlay) {
        if (autoPlay) {
            playPauseButton.setText("Pause");
        } else {
            playPauseButton.setText("Play");
        }
    }

    private void setTrackTextUI(@Nullable AudioTrack track) {
        if (track != null) {
            titleTextView.setText(track.getName());
            performerTextView.setText(track.getPerformer());
        } else if (player.hasData()){
            titleTextView.setText("");
            performerTextView.setText("loading");
        } else {
            titleTextView.setText("");
            performerTextView.setText("no data");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        this.player = AudioPlayer.getPlayer();

        this.player.attachControl(controlInterface);
    }

    @Override
    protected void onPause() {
        super.onPause();

        player.unattachControl(controlInterface);
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
