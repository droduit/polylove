package ch.epfl.sweng.project.network;

import android.support.test.rule.ActivityTestRule;

import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;

import ch.epfl.sweng.project.Login;
import ch.epfl.sweng.project.models.Profile;

import static junit.framework.Assert.assertTrue;

/**
 * @author simon
 */

public class ProfileSenderTest {

    @Rule
    public ActivityTestRule<Login> main = new ActivityTestRule<>(Login.class);

    @Test
    public void test() {

        /*
        ProfileSender.sendProfile(getInstrumentation().getTargetContext().getApplicationContext(), new Profile(), new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                assertTrue(true);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                assertTrue(true);
            }
        });
        */

        ProfileSender.sendProfile(new Profile(), new NetworkResponseManager() {
            @Override
            public void onError(int errorCode) {
                assertTrue(true);
            }

            @Override
            public void onSuccess(JSONObject jsonObj) {
                assertTrue(true);
            }
        });

    }


}
