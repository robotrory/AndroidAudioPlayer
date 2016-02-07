package audioplayertest;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.smithyproductions.audioplayer.AudioTrack;
import com.smithyproductions.audioplayer.trackProviders.TrackProvider;
import com.smithyproductions.audioplayertest.R;

/**
 * Created by rory on 21/01/16.
 */
public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.ViewHolder> {
    private final TrackProvider mTrackProvider;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public View mRootView;
        public TextView mNameText;
        public TextView mPerformerText;
        public View mSpeakerIcon;
        public ViewHolder(View rootView) {
            super(rootView);
            mRootView = rootView;
            mNameText = (TextView) rootView.findViewById(R.id.track_name);
            mPerformerText = (TextView) rootView.findViewById(R.id.track_artist);
            mSpeakerIcon = rootView.findViewById(R.id.speaker_icon);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public TrackAdapter(TrackProvider trackProvider) {
        mTrackProvider = trackProvider;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public TrackAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                         int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.track_item, parent, false);
        // set the view's size, margins, paddings and layout parameters
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final AudioTrack track = mTrackProvider.getTrackList().get(position);
        holder.mNameText.setText(track.getName());
        holder.mPerformerText.setText(track.getArtist());

        if (position % 2 == 0) {
            holder.mRootView.setBackgroundColor(0xFFFFFFFF);
        } else {
            holder.mRootView.setBackgroundColor(0xFFEDEDED);
        }

        if (mTrackProvider.getCurrentTrackIndex() == position) {
            holder.mSpeakerIcon.setVisibility(View.VISIBLE);
        } else {
            holder.mSpeakerIcon.setVisibility(View.INVISIBLE);
        }

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mTrackProvider.getTrackCount();
    }
}