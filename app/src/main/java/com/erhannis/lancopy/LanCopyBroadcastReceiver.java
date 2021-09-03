package com.erhannis.lancopy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class LanCopyBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "LanCopyBR";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive " + context + " " + intent);

        context.startActivity(new Intent(context, MainActivity.class));
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
    }
}