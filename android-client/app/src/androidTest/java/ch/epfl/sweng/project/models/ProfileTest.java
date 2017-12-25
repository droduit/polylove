package ch.epfl.sweng.project.models;

import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.Set;
import java.util.TreeSet;

import ch.epfl.sweng.project.R;
import ch.epfl.sweng.project.utils.DBUtils;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;


/**
 * Tests for the class Profile
 * @author Dominique Roduit
 */
@RunWith(AndroidJUnit4.class)
public final class ProfileTest {

    private static final String DESCRIPTION = "Je suis gentil";
    private static final int AGE_START = 16;
    private static final int AGE_END = 25;
    private Profile profile;
    private Set<Profile.Language> languages;

    @Before
    public void setup() {
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
    }

    @Test
    public void enumIsReachableByIndex() {
        assertEquals(Profile.Gender.Male, Profile.Gender.values()[0]);
        assertEquals(Profile.GenderInterest.Male, Profile.GenderInterest.values()[0]);
        assertEquals(Profile.Language.French, Profile.Language.values()[13]);
    }

    @Test
    public void testToString() {
        String expected = "{\n"+
                Profile.KEY_ID + " : 0, "+"\n"+
                Profile.KEY_GENDER + " : "+ Profile.Gender.Male.toString()+", "+"\n"+
                Profile.KEY_BIRTHDAY + " : 1993-11-18, "+"\n"+
                Profile.KEY_HOBBIES + " : Guitare, Maths, Sweng, "+"\n"+
                Profile.KEY_DESCRIPTION + " : "+DESCRIPTION+", "+"\n"+
                Profile.KEY_LANGUAGES + " : 9, 13, "+"\n"+
                Profile.KEY_GENDER_INTEREST + " : "+ Profile.GenderInterest.Female.toString()+", "+"\n"+
                Profile.KEY_AGE_START + " : "+AGE_START+", "+"\n"+
                Profile.KEY_AGE_END + " : "+AGE_END+" "+"\n"+
                "}";
        assertEquals(expected, profile.toString());
    }


    @Test
    public void getBirthday() {
        Date date = new Date();
        profile.setBirthday(date);
        assertEquals(profile.getBirthday(), date);
    }

    @Test
    public void testEnumSection() {
        Profile.Section section = Profile.Section.IN;
        Assert.assertEquals(section.getStringId(), R.string.section_in);
    }

    @Test
    public void toJsonTest() throws JSONException {
        JSONObject jsonObject = profile.toJson();
        assertEquals(DESCRIPTION, jsonObject.getString("description"));
        assertEquals(AGE_START, jsonObject.getJSONObject("ageInterest").getInt("start"));
        assertEquals(AGE_END, jsonObject.getJSONObject("ageInterest").getInt("end"));
    }

    @Test
    public void fromJsonTest() throws JSONException {
        JSONObject jsonObject = profile.toJson();
        jsonObject.put(Profile.KEY_BIRTHDAY, 753634621000L);
        jsonObject.put("id", 1L);
        Profile profileFromJSON = Profile.fromJSON(jsonObject);
        assertEquals(DESCRIPTION, profileFromJSON.getDescription());

        Profile profileFromJSONNull = Profile.fromJSON(null);
        assertNull(profileFromJSONNull);
    }

    @Test
    public void getLanguageSetToStringTest() {
        String langs = profile.getLanguageSetToString(getTargetContext(), languages);
        Assert.assertEquals("English, French", langs);

        assertEquals(getTargetContext().getString(R.string.choose), profile.getLanguageSetToString(getTargetContext(), null));
    }

    /*
    @Test
    public void getFormattedBirthdayTest() {
        Assert.assertEquals("18 novembre 1993", profile.getFormattedBirthday(getTargetContext()));
    }
*/
}
