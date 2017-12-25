package ch.epfl.sweng.project.models.db.contracts;

import android.provider.BaseColumns;

/**
 * Contract for the profiles's table of the database
 * @author Dominique Roduit
 */
public final class ProfileContract {
    private ProfileContract(){}

    public static class ProfileEntry implements BaseColumns {
        public static final String TABLE_NAME = "profiles";

        public static final String COLUMN_USER_ID = "user_id";
        public static final String COLUMN_SECTION = "section";
        public static final String COLUMN_GENDER = "gender";
        public static final String COLUMN_BIRTHDAY = "birthday";
        public static final String COLUMN_HOBBIES = "hobbies";
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_LANGUAGE = "languages";
        public static final String COLUMN_GENDER_INTEREST = "gender_interest";
        public static final String COLUMN_AGE_INTEREST_START = "age_interest_start";
        public static final String COLUMN_AGE_INTEREST_END = "age_interest_end";
        public static final String COLUMN_PHOTO = "photo";

        public static final String CREATE_TABLE_PROFILES =
        "CREATE TABLE IF NOT EXISTS " + ProfileEntry.TABLE_NAME + "( "
            + ProfileEntry._ID + " INTEGER PRIMARY KEY, "
            + ProfileEntry.COLUMN_USER_ID + " INTEGER, "
            + ProfileEntry.COLUMN_SECTION + " INTEGER, "
            + ProfileEntry.COLUMN_GENDER + " INTEGER, "
            + ProfileEntry.COLUMN_BIRTHDAY + " DATE, "
            + ProfileEntry.COLUMN_HOBBIES + " TEXT, "
            + ProfileEntry.COLUMN_DESCRIPTION + " TEXT, "
            + ProfileEntry.COLUMN_LANGUAGE + " VARCHAR, "
            + ProfileEntry.COLUMN_GENDER_INTEREST + " INTEGER, "
            + ProfileEntry.COLUMN_AGE_INTEREST_START + " INTEGER, "
            + ProfileEntry.COLUMN_AGE_INTEREST_END + " INTEGER, "
            + ProfileEntry.COLUMN_PHOTO + " TEXT "
        + ")";
    }
}