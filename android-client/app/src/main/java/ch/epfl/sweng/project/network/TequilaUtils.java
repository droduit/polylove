package ch.epfl.sweng.project.network;


import org.json.JSONException;
import org.json.JSONObject;

import static ch.epfl.sweng.project.network.RequestHandler.send;

/**
 * @author Simon
 */

public final class TequilaUtils {
    private static final String URL = "/login";
    private TequilaUtils(){}
/*
    public static String getAuthUrl(Context context, final Response.Listener<JSONObject> whenResponse, final Response.ErrorListener whenError){

        final JSONObject obj = new JSONObject();
        //final JSONObject jsonBody = new JSONObject(prof);
        RequestQueue queue = RequestHandler.getQueue(context);
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, URLAuth, obj,whenResponse, whenError) {
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                if (response != null && response.data.length == 0)
                    return Response.success(new JSONObject(), null);
                else
                    return super.parseNetworkResponse(response);
            }

        queue.add(request);
        return "";
    }*/

    public static void sendToken(String token, NetworkResponseManager nrm){

        final JSONObject obj = new JSONObject();
        try {
            obj.put("id", token);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        send(obj, URL, nrm);
    }
}
