package ch.epfl.sweng.project.network;

import android.graphics.Bitmap;

import ch.epfl.sweng.project.models.Avatar;
import ch.epfl.sweng.project.models.Profile;

import static ch.epfl.sweng.project.network.RequestHandler.send;
import static ch.epfl.sweng.project.network.RequestHandler.sendImage;

/**
 * @author Simon Guilloud
 */

public final class ProfileSender {
    private final static String URLA = "/user/avatar";
    private final static String URLP = "/user/profile";
    private final static String URLI = "/user/picture";

    private ProfileSender(){    }

    public static void sendProfile(Profile profile, final NetworkResponseManager res){
        send(profile.toJson(), URLP, res);
        sendPhoto(profile.getPhoto(), res);
    }

    public static void sendAvatar(Avatar avatar, final NetworkResponseManager res){
        send(avatar.toJson(), URLA, res);
    }

    public static void sendPhoto(Bitmap bmp , final NetworkResponseManager res){
        sendImage(bmp, URLI, res);
    }

}
