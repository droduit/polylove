package ch.epfl.sweng.project.tequila;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;


public final class AuthenticationProcess {
    private static String[] scopes = {"Tequila.profile"};

    public static void connect(final Context ctx) {
        String clientId = "40deb8e6823e767d6755ecb4@epfl.ch";
        String redirectUri = "polylove://login";

        String URL = "https://tequila.epfl.ch/cgi-bin/OAuth2IdP/auth" +
                "?response_type=code" +
                "&client_id=" + HttpUtils.urlEncode(clientId) +
                "&redirect_uri=" + HttpUtils.urlEncode(redirectUri) +
                "&scope=" + TextUtils.join(",", scopes);
        showAuthPage(URL, ctx);
    }

    private static void showAuthPage(final String url, Context ctx) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        ctx.startActivity(browserIntent);
    }





}