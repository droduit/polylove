package ch.epfl.sweng.project;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.test.rule.ActivityTestRule;
import android.widget.LinearLayout;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.sql.Date;
import java.util.Set;
import java.util.TreeSet;

import ch.epfl.sweng.project.models.Avatar;
import ch.epfl.sweng.project.models.Match;
import ch.epfl.sweng.project.models.Profile;
import ch.epfl.sweng.project.models.User;
import ch.epfl.sweng.project.utils.DBUtils;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/**
 * Tests for ViewProfileActivity
 *
 * @author Dominique Roduit / Lucie
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ViewProfileActivityTest {
    private static final String EXTRA_USER_ID = "userId";

    private final User user = new User(123, "aa", "aa", "aa");
    private final Profile profile1 = new Profile();
    private final Profile profile2 = new Profile(123, Profile.Section.AR, Profile.Gender.Male, "1990-02-02",
            "ok", "cdescir", "2",
            Profile.GenderInterest.Both, 20, 40);
    private final Avatar avatar = new Avatar(123, Profile.Gender.Female, Avatar.Eye.Blue,
            Avatar.HairColor.Blond, Avatar.HairStyle.Style2,
            Avatar.Skin.Dark, Avatar.Shirt.Style2);

    private static final String DESCRIPTION = "Je suis gentil";
    private static final int AGE_START = 16;
    private static final int AGE_END = 25;
    private Profile profile;
    private Set<Profile.Language> languages;


    private void launchIntent() {
        Intent intent = new Intent(main.getActivity().getApplicationContext(), ViewProfileActivity.class);
        intent.putExtra(EXTRA_USER_ID, 0);
        //main.launchActivity(intent);
        main.getActivity().startActivity(intent);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Rule
    public ActivityTestRule<ViewProfileActivity> main = new ActivityTestRule<>(ViewProfileActivity.class);


    @Before
    public void setUp() {
        Set<String> interests = new TreeSet<>();
        interests.add("Guitare");
        interests.add("Maths");
        interests.add("Sweng");

        languages = new TreeSet<>();
        languages.add(Profile.Language.French);
        languages.add(Profile.Language.English);

        profile = new Profile(0,
                Profile.Section.IN,
                Profile.Gender.Male,
                DBUtils.StringToDate("1993-11-18"),
                interests,
                DESCRIPTION,
                languages,
                Profile.GenderInterest.Female,
                AGE_START, AGE_END);

        launchIntent();
    }

    @Test
    public void profileCheck() {
        FloatingActionButton but = (FloatingActionButton) main.getActivity().findViewById(R.id.btnActionMessage);
        LinearLayout lin = (LinearLayout) main.getActivity().findViewById(R.id.profileLayout);
        CollapsingToolbarLayout col = (CollapsingToolbarLayout) main.getActivity().findViewById(R.id.collapsingToolbar);

        main.getActivity().profileCheck(true, but, lin, col);
    }

    @Test
    public void updateUITest() {
        launchIntent();

        profile2.setPhoto(Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8));

        main.getActivity().updateUI(user, profile1);
        main.getActivity().updateUI(user, profile2);
    }

    @Test
    public void checkUIVisibility() {
        main.getActivity();

        onView(withId(R.id.appbar))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testActionBar() {
        onView(withContentDescription(R.string.abc_action_bar_up_description)).perform(click());
    }

    @Test
    public void updateAll() {
        main.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                main.getActivity().updateAll(true, user, profile2, avatar);
            }
        });
    }

    @Test
    public void bitmapTest() {
        launchIntent();
        main.getActivity().onSuccessPhoto(Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8));
        launchIntent();
    }

    @Test
    public void userIdCheck() throws JSONException {
        FloatingActionButton but = (FloatingActionButton) main.getActivity().findViewById(R.id.btnActionMessage);
        main.getActivity().userIdCheck(123, but, new Match(123, 123, Match.State.Open, Date.valueOf("2000-01-01")));

        JSONObject json;
        json = new JSONObject();

        JSONObject profileObject = profile.toJson();
        launchIntent();
        profileObject.put(Profile.KEY_BIRTHDAY, 753634621000L);
        profileObject.put("id", 1L);

        json.put("user", user.toJSON());
        json.put("profile", profileObject);
        json.put("avatar", avatar.toJson());

        main.getActivity().onSuccessFetch(json, false);

        launchIntent();

        main.getActivity().onSuccessAvatarFetch(json, false);

        launchIntent();

        main.getActivity().executeFetchTask(true);
        main.getActivity().executeFetchTask(false);

        launchIntent();
    }
}