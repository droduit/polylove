package ch.epfl.sweng.project;

import android.content.Intent;
import android.support.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/**
 * Tests for FullImageActivity
 * @author Dominique Roduit
 */
public class FullImageActivityTest {

    @Rule
    public ActivityTestRule<FullImageActivity> main = new ActivityTestRule<>(FullImageActivity.class);

    @Before
    public void setUp() {

    }

    @Test
    public void testDisplayUI() {
        main.getActivity();
        onView(withId(R.id.fullImage)).check(matches(isDisplayed()));
    }

    @Test
    public void testWithBundle() {
        Intent intent = new Intent();
        intent.putExtra(ViewProfileActivity.EXTRA_USER_ID, 1);
        main.launchActivity(intent);
    }

    @Test
    public void testActionBar() {
        onView(withContentDescription(R.string.abc_action_bar_up_description)).perform(click());
    }

}