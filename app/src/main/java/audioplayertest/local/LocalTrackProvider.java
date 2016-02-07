package audioplayertest.local;

import com.smithyproductions.audioplayer.AudioTrack;
import com.smithyproductions.audioplayer.trackProviders.PlaylistTrackProvider;

import java.util.List;

/**
 * Created by rory on 07/02/16.
 */
public class LocalTrackProvider extends PlaylistTrackProvider {
    public LocalTrackProvider(List<AudioTrack> trackList) {
        super(trackList);
    }
}
