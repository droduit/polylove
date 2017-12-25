package ch.epfl.sweng.project.network;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Singleton class whose purpose is to provide a tool to
 * verify easily if the user's phone has a network connection
 * to act accordingly.
 *
 * @author Dominique Roduit
 */
public final class NetworkStatus {
    private static NetworkStatus sInstance = null;
    private Context context;

    private NetworkStatus(Context ctx) {
        context = ctx.getApplicationContext();
    }

    /**
     * Create a new instance of an object who has the purpose of check the current
     * network status in a new Thread
     * @param ctx Context of the activity from where we get the instance
     * @return Single instance of a NetworkStatus object
     */
    public static synchronized NetworkStatus getInstance(Context ctx) {
        if(sInstance == null) {
            sInstance = new NetworkStatus(ctx);
        }
        return sInstance;
    }

    /**
     * @return Whether a connexion is enable or not (wifi or mobile data)
     */
    public boolean isConnected() {
        boolean connected = false;
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            connected = networkInfo != null
                     && networkInfo.isAvailable()
                     && networkInfo.isConnected();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return connected;
    }

}