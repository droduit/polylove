package ch.epfl.sweng.project.models;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;

import ch.epfl.sweng.project.R;
import ch.epfl.sweng.project.models.db.DBHandler;
import ch.epfl.sweng.project.utils.AndroidUtils;
import ch.epfl.sweng.project.utils.DBUtils;

/**
 * This class wrap a Match between two users in a object.
 * Only one userId is hold, that is the ID of the person
 * with which the app's partner has the match.
 *
 * This class also has a role of Adapter between the
 * "Match" table of the database and the application.
 *
 * The field "id" of this object is important here, because we need
 * to have same ID between server and client and to link messages
 * with match.
 *
 * @author Dominique Roduit
 */
public final class Match {

    /* Keys of the data received from the server */
    public static final String KEY_MATCH_ID = "match";
    public static final String KEY_USER1_ID = "user1";
    public static final String KEY_USER2_ID = "user2";
    public static final String KEY_STATE = "state";
    public static final String KEY_CREATION_TIME = "createdAt";

    /* Match duration in hours */
    public static final int MATCH_DURATION = 1;

    /*
    Where two people are automatically matched in a day, the state remains pending, until :
    1) both choose to continue -> State become 'Open'
    2) one of them choose to not continue -> State become 'Close'
    */
    public enum State {
        /* - Rights : the partnerId as a restricted view on the profile of his 'partner'
         * - When : daily match is pending and not confirmed yet */
        Pending,
        /* - Rights : same as pending
         * - When : daily match is confirmed by the user but time is not finished yet */
        Confirmed,
        /* - Rights : can see all user's profile informations
         * - When : both users are interested and confirmed the match */
        Open,
        /* - Rights : same as pending
         * - When : time is out and 1 or both users decided that they don't want to match this person */
        Close
    }

    /* ID of the match in the database */
    private long id = 0;
    /* ID of the user concerned by this match */
    private long partnerId = 0;
    /* State of the match (see State enum above) */
    private State state = State.Close;
    /* Time when the match proposal happens */
    private Date time = new Date();
    /* Time when the match proposal will expire */
    private Date expirationTime = new Date();

    /**
     * Constructor for match between this and another user
     * @param id id of the match
     * @param partnerId id of the user "this" is matched with
     * @param state the match is still wanted by the people or not
     * @param time time when the CRON matched the people
     */
    public Match(long id, long partnerId, State state, Date time) {
        this.id = id;
        setPartnerId(partnerId);
        setState(state);
        setTime(time);
    }

    /**
     * Create a new Match object from a JSONObject received
     * @param matchJSON JSONObject containing the match's information
     * @param context Context of the calling activity
     * @return Match created on the basis of the given JSONObject
     * @throws JSONException if there is a JSON problem
     */
    public static Match fromJSON(JSONObject matchJSON, Context context) throws JSONException {
        if(!matchJSON.has(KEY_MATCH_ID) || !matchJSON.has(KEY_USER1_ID) ||
                !matchJSON.has(KEY_USER2_ID)) {
            return null;
        }

        long matchId = matchJSON.getLong(KEY_MATCH_ID);
        long user1Id = matchJSON.getLong(KEY_USER1_ID);
        long user2Id = matchJSON.getLong(KEY_USER2_ID);
        Match.State setState = State.Close;
        Date date = new Date();

        /*
        if(user1Id == user2Id) {
            return null;
        }
        */

        if(matchJSON.has(KEY_STATE)) {
            setState = State.valueOf(matchJSON.getString(KEY_STATE));
        }

        if(matchJSON.has(KEY_CREATION_TIME)) {
            date = DBUtils.timestampMsToDate(matchJSON.getLong(KEY_CREATION_TIME));
        }


        long myId = Settings.getInstance(context).getUserID();
        long matchedUserId = (user1Id == myId) ? user2Id : user1Id;

        return new Match(matchId, matchedUserId, setState, date);
    }

    /**
     * @return ID of the Match in the database
     */
    public long getId() {
        return id;
    }
    /**
     * @return ID of the user with who we have the match
     */
    public long getPartnerId() {
        return partnerId;
    }
    /**
     * Define the ID of the user with who we have the match
     * @param partnerId ID of the user with who we have the match
     */
    public void setPartnerId(long partnerId) {
        this.partnerId = partnerId;
    }
    /**
     * @return Current state of the match
     */
    public State getState() {
        return state;
    }
    /**
     * Define the current state of the match
     * @param state Current state of the match
     */
    public void setState(State state) {
        this.state = state;
    }

    /**
     * Set state of the match and save the new state in local database
     * @param ctx Context of the calling activity
     * @param state New State to apply this match
     * @return Number of rows affected in the database or -1 in case of failure
     */
    public long setState(Context ctx, State state) {
        setState(state);
        return DBHandler.getInstance(ctx).storeMatch(this);
    }
    /**
     * @return Time when the match was created
     */
    public Date getTime() {
        return (Date)time.clone();
    }
    /**
     * Define the time when the match was created
     * @param time Date which indicate the time when the match was created
     */
    public void setTime(Date time) {
        this.time = new Date(time.getTime());
    }

    /**
     * Compute the time left for this match until expiration in milliseconds.
     * @return Time left in milliseconds
     */
    public long getMillisecondsLeft() {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        long now = c.getTimeInMillis();
        long endToday = getEndDateInMillis();
        long oneDayInMillis = 24 * 60 * 60 * 1000;
        long endTomorrow = endToday + oneDayInMillis;

        long end = endToday;

        if(now > endToday) {
            end = endTomorrow;
        }

        return end - now;
    }

    /**
     * @return Timestamp of the next end time for the match
     */
    public static long getEndDateInMillis() {
        Calendar c = Calendar.getInstance();
        long today = (long) (Math.floor(c.getTimeInMillis() / (24 * 60 * 60 * 1000)) * 24 * 60 * 60 * 1000);
        return today + (21-1) * 60 * 60 * 1000;
    }

    /**
     * Return the last message from the conversation related to this match
     * @param ctx Context of the calling activity
     * @return Last message for this match's conversation
     */
    public Message getLastMessage(Context ctx) {
        return DBHandler.getInstance(ctx).getLastMessageFromMatch(getId());
    }

    /**
     * Give a friendly displayable username for the user concerned by the match,
     * according to the state of the match. If match is open, the full name is
     * returned, otherwise, the state is mentioned for closed matches and a text
     * who keep the user unknown for pending matches.
     *
     * @param ctx Context of the caller activity
     * @return Friendly displayable name for conversations according to the match's state
     */
    public String getFriendlyUsername(Context ctx) {
        User user = DBHandler.getInstance(ctx).getUser(getPartnerId());
        String name = getState().toString();
        if (user == null || getState() != State.Open) {
            if (getState() != State.Open) {
                if(getState() == State.Close) {
                    name = name + " - " + AndroidUtils.getFormattedTime(getTime());
                } else {
                    name = ctx.getString(R.string.todays_match);
                }
            }
        } else {
            name = user.getFullName();
        }
        return name;
    }

}
