package ch.epfl.sweng.project.models.db.contracts;

import android.provider.BaseColumns;

/**
 * Contract for the users's table of the database
 * @author Dominique Roduit
 */
public final class UserContract {
    private UserContract(){}

    public static class UserEntry implements BaseColumns {
        public static final String TABLE_NAME = "users";
        public static final String COLUMN_EMAIL = "email";
        public static final String COLUMN_FIRSTNAME = "firstname";
        public static final String COLUMN_LASTNAME = "lastname";

        public static final String CREATE_TABLE_USERS =
        "CREATE TABLE IF NOT EXISTS " + UserEntry.TABLE_NAME + "( "
            + UserEntry._ID + " INTEGER PRIMARY KEY, "
            + UserEntry.COLUMN_EMAIL + " VARCHAR, "
            + UserEntry.COLUMN_FIRSTNAME + " VARCHAR, "
            + UserEntry.COLUMN_LASTNAME + " VARCHAR "
        + ")";
    }
}