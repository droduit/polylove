package ch.epfl.sweng.project.fragments;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import ch.epfl.sweng.project.MainActivity;
import ch.epfl.sweng.project.R;
import ch.epfl.sweng.project.models.Avatar;
import ch.epfl.sweng.project.models.Match;
import ch.epfl.sweng.project.models.db.DBHandler;
import ch.epfl.sweng.project.network.MessageHandler;
import ch.epfl.sweng.project.network.NetworkResponseManager;

/**
 * Display the match of the day with a countdown
 * timer until the end of the available time, and button to choose
 * if we are interested in the proposed match.
 *
 * @author Christophe Badoux, Dominique Roduit
 */
public final class PendingFragment extends Fragment {

    /* Alpha value when a button like/dislike is enabled */
    private static final float ALPHA_ENABLE = 1F;
    /* Alpha value when a button like/dislike is disabled */
    private static final float ALPHA_DISABLE = 0.45F;
    /* Duration of the fade animation when toggle button (in milliseconds) */
    private static final int FADE_BT_DURATION = 1000;

    /* Button like (confirm) / unlike (cancel) */
    private ImageView btNo;
    private ImageView btYes;
    private RelativeLayout layoutReady;
    private CountDownTimer timer = null;

    /* Today's match */
    private Match match;
    private Activity activity;
    private static DBHandler db = null;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pending, container, false);

        activity = getActivity();
        Context context = activity.getApplicationContext();
        db = DBHandler.getInstance(getActivity().getApplicationContext());

        btNo = (ImageView) view.findViewById(R.id.btNo);
        btYes = (ImageView) view.findViewById(R.id.btYes);

        ImageView avatarIcon = (ImageView) view.findViewById(R.id.avatarIcon);

        match = db.getMatchOfToday();

        if(match != null){
            Avatar avatar = db.getAvatar(match.getPartnerId());
            if(avatar != null) {
                avatarIcon.setImageBitmap(avatar.getIcon(context));
            }
        }

        btNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(match != null) {
                    match.setState(getActivity().getApplicationContext(), Match.State.Pending);
                    toggleButtons();
                    Toast.makeText(getActivity(), getString(R.string.like_is_canceled), Toast.LENGTH_LONG).show();
                    new NotifyServer(match.getId(), false).execute();
                }
            }
        });
        btYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(match != null) {
                    match.setState(getActivity().getApplicationContext(), Match.State.Confirmed);
                    toggleButtons();
                    Toast.makeText(getActivity(), getString(R.string.liked_this_person), Toast.LENGTH_LONG).show();
                    new NotifyServer(match.getId(), true).execute();
                }
            }
        });

        return view;
    }


    private void createTimer() {
        View view = getView();
        if(view != null) {
            final TextView tvCountdown = (TextView) view.findViewById(R.id.countdown);

            long timeLeft = 0;
            match = db.getMatchOfToday();
            if (match != null) {
                timeLeft = match.getMillisecondsLeft();
                toggleButtons();
            }

            if (timeLeft >= 1000) {
                timer = new CountDownTimer(timeLeft, 1000) {
                    public void onTick(long millisUntilFinished) {
                        int hours = (int) Math.floor(millisUntilFinished / 3600000);
                        int minutes = (int) Math.floor((millisUntilFinished - hours * 3600000) / 60000);
                        int seconds = (int) Math.floor((millisUntilFinished - (hours * 3600000) - (minutes * 60000)) / 1000);
                        tvCountdown.setText(getFormattedHours(hours, minutes, seconds));
                    }

                    public void onFinish() {
                        if (getActivity() != null) {
                            Intent intent = new Intent(getActivity(), MainActivity.class);
                            intent.putExtra(MainFragment.EXTRA_UPDATE_MATCH_STATUS, true);
                            startActivity(intent);
                        }
                    }
                };
            }
        }
    }

    private class NotifyServer extends AsyncTask<Void, Void, Void> {

        private long matchId;
        private boolean confirmed;

        NotifyServer(long matchId, boolean confirmed) {
            this.matchId = matchId;
            this.confirmed = confirmed;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }

        @Override
        protected Void doInBackground(Void... params) {
            MessageHandler.messagerie().getConversationByIdOrCreate(matchId).sendStatus(confirmed, new NetworkResponseManager() {
                @Override
                public void onError(int errorCode) {
                    Log.d("error confirmation", "" + errorCode);
                }

                @Override
                public void onSuccess(JSONObject jsonObj) {
                    Log.d("success confirmation", jsonObj.toString());
                }
            });
            return null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        activity = getActivity();

        createTimer();
        if(timer != null) {
            timer.start();
        }
    }

    private String getFormattedHours(int hours, int minutes, int seconds) {
        return getFormattedDigit(hours) + ":" +
               getFormattedDigit(minutes) + ":" +
               getFormattedDigit(seconds);
    }

    private String getFormattedDigit(int digit) {
        if(digit < 10) {
            return "0"+digit;
        } else {
            return String.valueOf(digit);
        }
    }

    private void toggleButtons() {
        boolean isMatchConfirmed = (match.getState() == Match.State.Confirmed);

        float btYesAlpha = ALPHA_DISABLE;
        float btNoAlpha = ALPHA_ENABLE;
        int bgColorIdFrom = R.color.colorAccent;
        int bgColorIdTo = R.color.mainRibbon;

        if(isMatchConfirmed){
            btYesAlpha = ALPHA_ENABLE;
            btNoAlpha = ALPHA_DISABLE;
            bgColorIdFrom = R.color.mainRibbon;
            bgColorIdTo = R.color.colorAccent;
        }

        btNo.animate().alpha(btNoAlpha).setDuration(FADE_BT_DURATION).start();
        btYes.animate().alpha(btYesAlpha).setDuration(FADE_BT_DURATION).start();

        btNo.setEnabled(isMatchConfirmed);
        btYes.setEnabled(!isMatchConfirmed);


        int colorFrom = ContextCompat.getColor(getActivity(), bgColorIdFrom);
        int colorTo = ContextCompat.getColor(getActivity(), bgColorIdTo);

        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        colorAnimation.setDuration(450); // milliseconds
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                layoutReady = (RelativeLayout)getActivity().findViewById(R.id.layoutReady);
                if(layoutReady != null) {
                    layoutReady.setBackgroundColor((int) animator.getAnimatedValue());
                }
            }

        });
        colorAnimation.start();

    }
}