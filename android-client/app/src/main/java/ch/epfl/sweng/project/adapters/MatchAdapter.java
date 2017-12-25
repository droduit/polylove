package ch.epfl.sweng.project.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.makeramen.roundedimageview.RoundedImageView;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.epfl.sweng.project.R;
import ch.epfl.sweng.project.models.Avatar;
import ch.epfl.sweng.project.models.Match;
import ch.epfl.sweng.project.models.Message;
import ch.epfl.sweng.project.models.Profile;
import ch.epfl.sweng.project.models.db.DBHandler;

/**
 * @author Christophe Badoux
 */
public final class MatchAdapter extends ArrayAdapter<Match> {

    private static LayoutInflater inflater = null;
    private List<Match> matchs;
    private Map<Long, Bitmap> loadedImages;

    public MatchAdapter(Context context, List<Match> matchs) {
        super(context, 0, matchs);
        this.matchs = matchs;
        inflater = ((Activity) context).getLayoutInflater();
        loadedImages = new HashMap<>();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = inflater.inflate(R.layout.conversation, parent, false);
        TextView name = (TextView) convertView.findViewById(R.id.name);
        RoundedImageView avatar = (RoundedImageView) convertView.findViewById(R.id.avatarUserMatched);
        TextView tvLastMessage = (TextView) convertView.findViewById(R.id.lastMessage);
        TextView time = (TextView) convertView.findViewById(R.id.time);
        LinearLayout wrapper = (LinearLayout) convertView.findViewById(R.id.wrapperMatchLayout);

        Match match = getItem(position);


        String fullName = match.getFriendlyUsername(getContext());
        name.setText(fullName);

        // Last message & Date ---------
        Message lastMessage = match.getLastMessage(getContext());
        String timeMsg = "";
        String contentMsg = getContext().getString(R.string.write_to_your_match);
        if (lastMessage != null) {
            timeMsg = lastMessage.getFormattedTime();
            contentMsg = lastMessage.getContent();
        }
        tvLastMessage.setText(contentMsg);
        time.setText(timeMsg);

        int resColor = R.color.colorAccent;
        switch (match.getState()) {
            case Pending:
            case Confirmed:
                resColor = R.color.orange;
                break;
            case Close:
                resColor = R.color.icon_grey;
                break;
            case Open:
                resColor = R.color.colorAccent;
                break;
        }

        new AvatarIconTask(position, match.getId(), avatar).execute();

        avatar.setBorderColor(ContextCompat.getColor(getContext(), resColor));

        boolean pendingState = match.getState() == Match.State.Pending ||
                match.getState() == Match.State.Confirmed;
        if (pendingState) {
            ViewGroup.LayoutParams params = avatar.getLayoutParams();
            params.height = 160;
            params.width = 160;

            wrapper.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.todayMatchRow));
            wrapper.setPadding(wrapper.getPaddingLeft(), wrapper.getPaddingTop() + 20, wrapper.getPaddingRight(), wrapper.getPaddingBottom() + 20);

            name.setTextSize(17.5F);
            name.setTextColor(ContextCompat.getColor(getContext(), R.color.todayMatchName));
        }

        return convertView;
    }

    class AvatarIconTask extends AsyncTask<Integer, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private int position = 0;
        private long matchId;

        public AvatarIconTask(Integer position, long matchId, ImageView imageView) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<>(imageView);
            this.position = position;
            this.matchId = matchId;
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(Integer... params) {
            DBHandler db = DBHandler.getInstance(getContext());

            if(!loadedImages.containsKey(matchId)) {
                if (position < getCount()) {
                    Match match = getItem(position);
                    Avatar partnerAvatar = db.getAvatar(match.getPartnerId());
                    if (match.getState() == Match.State.Open) {
                        Profile partnerProfile = db.getProfile(match.getPartnerId());
                        if (partnerProfile != null) {
                            if (partnerProfile.getPhoto() != null) {
                                return partnerProfile.getPhoto();
                            } else {
                                if (partnerAvatar != null) {
                                    return partnerAvatar.getIcon(getContext());
                                }
                            }
                        }
                    } else {
                        if (partnerAvatar != null) {
                            return partnerAvatar.getIcon(getContext());
                        }
                    }
                }
            } else {
                return loadedImages.get(matchId);
            }
            return null;
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                    loadedImages.put(matchId, bitmap);
                }
            }
        }
    }
}
