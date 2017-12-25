package ch.epfl.sweng.project.models.db.contracts;

import android.provider.BaseColumns;

/**
 * Contract for the avatars's table of the database
 * @author Dominique Roduit
 */
public final class AvatarContract {
    private AvatarContract(){}

    public static class AvatarEntry implements BaseColumns {
        public static final String TABLE_NAME = "avatars";

        public static final String COLUMN_USER_ID = "user_id";
        public static final String COLUMN_GENDER = "gender";
        public static final String COLUMN_HAIR_COLOR = "hair_color";
        public static final String COLUMN_HAIR_STYLE = "hair_style";
        public static final String COLUMN_EYES = "eyes";
        public static final String COLUMN_SKIN = "skin";
        public static final String COLUMN_SHIRT = "shirt";
        public static final String COLUMN_IMAGE = "image";
        public static final String COLUMN_ICON = "icon";

        public static final String CREATE_TABLE_AVATARS =
        "CREATE TABLE IF NOT EXISTS " + AvatarEntry.TABLE_NAME + "( "
            + AvatarEntry._ID    + " INTEGER PRIMARY KEY, "
            + AvatarEntry.COLUMN_USER_ID + " INTEGER, "
            + AvatarEntry.COLUMN_GENDER + " INTEGER, "
            + AvatarEntry.COLUMN_HAIR_COLOR + " INTEGER, "
            + AvatarEntry.COLUMN_HAIR_STYLE + " INTEGER, "
            + AvatarEntry.COLUMN_EYES + " INTEGER, "
            + AvatarEntry.COLUMN_SKIN + " INTEGER, "
            + AvatarEntry.COLUMN_SHIRT + " INTEGER, "
            + AvatarEntry.COLUMN_IMAGE + " TEXT, "
            + AvatarEntry.COLUMN_ICON + " TEXT "
        + ")";
    }
}