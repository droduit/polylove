package ch.epfl.sweng.project.network.firebase;

import android.content.Context;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import org.json.JSONException;
import org.json.JSONObject;

import ch.epfl.sweng.project.network.NetworkResponseManager;

import static ch.epfl.sweng.project.network.RequestHandler.send;

/**
 * Receiver whose purpose is to receipt the user's Firebase token.
 *
 * @author Dominique Roduit
 */
public final class InstanceIDService extends FirebaseInstanceIdService {
    private static final String TAG = "FirebaseIDService";

    @Override
    public void onTokenRefresh() {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);
        sendRegistrationToServer(refreshedToken, getApplicationContext());
    }

    /**
     * Send the user's Firebase token to the server
     * @param token Firebase token
     * @param ctx Context of the activity
     */
    public static void sendRegistrationToServer(String token, Context ctx) {

        JSONObject obj = new JSONObject();
        try {
            obj.put("id", token);
        } catch (JSONException ignored) {}

        send(obj, "/user/firebaseId", new NetworkResponseManager() {
            @Override
            public void onSuccess(JSONObject response) {
                Log.d(TAG, "success - "+response.toString());
            }
            @Override
            public void onError(int errorCode) {
                Log.d(TAG, "error - "+errorCode);
            }
        });

    }
}
