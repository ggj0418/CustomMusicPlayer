package com.example.custommusicplayer.Audio;

import android.app.Application;

public class AudioApplication extends Application {
    private static AudioApplication mInstance;
    private AudioServiceInterface mInterface;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        mInterface = new AudioServiceInterface(getApplicationContext());
    }

    public static AudioApplication getInstance() {
        return mInstance;
    }

    public AudioServiceInterface getServiceInterface() {
        return mInterface;
    }
}
