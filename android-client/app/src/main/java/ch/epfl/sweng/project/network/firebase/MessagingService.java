package ch.epfl.sweng.project.network.firebase;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import ch.epfl.sweng.project.MainActivity;
import ch.epfl.sweng.project.R;
import ch.epfl.sweng.project.ViewProfileActivity;
import ch.epfl.sweng.project.ChatActivity;
import ch.epfl.sweng.project.fragments.MainFragment;
import ch.epfl.sweng.project.models.Avatar;
import ch.epfl.sweng.project.models.Match;
import ch.epfl.sweng.project.models.Message;
import ch.epfl.sweng.project.models.Profile;
import ch.epfl.sweng.project.models.Settings;
import ch.epfl.sweng.project.models.User;
import ch.epfl.sweng.project.models.db.DBHandler;
import ch.epfl.sweng.project.network.NetworkResponseManager;
import ch.epfl.sweng.project.network.NetworkResponseManagerForImages;
import ch.epfl.sweng.project.network.RequestsHelper;

import static ch.epfl.sweng.project.network.RequestsHelper.requestAvatar;

/**
 * Firebase messaging service handler
 * @author Dominique Roduit
 */
public final class MessagingService extends FirebaseMessagingService {

    private static final String TAG = "MessagingService";

    public static final String KEY_NEW_MESSAGE = "new-message";
    public static final String KEY_NEW_MATCH = "new-match";
    public static final String KEY_END_MATCH = "end-match";
    public static final String KEY_SUBJECT = "subject";

    private Context context;
    private boolean isTesting = false;

    //private MessagingService() {}

    /**
     * Used ONLY to test this class !
     * @param ctx Mock context
     */
    public void setMockContext(Context ctx) {
        this.context = ctx;
        this.isTesting = true;
    }

    /**
     * Display log in the console with all relevant information
     * @param remoteMessage All data sent by the server
     */
    public void displayLog(RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
        }

        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }

        Log.d(TAG, "Collapse Key : " + remoteMessage.getCollapseKey());
        Log.d(TAG, "Message Type : " + remoteMessage.getMessageType());
        Log.d(TAG, "Message Id : " + remoteMessage.getMessageId());
        Log.d(TAG, "To : " + remoteMessage.getTo());
        Log.d(TAG, "TTL : " + remoteMessage.getTtl());
        Log.d(TAG, "Sent Time : " + remoteMessage.getSentTime());
    }

    @Override
    public void onMessageReceived(final RemoteMessage remoteMessage) {
        if(!isTesting) {
            context = getApplicationContext();
        }

        displayLog(remoteMessage);

        if (remoteMessage.getData().size() > 0) {
            final Map<String, String> data = remoteMessage.getData();
            String subject = data.get(KEY_SUBJECT);

            if(subject != null) {
                switch (subject) {
                    // New match proposal : Match.Status = Pending --------------------------------
                    case KEY_NEW_MATCH:
                        newMatchReceived(remoteMessage);
                    break;
                    // End of a match has rang ----------------------------------------------------
                    case KEY_END_MATCH:
                        endMatchReceived(remoteMessage);
                    break;
                    // New message arrival -------------------------------------------------------
                    case KEY_NEW_MESSAGE:
                        newMessageReceived(remoteMessage);
                    break;

                    default:
                        displayNotification(remoteMessage, context.getString(R.string.notif_title_default),
                                context.getString(R.string.notif_content_default), MainActivity.class, null);
                }
            }
        } else {
            displayNotification(remoteMessage, context.getString(R.string.notif_title_default),
                    context.getString(R.string.notif_content_default), MainActivity.class, null);
        }
    }

    /**
     * Handle the reception of a new match proposal from the server
     * @param remoteMessage Message containing all the data sent by the server
     */
    private void newMatchReceived(final RemoteMessage remoteMessage) {
        final Map<String, String> data = remoteMessage.getData();
        data.put(Match.KEY_STATE, Match.State.Pending.toString());

        Match newMatchProposal = null;
        try {
            newMatchProposal = Match.fromJSON(new JSONObject(data), context);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(newMatchProposal != null) {
            final long storedMatchId = DBHandler.getInstance(context).storeMatch(newMatchProposal);
            final long partnerId = newMatchProposal.getPartnerId();
            requestAvatar(partnerId, new NetworkResponseManager() {
                @Override
                public void onError(int errorCode) { }
                @Override
                public void onSuccess(JSONObject jsonObj) {
                    onSuccessRequestAvatar(jsonObj, partnerId, storedMatchId, remoteMessage);
                }
            });
        }
    }

    /** Separate method to be testable separately */
    public void onSuccessRequestAvatar(JSONObject jsonObj, long partnerId, long storedMatchId, RemoteMessage remoteMessage) {
        try {
            Avatar avatar;
            if (!jsonObj.isNull("avatar")) {
                JSONObject avatarJSON = jsonObj.getJSONObject("avatar");
                avatarJSON.put(Avatar.KEY_ID, partnerId);

                if (!jsonObj.isNull(Avatar.KEY_GENDER)) {
                    avatarJSON.put(Avatar.KEY_GENDER, jsonObj.getString(Avatar.KEY_GENDER));
                }

                avatar = Avatar.fromJSON(avatarJSON);
                DBHandler.getInstance(context).storeAvatar(avatar);
            }

            if (storedMatchId != -1) {
                Intent receiverIntent = new Intent();
                receiverIntent.setAction(MainFragment.ACTION_RELOAD);
                receiverIntent.putExtra(MainFragment.EXTRA_RELOAD, "new-match");
                context.sendBroadcast(receiverIntent);
                notifyNewMatch(remoteMessage);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    /**
     * Handle the reception of the end of a match from the server
     * @param remoteMessage Message containing all the data sent by the server
     */
    private void endMatchReceived(final RemoteMessage remoteMessage) {
        final Map<String, String> data = remoteMessage.getData();

        Match.State newState = Match.State.valueOf(data.get(Match.KEY_STATE));
        long matchId = Long.parseLong(data.get("matchId"));

        if(DBHandler.getInstance(context).updateMatch(matchId, newState)) {
            final Match match = DBHandler.getInstance(context).getMatch(matchId);

            if(match != null) {
                if (newState == Match.State.Open) {
                    final Intent receiverIntent = new Intent();
                    receiverIntent.setAction(MainFragment.ACTION_RELOAD);
                    receiverIntent.putExtra(MainFragment.EXTRA_RELOAD, "end-match");

                    RequestsHelper.requestUser(match.getPartnerId(), new NetworkResponseManager() {
                        @Override
                        public void onError(int errorCode) {
                            context.sendBroadcast(receiverIntent);
                        }

                        @Override
                        public void onSuccess(JSONObject jsonObj) {
                            try {
                                User user = User.fromJSON(jsonObj.getJSONObject("user"));

                                JSONObject profileJSON = jsonObj.getJSONObject("profile");
                                profileJSON.put(Profile.KEY_ID, user.getId());
                                final Profile profile = Profile.fromJSON(profileJSON);

                                JSONObject avatarJSON = jsonObj.getJSONObject("avatar");
                                avatarJSON.put(Avatar.KEY_ID, user.getId());
                                avatarJSON.put(Avatar.KEY_GENDER, profile.getGender().toString());
                                Avatar avatar = Avatar.fromJSON(avatarJSON);

                                DBHandler.getInstance(context).storeProfile(profile);

                                RequestsHelper.requestPhoto(user.getId(), new NetworkResponseManagerForImages() {
                                    @Override
                                    public void onError(int errorCode) { }

                                    @Override
                                    public void onSuccess(Bitmap bitmap) {
                                        if(bitmap != null) {
                                            profile.setPhoto(bitmap);
                                            DBHandler.getInstance(context).storeProfile(profile);
                                        }
                                    }
                                });

                                DBHandler.getInstance(context).storeUser(user);
                                DBHandler.getInstance(context).storeAvatar(avatar);

                                context.sendBroadcast(receiverIntent);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    });

                }
            }

            notifyMatchConfirmation(remoteMessage, match);
        }
    }


    /**
     * Handle the reception of a new message from the server
     * @param remoteMessage Message containing all the data sent by the server
     */
    private void newMessageReceived(final RemoteMessage remoteMessage) {
        final Map<String, String> data = remoteMessage.getData();

        // Store message in local database
        if(ChatActivity.storeNewReceivedMessage(data, context)) {
            Intent receiverIntent = new Intent();
            receiverIntent.setAction(ChatActivity.ACTION_RELOAD);
            context.sendBroadcast(receiverIntent);

            Intent receiverListView = new Intent();
            receiverListView.setAction(MainFragment.ACTION_RELOAD);
            receiverListView.putExtra(MainFragment.EXTRA_RELOAD, "new-message");
            context.sendBroadcast(receiverListView);

            // display notification for new message
            notifyNewMessage(remoteMessage);
        }
    }

    /** ************************************************************************************ **/
    /** Notifications ---------------------------------------------------------------------- **/
    /** ************************************************************************************ **/
    /**
     * Handle the custom notification when a new match proposal is received
     * @param message Message containing all the data sent by the server
     */
    private void notifyNewMatch(RemoteMessage message) {
        String title = context.getString(R.string.notif_title_new_match);
        String content = context.getString(R.string.notif_content_new_match);

        displayNotification(message, title, content, MainActivity.class, null);
    }

    /**
     * Handle the custom notification when a new message is received
     * @param message Message containing all the data sent by the server
     */
    private void notifyNewMessage(RemoteMessage message) {
        Map<String, String> data = message.getData();

        long senderId = Long.parseLong(data.get(Message.KEY_SENDER_ID));
        Match relatedMatch = DBHandler.getInstance(context).getMatchWithUser(senderId);
        long matchId = -1;

        String title = context.getString(R.string.notif_title_new_message);
        String content = context.getString(R.string.notif_content_new_message);

        if(relatedMatch != null) {
            matchId = relatedMatch.getId();
            if(relatedMatch.getState() == Match.State.Open) {
                User usr = DBHandler.getInstance(context).getUser(senderId);
                if (usr != null) {
                    title = usr.getFullName();
                }
            }
        }

        if(data.containsKey("content")) {
            content = data.get("content");
        }

        Bundle extras = new Bundle();
        extras.putLong(ChatActivity.EXTRA_DEST_ID, senderId);
        extras.putLong(ChatActivity.EXTRA_MATCH_ID, matchId);


        displayNotification(message, title, content, ChatActivity.class, extras);
    }

    /**
     * Handle the custom notification when the end of a match is received
     * @param message Message containing all the data sent by the server
     * @param match New match created according to the received data
     */
    private void notifyMatchConfirmation(RemoteMessage message, Match match) {
        // Map<String, String> data = message.getData();
        if(match.getState() == Match.State.Open) {
            Bundle extras = new Bundle();
            extras.putLong(ViewProfileActivity.EXTRA_USER_ID, match.getPartnerId());
            extras.putBoolean(ViewProfileActivity.EXTRA_MATCH_CONFIRMATION, true);

            String title = context.getString(R.string.notif_title_match_confirmation);
            String content = context.getString(R.string.notif_content_match_confirmation);

            displayNotification(message, title, content, ViewProfileActivity.class, extras);
        }
    }


    /**
     * Display a notification in the system tray
     * @param message Message containing all information received by Firebase
     * @param title Title displayed in the notification
     * @param content Content text displayed in the notification
     * @param calledIntent Intent we want to call when the notification is clicked by the user
     * @param extras Extras data to pass to the called intent. Set null if don't want to pass extra data
     */
    private void displayNotification(RemoteMessage message, String title, String content, Class<?> calledIntent, Bundle extras) {

        // ------------------------------------------------------------
        Intent intent = new Intent(context, calledIntent);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if(extras != null) {
            intent.putExtras(extras);
        }
        // ------------------------------------------------------------

        if(Settings.getInstance(context).hasUserNotifications()){
            int notifyID = 1;
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(notifyID, getNotification(message, title, content, intent));
        }
    }

    /**
     * Get a custom notification object configured according to the given parameters
     * @param message Message containing all information received by Firebase
     * @param title Title displayed in the notification
     * @param content Content text displayed in the notification
     * @param intent Intent we want to call when the notification is clicked by the user
     * @return Customized Notification object
     */
    private Notification getNotification(RemoteMessage message, String title, String content, final Intent intent) {
        int numMessages = 0;

        Resources resources = context.getResources(),
                              systemResources = Resources.getSystem();

        Uri notificationSoundURI = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context)
        .setSmallIcon(R.drawable.ic_stat_name)
        .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
        .setAutoCancel(true)
        .setVibrate(new long[] { 1000, 1000 })
        .setSound(notificationSoundURI)
        .setLights(ContextCompat.getColor(context, systemResources.getIdentifier("config_defaultNotificationColor", "color", "android")),
                resources.getInteger(systemResources.getIdentifier("config_defaultNotificationLedOn", "integer", "android")),
                resources.getInteger(systemResources.getIdentifier("config_defaultNotificationLedOff", "integer", "android")))
        .setNumber(++numMessages);

        PendingIntent resultIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if(resultIntent != null) {
            notifBuilder.setContentIntent(resultIntent);
        }

        if(message.getNotification() != null) {
            notifBuilder.setContentTitle(message.getNotification().getTitle());
            notifBuilder.setContentText(message.getNotification().getBody());
        } else {
            if(title != null) {
                notifBuilder.setContentTitle(title);
            }
            if(content != null) {
                notifBuilder.setContentText(content);
            }
        }

        return notifBuilder.build();
    }
}
