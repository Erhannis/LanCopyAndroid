package com.erhannis.lancopy;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.erhannis.lancopy.refactor.LanCopyNet;

public class LanCopyService extends Service {
    public class LocalBinder extends Binder {
        public final LanCopyNet.UiInterface uii;

        public LocalBinder(LanCopyNet.UiInterface uii) {
            this.uii = uii;
        }
    }

    private NotificationManager mNM;
    private final LocalBinder mBinder;

    public LanCopyService() {
        LocalBinder binder0 = null;
        try {
            System.out.println("lancopy init");
            LanCopyNet.UiInterface uii = LanCopyNet.startNet();
            binder0 = new LocalBinder(uii);
            System.out.println("lancopy load");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        mBinder = binder0;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LanCopyService", "Received start id " + startId + ": " + intent);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        // Display a notification about us starting.  We put an icon in the status bar.
        showNotification();
        Toast.makeText(this, "LanCopy created", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        mNM.cancel(R.string.local_service_started);

        // Tell the user we stopped.
        Toast.makeText(this, "LanCopy destroyed", Toast.LENGTH_SHORT).show();
    }

    /**
     * Show a notification while this service is running.
     */
    // https://android.googlesource.com/platform/development/+/master/samples/ApiDemos/src/com/example/android/apis/app/StatusBarNotifications.java
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.local_service_started);

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0); //TODO set

        mBinder.uii.dataOwner.errOnce("//TODO Set swipe-down action on notification");

        // Set the info for the views that show in the notification panel.
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(android.R.drawable.stat_sys_upload_done)  //TODO set
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle("LanCopy")  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .build();

        // Send the notification.
        startForeground(R.string.local_service_started, notification);
    }
}