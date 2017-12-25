package ch.epfl.sweng.project;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.contrib.PickerActions;
import android.support.test.rule.ActivityTestRule;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import ch.epfl.sweng.project.models.Profile;
import ch.epfl.sweng.project.utils.DBUtils;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static ch.epfl.sweng.project.R.id.btGender;
import static ch.epfl.sweng.project.R.id.btGenderInterest;
import static ch.epfl.sweng.project.R.id.datePicker;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;


/**
 * Created by Lucie
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FillProfileActivityTest {

    private static final String DESCRIPTION = "Je suis gentil";
    private static final int AGE_START = 16;
    private static final int AGE_END = 25;
    private Profile profile;
    private Set<Profile.Language> languages;

    private Button btDate;
    private Button btSection;
    private Button btGender;
    private Button btGenderInterest;
    private ArrayAdapter<String> adapter;

    private void launchIntent() {
        Intent intent = new Intent(main.getActivity().getApplicationContext(), FillProfileActivity.class);
        main.launchActivity(intent);
    }

    // Useful for having what we need displayed on screen
    private static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        View view = activity.getCurrentFocus();
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Rule
    public ActivityTestRule<FillProfileActivity> main = new ActivityTestRule<>(FillProfileActivity.class);

    @Before
    public void startUp() {
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

        btDate = (Button) main.getActivity().findViewById(datePicker);
        btSection = (Button) main.getActivity().findViewById(R.id.btSection);
        btGender = (Button) main.getActivity().findViewById(R.id.btGender);
        btGenderInterest = (Button) main.getActivity().findViewById(R.id.btGenderInterest);
        ArrayList<String> arrayListHobbies = new ArrayList<>();
        adapter = new ArrayAdapter<String>(main.getActivity().getApplicationContext(), android.R.layout.simple_list_item_1, android.R.id.text1, arrayListHobbies) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                textView.setTextColor(ContextCompat.getColor(main.getActivity().getApplicationContext(), R.color.black_color));
                return textView;
            }
        };
    }

    @Test
    public void updatingMode() {
        main.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                main.getActivity().updatingMode(true, profile, btDate, btSection, btGender, btGenderInterest, adapter);
            }
        });
    }

    @Test
    public void genderIsDisplayed() throws Exception {
        onView(withId(R.id.btGender)).perform(ViewActions.scrollTo());
        onView(withId(R.id.btGender)).perform(click());
        onView(withText("Female"))
                .perform(click())
                .check(matches(isDisplayed()));
    }

    @Test
    public void genderInterestIsDisplayed() throws Exception {
        onView(withId(R.id.btGenderInterest)).perform(ViewActions.scrollTo());
        onView(withId(R.id.btGenderInterest)).perform(click());
        onView(withText("Female"))
                .perform(click())
                .check(matches(isDisplayed()));
    }

    @Test
    public void languagenterestIsDisplayed() throws Exception {
        launchIntent();
        onView(withId(R.id.languagesButton)).perform(ViewActions.scrollTo());
        onView(withId(R.id.languagesButton)).perform(click());
        onView(withText("Albanian"))
                .perform(click())
                .check(matches(isDisplayed()));

        onView(withId(R.id.save_action)).perform(click());
        launchIntent();
    }

    @Test
    public void sectionIsDisplayed() throws Exception {
        onView(withId(R.id.btSection)).perform(ViewActions.scrollTo());
        onView(withId(R.id.btSection)).perform(click());
        Thread.sleep(1000);

        onData(allOf(is(instanceOf(String.class)))).atPosition(2).perform(click());
        /*
        onView(withText("Architecture"))
                .perform(click())
                .check(matches(isDisplayed()));
                */
    }


    @Test
    public void removeHobbie() throws Exception {
        onView(withId(R.id.description)).perform(ViewActions.scrollTo());
        onView(withId(R.id.hobbies)).perform(typeText("Thing"));
        onView(withId(R.id.btAddHobby)).perform(click());

        // WAIT FOR SOME TIME AND HIDE KEYBOARD BECAUSE ESPRESSO IS SHIT
        hideKeyboard(main.getActivity());
        Thread.sleep(2000);

        onView(withText("Thing"))
                .perform(longClick());
    }


    @Test
    public void testHobbiesAlert() throws Exception {
        // Fill the form
        onView(withId(R.id.datePicker)).perform(click());
        onView(withClassName(Matchers.equalTo(DatePicker.class.getName()))).perform(PickerActions.setDate(1990, 6, 10));
        onView(withId(android.R.id.button1)).perform(click());

        // Create test profile
        Profile testProfile = new Profile();
        main.getActivity().fillProfile(testProfile);
    }

    @Test
    public void onActivityResultTest() throws Exception {
        Intent intent = new Intent();
        main.getActivity().onActivityResult(200, 200, intent);
    }

    /*
    @Test(expected = IOException.class)
    public void sendPictureTest() throws Exception {
        URL url = new URL("");
        Bitmap.Config config = Bitmap.Config.ALPHA_8;
        Bitmap bitmap = Bitmap.createBitmap(1, 1, config);
        main.getActivity().sendPicture(url, bitmap);
    }
    */

    @Test
    public void zTestProfileOk() throws Exception {

        // Fill the form
        onView(withId(R.id.datePicker)).perform(click());
        onView(withClassName(Matchers.equalTo(DatePicker.class.getName()))).perform(PickerActions.setDate(1990, 6, 10));
        onView(withId(android.R.id.button1)).perform(click());

        onView(withId(R.id.btSection)).perform(ViewActions.scrollTo());
        onView(withId(R.id.btSection)).perform(click());
        onView(withText("Architecture"))
                .perform(click())
                .check(matches(isDisplayed()));

        onView(withId(R.id.hobbies)).perform(ViewActions.scrollTo());
        onView(withId(R.id.hobbies)).perform(typeText("Thing"));
        onView(withId(R.id.btAddHobby)).perform(click());
        onView(withId(R.id.hobbies)).perform(typeText("thing1"));
        onView(withId(R.id.btAddHobby)).perform(click());

        onView(withId(R.id.description)).perform(ViewActions.scrollTo());
        onView(withId(R.id.description)).perform(typeText("This is my description."));

        onView(withId(R.id.languagesButton)).perform(ViewActions.scrollTo());
        onView(withId(R.id.languagesButton)).perform(click());
        onView(withText("Albanian"))
                .perform(click())
                .check(matches(isDisplayed()));
        onView(withId(R.id.save_action)).perform(click());

        onView(withId(R.id.btGender)).perform(ViewActions.scrollTo());
        onView(withId(R.id.btGender)).perform(click());
        onView(withText("Female"))
                .perform(click())
                .check(matches(isDisplayed()));

        onView(withId(R.id.btGenderInterest)).perform(ViewActions.scrollTo());
        onView(withId(R.id.btGenderInterest)).perform(click());
        onView(withText("Male"))
                .perform(click())
                .check(matches(isDisplayed()));

        // Create test profile
        Profile testProfile = new Profile();
        testProfile.setLanguages("4");
        main.getActivity().fillProfile(testProfile);

        // Asserts
        assertThat(testProfile.getBirthdayAsString(), is("1990-06-10"));
        Set<String> hobb = new HashSet<>();
        hobb.add("thing1");
        hobb.add("Thing");
        assertThat(testProfile.getHobbies(), is(hobb));
        assertThat(testProfile.getDescription(), is("This is my description."));
        //assertThat(testProfile.getLanguagesString(), is("Lituanien"));
        assertThat(testProfile.getGender(), is(Profile.Gender.Male));
        assertThat(testProfile.getGenderInterest(), is(Profile.GenderInterest.Female));

        onView(withId(R.id.save_action)).perform(click());
        Thread.sleep(10000);
    }

}