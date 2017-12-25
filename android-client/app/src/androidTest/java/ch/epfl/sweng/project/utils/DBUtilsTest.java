package ch.epfl.sweng.project.utils;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import ch.epfl.sweng.project.models.Profile;

import static junit.framework.Assert.assertTrue;

/**
 * Test for the class DBUtils
 * @author Dominique Roduit
 */
@RunWith(AndroidJUnit4.class)
public class DBUtilsTest {
    private static final long TIMESTAMP_DIFF_TOLERANCE = 30*60;

    private Set<String> setString;
    private Set<Profile.Language> setLanguages;
    private Profile profile;
    private static Integer INTEGER_ONE = Integer.valueOf(1);

    private static final Calendar TEST_DATE_OF_BIRTH = Calendar.getInstance();

    static {
        TEST_DATE_OF_BIRTH.set(1993, 18, 11);
    }

    @Before
    public void setup() {
        setString = new HashSet<>();
        setString.add("Guitare");
        setString.add("Maths");

        setLanguages = new HashSet<>();
        setLanguages.add(Profile.Language.French);
        setLanguages.add(Profile.Language.English);
    }

    @Test
    public void testConstructorIsPrivate() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<DBUtils> constructor = DBUtils.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    public void dateToStringIsCorrect() throws Exception {
        Field field = DBUtils.class.getDeclaredField("SQLDateFormat");
        field.setAccessible(true);
        field.set(null, new SimpleDateFormat("yyyy-MM-dd"));

        long timestamp = 753577200000L;
        Date dateTime = DBUtils.StringToDate("1993-11-18");
        Date dateFromTimestamp = DBUtils.timestampMsToDate(timestamp);
        String dateStr = DBUtils.DateToString(dateTime);

        Date dateFromStr = DBUtils.StringToDate(dateStr);
        assertTrue("timestamp1 : "+dateTime.getTime()+ " - timestamp 2 : "+dateFromStr.getTime(),
                datesAreEquals(dateTime, dateFromStr));
        assertTrue(timestampsAreEquals(timestamp, dateFromTimestamp.getTime()));

        DBUtils.DateToString(null);
        DBUtils.StringToDate(null);
    }

    @Test
    public void dateTimeToStringIsCorrect() {
        long timestamp = 753643380000L;
        Date dateTime = DBUtils.StringToDateTime("1993-11-18 18:23:00");
        Date dateFromTimestamp = DBUtils.timestampMsToDate(timestamp);
        String dateStr = DBUtils.DateTimeToString(dateTime);

        Date dateFromStr = DBUtils.StringToDateTime(dateStr);
        assertTrue("timestamp1 : "+dateTime.getTime()+ " - timestamp 2 : "+dateFromStr.getTime(),
                datesAreEquals(dateTime, dateFromStr));
        assertTrue(timestampsAreEquals(timestamp, dateFromTimestamp.getTime()));

        DBUtils.DateTimeToString(null);
    }

    /*
    @Test(expected = ParseException.class)
    public void StringToDateThrowException() {
        DBUtils.StringToDate("salut les amis");
    }
    */

    @Test
    public void testStringToSet() {
       DBUtils.StringToSet(null);
    }

    /**
     * Compare two dates with a tolerance of difference between them
     */
    private boolean datesAreEquals(Date date1, Date date2) {
        long diff = Math.abs(date1.getTime() - date2.getTime());
        return diff < TIMESTAMP_DIFF_TOLERANCE;
    }

    /**
     * Compare two timestamps with a tolerance of difference between them
     */
    private boolean timestampsAreEquals(long timestamp1, long timestamp2) {
        long diff = Math.abs(timestamp1 - timestamp2);
        return diff < TIMESTAMP_DIFF_TOLERANCE;
    }

}
