package ch.epfl.sweng.project.network;

import android.content.Context;
import android.graphics.Bitmap;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

import ch.epfl.sweng.project.AppController;

import static com.android.volley.toolbox.Volley.newRequestQueue;

/**
 * @author Simon
 *
 */

public final class RequestHandler {
    private final static String URL = "http://polylove.zehkae.net/api";
    private static RequestQueue queue = null;
    private static CookieManager cookieManager =
            new CookieManager(new PersistantCookieStore(AppController.getAppContext()), CookiePolicy.ACCEPT_ALL);

    private RequestHandler(){}

    public static RequestQueue getQueue(Context context){
        if (queue != null ){
            return queue;
        }
        CookieHandler.setDefault(cookieManager);
        queue = newRequestQueue(context.getApplicationContext());
        return queue;
    }

    public static void send(JSONObject obj, String URL, final NetworkResponseManager response){
        queue.add(new JsonObjectRequest(Request.Method.POST, RequestHandler.URL +URL, obj,response, response));
    }

    static void sendImage(Bitmap bmp, String URL, final NetworkResponseManager response){
        queue.add(new ImageObjectRequest(Request.Method.POST, RequestHandler.URL + URL, bmp, response, response));
    }

    static void ask(JSONObject obj, String URL, final NetworkResponseManager response){
        queue.add(new JsonObjectRequest(Request.Method.GET, RequestHandler.URL +URL, obj, response, response));
    }

    static void askImage(String URL, final NetworkResponseManagerForImages response){
        queue.add(new ImageRequest(RequestHandler.URL +URL, response, 0, 0,android.widget.ImageView.ScaleType.CENTER, null, response));
    }



}
