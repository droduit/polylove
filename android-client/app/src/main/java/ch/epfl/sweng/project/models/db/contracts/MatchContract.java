package ch.epfl.sweng.project.models.db.contracts;

import android.provider.BaseColumns;

/**
 * Contract for the matches's table of the database
 * @author Dominique Roduit
 */
public final class MatchContract {
    private MatchContract(){}

    public static class MatchEntry implements BaseColumns {
        public static final String TABLE_NAME = "matches";

        public static final String COLUMN_USER = "user";
        public static final String COLUMN_STATE = "state";
        public static final String COLUMN_TIME = "time";

        public static final String CREATE_TABLE_MATCHES =
        "CREATE TABLE IF NOT EXISTS " + MatchEntry.TABLE_NAME + "( "
            + MatchEntry._ID + " INTEGER PRIMARY KEY, "
            + MatchEntry.COLUMN_USER + " INTEGER NOT NULL, "
            + MatchEntry.COLUMN_STATE + " BOOL, "
            + MatchEntry.COLUMN_TIME + " DATETIME "
        + ")";
    }
}