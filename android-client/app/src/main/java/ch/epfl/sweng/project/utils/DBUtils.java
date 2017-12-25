package ch.epfl.sweng.project.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import ch.epfl.sweng.project.models.Profile;

/**
 * Util methods used in the whole application for all
 * needs relative to the data from and for the database.
 *
 * @author Dominique Roduit
 */

public final class DBUtils {

    private static final SimpleDateFormat SQLDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
    private static final SimpleDateFormat SQLDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    private DBUtils() {}

    /**
     * Convert a timestamp in milliseconds into a Date
     * @param timestampMs Timestamp in milliseconds
     * @return Date corresponding to the given timestamp
     */
    public static Date timestampMsToDate(long timestampMs) {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestampMs);
        return calendar.getTime();
    }

    /**
     * Return a formatted date to match with SQL format yyyy-MM-dd
     * @param date Date in java.util.Date format
     * @return String date in SQL format
     */
    public static String DateToString(Date date) {
        return SQLDateFormat.format(getCopyDate(date));
    }

    /**
     * Transform a DateTime formatted date in a
     * String matching with SQL format
     * @param date Date containing date and hours
     * @return String datetime in SQL format
     */
    public static String DateTimeToString(Date date) {
        return SQLDateTimeFormat.format(getCopyDate(date));
    }

    /**
     * Perform a defensive copy of the given date
     * @param date Date to copy
     * @return copy of the given date or the actual
     *         date if the given parameter is null.
     */
    private static Date getCopyDate(Date date) {
        if(date == null) {
            return new Date();
        } else{
            return new Date(date.getTime());
        }
    }

    /**
     * Return a Date object from a String object
     * @param str String to convert in Date object
     * @return Date Object from a String object
     */
    public static Date StringToDate(String str) {
        return StringToDateFormat(str, SQLDateFormat);
    }

    /**
     * Return a Date object from a String object
     * @param str String to convert in Date object
     * @return Date Object from a String object
     */
    public static Date StringToDateTime(String str) {
        return StringToDateFormat(str, SQLDateTimeFormat);
    }

    /**
     * Parse a string and convert it in a Date with a given DateFormat format
     * @param str String to parse in date format
     * @param format Format of the date
     * @return Date object parsed from the given string
     */
    private static Date StringToDateFormat(String str, DateFormat format) {
        format.setLenient(false);

        Date datetime = new Date();
        if(str == null) {
            return datetime;
        } else {

            try {
                datetime = format.parse(str);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            return datetime;
        }
    }

    /**
     * Split a String with comma separated values into a Set of these values
     * @param str String to split
     * @return Set containing the values of the String
     */
    public static Set<String> StringToSet(String str) {
        Set<String> set = new TreeSet<>();

        if (str == null || str.isEmpty()) {
            return set;
        } else {
            for (String s : Arrays.asList(str.split(","))) {
                set.add(s.trim());
            }
            return set;
        }
    }

    /**
     * Split a string of values into a Set of Language (enum)
     * @param str String to split
     * @return Set containing the values of the String in Language enum values
     */
    public static Set<Profile.Language> StringToLanguageSet(String str) {
        Set<String> set = StringToSet(str);
        Set<Profile.Language> lSet = new TreeSet<>();

        for (String s : set) {
            lSet.add(Profile.Language.values()[Integer.parseInt(s.trim())]);
        }
        return lSet;
    }
}
