package ch.epfl.sweng.project.tequila;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Utilities for to HTTP requests.
 *
 * @author Solal Pirelli
 */
final class HttpUtils {
    static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("UTF-8 is not supported... all hope is lost.");
        }
    }
}