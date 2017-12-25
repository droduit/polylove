package ch.epfl.sweng.project.network;

import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;

/**
 * @author Simon
 */

abstract class NetworkResponseManagerGeneral<T> implements Response.ErrorListener, Response.Listener<T> {

    @Override
    public void onResponse(T response) {
        onSuccess(response);
    }

    @Override
    public void onErrorResponse(VolleyError error){
        Log.e("STATE", error.toString());

        NetworkResponse response = error.networkResponse;

        if (response != null) {
            Log.e("STATE", new String(response.data));
            onError(response.statusCode);
        }
    }


    public abstract  void onError(int errorCode);

    public abstract void onSuccess(T jsonObj);

}