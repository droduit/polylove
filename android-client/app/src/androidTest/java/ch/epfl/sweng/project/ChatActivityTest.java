package ch.epfl.sweng.project;

import android.content.Context;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.widget.ListView;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/**
 * @author Tim Nguyen, Dominique Roduit
 */
@RunWith(AndroidJUnit4.class)
public class ChatActivityTest {

    private ListView lvMessages;
    private Context context;

    @Rule
    public ActivityTestRule<ChatActivity> main = new ActivityTestRule<>(ChatActivity.class);


    @Before
    public void setUp() throws Exception {
        lvMessages = (ListView) main.getActivity().findViewById(R.id.msgListView);
    }


    /*
     * This test locate the text field, write the messages and click on the send button.
     * It checks if the sent messages is correct.
     */
    @Test
    public void sendingMsgReturnReversedMsg() {
        String message = "Salut les Pudding Celebration ! :D";
        onView(withId(R.id.msgEditText)).perform(typeText(message), closeSoftKeyboard());
        onView(withId(R.id.sendMessageButton)).perform(click());
        onView(withId(R.id.textViewMsg)).check(matches(isDisplayed()));
    }


    @Test
    public void msgReceivedIsDisplayed() {
        Map<String, String> message = new HashMap<>();
        message.put("msgId", "12345");
        message.put("matchId", "11111");
        message.put("userId", "00000");
        message.put("content", "Pudding");
        message.put("dateTime", "");
        //displayReceivedMsg(message, context);
    }

    @Test
    public void testActionBar() {
        onView(withContentDescription(R.string.abc_action_bar_up_description)).perform(click());
    }

}