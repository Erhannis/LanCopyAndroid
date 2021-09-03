package com.erhannis.lancopy;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.legacy.content.WakefulBroadcastReceiver;

public class DelayedStartServiceBroadcastReceiver extends WakefulBroadcastReceiver {
    private static final String TAG = "DelayedStartSBR";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive " + context + " " + intent);
        String className = intent.getStringExtra("className");
        try {
            startWakefulService(context,new Intent(context,Class.forName(className)) );
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            completeWakefulIntent(intent);
        }
    }
}