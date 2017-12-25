package ch.epfl.sweng.project.models;

import android.content.Context;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Calendar;
import java.util.Date;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

/**
 * Tests for the class Match
 * @author Dominique Roduit
 */
@RunWith(AndroidJUnit4.class)
public class MatchTest {
    private long MATCHID = 1;
    private long USERID = 1;
    private Match.State STATE_OPEN = Match.State.Open;
    private Date date = new Date();
    private Match match;
    private Context context;
    private JSONObject matchJSON = new JSONObject();

    @Before
    public void setUp() {
        context = getTargetContext();
        match = new Match(MATCHID, USERID, STATE_OPEN, date);

        try {
            matchJSON.put(Match.KEY_MATCH_ID, 1L);
            matchJSON.put(Match.KEY_USER1_ID, 1L);
            matchJSON.put(Match.KEY_USER2_ID, 2L);
            matchJSON.put(Match.KEY_STATE, Match.State.Open.toString());
            matchJSON.put(Match.KEY_CREATION_TIME, 1481123216L);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testValues() {
        assertEquals(match.getId(), MATCHID);
        assertEquals(match.getPartnerId(), USERID);
        assertEquals(match.getState().ordinal(), STATE_OPEN.ordinal());
        assertEquals(date, match.getTime());
    }

    @Test
    public void testFromJSON() {
        try {
            Match match = Match.fromJSON(matchJSON, context);
            assertEquals(match.getId(), 1L);

            matchJSON.remove(Match.KEY_MATCH_ID);
            match = Match.fromJSON(matchJSON, context);
            assertNull(match);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testGetMillisecondsLeft() {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        long now = c.getTimeInMillis();
        long endToday = match.getEndDateInMillis();
        long oneDayInMillis = 24 * 60 * 60 * 1000;
        long endTomorrow = endToday + oneDayInMillis;

        long end = endToday;

        if(now > endToday) {
            end = endTomorrow;
        }

        long millisecondsLeft = end - now;
        boolean result = (Math.abs(millisecondsLeft-match.getMillisecondsLeft())) < 60;
        assertTrue(result);

    }

    @Test
    public void testGetEndDateInMillis() {
        Calendar c = Calendar.getInstance();
        long today = (long) (Math.floor(c.getTimeInMillis() / (24 * 60 * 60 * 1000)) * 24 * 60 * 60 * 1000);
        assertEquals(today + (21-1) * 60 * 60 * 1000, match.getEndDateInMillis());
    }

}
