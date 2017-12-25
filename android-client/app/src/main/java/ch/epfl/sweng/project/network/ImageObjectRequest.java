package ch.epfl.sweng.project.network;

import android.graphics.Bitmap;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

/**
 * Class to send a bitmap image to the server.
 * The byte of the bitmap are send in JPEG format in the body of the request.
 */
public class ImageObjectRequest extends Request<JSONObject> {
    private final Bitmap bmp;
    private final Response.Listener<JSONObject> successListener;

    public ImageObjectRequest(int method, String URL, Bitmap bmp, Response.Listener<JSONObject> suc, Response.ErrorListener err){
        super(method, URL, err);
        this.bmp = bmp;
        this.successListener = suc;
    }

    @Override
    public byte[] getBody() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        if(bmp !=null) {
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        }
        return stream.toByteArray();
    }
    //intestable
    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers));
            return Response.success(new JSONObject(jsonString),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException je) {
            return Response.error(new ParseError(je));
        }
    }

    @Override
    public String getBodyContentType()
    {
        return "image/jpeg";
    }

    @Override
    protected void deliverResponse(JSONObject response) {
        this.successListener.onResponse(response);
    }
}
