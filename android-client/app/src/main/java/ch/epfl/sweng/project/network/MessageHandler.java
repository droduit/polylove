package ch.epfl.sweng.project.network;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static ch.epfl.sweng.project.network.RequestHandler.ask;
import static ch.epfl.sweng.project.network.RequestHandler.send;

/**
 *@author Simon
 */

public final class MessageHandler {
    private static MessageHandler main = null;
    private static final String MESSAGE = "/message";
    private static final String MESSAGES = "/messages";
    private static final String MATCH = "/match";
    private static final String INTERESTED = "/interested";
    private static final String LOADMORE = "/loadMore";




    private Set<Conversation> allConv = new HashSet<>();


    private MessageHandler(){}

    /**
     *
     * @return Un singleton messagerie
     */
    public static MessageHandler messagerie(){
        if (main != null){
            return main;
        }
        main = new MessageHandler();
        return main;
    }

    void delete(){
        main = null;

    }


    private Conversation newConversation(long convId){

        Conversation newconv = new Conversation(convId);
        this.allConv.add(newconv);
        return newconv;
    }

    public Conversation getConversationByIdOrCreate(long id){
        for(Conversation c : this.allConv){
            if(c.getOtherId() == id){return c;}
        }
        return newConversation(id);
    }


    public class Conversation{

        private long convId;
        private Conversation(long otherId){
            this.convId = otherId;
        }

        public void loadMessages(Date date, NetworkResponseManager nrm){
            ask(new JSONObject(), MATCH+"/"+convId+MESSAGES+"/"+date.getTime(), nrm);
        }
/*
        public void sendMessage(String message, Date date){
            sendMessage(message, date, new NetworkResponseManager() {
                @Override
                public void onError(int errorCode) {}

                @Override
                public void onSuccess(JSONObject jsonObj) {}
            });
        }*/

        public void sendMessage(String message, Date date, NetworkResponseManager nrm){
            JSONObject jsonMessage = new JSONObject();

            try {
                jsonMessage.put("content", message);
                jsonMessage.put("sentAt", date.getTime());
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            send(jsonMessage, MATCH+"/"+this.convId+MESSAGE, nrm);
        }

        public void sendStatus(boolean confirmed,
                                      final NetworkResponseManager res){

            final JSONObject matchJSON = new JSONObject();
            try {
                matchJSON.put("interested", confirmed);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            send(matchJSON, MATCH+"/"+this.convId+INTERESTED, res);
        }

        long getOtherId(){
            return this.convId;
        }
    }


}
