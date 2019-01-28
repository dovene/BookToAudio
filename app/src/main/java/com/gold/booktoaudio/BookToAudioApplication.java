package com.gold.booktoaudio;

import android.app.Application;
import android.content.Context;

public class BookToAudioApplication extends Application {

    private static Context appContext;


    @Override
    public void onCreate() {
        super.onCreate();
        appContext = this.getApplicationContext();
    }

    public static Context getAppContext(){
        return appContext;
    }
}
