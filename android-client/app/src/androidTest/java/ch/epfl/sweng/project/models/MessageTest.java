package ch.epfl.sweng.project.models;

import android.support.test.runner.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static java.lang.System.currentTimeMillis;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

/**
 * @author Christophe
 */
@RunWith(AndroidJUnit4.class)
public class MessageTest {

    private Message msg1,msg2,msg3,msg4;
    private Date date1,date2,date3,date4;
    private long now = currentTimeMillis() / 1000;

    @Before
    public void setUp() throws Exception {
        User user = new User(1, "dominique.roduit@epfl.ch", "Dominique", "Roduit");

        date1 = new Date();
        date2 = new Date((now - 2 * 24 * 60 * 60) * 1000);
        date3 = new Date((now - 20 * 24 * 60 * 60) * 1000);
        date4 = new Date((now - 400 * 24 * 60 * 60) * 1000);

        msg1 = new Message(1, user.getId(), date1, 1, "content", true, Message.Status.SENT);
        msg2 = new Message(1, user.getId(), date2, 1, "content", true, Message.Status.SENT);
        msg3 = new Message(1, user.getId(), date3, 1, "content", true, Message.Status.SENT);
        msg4 = new Message(1, user.getId(), date4, 1, "content", true, Message.Status.SENT);
    }

    @Test
    public void formattedTimeTest() {


        Locale locale = Locale.ENGLISH;

        String time1 = msg1.getFormattedTime();
        String time2 = msg2.getFormattedTime();
        String time3 = msg3.getFormattedTime();
        String time4 = msg4.getFormattedTime();

        String expected1 = new SimpleDateFormat("HH:mm", locale).format(date1);
        String expected2 = new SimpleDateFormat("EEE", locale).format(date2);
        String expected3 = new SimpleDateFormat("MMM dd", locale).format(date3);
        String expected4 = new SimpleDateFormat("dd.MM.yyyy", locale).format(date4);

        assertEquals(expected1, time1);
        assertEquals(expected2, time2);
        assertEquals(expected3, time3);
        assertEquals(expected4, time4);
    }

    @Test
    public void getDateTest() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.ROOT);
        assertEquals(msg1.getDate(), dateFormat.format(date1));
    }

    @Test
    public void fromJSONTest() {
        JSONObject messJSON = new JSONObject();
        try {
            messJSON.put(Message.KEY_STATUS, Message.Status.SENT.toString());
            messJSON.put(Message.KEY_SENT_AT, (new Date()).getTime());
            messJSON.put(Message.KEY_MATCH_ID, 1L);
            messJSON.put(Message.KEY_SENDER_ID, 2L);
            messJSON.put(Message.KEY_MESSAGE_ID, 1L);
            messJSON.put(Message.KEY_CONTENT, "Salut");
            messJSON.put(Message.KEY_MY_ID, 3L);
            Message message = Message.fromJSON(messJSON);
            assertEquals("Salut", message.getContent());

            messJSON.remove(Message.KEY_MY_ID);
            message = Message.fromJSON(messJSON);
            assertFalse(message.isMine());

            messJSON.remove(Message.KEY_MESSAGE_ID);
            messJSON.put("messageId", 1L);
            message = Message.fromJSON(messJSON);
            assertEquals(1L, message.getMsgId());
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}