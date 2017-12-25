package ch.epfl.sweng.project.utils;

import android.support.test.runner.AndroidJUnit4;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import ch.epfl.sweng.project.EditAvatar;
import ch.epfl.sweng.project.FillProfileActivity;
import ch.epfl.sweng.project.Login;
import ch.epfl.sweng.project.MainActivity;
import ch.epfl.sweng.project.models.Avatar;
import ch.epfl.sweng.project.models.Profile;
import ch.epfl.sweng.project.models.User;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * Test for the class DBUtils
 * @author Dominique Roduit
 */
@RunWith(AndroidJUnit4.class)
public class AndroidUtilsTest {

    @Test
    public void testConstructorIsPrivate() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<AndroidUtils> constructor = AndroidUtils.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    public void getFormattedTimeTest() {
        assertEquals("", AndroidUtils.getFormattedTime(null));
    }

    @Test
    public void setListViewHeightBasedOnItemsTest() {
        ListView lv = new ListView(getTargetContext());

        assertFalse(AndroidUtils.setListViewHeightBasedOnItems(lv));

        String[] stringArray = new String[] { "Element1", "Element2" };
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(getTargetContext(), android.R.layout.simple_list_item_1, android.R.id.text1, stringArray);
        lv.setAdapter(modeAdapter);

        boolean ret = AndroidUtils.setListViewHeightBasedOnItems(lv);
        assertTrue(ret);

        assertFalse(AndroidUtils.setListViewHeightBasedOnItems(null));
    }

    @Test
    public void displayDialogTest() {
        AndroidUtils.displayDialog(null, null, null);
    }

    @Test
    public void getNextActivityTest() {
        Class<?> classRet;

        classRet = AndroidUtils.getNextActivity(null, null, null);
        assertEquals(Login.class.getName(), classRet.getName());

        User user = new User(2, "tim.nguyen@epfl.ch", "Tim", "Nguyen");

        classRet = AndroidUtils.getNextActivity(user, null, null);
        assertEquals(FillProfileActivity.class.getName(), classRet.getName());

        Profile profile = new Profile(1, Profile.Section.IN, Profile.Gender.Male, "1993-11-18",
                "Guitare, Math, Sweng", "Je suis un lutin", "0,1",
                Profile.GenderInterest.Female, 18, 25);

        classRet = AndroidUtils.getNextActivity(user, profile, null);
        assertEquals(EditAvatar.class.getName(), classRet.getName());

        Avatar avatar = new Avatar(1L, Profile.Gender.Male, Avatar.Eye.Green, Avatar.HairColor.Blond, Avatar.HairStyle.Style1, Avatar.Skin.Light, Avatar.Shirt.Style1);

        classRet = AndroidUtils.getNextActivity(user, profile, avatar);
        assertEquals(MainActivity.class.getName(), classRet.getName());
    }

}
