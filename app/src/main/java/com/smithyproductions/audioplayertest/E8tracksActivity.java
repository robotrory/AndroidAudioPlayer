package com.smithyproductions.audioplayertest;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.smithyproductions.audioplayer.Turntable;
import com.smithyproductions.audioplayer.AudioTrack;
import com.smithyproductions.audioplayer.controls.BitmapLoaderControl;
import com.smithyproductions.audioplayer.controls.ControlAdapter;
import com.smithyproductions.audioplayertest.e8tracks.E8tracksService;
import com.smithyproductions.audioplayertest.e8tracks.MixSetTrackProvider;
import com.smithyproductions.audioplayertest.e8tracks.MixTrackAdapter;
import com.smithyproductions.audioplayertest.e8tracks.models.MixInfoResponse;
import com.smithyproductions.audioplayertest.e8tracks.models.MixResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

public class E8tracksActivity extends AppCompatActivity implements BitmapLoaderControl.BitmapLoaderInterface {

    public static final int PROGRESS_MAX = 1000;
    private Turntable player;
    private TextView performerTextView;
    private TextView titleTextView;
    private Button playPauseButton;
    private Button nextButton;
    private Button previousButton;
    private ProgressBar progressBar;
    MixSetTrackProvider mixSetTrackProvider;
    private Button loadMixButton;
    private Button resetButton;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private MixTrackAdapter mAdapter;
    private ImageView artworkImageView;
    private BitmapLoaderControl bitmapLoader;

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
        artworkImageView = (ImageView) findViewById(R.id.artwork_imageview);

        performerTextView = (TextView) findViewById(R.id.performer_textview);
        titleTextView = (TextView) findViewById(R.id.title_textview);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setMax(PROGRESS_MAX);

        Turntable turntable = Turntable.getPlayer();

        if (turntable.getTrackProvider() != null && turntable.getTrackProvider() instanceof MixSetTrackProvider) {
            mixSetTrackProvider = (MixSetTrackProvider) turntable.getTrackProvider();
        } else {
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

            turntable.setTrackProvider(mixSetTrackProvider);
        }

        bitmapLoader = BitmapLoaderControl.getInstance();

        turntable.attachControl(bitmapLoader);

        setAudioPlayer(turntable);

        mRecyclerView = (RecyclerView) findViewById(R.id.track_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mAdapter = new MixTrackAdapter(mixSetTrackProvider);
        mRecyclerView.setAdapter(mAdapter);

        Intent intent = getIntent();
        handleIntent(intent);


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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider =
                (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
        mediaRouteActionProvider.setRouteSelector(player.getMediaRouteManager().getMediaRouteSelector());
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (player != null) {
            player.getMediaRouteManager().beginScan();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.getMediaRouteManager().endScan();
        }
    }

    protected void setAudioPlayer(Turntable turntable) {
        this.player = turntable;

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
                if (!mixSetTrackProvider.canSkip()) {
                    Toast.makeText(E8tracksActivity.this, "Run out of skips", Toast.LENGTH_SHORT).show();
                } else {
                    player.nextTrack();
                }
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
        public void onTrackChange(@Nullable AudioTrack track) {
            setTrackTextUI(track);
            setArtwork(track);
            mAdapter.notifyDataSetChanged();
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


    @Override
    public void onCurrentAudioTrackBitmapReady() {
        setArtwork(player.getTrack());
    }

    private void setArtwork(final AudioTrack audioTrack) {
        if(bitmapLoader.hasBitmapForTrack(audioTrack)) {
            artworkImageView.setImageBitmap(bitmapLoader.getCurrentBitmap());
        } else {
            artworkImageView.setImageBitmap(null);
        }
    }

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
            performerTextView.setText(track.getArtist());
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

        this.player = Turntable.getPlayer();

        this.player.attachControl(controlInterface);

        bitmapLoader.attachBitmapLoaderInterface(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        player.detachControl(controlInterface);

        bitmapLoader.detachBitmapLoaderInterface(this);
    }

}
