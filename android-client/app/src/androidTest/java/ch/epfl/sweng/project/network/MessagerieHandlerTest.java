package ch.epfl.sweng.project.network;

import org.json.JSONObject;
import org.junit.Test;

import java.util.Date;


import static junit.framework.Assert.assertTrue;


public class MessagerieHandlerTest {

    @Test
    public void test() {
        MessageHandler mess = MessageHandler.messagerie();
        MessageHandler mess2 = MessageHandler.messagerie();
        assertTrue(mess == mess2);
        mess.delete();
        MessageHandler mess3 = MessageHandler.messagerie();
        assertTrue(mess2 != mess3);

        MessageHandler.Conversation c1 = mess3.getConversationByIdOrCreate(20);
        MessageHandler.Conversation c2 = mess3.getConversationByIdOrCreate(20);
        assertTrue(c1 == c2);
        c1.sendMessage("Bonjour", new Date(), new NetworkResponseManager() {
            @Override
            public void onError(int errorCode) {

            }

            @Override
            public void onSuccess(JSONObject jsonObj) {

            }
        });
        c1.sendStatus(true, new NetworkResponseManager() {
            @Override
            public void onError(int errorCode) {

            }

            @Override
            public void onSuccess(JSONObject jsonObj) {

            }
        });

        assertTrue(true);
    }
}
