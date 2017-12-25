package ch.epfl.sweng.project;

import android.support.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/**
 * Tests for FillProfileActivity_lang
 * @author Dominique Roduit
 */
public class FillProfileActivity_langTest {

    @Rule
    public ActivityTestRule<FillProfileActivity_lang> main = new ActivityTestRule<>(FillProfileActivity_lang.class);

    @Before
    public void setUp() {

    }

    @Test
    public void checkUIVisibility() {
        main.getActivity();

        onView(withId(R.id.lvLanguages))
                .check(matches(isDisplayed()));

    }


}