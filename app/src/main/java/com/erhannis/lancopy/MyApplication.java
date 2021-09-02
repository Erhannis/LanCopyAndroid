package com.erhannis.lancopy;

import android.content.Context;
import android.content.Intent;

import androidx.multidex.MultiDexApplication;

public class MyApplication extends MultiDexApplication {
    private static Context CONTEXT = null;

    @Override
    public void onCreate() {
        super.onCreate();
        CONTEXT = this.getApplicationContext();
        startService(new Intent(this, LanCopyService.class));
    }

    public static Context getContext() {
        return CONTEXT;
    }
}
