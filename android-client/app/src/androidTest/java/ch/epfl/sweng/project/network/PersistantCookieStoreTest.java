package ch.epfl.sweng.project.network;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.Test;

import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by Lucie
 */

public class PersistantCookieStoreTest {

    private Context context = InstrumentationRegistry.getContext();
    private final PersistantCookieStore PCS = new PersistantCookieStore(context);

    @Test
    public void addTest() {
        URI uri = null;
        try {
            uri = new URI("");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        HttpCookie cookie = new HttpCookie("cook", "ok");
        PCS.add(uri, cookie);
    }

    @Test
    public void getTest() {
        URI uri = null;
        try {
            uri = new URI("");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        HttpCookie cookie = new HttpCookie("cook", "ok");
        PCS.add(uri, cookie);

        PCS.get(uri);
    }

    @Test
    public void getCookiesTest() {
        URI uri = null;
        try {
            uri = new URI("aaa");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        HttpCookie cookie = new HttpCookie("cook", "ok");
        PCS.add(uri, cookie);

        PCS.getCookies();
    }

    @Test
    public void getURIsTest() {
        URI uri = null;
        try {
            uri = new URI("aaa");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        HttpCookie cookie = new HttpCookie("cook", "ok");
        PCS.add(uri, cookie);

        PCS.getURIs();
    }

    @Test
    public void removeTest() {
        URI uri = null;
        try {
            uri = new URI("aaa");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        HttpCookie cookie = new HttpCookie("cook", "ok");

        PCS.add(uri, cookie);
        PCS.remove(uri, cookie);
    }

    @Test
    public void removeAllTest() {
        URI uri = null;
        try {
            uri = new URI("aaa");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        HttpCookie cookie = new HttpCookie("cook", "ok");

        PCS.add(uri, cookie);
        PCS.removeAll();
    }

    @Test
    public void encodeCookieTest() {

        HttpCookie cookie = new HttpCookie("cook", "ok");
        PersistantCookieStore.PersistantCookie perCook = new PersistantCookieStore.PersistantCookie(cookie);

        PCS.encodeCookie(perCook);
    }

    @Test
    public void decodeCookieTest() {
        HttpCookie cookie = new HttpCookie("cook", "ok");
        PersistantCookieStore.PersistantCookie perCook = new PersistantCookieStore.PersistantCookie(cookie);

        String encode = PCS.encodeCookie(perCook);
        PCS.decodeCookie(encode);
    }

    @Test
    public void byteHexTest() {
        byte[] bytes = {0};
        PCS.byteArrayToHexString(bytes);
    }

    @Test
    public void HexByteTest() {
        String hex = "";
        PCS.hexStringToByteArray(hex);
    }
}
