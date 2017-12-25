package ch.epfl.sweng.project.models.db;

import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ch.epfl.sweng.project.R;
import ch.epfl.sweng.project.models.Avatar;
import ch.epfl.sweng.project.models.Match;
import ch.epfl.sweng.project.models.Message;
import ch.epfl.sweng.project.models.Profile;
import ch.epfl.sweng.project.models.User;
import ch.epfl.sweng.project.utils.AndroidUtils;
import ch.epfl.sweng.project.utils.DBUtils;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

/**
 * Test class for the DBHandler : database handling provider.
 * @author Dominique Roduit
 */
@RunWith(AndroidJUnit4.class)
public class DBHandlerTest {

    private final long USER_ID_1 = 1L;
    private final long MATCH_ID = 1L;

    private static DBHandler db;
    private User usr1, usr2;
    private Profile profile1;
    private Avatar avatar1;
    private List<Message> msg;

    @Before
    public void setUp() throws Exception {
        getTargetContext().deleteDatabase(DBHandler.DATABASE_NAME);
        db = DBHandler.getInstance(getTargetContext());

        usr1 = new User(USER_ID_1, "dominique.roduit@epfl.ch", "Dominique Jean", "Roduit");
        usr2 = new User(2, "tim.nguyen@epfl.ch", "Tim", "Nguyen");

        profile1 = new Profile(1, Profile.Section.IN, Profile.Gender.Male, "1993-11-18",
           "Guitare, Math, Sweng", "Je suis un lutin", "0,1",
            Profile.GenderInterest.Female, 18, 25);


        long AVATAR1_ID = 1L;
        avatar1 = new Avatar(AVATAR1_ID, Profile.Gender.Male, Avatar.Eye.Green, Avatar.HairColor.Blond, Avatar.HairStyle.Style1, Avatar.Skin.Light, Avatar.Shirt.Style1);


         msg = new ArrayList<>();
        for(int i = 0; i < 100; ++i) {
            msg.add(new Message(i, usr1.getId(), new Date(new Date().getTime() + i*60*60), 1, "Mon message "+i, i%2 == 0, Message.Status.SENT));
        }
    }

    @After
    public void tearDown() throws Exception {
        if(db != null) db.close();
    }

    @Test
    public void testOpeningDb() throws Exception {
        assertNotNull(db);
        SQLiteDatabase sqliteDb = db.getWritableDatabase();
        assertNotNull(sqliteDb);
    }

    @Test
    public void UserInsertAndFetch() {
        long newUsrId1 = db.storeUser(usr1);
        User usr1Fetched = db.getUser(newUsrId1);
        if(usr1Fetched != null) {
            assertEquals(usr1Fetched.getFullName(), "Dominique Jean Roduit");
        }

        long newUsrId2 = db.storeUser(usr2);
        User usr2Fetched = db.getUser(newUsrId2);
        if(usr2Fetched != null) {
            assertEquals(usr2Fetched.getFullName(), "Tim Nguyen");
        }
    }

    @Test
    public void UserUpdate() {
        db.storeUser(usr1);
        usr1.setFirstName("Lutin");
        long newUsrId4 = db.storeUser(usr1);
        assertEquals(newUsrId4, usr1.getId());
    }

    @Test
    public void ProfileInsertAndFetch() {
        db.storeProfile(profile1);
        Profile fetchP1 = db.getProfile(profile1.getUserId());

        if(fetchP1 != null) {
            assertEquals(fetchP1.getBirthdayAsString(), profile1.getBirthdayAsString());
            assertEquals(fetchP1.getDescription(), profile1.getDescription());
            assertEquals(fetchP1.getGender(), profile1.getGender());
            assertEquals(fetchP1.getAgeInterestStart(), profile1.getAgeInterestStart());
            assertEquals(fetchP1.getAgeInterestEnd(), profile1.getAgeInterestEnd());
            assertEquals(fetchP1.getGenderInterest(), profile1.getGenderInterest());
            assertEquals(fetchP1.getUserId(), profile1.getUserId());
            assertEquals(fetchP1.getUserId(), usr1.getId());
        }
    }

    @Test
    public void ProfileUpdate() {
        db.storeProfile(profile1);
        profile1.setDescription("je suis un gentil garconnet");
        db.storeProfile(profile1);
        Profile updProfile = db.getProfile(profile1.getUserId());
        if(updProfile != null) {
            assertEquals(profile1.getDescription(), updProfile.getDescription());
        }
    }

    @Test
    public void AvatarInsertAndFetch() {
        db.storeAvatar(avatar1);
        Avatar fetchA1 = db.getAvatar(avatar1.getUserId());

        if(fetchA1 != null) {
            assertEquals(fetchA1.getUserId(), usr1.getId());
            assertEquals(fetchA1.toString(), avatar1.toString());
        }
    }

    @Test
    public void avatarUpdate() {
        db.storeAvatar(avatar1);
        avatar1.setEyeColor(Avatar.Eye.Brown);
        db.storeAvatar(avatar1);
        Avatar updAvatar = db.getAvatar(avatar1.getUserId());
        if(updAvatar != null) {
            assertEquals(updAvatar.getEyeColor(), avatar1.getEyeColor());
        }
    }

    @Test
    public void profilCanRetrieveUser() {
        long newUsrId1 = db.storeUser(usr1);
        usr1.setId(newUsrId1);

        User user1 = profile1.getUser(db);
        assertEquals(user1.getId(), usr1.getId());
        assertEquals(user1.getEmail(), usr1.getEmail());
        assertEquals(user1.getFullName(), usr1.getFullName());
        assertEquals(user1.getEmail(), usr1.getEmail());
    }

    @Test
    public void onUpgrade() {
        db.onUpgrade(db.getWritableDatabase(), DBHandler.DATABASE_VERSION, DBHandler.DATABASE_VERSION+1);
        long id = db.storeUser(usr1);
        assertEquals(id, usr1.getId());
    }

    @Test
    public void nullObjectsStored() {
        assertEquals(-1, db.storeUser(null));
        assertEquals(-1, db.storeProfile(null));
        assertEquals(-1, db.storeAvatar(null));
    }

    @Test
    public void inexistantEntityFetched() {
        assertNull(db.getUser(-1));
        assertNull(db.getProfile(-1));
        assertNull(db.getAvatar(-1));
    }

    @Test
    public void avatarCanRetrieveUser() {
        long newUsrId1 = db.storeUser(usr1);
        usr1.setId(newUsrId1);

        User user1 = avatar1.getUser(db);
        assertEquals(user1.getId(), usr1.getId());
        assertEquals(user1.getEmail(), usr1.getEmail());
        assertEquals(user1.getFullName(), usr1.getFullName());
        assertEquals(user1.getEmail(), usr1.getEmail());
    }


    @Test
    public void getConversationsTest2() {
        Match match = new Match(1, 2, Match.State.Open, DBUtils.StringToDateTime("2016-10-26 18:12:15"));
        db.storeMatch(match);

        Match match2 = new Match(2, 3, Match.State.Open, DBUtils.StringToDateTime("2016-12-22 15:10:17"));
        db.storeMatch(match2);

        Match match3 = new Match(3, 2, Match.State.Close, DBUtils.StringToDateTime("2016-11-18 12:12:00"));
        db.storeMatch(match3);

        List<Match> matchs = db.getConversations();
        assertEquals(3, matchs.size());
    }

    @Test
    public void storeMessage() {

        int MESSAGE_NUMBER = 60;
        for(int i = 0; i < MESSAGE_NUMBER; ++i) {
            db.storeMessage(msg.get(i));
        }

        List<Message> msgList = db.getMessagesForMatch(MATCH_ID);
        assertEquals(59, msgList.size());

        assertEquals(-1, db.storeMessage(null));
    }

    @Test
    public void deleteMessage() {
        long id1 = db.storeMessage(msg.get(1));
        db.storeMessage(msg.get(2));
        db.storeMessage(msg.get(3));
        List<Message> listMsg = db.getMessagesForMatch(MATCH_ID);
        assertEquals(3, listMsg.size());

        db.deleteMessage(id1);
        List<Message> listMsgAfterDeletion = db.getMessagesForMatch(MATCH_ID);
        assertEquals(2, listMsgAfterDeletion.size());
    }

    @Test
    public void getLastMessageFromMatch() {
        for(int i = 0; i < 5; ++i) {
            Message m = msg.get(i);
            db.storeMessage(m);
        }
        Message lastMsg = db.getLastMessageFromMatch(MATCH_ID);
        if(lastMsg != null) {
            assertEquals("Mon message 4", lastMsg.getContent());
        }

        Message msgNull = db.getLastMessageFromMatch(-1);
        assertNull(msgNull);
    }

     @Test
    public void cleanTableTest() {
         long newAvatarId = db.storeAvatar(avatar1);
         db.cleanTables();
         assertNull(db.getProfile(newAvatarId));
    }

    @Test
    public void storeProfileWithPhoto() {
        Bitmap bitmap = BitmapFactory.decodeResource(getTargetContext().getResources(), R.drawable.boy_hair_style1_blond);
        profile1.setPhoto(bitmap);
        db.storeProfile(profile1);
        Profile newProfile = db.getProfile(profile1.getUserId());
        if(newProfile != null) {
            assertEquals(newProfile.getPhoto().getHeight(), bitmap.getHeight());
        }
    }

    @Test
    public void storeAvatarWithImageAndIcon() {
        Bitmap bitmap1 = BitmapFactory.decodeResource(getTargetContext().getResources(), R.drawable.girl_eyes_green);
        Bitmap bitmap2 = BitmapFactory.decodeResource(getTargetContext().getResources(), R.drawable.bubble1);
        avatar1.setImage(bitmap1);
        avatar1.setIcon(bitmap2);
        db.storeAvatar(avatar1);
        Avatar newAvatar = db.getAvatar(avatar1.getUserId());
        if(newAvatar != null) {
            assertEquals(newAvatar.getImage().getHeight(), bitmap1.getHeight());
            assertEquals(newAvatar.getIcon().getHeight(), bitmap2.getHeight());
        }
    }

    @Test
    public void getMatchWithUserTest() {
        db.storeMatch(new Match(-1, USER_ID_1, Match.State.Open, new Date()));
        Match matchWithUser1 = db.getMatchWithUser(USER_ID_1);
        Match nullMatch = db.getMatchWithUser(-1);

        if(matchWithUser1 != null) {
            assertEquals(USER_ID_1, matchWithUser1.getPartnerId());
        }
        assertNull(nullMatch);

        Match nullMatch2 = db.getMatchWithUser(5);
        assertNull(nullMatch2);
    }

    @Test
    public void getMatchOfTodayTest() {
        Match noMatchOfToday = db.getMatchOfToday();
        assertNull(noMatchOfToday);

        db.storeMatch(new Match(1, USER_ID_1, Match.State.Pending, new Date()));
        Match matchOfToday = db.getMatchOfToday();
        assertEquals(USER_ID_1, matchOfToday.getPartnerId());
    }

    @Test
    public void existsMatchWithUserTest() {
        assertFalse(db.existsMatchWithUser(-1));
        db.storeMatch(new Match(-1, USER_ID_1, Match.State.Open, new Date()));
        assertTrue(db.existsMatchWithUser(USER_ID_1));
    }

    @Test
    public void storeMatchTest() {
        assertEquals(-1, db.storeMatch(null));

        Match match = new Match(5, USER_ID_1, Match.State.Open, new Date());
        db.storeMatch(match);
        assertEquals(Match.State.Open.ordinal(), match.getState().ordinal());
        match.setState(Match.State.Close);
        db.storeMatch(match);
        assertEquals(Match.State.Close.ordinal(), match.getState().ordinal());
    }

    @Test
    public void testMatchState() {
        Match match = new Match(1L, 2L, Match.State.Open, new Date());
        long insertedRow = db.storeMatch(match);
        long affectedRow = match.setState(getTargetContext(), Match.State.Close);
        assertEquals(insertedRow, affectedRow);
    }

    @Test
    public void testDeleteMatch() {
        db.storeMatch(new Match(1L, 2L, Match.State.Close, new Date()));
        assertNotNull(db.getMatch(1L));
        db.deleteMatch(1L);
        assertNull(db.getMatch(1L));
    }

    @Test
    public void testUpdateMatch() {
        Match match = new Match(1L, 2L, Match.State.Close, new Date());
        db.storeMatch(match);
        db.updateMatch(match.getId(), Match.State.Open);
        Match storedMatch = db.getMatch(match.getId());
        if(storedMatch != null) {
            assertEquals(Match.State.Open.ordinal(), storedMatch.getState().ordinal());
        }
        assertFalse(db.updateMatch(5000L, Match.State.Confirmed));
    }

    @Test
    public void testGetMessage() {
        Message message = new Message(1L, 2L, new Date(), 3L, "Salut", true, Message.Status.SENT);
        db.storeMessage(message);
        Message msgRetrieve = db.getMessage(message.getMsgId());
        if(msgRetrieve != null) {
            assertEquals("Salut", msgRetrieve.getContent());
        }

        assertNull(db.getMessage(-5000L));
    }

    @Test
    public void updateMessageStatusTest() {
        Message message = new Message(1L, 2L, new Date(), 3L, "Salut", true, Message.Status.SENT);
        db.storeMessage(message);
        Message msgRetrieve = db.getMessage(message.getMsgId());
        if(msgRetrieve != null) {
            db.updateMessageStatus(msgRetrieve.getMsgId(), Message.Status.WAIT_SENDING);
        }
        msgRetrieve = db.getMessage(message.getMsgId());
        if(msgRetrieve != null) {
            assertEquals(Message.Status.WAIT_SENDING.ordinal(), msgRetrieve.getStatus().ordinal());
        }
        long ret = db.updateMessageStatus(-500L, Message.Status.SENT);
        assertEquals(-1, ret);
    }


    @Test
    public void storeMessageIfNotExists() {
        assertEquals(-1, db.storeMessageIfNotExists(null));
        Message message = new Message(1L, 2L, new Date(), 3L, "Salut", true, Message.Status.SENT);
        assertEquals(1, db.storeMessageIfNotExists(message));
        assertEquals(-1, db.storeMessageIfNotExists(message));
    }

    @Test
    public void MatchGetLastMessageTest() {
        Match match = new Match(5L, 1L, Match.State.Open, new Date());
        Message message = match.getLastMessage(getTargetContext());
        assertNull(message);

        Message msgToStore = new Message(-1, 1L, new Date(), 5L, "salut", true, Message.Status.SENT);
        db.storeMessage(msgToStore);
        message = match.getLastMessage(getTargetContext());
        if(message != null) {
            assertEquals("salut", message.getContent());
        }

    }

    @Test
    public void MatchGetFriendlyUsername() {
        Match match = new Match(1L, 1L, Match.State.Close, new Date());
        assertEquals(match.getState().toString() + " - " + AndroidUtils.getFormattedTime(match.getTime()), match.getFriendlyUsername(getTargetContext()));

        match.setState(Match.State.Pending);
        assertEquals(getTargetContext().getString(R.string.todays_match), match.getFriendlyUsername(getTargetContext()));

        match.setState(Match.State.Open);
        User user = new User(1L, "dominique.roduit@epfl.ch", "Dominique", "Roduit");
        db.storeUser(user);
        assertEquals(user.getFullName(), match.getFriendlyUsername(getTargetContext()));
    }

}
