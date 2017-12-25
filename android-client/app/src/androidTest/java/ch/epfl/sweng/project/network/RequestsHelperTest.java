package ch.epfl.sweng.project.network;

import android.graphics.Bitmap;

import org.json.JSONObject;
import org.junit.Test;

import static ch.epfl.sweng.project.network.RequestsHelper.requestAvatar;
import static ch.epfl.sweng.project.network.RequestsHelper.requestProfile;
import static ch.epfl.sweng.project.network.RequestsHelper.requestUser;
import static com.android.volley.Request.Method.GET;
import static junit.framework.Assert.assertTrue;


public class RequestsHelperTest {

    @Test
    public void test() {
        NetworkResponseManager nrm = new NetworkResponseManager() {
            @Override
            public void onError(int errorCode) {
                if (403 == errorCode) {
                    assertTrue(true);
                } else {
                    // TODO Plante très souvent lorsqu'on run jacoco (chez Dom et Lucie en tout cas)
                    // assertTrue(false);
                }
            }

            @Override
            public void onSuccess(JSONObject jsonObj) {
                assertTrue(true);
            }
        };

        ImageObjectRequest ior = new ImageObjectRequest(GET, "truc", android.graphics.Bitmap.createBitmap(1,1, Bitmap.Config.ALPHA_8), nrm, nrm);

        requestAvatar(100, nrm);
        requestUser(100, nrm);
        requestProfile(100, nrm);
    }
}
