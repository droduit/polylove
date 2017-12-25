package ch.epfl.sweng.project;

import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.support.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ch.epfl.sweng.project.models.Settings;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for SettingsActivity
 * @author Dominique Roduit
 */
public class SettingsActivityTest {
    private PreferenceFragment fragment;

    @Rule
    public ActivityTestRule<SettingsActivity> main = new ActivityTestRule<>(SettingsActivity.class);

    @Before
    public void setUp() {
        fragment = (PreferenceFragment) main.getActivity().getFragmentManager().findFragmentById(R.id.mainContent);
    }

    @Test
    public void changeNotification() {
        onView(withText(R.string.notifications))
                .perform(click())
                .check(matches(isDisplayed()));
    }

    @Test
    public void changeAutoLogin() {
        onView(withText(R.string.stay_connected))
                .perform(click())
                .check(matches(isDisplayed()));
    }

    /*

    @Test
    public void changeLanguage() {
        onView(withText(R.string.language)).perform(click());
        onView(withText(Settings.getLanguageEntries()[0])).perform(click());
        onView(withText(R.string.language)).check(matches(isDisplayed()));

        ListPreference languagePref = (ListPreference) fragment.findPreference("language");
        assertThat(languagePref.getValue(), is("0"));
    }

    */

    @Test
    public void validateActivity() {
        onView(withId(R.id.save_action)).perform(click());
    }

}