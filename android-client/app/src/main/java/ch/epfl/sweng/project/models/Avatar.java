package ch.epfl.sweng.project.models;

import android.content.Context;
import android.graphics.Bitmap;

import org.json.JSONException;
import org.json.JSONObject;

import ch.epfl.sweng.project.models.db.DBHandler;
import ch.epfl.sweng.project.utils.AvatarGraphics;

/**
 * Wrap the avatar of a User in a single object
 * holding all the parts composing it and allowing
 * to get a flattened bitmap image.
 *
 * @author Dominique Roduit
 */
public final class Avatar {

    /* JSON Keys -------------------------- */
    public static final String KEY_ID = "id";
    public static final String KEY_GENDER = "gender";
    private static final String KEY_EYE = "eyes";
    private static final String KEY_SKIN = "skin";
    private static final String KEY_HAIR_STYLE = "hairStyle";
    private static final String KEY_HAIR_COLOR = "hairColor";
    private static final String KEY_SHIRT = "shirt";

    /* Enum relevant for an Avatar -------- */
    public enum HairStyle {
        Style1, Style2, Style3, Style4, Style5, Style6, Style7
    }
    public enum HairColor {
        Blond, Brown, Black, Ginger
    }
    public enum Eye {
        Blue, Green, Brown
    }
    public enum Skin {
        Dark, Medium, Light
    }
    public enum Shirt {
        Style1, Style2, Style3
    }

    /* Default avatar values -------------- */
    private long userId = 0;
    private Eye eyeColor = Eye.Blue;
    private HairColor hairColor = HairColor.Blond;
    private HairStyle hairStyle = HairStyle.Style1;
    private Skin skinTone = Skin.Medium;
    private Shirt shirt = Shirt.Style1;
    private Profile.Gender gender;

    // Flattened bitmaps of the avatar in two different size
    private AvatarGraphics graphics;
    private Bitmap image = null;
    private Bitmap icon = null;

    /**
     * Used to create an avatar step by step, calling all the setters one after another.
     * If one of the field is not defined, the default value will be kept.
     */
    public Avatar() {
        setGraphics(new AvatarGraphics(this));
    }

    /**
     * Create an Avatar for the user with ID userId
     * @param userId ID of the user for which we create the avatar
     * @param gender Gender of the avatar (to determine the base image to display)
     * @param eyeColor Eye color of the avatar
     * @param hairColor Hair color of the avatar
     * @param hairStyle Hair style / haircut of the avatar
     * @param skinTone Skin color of the avatar
     * @param shirt Type of clothes the avatar wears
     */
    public Avatar(long userId, Profile.Gender gender, Eye eyeColor, HairColor hairColor, HairStyle hairStyle, Skin skinTone, Shirt shirt) {
        this();
        setUserId(userId);
        setGender(gender);
        setEyeColor(eyeColor);
        setHairColor(hairColor);
        setHairStyle(hairStyle);
        setSkinTone(skinTone);
        setShirt(shirt);
    }


    /**
     * @return Avatar in JSONObject format
     */
    public JSONObject toJson() {
        JSONObject jsonAvatar = new JSONObject();
        try {
            jsonAvatar.put(KEY_EYE, eyeColor.toString());
            jsonAvatar.put(KEY_SKIN, skinTone.toString());
            jsonAvatar.put(KEY_HAIR_STYLE, hairStyle.toString());
            jsonAvatar.put(KEY_HAIR_COLOR, hairColor.toString());
            jsonAvatar.put(KEY_SHIRT, shirt.toString());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return jsonAvatar;
    }

    /**
     * Create an Avatar object from a JSONObject
     * @param avatar Avatar received from the server in JSON
     * @return Avatar Object corresponding to the JSON received
     */
    public static Avatar fromJSON(JSONObject avatar) throws JSONException {
        if(avatar == null) {
            return null;
        }

        long id = -1;
        if(!avatar.isNull(KEY_ID)) {
            id = avatar.getLong(KEY_ID);
        }

        Profile.Gender gender = Profile.Gender.Male;
        if(!avatar.isNull(KEY_GENDER)) {
            gender = Profile.Gender.valueOf(avatar.getString(KEY_GENDER));
        }

        return new Avatar(
            id,
            gender,
            Eye.valueOf(avatar.getString(KEY_EYE)),
            HairColor.valueOf(avatar.getString(KEY_HAIR_COLOR)),
            HairStyle.valueOf(avatar.getString(KEY_HAIR_STYLE)),
            Skin.valueOf(avatar.getString(KEY_SKIN)),
            Shirt.valueOf(avatar.getString(KEY_SHIRT))
        );
    }

    /**
     * Return the User object associated to this avatar
     * @param db DBHandler object, instance of the database connection
     * @return User who has this avatar
     */
    public User getUser(DBHandler db) {
        return db.getUser(this.getUserId());
    }

    /**
     * @return ID of the user whose the avatar belongs to
     */
    public long getUserId() {
        return userId;
    }
    /**
     * Define the user ID whose the avatar belongs to
     * @param userId User ID whose the avatar belongs to
     */
    public void setUserId(long userId) {
        this.userId = userId;
    }

    /**
     * @return Gender of the avatar
     */
    public Profile.Gender getGender() {
        return this.gender;
    }

    /**
     * Define the gender of the avatar
     * @param gender Gender of the avatar
     */
    public void setGender(Profile.Gender gender) {
        this.gender = gender;
        updateGraphics();
    }

    /**
     * @return Eye color of the avatar
     */
    public Eye getEyeColor() {
        return eyeColor;
    }
    /**
     * Define the eye color of the avatar
     * @param eyeColor Eye color of the avatar
     */
    public void setEyeColor(Eye eyeColor) {
        this.eyeColor = eyeColor;
        updateGraphics();
    }

    /**
     * @return Hair color of the avatar
     */
    public HairColor getHairColor() {
        return hairColor;
    }
    /**
     * Define the hair color of the avatar
     * @param hairColor Hair color of the avatar
     */
    public void setHairColor(HairColor hairColor) {
        this.hairColor = hairColor;
        updateGraphics();
    }

    /**
     * @return Skin color of the avatar
     */
    public Skin getSkinTone() {
        return skinTone;
    }
    /**
     * Define the skin color of the avatar
     * @param skinTone Skin color of the avatar
     */
    public void setSkinTone(Skin skinTone) {
        this.skinTone = skinTone;
        updateGraphics();
    }

    /**
     * @return Hair style of the avatar
     */
    public HairStyle getHairStyle() { return hairStyle; }
    /**
     * Define the hair style of the avatar
     * @param hairStyle Hair style of the avatar
     */
    public void setHairStyle(HairStyle hairStyle) {
        this.hairStyle = hairStyle;
        updateGraphics();
    }

    /**
     * @return Type of clothes worn by the avatar
     */
    public Shirt getShirt() { return shirt; }
    /**
     * Define the kind of clothes worn by the avatar
     * @param shirt Kind of clothes worn by the avatar
     */
    public void setShirt(Shirt shirt) {
        this.shirt = shirt;
        updateGraphics();
    }

    /**
     * Define the graphic bitmap object generator for this avatar
     * @param graphics Bitmap handler for the avatar.
     */
    public void setGraphics(final AvatarGraphics graphics) {
        this.graphics = graphics;
    }

    /**
     * Update the bitmaps related to this avatar.
     * Called each time we modify something in the avatar properties.
     */
    private void updateGraphics() {
        if(graphics != null) {
            graphics.setAvatar(this);
        }
    }


    public Bitmap getImage(){ return image; }
    public Bitmap getImage(Context context) {
        if(image == null){
            updateImage(context);
        }
        return image;
    }
    public void setImage(Bitmap image) { this.image = image; }

    public Bitmap getIcon(Context context){
        if(icon == null){
            updateImage(context);
        }
        return icon;
    }
    public Bitmap getIcon(){ return icon; }
    public void setIcon(Bitmap icon){ this.icon = icon; }

    public void updateImage(Context context) {
        graphics.updateImage(context);
        setImage(graphics.getAvatar().getImage());
        setIcon(graphics.getAvatar().getIcon());
    }

    public String getBaseImage() {
        return graphics.getBaseImage();
    }
    public String getHairImage() {
        return graphics.getHairImage();
    }
    public String getSkinImage(){
        return graphics.getSkinImage();
    }
    public String getEyesImage() {
        return graphics.getEyesImage();
    }
    public String getShirtImage() {
        return graphics.getShirtImage();
    }

    @Override
    public String toString() {
        return "{\n"+
                KEY_ID + " : "+getUserId()+", "+"\n"+
                KEY_SKIN + " : "+ getSkinTone().toString()+", "+"\n"+
                KEY_EYE + " : "+ getEyeColor().toString()+", "+"\n"+
                KEY_HAIR_STYLE + " : "+getHairStyle().toString()+", "+"\n"+
                KEY_HAIR_COLOR + " : "+getHairColor().toString()+", "+"\n"+
        "}";
    }
}