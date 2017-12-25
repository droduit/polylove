package ch.epfl.sweng.project.tequila;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.Test;

/**
 * Created by Lucie
 */

public class AuthTest {
    private Context context = InstrumentationRegistry.getContext();

    @Test
    public void connectTest() {
        AuthenticationProcess.connect(context);
    }

    @Test
    public void solalTest(){
        HttpUtils.urlEncode("aaa");
    }
}
