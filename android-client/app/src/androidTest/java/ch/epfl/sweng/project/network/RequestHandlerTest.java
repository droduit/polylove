package ch.epfl.sweng.project.network;


import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;

import com.android.volley.RequestQueue;

import org.junit.Rule;
import org.junit.Test;

import ch.epfl.sweng.project.Login;
import static org.junit.Assert.assertEquals;

public class RequestHandlerTest {

    @Rule
    public ActivityTestRule<Login> main = new ActivityTestRule<>(Login.class);

    /*
    @Test
    public void messageTest() throws Exception {
        String k = reverseMessage("Bonjour");
        assertThat(k, is("ruojnoB"));
    }
    */

    // TODO: More NullPointerException
    @Test
    public void queueTest() {
        RequestQueue q = RequestHandler.getQueue(InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext());
        assertEquals(q, RequestHandler.getQueue(InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext()));
    }


}


