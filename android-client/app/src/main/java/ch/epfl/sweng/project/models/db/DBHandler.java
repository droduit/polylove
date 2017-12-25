package ch.epfl.sweng.project.models.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import ch.epfl.sweng.project.models.Avatar;
import ch.epfl.sweng.project.models.Match;
import ch.epfl.sweng.project.models.Message;
import ch.epfl.sweng.project.models.Profile;
import ch.epfl.sweng.project.models.User;
import ch.epfl.sweng.project.models.db.contracts.AvatarContract.AvatarEntry;
import ch.epfl.sweng.project.models.db.contracts.MatchContract.MatchEntry;
import ch.epfl.sweng.project.models.db.contracts.MessageContract.MessageEntry;
import ch.epfl.sweng.project.models.db.contracts.ProfileContract.ProfileEntry;
import ch.epfl.sweng.project.models.db.contracts.UserContract.UserEntry;
import ch.epfl.sweng.project.utils.AndroidUtils;
import ch.epfl.sweng.project.utils.DBUtils;

/**
 * DBHandler is a Singleton class for 3 main purposes :
 *
 *  1) Create the tables for the database of the whole application.
 *  2) Provide an instance of a connection to this database.
 *  3) Provide all the useful related methods to insert, update, get and
 *    delete informations from the database.
 *
 * The database is the local cache of the device, stored in a SQLite file.
 *
 * <b>Store</b>
 * All methods to store a data follow the naming convention : storeData(Object data).
 * Most of them are designed to both insert, or update the given data if she was already existing before.
 * They all return the same kind of value :
 *  - For an insertion : ID of the new item inserted in the database.
 *  - For an update : number of affected rows in the database.
 *
 * <b>Fetch</b>
 * All methods to get a data follow the naming convention : getData(long dataId).
 * They all return the corresponding data Object if the row with ID dataId exists,
 * otherwise, they return null.
 *
 * @author Dominique Roduit
 * Email : dominique.roduit@epfl.ch
 */
public final class DBHandler extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "polylove.db";
    /* The version need to be increased for each change in the database's structure */
    public static final int DATABASE_VERSION = 20;

    /* List of the tables in this database */
    private final List<String> tableNames = Collections.unmodifiableList(
        Arrays.asList(
            UserEntry.TABLE_NAME,
            ProfileEntry.TABLE_NAME,
            AvatarEntry.TABLE_NAME,
            MessageEntry.TABLE_NAME,
            MatchEntry.TABLE_NAME
        )
    );

    private static DBHandler sInstance;

    private DBHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Ensures that only one DBHandler object will ever exist at any time.
     * @param context Application context
     * @return Database connection
     */
    public static synchronized DBHandler getInstance(Context context) {
        if(sInstance == null) {
            sInstance = new DBHandler(context.getApplicationContext());
        }
        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(UserEntry.CREATE_TABLE_USERS);
        db.execSQL(ProfileEntry.CREATE_TABLE_PROFILES);
        db.execSQL(AvatarEntry.CREATE_TABLE_AVATARS);
        db.execSQL(MatchEntry.CREATE_TABLE_MATCHES);
        db.execSQL(MessageEntry.CREATE_TABLE_MESSAGES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // on version upgrade, drop older tables
        for(String table : tableNames) {
            db.execSQL("DROP TABLE IF EXISTS " + table);
        }
        // create new tables
        onCreate(db);
    }

    /**
     * Clean all the tables of the databases (remove all their contents)
     */
    public void cleanTables() {
        SQLiteDatabase db = this.getWritableDatabase();
        for(String table : tableNames) {
            db.execSQL("DELETE FROM " + table);
        }
    }

    /**
     * Store (insert or update) in the database a new user, with his profile and avatar
     * @param user User object containing the infos about user
     * @return INSERTION : New item_row_checkbox id in the table, UPDATING : number of updated rows
     */
    public long storeUser(User user) {

        if(user == null) return -1;

        SQLiteDatabase db = this.getWritableDatabase();
        long userId = -1;

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(UserEntry._ID, user.getId());
            values.put(UserEntry.COLUMN_EMAIL, user.getEmail());
            values.put(UserEntry.COLUMN_FIRSTNAME, user.getFirstName());
            values.put(UserEntry.COLUMN_LASTNAME, user.getLastName());

            // try to update the user if he already exists
            int rows = db.update(UserEntry.TABLE_NAME, values, UserEntry.COLUMN_EMAIL + "= ?", new String[]{user.getEmail()});

            if (rows == 1) { // Update succeeded
                // Get PK of user we just updated
                String usersSelectQuery = String.format("SELECT %s FROM %s WHERE %s = ?", UserEntry._ID, UserEntry.TABLE_NAME, UserEntry.COLUMN_EMAIL);
                Cursor cursor = db.rawQuery(usersSelectQuery, new String[]{String.valueOf(user.getEmail())});

                try {
                    if (cursor.moveToFirst()) {
                        userId = cursor.getInt(0);
                        user.setId(userId);
                        db.setTransactionSuccessful();
                    }
                } finally {
                    if (cursor != null && !cursor.isClosed()) {
                        cursor.close();
                    }
                }
            } else { // Did not already exist -> Insert new
                userId = db.insertOrThrow(UserEntry.TABLE_NAME, null, values);
                db.setTransactionSuccessful();
            }
        } finally {
            db.endTransaction();
        }
        return userId;

    }

    /**
     * Return the User object corresponding to the userId in the database
     * @param userId User ID of the user we want to retrieve
     * @return User object of the stored user in the database
     */
    public User getUser(long userId) {
        if(userId < 0) return null;

        final String SELECT_QUERY = String.format(
                "SELECT * FROM %s WHERE %s = ?",
                UserEntry.TABLE_NAME, UserEntry._ID
        );

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery(SELECT_QUERY, new String[]{ String.valueOf(userId) });

        User user = null;

        try {
            if(res.moveToFirst()) {
                user = new User(
                        res.getLong(res.getColumnIndex(UserEntry._ID)),
                        res.getString(res.getColumnIndex(UserEntry.COLUMN_EMAIL)),
                        res.getString(res.getColumnIndex(UserEntry.COLUMN_FIRSTNAME)),
                        res.getString(res.getColumnIndex(UserEntry.COLUMN_LASTNAME))
                );
            }
        } finally {
            if(res != null && !res.isClosed()) {
                res.close();
            }
        }

        return user;
    }


    /**
     * Store in the database the profile of the user (Insert if it didn't already exist, update existing otherwise)
     * (given by the user_id field of profile object)
     * @param profile Profile of the user
     * @return INSERTION : New item_row_checkbox id in the table, UPDATING : Number of affected rows
     */
    public long storeProfile(Profile profile) {
        if(profile == null) return -1;

        SQLiteDatabase db = this.getWritableDatabase();
        long profileId = -1;

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(ProfileEntry.COLUMN_USER_ID, profile.getUserId());
            values.put(ProfileEntry.COLUMN_SECTION, profile.getSection().ordinal());
            values.put(ProfileEntry.COLUMN_GENDER, profile.getGender().ordinal());
            values.put(ProfileEntry.COLUMN_BIRTHDAY, profile.getBirthdayAsString());
            values.put(ProfileEntry.COLUMN_HOBBIES, profile.getInterestsString());
            values.put(ProfileEntry.COLUMN_DESCRIPTION, profile.getDescription());
            values.put(ProfileEntry.COLUMN_LANGUAGE, profile.getLanguagesString());
            values.put(ProfileEntry.COLUMN_GENDER_INTEREST, profile.getGenderInterest().ordinal());
            values.put(ProfileEntry.COLUMN_AGE_INTEREST_START, profile.getAgeInterestStart());
            values.put(ProfileEntry.COLUMN_AGE_INTEREST_END, profile.getAgeInterestEnd());
            if(profile.getPhoto() != null) {
                values.put(ProfileEntry.COLUMN_PHOTO, AndroidUtils.toBase64(profile.getPhoto()));
            } else {
                values.putNull(ProfileEntry.COLUMN_PHOTO);
            }

            int rows = db.update(ProfileEntry.TABLE_NAME, values, ProfileEntry.COLUMN_USER_ID+" = ?", new String[]{ String.valueOf(profile.getUserId()) });

            if(rows == 1) { // Update succeeded
                String profileSelectQuery = String.format("SELECT %s FROM %s WHERE %s = ?",
                        ProfileEntry._ID, ProfileEntry.TABLE_NAME, ProfileEntry.COLUMN_USER_ID);
                Cursor cursor = db.rawQuery(profileSelectQuery, new String[]{ String.valueOf(profile.getUserId()) });

                try {
                    if(cursor.moveToFirst()) {
                        profileId = cursor.getInt(0);
                        db.setTransactionSuccessful();
                    }
                } finally {
                    if(cursor != null && !cursor.isClosed()) {
                        cursor.close();
                    }
                }
            } else { // Did not already exist -> insert new
                profileId = db.insertOrThrow(ProfileEntry.TABLE_NAME, null, values);
                db.setTransactionSuccessful();
            }

        } finally {
            db.endTransaction();
        }

        return profileId;
    }

    /**
     * Return the Profile object corresponding to the userId in the database
     * @param userId User ID of the user we want to retrieve the profile
     * @return Profile object stored in the database
     */
    public Profile getProfile(long userId) {
        if(userId < 0) return null;

        final String SELECT_QUERY = String.format(
                "SELECT * FROM %s WHERE %s = ?",
                ProfileEntry.TABLE_NAME, ProfileEntry.COLUMN_USER_ID
        );

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery(SELECT_QUERY, new String[]{ String.valueOf(userId) });

        Profile profile = null;

        try {
            if(res.moveToFirst()) {
                profile = new Profile();
                profile.setUserId(res.getInt(res.getColumnIndex(ProfileEntry.COLUMN_USER_ID)));
                profile.setGender(res.getInt(res.getColumnIndex(ProfileEntry.COLUMN_GENDER)));
                profile.setBirthday(res.getString(res.getColumnIndex(ProfileEntry.COLUMN_BIRTHDAY)));
                profile.setHobbies(res.getString(res.getColumnIndex(ProfileEntry.COLUMN_HOBBIES)));
                profile.setDescription(res.getString(res.getColumnIndex(ProfileEntry.COLUMN_DESCRIPTION)));
                profile.setLanguages(res.getString(res.getColumnIndex(ProfileEntry.COLUMN_LANGUAGE)));
                profile.setGenderInterest(res.getInt(res.getColumnIndex(ProfileEntry.COLUMN_GENDER_INTEREST)));
                profile.setAgeInterestStart(res.getInt(res.getColumnIndex(ProfileEntry.COLUMN_AGE_INTEREST_START)));
                profile.setAgeInterestEnd(res.getInt(res.getColumnIndex(ProfileEntry.COLUMN_AGE_INTEREST_END)));
                profile.setSection(Profile.Section.values()[res.getInt(res.getColumnIndex(ProfileEntry.COLUMN_SECTION))]);

                String strImage = res.getString(res.getColumnIndex(ProfileEntry.COLUMN_PHOTO));
                if(strImage == null) {
                    profile.setPhoto(null);
                } else {
                    Bitmap photo = AndroidUtils.fromBase64(strImage);
                    profile.setPhoto(photo);
                }
            } else {
                profile = null;
            }
        } finally {
            if(res != null && !res.isClosed()) {
                res.close();
            }
        }
        return profile;
    }

    /**
     * Store the avatar of the user (Insert if it didn't already exist, update existing otherwise)
     * @param avatar Avatar Object to store in the database
     * @return Id of the stored avatar (new ID if it didn't already exist)
     */
    public long storeAvatar(Avatar avatar) {
        if(avatar == null) return -1;

        SQLiteDatabase db = this.getWritableDatabase();
        long avatarId = -1;

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(AvatarEntry.COLUMN_USER_ID, avatar.getUserId());
            values.put(AvatarEntry.COLUMN_GENDER, avatar.getGender().ordinal());
            values.put(AvatarEntry.COLUMN_HAIR_COLOR, avatar.getHairColor().ordinal());
            values.put(AvatarEntry.COLUMN_HAIR_STYLE, avatar.getHairStyle().ordinal());
            values.put(AvatarEntry.COLUMN_EYES, avatar.getEyeColor().ordinal());
            values.put(AvatarEntry.COLUMN_SKIN, avatar.getSkinTone().ordinal());
            values.put(AvatarEntry.COLUMN_SHIRT, avatar.getShirt().ordinal());
            if(avatar.getImage() != null) {
                values.put(AvatarEntry.COLUMN_IMAGE, AndroidUtils.toBase64(avatar.getImage()));
            } else {
                values.putNull(AvatarEntry.COLUMN_IMAGE);
            }
            if(avatar.getIcon() != null) {
                values.put(AvatarEntry.COLUMN_ICON, AndroidUtils.toBase64(avatar.getIcon()));
            } else {
                values.putNull(AvatarEntry.COLUMN_ICON);
            }


            int rows = db.update(AvatarEntry.TABLE_NAME, values, AvatarEntry.COLUMN_USER_ID+" = ?",
                    new String[]{ String.valueOf(avatar.getUserId()) });

            if(rows == 1) { // Update succeeded
                String profileSelectQuery = String.format("SELECT %s FROM %s WHERE %s = ?",
                        AvatarEntry._ID, AvatarEntry.TABLE_NAME, AvatarEntry.COLUMN_USER_ID);
                Cursor cursor = db.rawQuery(profileSelectQuery, new String[]{ String.valueOf(avatar.getUserId()) });

                try {
                    if(cursor.moveToFirst()) {
                        avatarId = cursor.getInt(0);
                        db.setTransactionSuccessful();
                    }
                } finally {
                    if(cursor != null && !cursor.isClosed()) {
                        cursor.close();
                    }
                }
            } else { // Did not already exist -> insert new
                avatarId = db.insertOrThrow(AvatarEntry.TABLE_NAME, null, values);
                db.setTransactionSuccessful();
            }

        } finally {
            db.endTransaction();
        }

        return avatarId;
    }

    /**
     * Return the Avatar object corresponding to the userId in the database
     * @param userId User ID of the user we want to retrieve the avatar
     * @return Avatar object stored in the database
     */
    public Avatar getAvatar(long userId) {
        if(userId <= 0) return null;

        final String SELECT_QUERY = String.format(
                "SELECT * FROM %s WHERE %s = ?",
                AvatarEntry.TABLE_NAME, AvatarEntry.COLUMN_USER_ID
        );

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery(SELECT_QUERY, new String[]{ String.valueOf(userId) });

        Avatar avatar = null;

        try {
            if(res.moveToFirst()) {
                avatar = new Avatar();
                avatar.setUserId(res.getInt(res.getColumnIndex(AvatarEntry.COLUMN_USER_ID)));
                avatar.setGender(Profile.Gender.values()[res.getInt(res.getColumnIndex(AvatarEntry.COLUMN_GENDER))]);
                avatar.setEyeColor(Avatar.Eye.values()[res.getInt(res.getColumnIndex(AvatarEntry.COLUMN_EYES))]);
                avatar.setHairColor(Avatar.HairColor.values()[res.getInt(res.getColumnIndex(AvatarEntry.COLUMN_HAIR_COLOR))]);
                avatar.setHairStyle(Avatar.HairStyle.values()[res.getInt(res.getColumnIndex(AvatarEntry.COLUMN_HAIR_STYLE))]);
                avatar.setSkinTone(Avatar.Skin.values()[res.getInt(res.getColumnIndex(AvatarEntry.COLUMN_SKIN))]);
                avatar.setShirt(Avatar.Shirt.values()[res.getInt(res.getColumnIndex(AvatarEntry.COLUMN_SHIRT))]);

                String strImage = res.getString(res.getColumnIndex(AvatarEntry.COLUMN_IMAGE));
                if(strImage == null || strImage.isEmpty()) {
                    avatar.setImage(null);
                } else {
                    Bitmap image = AndroidUtils.fromBase64(strImage);
                    avatar.setImage(image);
                }

                String strIcon = res.getString(res.getColumnIndex(AvatarEntry.COLUMN_ICON));
                if(strIcon == null || strIcon.isEmpty()) {
                    avatar.setIcon(null);
                } else {
                    Bitmap icon = AndroidUtils.fromBase64(strIcon);
                    avatar.setIcon(icon);
                }
            }
        } finally {
            if(res != null && !res.isClosed()) {
                res.close();
            }
        }
        return avatar;
    }


    /**
     * Return a list of Match objects from the locale database, for the given sql SELECT query
     * @param selectQuery SQL SELECT query
     * @param params A list of values who will replace the ? characters of you SELECT query (in order)
     * @return List of fetched Matchs
     */
    private List<Match> getMatchsWithQuery(String selectQuery, String[] params) {

        if(selectQuery == null) return null;

        String[] parameters = (params == null) ? new String[]{} : params;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery(selectQuery, parameters);

        List<Match> matchList = new ArrayList<>();

        try {
            while(res.moveToNext()) {
                matchList.add(
                    new Match(
                            res.getLong(res.getColumnIndex(MatchEntry._ID)),
                            res.getInt(res.getColumnIndex(MatchEntry.COLUMN_USER)),
                            Match.State.values()[res.getInt(res.getColumnIndex(MatchEntry.COLUMN_STATE))],
                            DBUtils.StringToDateTime(res.getString(res.getColumnIndex(MatchEntry.COLUMN_TIME)))
                    )
                );
            }
        } finally {
            if(res != null && !res.isClosed()) {
                res.close();
            }
        }
        return matchList;
    }

    /**
     * Return informations about the match related to the user we give in parameter,
     * or null if we don't have a match with this user.
     * @param userId User ID for which we want to retrieve the match
     * @return Match object with the user given by userId
     */
    public Match getMatchWithUser(long userId) {
        if(userId < 0) {
            return null;
        }
        final String SELECT_QUERY = String.format(
                "SELECT * FROM %s WHERE %s = ?",
                MatchEntry.TABLE_NAME,  MatchEntry.COLUMN_USER
        );
        String[] params = new String[]{ String.valueOf(userId) };
        List<Match> matchs = getMatchsWithQuery(SELECT_QUERY, params);

        if(matchs.size() == 0) {
            return null;
        } else {
            return matchs.get(0);
        }
    }

    /**
     * @return The user's match of the day
     */
    public Match getMatchOfToday() {
        final String SELECT_QUERY = String.format(
                "SELECT * FROM %s " +
                        "WHERE date(%s) = date('now') AND (%s = ? OR %s = ?) " +
                        "ORDER BY %s DESC",
                MatchEntry.TABLE_NAME,
                MatchEntry.COLUMN_TIME, MatchEntry.COLUMN_STATE, MatchEntry.COLUMN_STATE,
                MatchEntry.COLUMN_TIME
        );

        String[] params = new String[]{
                String.valueOf(Match.State.Pending.ordinal()),
                String.valueOf(Match.State.Confirmed.ordinal())
        };

        List<Match> matchs = getMatchsWithQuery(SELECT_QUERY, params);
        if(matchs == null) return null;

        if(matchs.size() > 0) {
            return matchs.get(0);
        } else {
            return null;
        }
    }

    /**
     * Close all the matches older than today (or from today), which are not confirmed.
     * @return Number of updated rows
     */
    public long closeOldMatchesNotConfirmed() {
        final String SELECT_QUERY = String.format(
        "SELECT * FROM %s WHERE %s < datetime('now', '-"+ (Match.MATCH_DURATION) +" hours') AND %s = ?",
                MatchEntry.TABLE_NAME, MatchEntry.COLUMN_TIME, MatchEntry.COLUMN_STATE
        );
        List<Match> matchs = getMatchsWithQuery(SELECT_QUERY, new String[]{ String.valueOf(Match.State.Pending) });
        if(matchs == null) return 0;

        long updatedRows = 0;
        for(Match m : matchs) {
            Log.d("match will be closed", "time left : "+m.getMillisecondsLeft()+" - idMatch : "+m.getId());
            //if(m.getMillisecondsLeft() <= 0) {
                m.setState(Match.State.Close);
                updatedRows += this.storeMatch(m);
            //}
        }
        return updatedRows;
    }

    /**
     * Return a List with the Matchs of the User
     * @return List of the user's matchs
     */
    public boolean existsMatchWithUser(long userId) {
        if(userId < 0) {
            return false;
        }
        Match match = getMatchWithUser(userId);
        return match != null;
    }

    /**
     * @return The list of non closed conversations for the user
     * (a conversation is an open or pending match)
     * in descending order of the reception time.
     */
    public List<Match> getConversations() {
        final String SELECT_QUERY = String.format(
                "SELECT " +
                        " %s._ID, %s, %s, %s" +
                        " FROM %s " +
                        "LEFT JOIN %s ON %s = %s.%s " +
                        //"WHERE %s != ? " +
                        "GROUP BY %s.%s " +
                        "ORDER BY %s ASC, %s DESC, %s DESC",
                MatchEntry.TABLE_NAME, MatchEntry.COLUMN_USER, MatchEntry.COLUMN_STATE, MatchEntry.COLUMN_TIME,
                MatchEntry.TABLE_NAME,
                MessageEntry.TABLE_NAME, MessageEntry.COLUMN_MATCH_ID, MatchEntry.TABLE_NAME, MatchEntry._ID,
                //MatchEntry.COLUMN_STATE,
                MatchEntry.TABLE_NAME, MatchEntry._ID,
                MatchEntry.COLUMN_STATE, MessageEntry.COLUMN_DATE, MatchEntry.COLUMN_TIME
        );


        return getMatchsWithQuery(SELECT_QUERY, null);
    }

    /**
     * Delete the match with the given ID from the local cache
     * @param matchId ID of the match we want to delete
     */
    public int deleteMatch(long matchId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String[] args = { String.valueOf(matchId) };
        return db.delete(MatchEntry.TABLE_NAME, MatchEntry._ID + " = ?", args);
    }

    /**
     * Store a match in the local cache
     * @param match Match object to store
     * @return Id of the stored match (new ID if it didn't already exist)
     */
    public long storeMatch(Match match) {
        if(match == null) return -1;

        SQLiteDatabase db = this.getWritableDatabase();
        long matchId = -1;

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            if(match.getId() != -1) {
                values.put(MatchEntry._ID, match.getId());
            }
            values.put(MatchEntry.COLUMN_USER, match.getPartnerId());
            values.put(MatchEntry.COLUMN_STATE, match.getState().ordinal());
            values.put(MatchEntry.COLUMN_TIME, DBUtils.DateTimeToString(match.getTime()));


            int rows = db.update(MatchEntry.TABLE_NAME, values, MatchEntry._ID+" = ?",
                    new String[]{ String.valueOf(match.getId()) });

            if(rows == 1) { // Update succeeded
                String profileSelectQuery = String.format("SELECT %s FROM %s WHERE %s = ?",
                        MatchEntry._ID, MatchEntry.TABLE_NAME, MatchEntry._ID);
                Cursor cursor = db.rawQuery(profileSelectQuery, new String[]{ String.valueOf(match.getId()) });

                try {
                    if(cursor.moveToFirst()) {
                        matchId = cursor.getInt(0);
                        db.setTransactionSuccessful();
                    }
                } finally {
                    if(cursor != null && !cursor.isClosed()) {
                        cursor.close();
                    }
                }
            } else { // Did not already exist -> insert new
                //matchId = db.insertWithOnConflict(MatchEntry.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                matchId = db.insertOrThrow(MatchEntry.TABLE_NAME, null, values);
                db.setTransactionSuccessful();
            }

        } finally {
            db.endTransaction();
        }

        return matchId;
    }

    /**
     * Store a message of the chat room in the user's local database
     * @param message The message to be stored
     * @return New ID of the stored message
     */
    public long storeMessage(Message message) {
        if(message == null) return -1;

        SQLiteDatabase db = this.getWritableDatabase();
        long messageId = -1;

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();

            //values.put(MessageEntry._ID, message.getMsgId());

            values.put(MessageEntry.COLUMN_FROM, message.getSourceId());
            values.put(MessageEntry.COLUMN_DATE, DBUtils.DateTimeToString(message.getDateTime()));
            values.put(MessageEntry.COLUMN_MATCH_ID, message.getMatchId());
            values.put(MessageEntry.COLUMN_BODY, message.getContent());
            values.put(MessageEntry.COLUMN_IS_MINE, message.isMine());
            values.put(MessageEntry.COLUMN_STATUS, message.getStatus().ordinal());

            int rows = db.update(MessageEntry.TABLE_NAME, values, MessageEntry._ID+" = ?",
                                 new String[]{ String.valueOf(message.getMsgId()) });

            if(rows == 1) { // Update succeeded
                String profileSelectQuery = String.format("SELECT %s FROM %s WHERE %s = ?",
                        MessageEntry._ID, MessageEntry.TABLE_NAME, MessageEntry._ID);
                Cursor cursor = db.rawQuery(profileSelectQuery, new String[]{ String.valueOf(message.getMsgId()) });

                try {
                    if(cursor.moveToFirst()) {
                        messageId = cursor.getInt(0);
                        db.setTransactionSuccessful();
                    }
                } finally {
                    if(cursor != null && !cursor.isClosed()) {
                        cursor.close();
                    }
                }
            } else { // Did not already exist -> insert new
                messageId = db.insertOrThrow(MessageEntry.TABLE_NAME, null, values);
                db.setTransactionSuccessful();
            }
        } finally {
            db.endTransaction();
        }

        return messageId;
    }


    /**
     * Retrieve a list of the last 50 messages for a chat
     * @param matchId ID of the match whose the message is related
     * @return List of the messages for the given match (chat)
     */
    public List<Message> getMessagesForMatch(long matchId) {
        return getMessagesForMatch(matchId, 180);
    }

    /**
     * Return a List with all the messages corresponding to the given SQL SELECT statement
     * and params.
     * @param selectQuery SELECT SQL query to retrieve the relevant messages
     * @param params Sorted array of string parameter for the given SELECT statement.
     *               All the ? in the statement will be replaced by the given parameters, in order.
     * @return ArrayList of the messages corresponding to the given statement/parameters,
     *         or an empty ArrayList if no result was found.
     */
    private List<Message> getMessagesWithQuery(String selectQuery, String[] params) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery(selectQuery, params);

        List<Message> messageList = new ArrayList<>();

        try {
            while(res.moveToNext()) {
                Message message = new Message(
                        res.getLong(res.getColumnIndex(MessageEntry._ID)),
                        res.getInt(res.getColumnIndex(MessageEntry.COLUMN_FROM)),
                        DBUtils.StringToDateTime(res.getString(res.getColumnIndex(MessageEntry.COLUMN_DATE))),
                        res.getLong(res.getColumnIndex(MessageEntry.COLUMN_MATCH_ID)),
                        res.getString(res.getColumnIndex(MessageEntry.COLUMN_BODY)),
                        (res.getInt(res.getColumnIndex(MessageEntry.COLUMN_IS_MINE)) == 1),
                        Message.Status.values()[res.getInt(res.getColumnIndex(MessageEntry.COLUMN_STATUS))]
                );

                messageList.add(message);
            }
        } finally {
            if(res != null && !res.isClosed()) {
                res.close();
            }
        }

        return messageList;
    }

    /**
     * Retrieve a list of the last n messages for a chat (matchId),
     * n being the parameter messageNumber
     * @param matchId ID of the match whose the message is related
     * @param messageNumber Number of messages we want to retrieve
     * @return List of the messages for the given match (chat)
     */
    private List<Message> getMessagesForMatch(long matchId, int messageNumber) {
        String columnOrder = MessageEntry.COLUMN_DATE;

        final String SELECT_QUERY = String.format(
                "SELECT * FROM " +
                        "(SELECT * FROM %s WHERE %s = ? ORDER BY %s DESC LIMIT %s) " +
                "ORDER BY %s ASC",
                MessageEntry.TABLE_NAME, MessageEntry.COLUMN_MATCH_ID, columnOrder, String.valueOf(messageNumber),
                columnOrder
        );

        String[] params = new String[]{ String.valueOf(matchId) };

        return getMessagesWithQuery(SELECT_QUERY, params);
    }

    /**
     * Delete the message with the given ID from the local cache
     * @param msgId ID of the message we want to delete
     */
    public int deleteMessage(long msgId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String[] args = { String.valueOf(msgId) };
        return db.delete(MessageEntry.TABLE_NAME, MessageEntry._ID + " = ?", args);
    }

    /**
     * Retrieve the last message of a conversation (a match)
     * @param matchId ID of the match
     * @return Last message of the conversation with the given ID
     */
    public Message getLastMessageFromMatch(long matchId) {
        List<Message> lstMsg = getMessagesForMatch(matchId, 1);
        if(lstMsg.size() == 0) return null;
        return lstMsg.get(0);
    }

    /**
     * Update the match corresponding to the given matchId
     * @param matchId ID of the match we want to update
     * @return true if the match was
     */
    public boolean updateMatch(long matchId, Match.State state) {
        Match match = getMatch(matchId);
        if(match == null)
            return false;

        match.setState(state);
        return (storeMatch(match) != -1);
    }

    /**
     * Return the match corresponding to its given ID
     * @param matchId ID of the match to retrieve
     * @return Match corresponding to the given ID or null if it doesn't exist
     */
    public Match getMatch(long matchId) {
        final String SELECT_QUERY = String.format(
                "SELECT * FROM %s WHERE %s = ?",
                MatchEntry.TABLE_NAME, MatchEntry._ID
        );
        String[] params = new String[]{ String.valueOf(matchId) };
        List<Match> matches = getMatchsWithQuery(SELECT_QUERY, params);

        if(matches == null || matches.size() < 1) {
            return null;
        }

        return matches.get(0);
    }

    /**
     * @return The list of all messages that the user has sent offline.
     * Messages sent offline have the flag status WAIT_SENDING
     */
    public List<Message> getOfflineMessages() {
        final String SELECT_QUERY = String.format(Locale.getDefault(),
            "SELECT * FROM %s " +
                "WHERE %s = %d " +
                "ORDER BY %s ASC, %s ASC",
            MessageEntry.TABLE_NAME,
            MessageEntry.COLUMN_STATUS, Message.Status.WAIT_SENDING.ordinal(),
            MessageEntry.COLUMN_MATCH_ID, MessageEntry.COLUMN_DATE
        );

        return getMessagesWithQuery(SELECT_QUERY, null);
    }

    /**
     * To retrieve a message with its database ID
     * @param msgId Message ID in the database
     * @return Message Object corresponding to the given ID
     */
    public Message getMessage(long msgId) {
        final String SELECT_QUERY = String.format(Locale.getDefault(),
                "SELECT * FROM %s WHERE %s = ?",
                MessageEntry.TABLE_NAME, MessageEntry._ID
        );
        String[] params = new String[]{ String.valueOf(msgId) };
        List<Message> messages = getMessagesWithQuery(SELECT_QUERY, params);

        if(messages.size() == 0)
            return null;

        return messages.get(0);
    }

    /**
     * Update the state of the message denoted by its ID
     * @param msgId ID of the message we want to update the status
     * @param newStatus New status to assign to the message
     * @return -1 if update fails, the number of affected row(s) otherwise
     */
    public long updateMessageStatus(long msgId, Message.Status newStatus) {
        Message message = getMessage(msgId);

        if(message == null)
            return -1;

        message.setStatus(newStatus);
        return storeMessage(message);
    }

    /**
     * Verify if a given Message object already exists in the database.
     * The match between the given message and database's message in done
     * in checking that the following fields are the same :
     *  - Date of the message
     *  - Author of the message
     *  - Conversation related to this message
     *  - Content of the message
     * @param message Message to check if it already exists in database
     * @return true if the message exists, false otherwise
     */
    private boolean existsMessage(Message message) {
        if(message == null)
            return false;

        final String SELECT_QUERY = String.format(
            "SELECT * FROM %s " +
            "WHERE %s = datetime(?, 'unixepoch', 'localtime') AND %s = ? AND %s = ? AND %s = ?",
            MessageEntry.TABLE_NAME,
            MessageEntry.COLUMN_DATE, MessageEntry.COLUMN_FROM, MessageEntry.COLUMN_MATCH_ID, MessageEntry.COLUMN_BODY
        );

        String[] params = new String[]{
            String.valueOf(message.getDateTime().getTime() / 1000),
            String.valueOf(message.getSourceId()),
            String.valueOf(message.getMatchId()),
            message.getContent()
        };

        List<Message> messages = getMessagesWithQuery(SELECT_QUERY, params);
        return (messages.size() > 0);
    }

    /**
     * Store a message in the database only if it didn't exists before.
     * @param message Message to store
     * @return ID of the new stored message or -1 if the message already exists
     */
    public long storeMessageIfNotExists(Message message) {
        if(message == null)
            return -1;

        if(!existsMessage(message)) {
            return storeMessage(message);
        }
        return -1;
    }
}