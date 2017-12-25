package ch.epfl.sweng.project.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Locale;

/**
 * Provides methods to access and set easily all the values kept
 * in the shared preferences key-value file.
 *
 * @author Dominique Roduit
 */
public final class Settings {
    // Key of preferences --------------------------------
    public final static String USER_KEY = "userId";
    public final static String NOTIFICATION_KEY = "notifications";
    public final static String LANGUAGE_KEY = "language";
    public final static String AUTO_LOGIN_KEY = "auto-login";

    // Default values for settings -----------------------
    public final static boolean DEFAULT_AUTO_LOGIN = true;
    public final static boolean DEFAULT_NOTIFICATIONS = true;
    public final static int DEFAULT_LANGUAGE = Language.English.ordinal();

    // Keep an access to the preferences
    private static Settings sInstance;
    private static SharedPreferences settings;

    // Available languages in this application ----------
    public enum Language {
        French("Français"), English("English");

        private String displayName;

        Language(String displayName) {
            this.displayName = displayName;
        }

        /**
         * @return Displayable user friendly name of the language
         */
        public String getDisplayName() { return displayName; }
    }
    // --------------------------------------------------

    /**
     * Create a "connection" to the SharedPreferences file containing all the
     * gettable and settable settings in this class
     * @param context Context of the activity from which we get an instance of this object
     */
    private Settings(Context context) {
        settings = PreferenceManager.getDefaultSharedPreferences(context); //context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE);
    }

    /**
     * Create a single instance of a Settings object,
     * wrapping a connection to a SharedPreferences file.
     * @param context Context of the activity from which we want the instance
     * @return Instance of a Settings object
     */
    public static Settings getInstance(Context context) {
        if(sInstance == null) {
            sInstance = new Settings(context);
        }
        return sInstance;
    }

    /**
     * @return The reference to the SharedPreferences object
     *         associated with this Settings instance
     */
    public SharedPreferences getObject() {
        return settings;
    }

    /**
     * @return The reference to the SharedPreferences file in writable mode
     */
    public SharedPreferences.Editor getEditableSettings () {
        return settings.edit();
    }


    // Notifications preferences -----------------------------------
    /**
     * @return Value of the notification preference
     */
    public boolean hasUserNotifications() {
        return settings.getBoolean(NOTIFICATION_KEY, DEFAULT_NOTIFICATIONS);
    }
    // -------------------------------------------------------------


    // Language preferences ----------------------------------------
    /**
     * @return Value of the language preference
     */
    public int getLanguage() {
        return Integer.parseInt(settings.getString(LANGUAGE_KEY, String.valueOf(DEFAULT_LANGUAGE)));
    }

    /**
     * @return Local value corresponding to the language preference
     */
    public Locale getLocale() {
        return (this.getLanguage() == 0) ? Locale.FRENCH : Locale.ENGLISH;
    }

    /**
     * @return Array of user friendly names of
     *         the languages available in this application
     */
    public static String[] getLanguageEntries() {
        String[] entries = new String[Language.values().length];
        int i = 0;
        for(Language l : Language.values()) {
            entries[i++] = l.getDisplayName();
        }
        return entries;
    }
    // --------------------------------------------------------------


    // Auto-login preferences ---------------------------------------
    /**
     * @return Value of the auto-login preference
     */
    public boolean hasAutoLogin() {
        return settings.getBoolean(AUTO_LOGIN_KEY, DEFAULT_AUTO_LOGIN);
    }
    // --------------------------------------------------------------


    // User ID we have to access from the whole app -----------------
    /**
     * Save the user ID in the shared preferences file to access it
     * from everywhere in the application.
     * @param userID User ID from the database
     */
    public void setUserID(long userID) {
        SharedPreferences.Editor editableSettings = settings.edit();
        editableSettings.putLong(USER_KEY, userID);
        editableSettings.apply();
    }

    /**
     * @return User ID saved in the shared preference file
     */
    public long getUserID() {
        return settings.getLong(USER_KEY, 0);
    }
    // --------------------------------------------------------------

}