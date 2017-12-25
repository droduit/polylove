package ch.epfl.sweng.project.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import ch.epfl.sweng.project.utils.AndroidUtils;
import ch.epfl.sweng.project.utils.DBUtils;

/**
 * Class who represents a message from the database.
 *
 * @author Dominique Roduit
 */
public final class Message {

    /* Keys of the data received from the server */
    public static final String KEY_STATUS = "status";
    public static final String KEY_SENT_AT = "sentAt";
    public static final String KEY_MATCH_ID = "matchId";
    public static final String KEY_SENDER_ID = "senderId";
    public static final String KEY_MESSAGE_ID = "id";
    public static final String KEY_CONTENT = "content";
    public static final String KEY_MY_ID = "myId";

    /**
     * Represent the status of a message through the time
     */
    public enum Status {
        /* When the message is in the local cache but not sent to the server */
        WAIT_SENDING,
        /* When the message is sent to the server but not acknowledged */
        WAIT_ACK,
        /* When the message is sent to the server and acknowledged */
        SENT
    }

    /* ID of the message in the database */
    private long msgId;
    /* ID of the user source*/
    private long sourceId;
    /* Date where the message was sent */
    private Date dateTime;
    /* ID of the match to which the message belongs to  */
    private long matchId;
    /* Body of the message */
    private String content;
    /* Determine if the message is from myself or the other user */
    private boolean isMine;
    /* Flag to determine the send status of the message */
    private Status status;

    /**
     * Create a new message belonging to a conversation represented by a match (with matchId),
     * sent by the user with ID sourceId.
     * @param msgId ID of the message (used only to retrieve from database,
     *              this ID is not stored in database when the message object is stored)
     * @param sourceId ID of the user who sent this message
     * @param dateTime Date where the message was sent
     * @param matchId ID of the conversation (match) whose the message belongs
     * @param content Text of the message
     * @param isMine Whether the message has be sent by me or its a received message
     * @param status Status of the message
     */
    public Message(long msgId, long sourceId, Date dateTime, long matchId, String content, boolean isMine, Status status) {
        this.sourceId = sourceId;
        this.dateTime = new Date(dateTime.getTime());
        this.matchId = matchId;
        this.content = content;

        isMine(isMine);
        setMsgId(msgId);
        setStatus(status);
        this.msgId = msgId;
    }

    /**
     * Create a Message object from a given JSONObject
     * @param message The message object in form of a JSONObject
     * @return Message Object corresponding to the JSONObject
     * @throws JSONException If something goes wrong with the JSON handled
     */
    public static Message fromJSON(JSONObject message) throws JSONException {
        if(message == null)
            return null;

        long msgId;
        if(!message.isNull(KEY_MESSAGE_ID)) {
            msgId = message.getLong(KEY_MESSAGE_ID);
        } else {
            msgId = message.getLong("messageId");
        }

        long matchId = message.getLong(KEY_MATCH_ID);
        long userId = message.getLong(KEY_SENDER_ID);
        String content = message.getString(KEY_CONTENT);
        long timestamp = message.getLong(KEY_SENT_AT);
        Date dateTime = DBUtils.timestampMsToDate(timestamp);
        Message.Status status = Message.Status.valueOf(message.getString(KEY_STATUS));
        boolean isMine = false;
        if(!message.isNull(KEY_MY_ID)) {
            isMine = message.getLong(KEY_MY_ID) == userId;
        }

        return new Message(msgId, userId, dateTime, matchId, content, isMine, status);
    }


    // ---------------------- GETTERS & SETTERS ------------------------------

    /**
     * Return the formatted time for a message.
     * The format can be the following :
     *  - HH:mm : for the messages of the current day
     *  - Wed, Thu, ... : for the messages of the current week
     *  - Oct 19, Sept 08, ... : for the messages older than the current week
     *  - 15.12.15, ... : for the messages older than the current year
     * @return Time in the format described above
     */
    public String getFormattedTime() {
        return AndroidUtils.getFormattedTime(getDateTime());
    }


    /**
     * @return Hour of the message in the format HH:mm
     */
    public String getTime() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        return dateTime != null ? timeFormat.format(getDateTime()) : "";
    }

    /**
     * @return Date of the message in the format dd.MM.yyyy
     */
    public String getDate() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("dd.MM.yyyy");
        return dateTime != null ? timeFormat.format(getDateTime()) : "";
    }

    /**
     * @return ID of the user who sent the message
     */
    public long getSourceId() {
        return sourceId;
    }
    /**
     * @return Date of the message
     */
    public Date getDateTime() {
        return (Date) dateTime.clone();
    }
    /**
     * @return Match ID whose the message belongs to
     */
    public long getMatchId() {
        return matchId;
    }
    /**
     * @return Text of the message
     */
    public String getContent() {
        return content;
    }
    /**
     * @return Whether the message is sent by me or not
     */
    public boolean isMine() {
        return isMine;
    }
    /**
     * Define if the message is sent by me or not
     * @param value true if message is sent by me
     */
    public void isMine(boolean value) {
        this.isMine = value;
    }
    /**
     * @return ID of the message from the database
     */
    public long getMsgId() {
        return msgId;
    }
    /**
     *  Define the ID of the message
     * @param id ID of the message
     */
    public void setMsgId(long id) { this.msgId = id; }

    /**
     * @return Current status of the message
     */
    public Status getStatus() {
        return status;
    }
    /**
     * Define the current status of the message
     * @param status Current status of the message
     */
    public void setStatus(Status status) {
        this.status = status;
    }


/*
    public static Message fromJSON(JSONObject message, Map<String, Object> infos) throws JSONException {
        if(message == null)
            return null;

        long msgId;
        if(!message.isNull(KEY_MESSAGE_ID)) {
            msgId = message.getLong(KEY_MESSAGE_ID);
        } else {
            msgId = message.getLong("messageId");
        }

        long matchId = (long)getObjectOrDefault(KEY_MATCH_ID, message, infos);
        long userId = (long)getObjectOrDefault(KEY_SENDER_ID, message, infos);
        String content = (String)getObjectOrDefault(KEY_CONTENT, message, infos);
        long timestamp = (long)getObjectOrDefault(KEY_SENT_AT, message, infos);
        Date dateTime = DBUtils.timestampMsToDate(timestamp);
        Message.Status status;
        if (message.isNull(KEY_STATUS)){
            status = Status.SENT;
        }else{
            status = Message.Status.valueOf(message.getString(KEY_STATUS));
        }
        boolean isMine = ((long)getObjectOrDefault(KEY_MY_ID, message, infos))==userId;
        return new Message(msgId, userId, dateTime, matchId, content, isMine, status);
    }

    private static Object getObjectOrDefault(String key, JSONObject message, Map<String, Object> infos) throws JSONException {
        if (message.isNull(key)){
            return infos.get(key);
        }
        else{
            return message.get(key);
        }
    }*/
}
