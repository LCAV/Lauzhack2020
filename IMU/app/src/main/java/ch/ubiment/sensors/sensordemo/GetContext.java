package ch.ubiment.sensors.sensordemo;

import android.app.Application;
import android.content.Context;

public class GetContext extends Application {
    private static Context context;

    public void onCreate() {
        super.onCreate();
        GetContext.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return GetContext.context;
    }
}
