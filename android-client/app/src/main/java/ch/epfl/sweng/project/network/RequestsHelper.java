package ch.epfl.sweng.project.network;


import org.json.JSONObject;

import static ch.epfl.sweng.project.network.RequestHandler.ask;
import static ch.epfl.sweng.project.network.RequestHandler.askImage;

/**
 * @author Simon
 */

public final class RequestsHelper {
    private final static String AVATAR = "/avatar";
    private final static String PROFILE = "/profile";
    private final static String PHOTO = "/picture";
    private final static String USER = "/user";
    private final static String MATCHES = "/matches";

    private RequestsHelper(){}

    public static void requestAvatar(long userId, NetworkResponseManager nrm){
        ask(new JSONObject(), USER+"/"+userId+AVATAR, nrm);
    }

    public static void requestProfile(long userId, NetworkResponseManager nrm){
        ask(new JSONObject(), USER+"/"+userId+PROFILE, nrm);
    }

    public static void requestPhoto(long userId, NetworkResponseManagerForImages nrm){
        askImage(USER+"/"+userId+PHOTO, nrm);
    }

    public static void requestPhoto(NetworkResponseManagerForImages nrm) {
        askImage(USER+PHOTO, nrm);
    }

    public static void requestUser(long userId, NetworkResponseManager nrm){
        ask(new JSONObject(), USER+"/"+userId, nrm);
    }

    public static void requestMatches(NetworkResponseManager nrm){
        ask(new JSONObject(), USER+MATCHES, nrm);
    }

}
