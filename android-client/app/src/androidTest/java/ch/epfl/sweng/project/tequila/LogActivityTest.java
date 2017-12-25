package ch.epfl.sweng.project.tequila;

import android.content.Intent;
import android.support.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ch.epfl.sweng.project.ViewProfileActivity;

/**
 * Created by Lucie
 */

public class LogActivityTest {

    private static final String EXTRA_USER_ID = "aa";
    @Rule
    public ActivityTestRule<LogActivity> main = new ActivityTestRule<>(LogActivity.class);

    @Before
    public void setUp() {
        Intent intent = new Intent(main.getActivity().getApplicationContext(), ViewProfileActivity.class);
        intent.putExtra(EXTRA_USER_ID, 0);
        main.launchActivity(intent);
    }

    @Test
    public void runActivity() {
        Intent intent = new Intent(main.getActivity().getApplicationContext(), ViewProfileActivity.class);
        intent.putExtra(EXTRA_USER_ID, 0);
        main.launchActivity(intent);
    }
}
