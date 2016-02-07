package audioplayertest.e8tracks;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.smithyproductions.audioplayer.Turntable;
import com.smithyproductions.audioplayer.controls.BitmapLoaderControl;
import com.smithyproductions.audioplayer.controls.NotificationControl;
import com.smithyproductions.audioplayertest.R;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import audioplayertest.BasePlayerActivity;
import audioplayertest.e8tracks.models.MixInfoResponse;
import audioplayertest.e8tracks.models.MixResponse;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

public class E8tracksActivity extends BasePlayerActivity implements BitmapLoaderControl.BitmapLoaderInterface {

    private static final boolean AUTO_LOAD_MIX = false;
    MixSetTrackProvider mMixSetTrackProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("");

        Intent intent = getIntent();
        handleIntent(intent);

        mExtraFunctionButton.setText(R.string.action_load_mix);

        mExtraFunctionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playMixFromUrl("http://8tracks.com/midnight_citizen/fifty-anthems-of-2015.jsonh");
            }
        });

        mPreviousButton.setEnabled(false);

    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        String type = intent.getType();
        String mixUrl = null;

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                List<String> links = new ArrayList<>();
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

        if(mixUrl == null && AUTO_LOAD_MIX) {
            mixUrl = "http://8tracks.com/midnight_citizen/fifty-anthems-of-2015.jsonh";
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
                    mMixSetTrackProvider.reset();
                    mMixSetTrackProvider.loadMix(response.body().mix);
                }

                @Override
                public void onFailure(Throwable t) {

                }
            });
        }
    }

    @Override
    protected void setAudioPlayer(Turntable turntable) {
        super.setAudioPlayer(turntable);

        if (turntable.getTrackProvider() != null && turntable.getTrackProvider() instanceof MixSetTrackProvider) {
            mMixSetTrackProvider = (MixSetTrackProvider) turntable.getTrackProvider();
        } else {
            mMixSetTrackProvider = new MixSetTrackProvider();

            mMixSetTrackProvider.addMixSetTrackProviderInterface(new MixSetTrackProvider.MixSetTrackProviderInterface() {
                @Override
                public void onMixChange(@Nullable MixResponse mixResponse) {
                    if (mixResponse != null) {
                        setTitle(mixResponse.name);
                    } else {
                        setTitle("");
                    }
                }
            });

            turntable.setTrackProvider(mMixSetTrackProvider);
        }

        final NotificationControl notificationControl = e8tracksNotificationControl.getInstance(this);
        notificationControl.setPendingIntent(PendingIntent.getActivity(this, 0, new Intent(this, E8tracksActivity.class), 0));
        turntable.attachControl(notificationControl);

        turntable.attachControl(e8tracksMediaSessionControl.getInstance(this));

    }

    @Override
    protected void handleNextClick() {
        if (!mMixSetTrackProvider.canSkip()) {
            Toast.makeText(E8tracksActivity.this, "Run out of skips", Toast.LENGTH_SHORT).show();
        } else {
            mPlayer.nextTrack();
        }
    }
}
