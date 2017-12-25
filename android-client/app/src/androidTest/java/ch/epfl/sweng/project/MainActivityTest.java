package ch.epfl.sweng.project;

import android.support.test.espresso.Espresso;
import android.support.test.rule.ActivityTestRule;
import android.support.v7.widget.AppCompatImageButton;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toolbar;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

/**
 * Tests for MainActivity
 * @author Dominique Roduit
 */
public class MainActivityTest {
    private ListView lvMatchs;

    @Rule
    public ActivityTestRule<MainActivity> main = new ActivityTestRule<>(MainActivity.class);

    @Before
    public void setUp() {
        lvMatchs = (ListView)main.getActivity().findViewById(R.id.listView);
    }

    private static Matcher<View> androidHomeMatcher() {
        return allOf(
                withParent(withClassName(is(Toolbar.class.getName()))),
                withClassName(anyOf(
                        is(ImageButton.class.getName()),
                        is(AppCompatImageButton.class.getName())
                )));
    }

    @Test
    public void clickHamburgerIcon() throws Exception {
        // onView(androidHomeMatcher()).perform(click());
        Espresso.pressBack();
    }

    @Test
    public void testCount() {
        main.getActivity();

        if(lvMatchs.getAdapter().getCount() > 0) {
            onView(withId(R.id.listView)).check(matches(isDisplayed()));
        }
    }

    // TODO
    // 1. Cliquer sur ouvrir le swipe menu
    // 2. Cliquer sur chaque item du swipe menu
    // 3. Cliquer sur la View avec l'id R.id.ribbonProfile
    // 4. Fermer le swipe menu

    // 5. Insérer des messages WAIT_SENDING
    // 6. Appeler la fonction sendOfflineMessages

}