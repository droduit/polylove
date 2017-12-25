package ch.epfl.sweng.project;

import android.app.Application;
import android.content.Context;

import ch.epfl.sweng.project.network.RequestHandler;

/**
 * Created by Simon on 13.11.2016.
 */

public class AppController extends Application {
    private static Context globalContext;
    @Override
    public void onCreate(){
        super.onCreate();
        globalContext = getApplicationContext();
        RequestHandler.getQueue(this);
    }

    public static Context getAppContext() {
        return globalContext;
    }
}
