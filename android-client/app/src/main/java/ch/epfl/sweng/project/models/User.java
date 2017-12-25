package ch.epfl.sweng.project.models;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

import ch.epfl.sweng.project.models.db.DBHandler;

/**
 * Represent a User of the application.
 * A User object wrap an entry of the User's database table.
 *
 * @author Dominique Roduit
 */
public final class User implements Serializable {

    /* Keys of the data received from the server */
    private static final String KEY_ID = "id";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_FIRSTNAME = "firstname";
    private static final String KEY_LASTNAME = "lastname";

    /* Fields concerning the User object */
    private long id = -1;
    private String email = "";
    private String firstName = "";
    private String lastName = "";
    private Profile _profile = null;
    private Avatar _avatar = null;

    /**
     * Create a new User object, holding database
     * information related to the user.
     * Most of values are given by tequila.
     * @param id ID of the user in database
     * @param email E-mail of user
     * @param firstName First name of user
     * @param lastName Last name of user
     */
    public User(long id, String email, String firstName, String lastName) {
        setId(id);
        setEmail(email);
        setFirstName(firstName);
        setLastName(lastName);
    }

    /**
     * @return The user object in JSONObject format
     */
    public JSONObject toJSON() {

        JSONObject jsonUser = new JSONObject();
        try {
            jsonUser.put(KEY_ID, this.getId());
            jsonUser.put(KEY_EMAIL, this.getEmail());
            jsonUser.put(KEY_FIRSTNAME, this.getFirstName());
            jsonUser.put(KEY_LASTNAME, this.getLastName());

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return jsonUser;
    }

    /**
     * Create a User object from a JSONObject
     * @param userJSON User in JSONObject format
     * @return User object from the given JSON object
     * @throws JSONException
     */
    public static User fromJSON(JSONObject userJSON) throws JSONException {
        return new User(
                userJSON.getLong(KEY_ID),
                userJSON.getString(KEY_EMAIL),
                userJSON.getString(KEY_FIRSTNAME),
                userJSON.getString(KEY_LASTNAME)
        );
    }


    /**
     * @return Full name of the user in the format "First name Last name"
     */
    public String getFullName() {
        return getFirstName() + " " + getLastName();
    }

    /**
     * @return ID of the user corresponding to the one in the database
     */
    public long getId() {
        return id;
    }

    /**
     * Define the user ID and if a profile / avatar is attached,
     * also update their user ID.
     * @param id ID of the user
     */
    public void setId(long id) {
        this.id = id;

        if (this._profile != null)
            this._profile.setUserId(id);

        if (this._avatar != null)
            this._avatar.setUserId(id);
    }

    /**
     * @return E-mail of the user
     */
    public String getEmail() {
        return email;
    }

    /**
     * Define the e-mail of the user
     * @param email E-mail of the user
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return Profile corresponding to this user, if exists (from the database)
     */
    public Profile get_profile(Context ctx) {
        Profile retProfile = this._profile;
        if (retProfile == null) {
            DBHandler db = DBHandler.getInstance(ctx);
            retProfile = db.getProfile(this.getId());
            this._profile = retProfile;
        }
        return retProfile;
    }

    /**
     * @return First name of the user
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Define the First name of the user
     * @param firstName First name of the user
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * @return Last name of the user
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Define the Last name of the user
     * @param lastname Last name of the user
     */
    public void setLastName(String lastname) {
        this.lastName = lastname;
    }

    /**
     * @return Avatar corresponding to this user, if exists (from the database)
     */
    public Avatar getAvatar(Context ctx) {
        Avatar retAvatar = this._avatar;
        if (retAvatar == null) {
            DBHandler db = DBHandler.getInstance(ctx);
            retAvatar = db.getAvatar(this.getId());
            this._avatar = retAvatar;
        }
        return retAvatar;
    }

    @Override
    public String toString() {
        return "{\n" +
            KEY_ID + " : " + getId() + ", " + "\n" +
            KEY_EMAIL + " : " + getEmail() + ", " + "\n" +
            KEY_FIRSTNAME + " : " + getFirstName() + ", " + "\n" +
            KEY_LASTNAME + " : " + getLastName() + " " + "\n" +
        "}";
    }

}