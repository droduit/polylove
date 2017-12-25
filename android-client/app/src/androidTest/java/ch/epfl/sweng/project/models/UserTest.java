package ch.epfl.sweng.project.models;

import android.content.Context;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ch.epfl.sweng.project.models.db.DBHandler;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertEquals;

/**
 * Tests for the class User
 * @author Dominique Roduit
 */
@RunWith(AndroidJUnit4.class)
public class UserTest  {

    private static DBHandler db;
    private User usr;
    private Profile profile1;
    private Avatar avatar1;
    private Context ctx;

    @Before
    public void setUp() throws Exception {
        ctx = getTargetContext();
        ctx.deleteDatabase(DBHandler.DATABASE_NAME);
        db = DBHandler.getInstance(ctx);

        long USERID_1 = 1;
        profile1 = new Profile(USERID_1, Profile.Section.IN, Profile.Gender.Male, "1993-11-18",
                "Guitare, Math, Sweng", "Je suis un lutin", "0,1",
                Profile.GenderInterest.Female, 18, 25);

        avatar1 = new Avatar(USERID_1, Profile.Gender.Male, Avatar.Eye.Green, Avatar.HairColor.Blond, Avatar.HairStyle.Style1, Avatar.Skin.Light, Avatar.Shirt.Style1);

        usr = new User(USERID_1, "dominique.roduit@epfl.ch", "Dominique", "Roduit");
    }

    @After
    public void tearDown() throws Exception {
        if(db != null) db.close();
    }

    @Test
    public void TestToString() {
        String expected =  "{\n"+
                "id : 1, "+"\n"+
                "email : dominique.roduit@epfl.ch, "+"\n"+
                "firstname : Dominique, "+"\n"+
                "lastname : Roduit "+"\n"+
                "}";
        assertEquals(expected, usr.toString());
    }


    @Test
    public void testGetProfile() {
        db.storeProfile(profile1);
        assertEquals(usr.get_profile(ctx).getDescription(), profile1.getDescription());
        usr.setId(5);
        assertEquals(usr.getId(), usr.get_profile(ctx).getUserId());
    }

    @Test
    public void testGetAvatar() {
        db.storeAvatar(avatar1);
        assertEquals(usr.getAvatar(ctx).getHairColor().ordinal(), avatar1.getHairColor().ordinal());
        usr.setId(5);
        assertEquals(usr.getId(), usr.getAvatar(ctx).getUserId());
    }


    @Test
    public void fromJSONTest() throws JSONException {
        String JSONString =  "{'id' : 1, 'email' : 'dominique.roduit@epfl.ch', 'firstname' : 'Dominique', 'lastname' : 'Roduit'}";
        JSONObject jsonObject = new JSONObject(JSONString);

        User userFromJson = User.fromJSON(jsonObject);
        assertEquals("Dominique", userFromJson.getFirstName());
    }

}
