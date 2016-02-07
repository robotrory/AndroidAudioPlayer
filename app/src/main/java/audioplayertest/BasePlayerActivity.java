package audioplayertest;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteActionProvider;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.smithyproductions.audioplayer.MediaRouter.MediaRouteManager;
import com.smithyproductions.audioplayer.Turntable;
import com.smithyproductions.audioplayer.AudioTrack;
import com.smithyproductions.audioplayer.controls.BitmapLoaderControl;
import com.smithyproductions.audioplayer.controls.ControlAdapter;
import com.smithyproductions.audioplayer.interfaces.ControlType;
import com.smithyproductions.audioplayertest.R;

public class BasePlayerActivity extends AppCompatActivity implements BitmapLoaderControl.BitmapLoaderInterface {

    public static final int PROGRESS_MAX = 1000;
    protected Turntable mPlayer;
    private TextView mPerformerTextView;
    private TextView mTitleTextView;
    private Button mPlayPauseButton;
    private Button mNextButton;
    protected Button mPreviousButton;
    private ProgressBar mProgressBar;
    protected Button mExtraFunctionButton;
    private Button mResetButton;
    protected RecyclerView mRecyclerView;
    private ImageView mArtworkImageView;
    private BitmapLoaderControl mBitmapLoader;
    private @Nullable TrackAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mPlayPauseButton = (Button) findViewById(R.id.play_pause_button);
        mNextButton = (Button) findViewById(R.id.next_button);
        mPreviousButton = (Button) findViewById(R.id.previous_button);
        mExtraFunctionButton = (Button) findViewById(R.id.extra_function_button);
        mResetButton = (Button) findViewById(R.id.reset_button);
        mArtworkImageView = (ImageView) findViewById(R.id.artwork_imageview);

        mPerformerTextView = (TextView) findViewById(R.id.performer_textview);
        mTitleTextView = (TextView) findViewById(R.id.title_textview);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        mProgressBar.setMax(PROGRESS_MAX);

        setAudioPlayer(Turntable.getPlayer());

        mRecyclerView = (RecyclerView) findViewById(R.id.track_recycler_view);

        if (mPlayer.getTrackProvider() != null) {
            mAdapter = new TrackAdapter(mPlayer.getTrackProvider());
            mRecyclerView.setAdapter(mAdapter);
        }
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem mediaRouteMenuItem = menu.findItem(R.id.media_route_menu_item);
        MediaRouteActionProvider mediaRouteActionProvider =
                (MediaRouteActionProvider) MenuItemCompat.getActionProvider(mediaRouteMenuItem);
        mediaRouteActionProvider.setRouteSelector(MediaRouteManager.getUiComponent().getMediaRouteSelector());
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mPlayer != null) {
            MediaRouteManager.getUiComponent().beginScan();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mPlayer != null) {
            MediaRouteManager.getUiComponent().endScan();
        }
    }

    protected void setAudioPlayer(Turntable turntable) {
        this.mPlayer = turntable;


        mBitmapLoader = BitmapLoaderControl.getInstance();

        this.mPlayer.attachControl(mBitmapLoader);

        mPlayPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handlePlayPauseClick();
            }
        });

        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleNextClick();
            }
        });

        mPreviousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handlePreviousClick();
            }
        });

        mResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleResetClick();
            }
        });

    }

    protected void handlePlayPauseClick() {
        setPlayPauseUI(mPlayer.isAutoPlay());
        if (mPlayer.isAutoPlay()) {
            mPlayer.pause();
        } else {
            mPlayer.play();
        }
    }

    protected void handleResetClick() {
        mPlayer.stop();
    }

    protected void handlePreviousClick() {
        mPlayer.previousTrack();
    }

    protected void handleNextClick() {
        mPlayer.nextTrack();
    }

    final ControlAdapter controlInterface = new ControlAdapter() {
        @Override
        public void onTrackChange(@Nullable AudioTrack track) {
            handleOnTrackChange(track);
        }

        @Override
        public void onAutoPlayChange(boolean autoPlay) {
            handleOnAutoPlayChange(autoPlay);
        }

        @Override
        public void onDataChange(boolean hasData) {
            handleOnDataChange(hasData);
        }

        @Override
        public ControlType getControlType() {
            return ControlType.MISCELLANEOUS;
        }

        @Override
        public void onProgressChange(float progress) {
            handleOnProgressChange(progress);
        }
    };

    protected void handleOnProgressChange(float progress) {
        mProgressBar.setProgress((int) (progress * PROGRESS_MAX));
    }

    protected void handleOnDataChange(boolean hasData) {
        mPlayPauseButton.setEnabled(hasData);
        mPreviousButton.setEnabled(hasData);
        mNextButton.setEnabled(hasData);
    }

    protected void handleOnAutoPlayChange(boolean autoPlay) {
        setPlayPauseUI(autoPlay);
    }

    protected void handleOnTrackChange(@Nullable AudioTrack track) {
        setTrackTextUI(track);
        setArtwork(track);
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }


    @Override
    public void onCurrentAudioTrackBitmapReady() {
        setArtwork(mPlayer.getTrack());
    }

    protected void setArtwork(final AudioTrack audioTrack) {
        if(mBitmapLoader.hasBitmapForTrack(audioTrack)) {
            mArtworkImageView.setImageBitmap(mBitmapLoader.getCurrentBitmap());
        } else {
            mArtworkImageView.setImageBitmap(null);
        }
    }

    protected void setPlayPauseUI(boolean autoPlay) {
        if (autoPlay) {
            mPlayPauseButton.setText(R.string.action_pause);
        } else {
            mPlayPauseButton.setText(R.string.action_play);
        }
    }

    protected void setTrackTextUI(@Nullable AudioTrack track) {
        if (track != null) {
            mTitleTextView.setText(track.getName());
            mPerformerTextView.setText(track.getArtist());
        } else if (mPlayer.hasData()){
            mTitleTextView.setText("");
            mPerformerTextView.setText(R.string.status_loading);
        } else {
            mTitleTextView.setText("");
            mPerformerTextView.setText(R.string.status_no_data);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        this.mPlayer = Turntable.getPlayer();

        this.mPlayer.attachControl(controlInterface);

        mBitmapLoader.attachBitmapLoaderInterface(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mPlayer.detachControl(controlInterface);

        mBitmapLoader.detachBitmapLoaderInterface(this);
    }

}
