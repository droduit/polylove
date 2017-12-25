package ch.epfl.sweng.project.models.db.contracts;

import android.provider.BaseColumns;

/**
 * Contract for the messages's table of the database
 * @author Dominique Roduit
 */
public final class MessageContract {
    private MessageContract(){}

    public static class MessageEntry implements BaseColumns {
        public static final String TABLE_NAME = "messages";

        public static final String COLUMN_FROM = "author";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_MATCH_ID = "match";
        public static final String COLUMN_BODY = "body";
        public static final String COLUMN_IS_MINE = "is_mine";
        public static final String COLUMN_STATUS = "status";

        public static final String CREATE_TABLE_MESSAGES =
        "CREATE TABLE IF NOT EXISTS " + MessageEntry.TABLE_NAME + "( "
            + MessageEntry._ID + " INTEGER PRIMARY KEY, "
            + MessageEntry.COLUMN_FROM + " INTEGER, "
            + MessageEntry.COLUMN_DATE + " DATETIME, "
            + MessageEntry.COLUMN_MATCH_ID + " INTEGER, "
            + MessageEntry.COLUMN_BODY + " TEXT, "
            + MessageEntry.COLUMN_IS_MINE + " INTEGER, "
            + MessageEntry.COLUMN_STATUS + " INTEGER"
        + ")";
    }
}