package ch.epfl.sweng.project.models.db;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import ch.epfl.sweng.project.models.db.contracts.AvatarContract;
import ch.epfl.sweng.project.models.db.contracts.MatchContract;
import ch.epfl.sweng.project.models.db.contracts.MessageContract;
import ch.epfl.sweng.project.models.db.contracts.ProfileContract;
import ch.epfl.sweng.project.models.db.contracts.UserContract;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Test for the contracts class used by DBHandler
 * @author Dominique Roduit
 */
@RunWith(AndroidJUnit4.class)
public class DBContractsTests {


    @Test
    public void testProfileContract() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<ProfileContract> constructor = ProfileContract.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
        assertNotNull(ProfileContract.ProfileEntry.class.getDeclaredFields());
    }

    @Test
    public void testAvatarContract() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<AvatarContract> constructor = AvatarContract.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    public void testMessageContract() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<MessageContract> constructor = MessageContract.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    public void testMatchContract() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<MatchContract> constructor = MatchContract.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    public void testUserContract() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<UserContract> constructor = UserContract.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
    }

}
