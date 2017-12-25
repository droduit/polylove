package ch.epfl.sweng.project.network.firebase;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ServiceTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import ch.epfl.sweng.project.models.Avatar;
import ch.epfl.sweng.project.models.Match;
import ch.epfl.sweng.project.models.Message;
import ch.epfl.sweng.project.models.Profile;
import ch.epfl.sweng.project.models.Settings;
import ch.epfl.sweng.project.models.User;
import ch.epfl.sweng.project.models.db.DBHandler;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertTrue;

/**
 * Tests for MessagingService
 * @author Dominique Roduit
 */
@RunWith(AndroidJUnit4.class)
public class MessagingServiceTest {

    @Rule
    public final ServiceTestRule serviceRule = new ServiceTestRule();

    private RemoteMessage message;
    private MessagingService ms;
    private Map<String, String> data = new HashMap<>();
    private String to;
    private static DBHandler db;

    private RemoteMessage createMessage(Map<String, String> data) {
        RemoteMessage.Builder msgBuilder = new RemoteMessage.Builder(to);
        msgBuilder.setData(data);
        return msgBuilder.build();
    }

    @Before
    public void setUp() {
        getTargetContext().deleteDatabase(DBHandler.DATABASE_NAME);
        db = DBHandler.getInstance(getTargetContext());

        ms = new MessagingService();
        ms.setMockContext(getTargetContext());

        String token = "fDQVQcVSwsA:APA91bGSHzhJ6r5qUl3vnD_zQm5AmxvQVOkR3Le1SqT4xqJl7-8Dqi8CS66h-FCj3EPV8d1hFs4J_IUtEuZI-kfJErokg8KAJQX_OhzZei_o6eqTbuK9B-s0SNv8oCtfHDzXlU8pm5d8";
        to = String.format("%s@gcm.googleapis.com", token);

        RemoteMessage.Builder msgBuilder = new RemoteMessage.Builder(to);
        data.put(Match.KEY_MATCH_ID, "1");
        data.put(Match.KEY_USER1_ID, "1");
        data.put(Match.KEY_USER2_ID, "2");
        data.put(Match.KEY_CREATION_TIME, "753643380000");
        msgBuilder.setData(data);
        message = msgBuilder.build();

        SharedPreferences.Editor editor = Settings.getInstance(getTargetContext()).getEditableSettings();
        editor.putBoolean(Settings.NOTIFICATION_KEY, true);
        editor.commit();

        User userMe = new User(1, "dominique.roduit@epfl.ch", "Dominique", "Roduit");
        db.storeUser(userMe);
    }

    @After
    public void tearDown() throws Exception {
        if(db != null) db.close();
    }

    @Test
    public void testWithStartedService() {
        try {
            serviceRule.startService(
                    new Intent(InstrumentationRegistry.getTargetContext(), MessagingService.class)
            );
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testWithBoundService() {
        try {
            IBinder binder = serviceRule.bindService(
                new Intent(InstrumentationRegistry.getTargetContext(), MessagingService.class)
            );
            assertTrue(binder.pingBinder());

        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testOnMessageReceivedDefault() {
        ms.onMessageReceived(message);
    }

    @Test
    public void testWithoutData() {
        RemoteMessage.Builder msgBuilder = new RemoteMessage.Builder(to);
        ms.onMessageReceived(msgBuilder.build());
    }

    @Test
    public void testWithDifferentSubject() {
        data.put(MessagingService.KEY_SUBJECT, "au-hasard");
        ms.onMessageReceived(createMessage(data));
    }

    @Test
    public void testNewMatch() {
        data.put(MessagingService.KEY_SUBJECT, MessagingService.KEY_NEW_MATCH);

        ms.onMessageReceived(createMessage(data));
    }

    @Test
    public void testEndMatch() {
        Match match = new Match(1, 2, Match.State.Pending, new Date());
        db.storeMatch(match);

        data.put(MessagingService.KEY_SUBJECT, MessagingService.KEY_END_MATCH);
        data.put("matchId", "1");
        data.put(Match.KEY_STATE, Match.State.Open.toString());

        ms.onMessageReceived(createMessage(data));
    }

    @Test
    public void testNewMessage() {
        Match match = new Match(1, 2, Match.State.Open, new Date());
        db.storeMatch(match);

        User userMatched = new User(2, "wilai.girardi.nguyen@epfl.ch", "Wilai", "Girardi");
        db.storeUser(userMatched);

        data.put(MessagingService.KEY_SUBJECT, MessagingService.KEY_NEW_MESSAGE);
        data.put(Message.KEY_CONTENT, "Salut");
        data.put(Message.KEY_MATCH_ID, "1");
        data.put(Message.KEY_MESSAGE_ID, "1");
        data.put(Message.KEY_MY_ID, "1");
        data.put(Message.KEY_SENDER_ID, "2");
        data.put(Message.KEY_SENT_AT, "753643380000");
        data.put(Message.KEY_STATUS, Message.Status.SENT.toString());

        ms.onMessageReceived(createMessage(data));
    }


    @Test
    public void testOnSuccessRequestAvatar() {
        Match match = new Match(1, 2, Match.State.Pending, new Date());
        JSONObject jsonObj = null;
        try {
            jsonObj = new JSONObject();

            Avatar avatar = new Avatar(1, Profile.Gender.Male, Avatar.Eye.Green,
                                        Avatar.HairColor.Blond, Avatar.HairStyle.Style1,
                                        Avatar.Skin.Light, Avatar.Shirt.Style1);
            jsonObj.put("avatar", avatar.toJson().put(Avatar.KEY_GENDER, Profile.Gender.Male));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        long partnerId = 2;
        long storedMatchId = db.storeMatch(match);

        ms.onSuccessRequestAvatar(jsonObj, partnerId, storedMatchId, message);
    }



}