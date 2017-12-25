package ch.epfl.sweng.project;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.test.rule.ActivityTestRule;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.RelativeLayout;

import org.junit.Rule;
import org.junit.Test;

import ch.epfl.sweng.project.models.Avatar;
import ch.epfl.sweng.project.models.Settings;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeLeft;
import static android.support.test.espresso.action.ViewActions.swipeRight;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static junit.framework.Assert.assertEquals;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;

/**
 * @author Christophe Badoux & Tim Nguyen
 */
public class EditAvatarTest {

    @Rule
    public ActivityTestRule<EditAvatar> main = new ActivityTestRule<>(EditAvatar.class);

/*
    public EditAvatarTest() {
        super(EditAvatar.class);
    }
*/

    /*
    @Override
    public void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
    }

    */

    @Test
    public void test() throws Exception {
        int skinTone;
        int hairColor;
        int eyeColor;

        AppCompatActivity activity = main.getActivity();

        SharedPreferences.Editor editableSettings = Settings.getInstance(activity.getApplicationContext()).getEditableSettings();
        editableSettings.putInt("Shirt", 0);
        editableSettings.putInt("HairStyle", 0);

        SharedPreferences avatar = Settings.getInstance(activity.getApplicationContext()).getObject();

        RelativeLayout tipLayout = (RelativeLayout) activity.findViewById(R.id.tipLayout);
        if (tipLayout.getVisibility() == View.VISIBLE) {
            onView(withId(R.id.tipLayout)).perform(click());
        }

        // test skin box
        onView(withId(R.id.skin)).perform(click());
        onData(allOf(is(instanceOf(String.class)))).atPosition(2).perform(click());
        skinTone = avatar.getInt("Skin", 3);
        assertEquals(2, skinTone);

        // test haircolor box
        onView(withId(R.id.hairColor)).perform(click());
        onData(allOf(is(instanceOf(String.class)))).atPosition(2).perform(click());
        hairColor = avatar.getInt("HairColor", 3);
        assertEquals(2, hairColor);

        // test eyes box
        onView(withId(R.id.eyes)).perform(click());
        onData(allOf(is(instanceOf(String.class)))).atPosition(2).perform(click());
        eyeColor = avatar.getInt("Eyes", 3);
        assertEquals(2, eyeColor);

        // test right swipe (hair)
        Avatar.HairStyle nextHairStyle = Avatar.HairStyle.Style2;
        onView(withId(R.id.blank_text_for_test)).perform(swipeLeft());
        Avatar.HairStyle checkHairStyle = Avatar.HairStyle.values()[avatar.getInt("HairStyle", 3)];
        assertEquals(nextHairStyle, checkHairStyle);

        // test left swipe (hair)
        nextHairStyle = Avatar.HairStyle.Style1;
        onView(withId(R.id.blank_text_for_test)).perform(swipeRight());
        checkHairStyle = Avatar.HairStyle.values()[avatar.getInt("HairStyle", 3)];
        assertEquals(nextHairStyle, checkHairStyle);

        // test right swipe (clothes)
        Avatar.Shirt nextShirt = Avatar.Shirt.Style2;
        onView(withId(R.id.shirtImage)).perform(swipeLeft());
        Avatar.Shirt checkShirt = Avatar.Shirt.values()[avatar.getInt("Shirt", 3)];
        assertEquals(nextShirt, checkShirt);

        // test left swipe (clothes)
        nextShirt = Avatar.Shirt.Style1;
        onView(withId(R.id.shirtImage)).perform(swipeRight());
        checkShirt = Avatar.Shirt.values()[avatar.getInt("Shirt", 3)];
        assertEquals(nextShirt, checkShirt);

        activity.finish();
    }
}