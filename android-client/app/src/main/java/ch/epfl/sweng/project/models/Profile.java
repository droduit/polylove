package ch.epfl.sweng.project.models;

import android.content.Context;
import android.graphics.Bitmap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import ch.epfl.sweng.project.R;
import ch.epfl.sweng.project.models.db.DBHandler;
import ch.epfl.sweng.project.network.Utils;
import ch.epfl.sweng.project.utils.AndroidUtils;
import ch.epfl.sweng.project.utils.DBUtils;

/**
 * Wrap a user profile into an object similar
 * to the one in the local database.
 *
 * @author Dominique Roduit
 *
 */
public final class Profile implements Serializable {

    /* Keys of the data received from the server */
    public static final String KEY_ID = "id";
    public static final String KEY_SECTION = "section";
    public static final String KEY_GENDER = "gender";
    public static final String KEY_BIRTHDAY = "birthday";
    public static final String KEY_HOBBIES = "hobbies";
    public static final String KEY_DESCRIPTION = "description";
    public static final String KEY_LANGUAGES = "languages";
    public static final String KEY_GENDER_INTEREST = "genderInterest";
    public static final String KEY_AGE_INTEREST = "ageInterest";
    public static final String KEY_AGE_START = "start";
    public static final String KEY_AGE_END = "end";

    /* MIN and MAX age available in profile RangeSeekBar */
    public final static int MIN_AGE = 16;
    public final static int MAX_AGE = 60;

    /* Enums related to the profiles ------------------ */
    /**
     * Represent the gender of the user
     */
    public enum Gender {
        Male, Female
    }

    /**
     * Represent the gender in which the user is interested
     */
    public enum GenderInterest {
        Male, Female, Both
    }

    /**
     * Available languages that users can choose
     * as their spoken languages
     */
    public enum Language {
        Albanian, Arabic,
        Bulgarian,
        Catalan, Chinese, Croatian, Czech,
        Danish, Dutch,
        English, Estonian,
        Filipino, Finnish, French,
        Georgian, German, Greek,
        Hebrew, Hungarian,
        Icelandic, Italian,
        Japanese,
        Korean,
        Latvian, Lithuanian,
        Macedonian, Malagasy, Maltese,
        Norwegian,
        Polish, Portuguese,
        Romanian, Russian,
        Serbian, Slovak, Slovenian, Spanish, Swedish,
        Thai, Turkish,
        Ukrainian,
        Vietnamese
    }

    /**
     * Section of the user
     */
    public enum Section {
        AR(R.string.section_ar),
        GC(R.string.section_gc),
        SIE(R.string.section_sie),
        CGC(R.string.section_cgc),
        MA(R.string.section_ma),
        PH(R.string.section_ph),
        EL(R.string.section_el),
        GM(R.string.section_gm),
        MX(R.string.section_mx),
        MT(R.string.section_mt),
        IN(R.string.section_in),
        SC(R.string.section_sc),
        STV(R.string.section_stv),
        CMS(R.string.section_cms);

        private int stringId = 0;

        Section(int stringId) {
            this.stringId = stringId;
        }

        public int getStringId() {
            return stringId;
        }
    }
    /* ------------------------------------------------ */

    /* ID of this user in the database.
    Default value is 0 (no user with ID 0 exists in the database) */
    private long userId = 0;
    /* Section of the user */
    private Section section = Section.IN;
    /* Gender of the user */
    private Gender gender = Gender.Male;
    /* Birth date of the user */
    private Date birthday = new Date();
    /* Hobbies / passions of the user */
    private Set<String> hobbies = new TreeSet<>();
    /* Description or free field where the user can write whatever he wants */
    private String description = "";
    /* Languages spoken by the user */
    private Set<Language> languages = new TreeSet<>();
    /* Gender in which the user is interested */
    private GenderInterest genderInterest = GenderInterest.Female;
    /* Age min. for the matches proposed to this user */
    private int ageInterestStart = 18;
    /* Age max. for the matches proposed to this user */
    private int ageInterestEnd = 50;
    /* Profile's photo of this user (optional) */
    private Bitmap photo = null;

    /**
     * Only used to create a profile step by step using all setters in certain situations,
     * as this class contains a lot of fields.
     *
     * It shouldn't be an issue, because the Profile object is used to wrap an
     * entry of the database, so the fields should always be filled.
     *
     * If a Profile object is created by hand (for example for the tests) and some
     * fields are missing, it's not important, we will just take the default values.
     */
    public Profile() {}

    /**
     * Create a new user's profile with the given values.
     * If the user has a photo, it can be attached to this object using setPhoto().
     * @param userId ID of this user in the database
     * @param section Section of the user
     * @param gender Gender of the user
     * @param birthday Birth date of the user
     * @param hobbies Hobbies / passions of the user
     * @param description Description or free field where the user can write whatever he wants
     * @param languages Languages spoken by the user
     * @param genderInterest Gender in which the user is interested
     * @param ageInterestStart Age min. for the matches proposed to this user
     * @param ageInterestEnd Age max. for the matches proposed to this user
     */
    public Profile(long userId, Section section, Gender gender, Date birthday,
                   Set<String> hobbies, String description, Set<Language> languages,
                   GenderInterest genderInterest, int ageInterestStart, int ageInterestEnd) {
        setUserId(userId);
        setSection(section);
        setGender(gender);
        setBirthday(birthday);
        setHobbies(hobbies);
        setDescription(description);
        setLanguages(languages);
        setGenderInterest(genderInterest);
        setAgeInterestStart(ageInterestStart);
        setAgeInterestEnd(ageInterestEnd);
    }

    /**
     * Constructor used to give the hobbies, languages and birthday in string format.
     * The given string will be converted to the relevant format.
     * @param userId ID of this user in the database
     * @param section Section of the user
     * @param gender Gender of the user
     * @param birthday Birth date of the user (in String SQL format YYYY-mm-dd)
     * @param hobbies Hobbies / passions of the user (in String with separated comma values)
     * @param description Description or free field where the user can write whatever he wants
     * @param languages Languages spoken by the user (in String with separated comma values)
     * @param genderInterest Gender in which the user is interested
     * @param ageInterestStart Age min. for the matches proposed to this user
     * @param ageInterestEnd Age max. for the matches proposed to this user
     */
    public Profile(long userId, Section section, Gender gender, String birthday,
                   String hobbies, String description, String languages,
                   GenderInterest genderInterest, int ageInterestStart, int ageInterestEnd) {

        this(userId, section, gender, new Date(), new HashSet<String>(),
            description, new HashSet<Language>(),
                genderInterest, ageInterestStart, ageInterestEnd);

        setHobbies(hobbies);
        setLanguages(languages);
        setBirthday(birthday);
    }


    /**
     * @return The Profile object in JSONObject format
     */
    public JSONObject toJson() {
        //La JSONException doit être traitée ici car la personne qui emploit toJson n'a aucune prise ou responsabilité dans le traitement de cette erreur.
        //Il faut juste être sûr que chaque valeur est définie pour Json : pas de null pour name, pas de NaN ou inf dans les champs numériques, etc.
        //N'importe qu'elle cas qui peut provoquer un JSONException n'est pas recoverable par l'utilisateur de la fonction.
        JSONObject jsonProfile = new JSONObject();
        try {
            JSONArray jsonLanguages = new JSONArray();
            for (Language lang : this.getLanguages())
                jsonLanguages.put(lang.toString());

            jsonProfile.put(KEY_SECTION, getSection().toString());
            jsonProfile.put(KEY_GENDER, this.getGender().toString());
            jsonProfile.put(KEY_BIRTHDAY, Utils.DATE_FORMAT.format(this.getBirthday()));
            jsonProfile.put(KEY_HOBBIES, new JSONArray(this.getHobbies()));
            jsonProfile.put(KEY_DESCRIPTION, this.getDescription());
            jsonProfile.put(KEY_LANGUAGES, jsonLanguages);
            jsonProfile.put(KEY_GENDER_INTEREST, this.getGenderInterest());

            JSONObject ageInterest = new JSONObject();

            ageInterest.put(KEY_AGE_START, this.getAgeInterestStart());
            ageInterest.put(KEY_AGE_END, this.getAgeInterestEnd());

            jsonProfile.put(KEY_AGE_INTEREST, ageInterest);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return jsonProfile;
    }


    /**
     * Create a Profile object from a JSONObject
     * @param profile The profile received in JSON from the server
     * @return The corresponding Profile object
     * @throws JSONException
     */
    public static Profile fromJSON(final JSONObject profile) throws JSONException {
        if(profile == null) {
            return null;
        }

        // Hobbies ---------
        Set<String> hobbies = new TreeSet<>();
        JSONArray hobbiesJSON = profile.getJSONArray(KEY_HOBBIES);
        if(hobbiesJSON != null) {
            int hobbiesLen = hobbiesJSON.length();
            for(int i = 0; i < hobbiesLen; ++i) {
                hobbies.add(hobbiesJSON.get(i).toString());
            }
        }

        // Languages ---------
        Set<Language> languages = new TreeSet<>();
        JSONArray languagesJSON = profile.getJSONArray(KEY_LANGUAGES);
        if(languagesJSON != null) {
            int languagesLen = languagesJSON.length();
            for(int i = 0; i < languagesLen; ++i) {
                languages.add(Language.valueOf(languagesJSON.get(i).toString()));
            }
        }

        JSONObject ageInterest = profile.getJSONObject(KEY_AGE_INTEREST);

        return new Profile(
            profile.getLong(KEY_ID),
            Section.valueOf(profile.isNull(KEY_SECTION) ? Section.IN.toString() : profile.getString(KEY_SECTION)),
            Gender.valueOf(profile.isNull(KEY_GENDER) ? Gender.Male.toString() : profile.getString(KEY_GENDER)),
            DBUtils.timestampMsToDate(profile.getLong(KEY_BIRTHDAY)),
            hobbies,
            profile.getString(KEY_DESCRIPTION),
            languages,
            GenderInterest.valueOf(profile.getString(KEY_GENDER_INTEREST)),
            ageInterest.getInt(KEY_AGE_START),
            ageInterest.getInt(KEY_AGE_END)
        );
    }

    /**
     * @return Return the set's values in a comma separated values string
     */
    public String getInterestsString() {
        if(getHobbies() == null) return null;

        String str = "";
        int idx = 0;
        for(String interest : getHobbies()) {
            str += interest;
            ++idx;
            if(idx < getHobbies().size())
                str += ", ";
        }
        return str;
    }

    /**
     * @return Return the set's values in a comma separated values string
     */
    public String getLanguagesString() {
        if(getLanguages() == null) return null;

        String str = "";
        int idx = 0;
        for(Language l : getLanguages()) {
            str += l.ordinal();
            ++idx;
            if(idx < getLanguages().size())
                str += ", ";
        }
        return str;
    }


    /**
     * Return the stringified value of a set of language (the real name of languages separated by commas)
     *
     * @param ctx Context of the activity from which we call the method
     * @param langs set of Language
     * @return Stringified set of Language
     */
    public String getLanguageSetToString(Context ctx, Set<Profile.Language> langs) {
        String langStr = "";
        if (langs == null) return ctx.getString(R.string.choose);

        for (Profile.Language l : langs) {
            langStr += l.toString() + ", ";
        }
        if (langStr.length() > 2)
            langStr = langStr.substring(0, langStr.length() - 2);
        else
            return ctx.getString(R.string.choose);

        return langStr;
    }


    // ---------------------- GETTERS & SETTERS ------------------------------

    /**
     * Return the User object associated to this profile
     * @param db DBHandler object, instance of the database connection
     * @return User who has this profile
     */
    public User getUser(DBHandler db) {
        return db.getUser(this.getUserId());
    }

    /**
     * @return ID (in the database) of the user owning this profile.
     */
    public long getUserId() { return userId;}

    /**
     * Set the ID of the user (same as in database)
     * Only used in the application and for retrieve information about users.
     * @param userId User ID
     */
    public void setUserId(long userId) { this.userId = userId; }

    /**
     * @return Section of the user
     */
    public Section getSection() {
        return section;
    }

    /**
     * Define the section of the user
     * @param section Section of the user
     */
    public void setSection(Section section) {
        this.section = section;
    }

    /**
     * @return Gender of the user
     */
    public Gender getGender() {
        return gender;
    }

    /**
     * Define the gender of the user
     * @param gender Gender of the user
     */
    public void setGender(Gender gender) {
        this.gender = gender;
    }

    /**
     * Define the gender of the user with the ordinal
     * of its corresponding value in the Gender enum
     * @param gender Ordinal value of the gender to define
     */
    public void setGender(int gender) { setGender(Gender.values()[gender]); }

    /**
     * @return Birth date of the user
     */
    public Date getBirthday() {
        return (Date) birthday.clone();
    }
    /**
     * @return Birth date in SQL format (YYYY-mm-dd)
     */
    public String getBirthdayAsString() { return DBUtils.DateToString(birthday); }
    /**
     * @return Friendly readable Birth date in format "10 December 1990"
     */
    public String getFormattedBirthday(Context ctx) {
        Settings settings = Settings.getInstance(ctx);
        Locale locale = settings.getLocale();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", locale);
        return dateFormat.format(this.birthday);
    }

    /**
     * Define the birth date of the user
     * @param birthday Birthday of the user in Date format
     */
    public void setBirthday(Date birthday) {
        this.birthday = new Date(birthday.getTime());
    }

    /**
     * Define the birth date of the user according to the given String in SQL format "YYYY-mm-dd"
     * @param str SQL date in format "YYYY-mm-dd"
     */
    public void setBirthday(String str) { setBirthday(DBUtils.StringToDate(str));}

    /**
     * @return Immutable collection of hobbies
     */
    public Set<String> getHobbies() {
        return Collections.unmodifiableSet(hobbies);
    }

    /**
     * Define the hobbies of the user according to the given set
     * @param hobbies Set of hobbies
     */
    public void setHobbies(Set<String> hobbies) {
        this.hobbies = new TreeSet<>(hobbies);
    }

    /**
     * Define the hobbies of the user according to the given String
     * in comma separated values format.
     * @param str Hobbies in comma separated values format
     */
    public void setHobbies(String str) { setHobbies(DBUtils.StringToSet(str)); }

    /**
     * @return Description given by the user
     */
    public String getDescription() {
        return description;
    }

    /**
     * Define the definition / free field the user give us
     * @param description Description given by the user
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return Immutable collection of languages chosen by the users
     */
    public Set<Language> getLanguages() {
        return Collections.unmodifiableSet(languages);
    }

    /**
     * Define the languages spoken by the user
     * @param languages Set of languages
     */
    public void setLanguages(Set<Language> languages) {
        this.languages = new TreeSet<>(languages);
    }

    /**
     * Define the languages spoken by the user, according to the given
     * String in comma separated values format (the ordinal values of the Language enum).
     * @param str Ordinal values of the Language enum in comma separated values
     */
    public void setLanguages(String str) { setLanguages(DBUtils.StringToLanguageSet(str)); }

    /**
     * @return Gender in which the user informed that he is interested
     */
    public GenderInterest getGenderInterest() {
        return genderInterest;
    }

    /**
     * Define the gender in which the user is interested
     * @param genderInterest Gender in which the user is interested
     */
    public void setGenderInterest(GenderInterest genderInterest) {
        this.genderInterest = genderInterest;
    }

    /**
     * Define the gender in which the user is interested, according to its
     * ordinal values in the GenderInterest enum.
     * @param sGender Ordinal value of the GenderInterest enum
     */
    public void setGenderInterest(int sGender) { setGenderInterest(GenderInterest.values()[sGender]); }

    /**
     * @return Min age the user is interested in
     */
    public int getAgeInterestStart() {
        return ageInterestStart;
    }

    /**
     * Define the min. age the user is interested in
     * @param ageInterestStart Min. age the user is interested in
     */
    public void setAgeInterestStart(int ageInterestStart) {
        this.ageInterestStart = (int)AndroidUtils.clamp(ageInterestStart, MIN_AGE, MAX_AGE);
    }

    /**
     * @return Max age the user is interested in
     */
    public int getAgeInterestEnd() {
        return ageInterestEnd;
    }

    /**
     * Define the max. age the user is interested in
     * @param ageInterestEnd Max. age the user is interested in
     */
    public void setAgeInterestEnd(int ageInterestEnd) {
        this.ageInterestEnd = (int)AndroidUtils.clamp(ageInterestEnd, MIN_AGE, MAX_AGE);
    }

    /**
     * @return Picture attached to this profile
     */
    public Bitmap getPhoto() {
        return photo;
    }

    /**
     * Attach the user's picture to his profile
     * @param photo Bitmap picture
     */
    public void setPhoto(Bitmap photo) {
        this.photo = photo;
    }


    @Override
    public String toString() {
        return "{\n"+
            KEY_ID + " : "+getUserId()+", "+"\n"+
            KEY_GENDER + " : "+getGender().toString()+", "+"\n"+
            KEY_BIRTHDAY + " : "+DBUtils.DateToString(getBirthday())+", "+"\n"+
            KEY_HOBBIES + " : "+getInterestsString()+", "+"\n"+
            KEY_DESCRIPTION + " : "+getDescription()+", "+"\n"+
            KEY_LANGUAGES + " : "+getLanguagesString()+", "+"\n"+
            KEY_GENDER_INTEREST + " : "+ getGenderInterest().toString()+", "+"\n"+
            KEY_AGE_START + " : "+ getAgeInterestStart()+", "+"\n"+
            KEY_AGE_END + " : "+ getAgeInterestEnd()+" "+"\n"+
        "}";
    }

}
