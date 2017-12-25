package ch.epfl.sweng.project.models;

import android.content.SharedPreferences;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Tests for the class Settings
 * @author Dominique Roduit
 */
@RunWith(AndroidJUnit4.class)
public class SettingsTest {

    private static Settings settings;
    private static SharedPreferences.Editor editor;

    @Before
    public void setUp() {
        settings = Settings.getInstance(getTargetContext());
        editor = settings.getEditableSettings();
    }

    @Test
    public void languageDisplayNameTest() {
        assertThat(Settings.Language.English.getDisplayName(), is("English"));
    }

    @Test
    public void hasUserNotificationsTest() {
        editor.putBoolean(Settings.NOTIFICATION_KEY, false);
        editor.commit();
        assertThat(settings.hasUserNotifications(), is(false));
    }

    @Test
    public void setLanguageTest() {
        editor.putString(Settings.LANGUAGE_KEY, String.valueOf(Settings.Language.French.ordinal()));
        editor.commit();
        assertThat(settings.getLanguage(), is(Settings.Language.French.ordinal()));
        assertThat(settings.getLocale(), is(Locale.FRENCH));
    }

    @Test
    public void getLocaleTest() {
        String[] languageEntries = Settings.getLanguageEntries();
        String[] expectedEntries = new String[]{"Français", "English"};
        assertThat(languageEntries, is(expectedEntries));
    }

    @Test
    public void hasAutoLoginTest() {
        editor.putBoolean(Settings.AUTO_LOGIN_KEY, false);
        editor.commit();
        assertThat(settings.hasAutoLogin(), is(false));
    }

    @Test
    public void testSetUserID() {
        settings.setUserID(5L);
        assertEquals(5L, settings.getUserID());
    }

    /*
    @Test
    public void isMatchConfirmed() {
        settings.setMatchConfirmed(true);
        assertThat(settings.isMatchConfirmed(), is(true));
    }
    */
}
