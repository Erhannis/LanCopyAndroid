package com.erhannis.lancopy;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.view.Display.DEFAULT_DISPLAY;
import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.erhannis.android.ekandroid.ui.Dialogs;
import com.erhannis.lancopy.refactor.Advertisement;
import com.erhannis.lancopy.refactor.Comm;
import com.erhannis.lancopy.refactor.LanCopyNet;
import com.erhannis.lancopy.refactor.Summary;
import com.erhannis.lancopy.ui.main.NodeLine;
import com.erhannis.mathnstuff.MeUtils;
import com.erhannis.mathnstuff.Pair;
import com.erhannis.mathnstuff.utils.NumberedConcurrentHashMap;
import com.erhannis.mathnstuff.utils.Observable;
import com.erhannis.mathnstuff.utils.Options;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import jcsp.helpers.JcspUtils;
import jcsp.lang.Alternative;
import jcsp.lang.AltingChannelInputInt;
import jcsp.lang.Any2OneChannelInt;
import jcsp.lang.Channel;
import jcsp.lang.ChannelOutput;
import jcsp.lang.ChannelOutputInt;
import jcsp.lang.Guard;
import jcsp.lang.ProcessManager;
import jcsp.util.ints.OverWriteOldestBufferInt;

public class LanCopyService extends Service {
    private static final String TAG = "LanCopyService";

    public class LocalBinder extends Binder {
        public final LanCopyNet.UiInterface uii;
        public final CopyOnWriteArrayList<NodeLine> nodeLines = new CopyOnWriteArrayList<>();
        //TODO Make better
        public final Observable<Object> nodeLinesChanged = new Observable<Object>(true);
        public final Observable<String> loadedText = new Observable<String>(false); //TODO Use
        public final NumberedConcurrentHashMap<ChannelOutput<Uri>> fileSaveResults = new NumberedConcurrentHashMap<>();

        public LocalBinder(LanCopyNet.UiInterface uii) {
            this.uii = uii;
        }
    }

    private static final String ACTION_DESTROY = "com.erhannis.lancopy.LanCopyService.DESTROY";
    private static final String ACTION_RESTART = "com.erhannis.lancopy.LanCopyService.RESTART";

    private NotificationManager mNM;
    private final LocalBinder mBinder;
    private ConcurrentHashMap<Comm, Boolean> commStatus = new ConcurrentHashMap<>();

    public LanCopyService() {
        LocalBinder binder0 = null;
        try {
            System.out.println("lancopy init");
            LanCopyNet.UiInterface[] uii0 = new LanCopyNet.UiInterface[1];
            Any2OneChannelInt showLocalFingerprintChannel = Channel.any2oneInt(new OverWriteOldestBufferInt(1));
            AltingChannelInputInt showLocalFingerprintIn = showLocalFingerprintChannel.in();

            ChannelOutputInt showLocalFingerprintOut = JcspUtils.logDeadlock(showLocalFingerprintChannel.out());
            File filesDir = MyApplication.getContext().getFilesDir();
            Options options = Options.demandOptions(new File(filesDir, "options.dat").getAbsolutePath());

            options.getOrDefault("Security.PROTOCOL", "TLSv1.2");
            options.getOrDefault("Security.KEYSTORE_PATH", new File(filesDir, "lancopy.ks").getAbsolutePath());
            options.getOrDefault("Security.TRUSTSTORE_PATH", new File(filesDir, "lancopy.ts").getAbsolutePath());
            options.getOrDefault("Comms.tcp.unauth_http.enabled", true);
            options.getOrDefault("Comms.tcp.unauth_http.show_confirmation", true);

            //DO Tell user be patient while generate keys

            DataOwner dataOwner = new DataOwner(options, showLocalFingerprintOut, (msg) -> {
                String localFingerprint = "UNKNOWN";
                LanCopyNet.UiInterface luii = uii0[0];
                if (luii != null) {
                    localFingerprint = luii.dataOwner.tlsContext.sha256Fingerprint;
                }
                msg = msg + "\n\n" + "Local fingerprint is\n" + localFingerprint;
                // Sorta hacky, sorry

                return Dialogs.confirmDialog(this, "Security error", msg);
            });
            LanCopyNet.UiInterface uii = LanCopyNet.startNet(dataOwner, showLocalFingerprintOut);
            uii0[0] = uii;
            binder0 = new LocalBinder(uii);
            /*
            this.cbLoopClipboard.setSelected((Boolean) dataOwner.options.getOrDefault("LOOP_CLIPBOARD", false));

            String savePath = (String) uii.dataOwner.options.getOrDefault("DEFAULT_SAVE_PATH", "");
            if (savePath != null && !savePath.trim().isEmpty()) {
                FilesData.fileChooser.setCurrentDirectory(new File(savePath.trim()));
            }

            String openPath = (String) dataOwner.options.getOrDefault("DEFAULT_OPEN_PATH", "");
            if (openPath != null && !openPath.trim().isEmpty()) {
                this.fileChooser.setCurrentDirectory(new File(openPath.trim()));
            }

            if (initialData != null) {
                setData(initialData);
            }

             */

            /*
            //TODO Deal with dialogs.  This is broken from here, fyi.
            ProgressDialog pd = new ProgressDialog(this, R.style.Theme_AppCompat_Dialog);
            pd.setTitle("Progress...");
            pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            pd.show();

             */

            LocalBinder binder00 = binder0;
            new ProcessManager(() -> {
                Alternative alt = new Alternative(new Guard[]{uii.adIn, uii.commStatusIn, uii.summaryIn, uii.confirmationServer, showLocalFingerprintIn});
                HashMap<UUID, Summary> summarys = new HashMap<>();
                List<Advertisement> roster = uii.rosterCall.call(null);
                for (Advertisement ad : roster) {
                    //TODO Creating a false Summary makes me uncomfortable
                    summarys.put(ad.id, new Summary(ad.id, ad.timestamp, "???"));
                }
                while (true) {
                    switch (alt.fairSelect()) {
                        case 0: // adIn
                        {
                            Advertisement ad = uii.adIn.read();
                            System.out.println("UI rx " + ad);
                            if (!summarys.containsKey(ad.id)) {
                                //TODO Creating a false Summary makes me uncomfortable
                                summarys.put(ad.id, new Summary(ad.id, ad.timestamp, "???"));
                            }
                            uii.dataOwner.errOnce("//TODO Update CommsFrames");
//                            Iterator<CommsFrame> cfi = commsFrames.iterator();
//                            while (cfi.hasNext()) {
//                                CommsFrame cf = cfi.next();
//                                if (cf.isDisplayable()) {
//                                    cf.update(ad);
//                                } else {
//                                    cfi.remove();
//                                }
//                            }
                            uii.subscribeOut.write(ad.comms);
                            break;
                        }
                        case 1: // commStatusIn
                        {
                            Pair<Comm, Boolean> status = uii.commStatusIn.read();
                            commStatus.put(status.a, status.b);
                            System.out.println("UI rx " + status);
                            uii.dataOwner.errOnce("//TODO Update CommsFrames");
//                            Iterator<CommsFrame> cfi = commsFrames.iterator();
//                            while (cfi.hasNext()) {
//                                CommsFrame cf = cfi.next();
//                                if (cf.isDisplayable()) {
//                                    cf.update(status);
//                                } else {
//                                    cfi.remove();
//                                }
//                            }
                            break;
                        }
                        case 2: // summaryIn
                        {
                            Summary summary = uii.summaryIn.read();
                            System.out.println("UI rx " + summary);
                            summarys.put(summary.id, summary);
                            break;
                        }
                        case 3: { // uii.confirmationServer
                            String msg = uii.confirmationServer.startRead();
                            boolean result = Dialogs.confirmDialog(this, "Confirmation", msg);
                            uii.confirmationServer.endRead(result);
                            break;
                        }
                        case 4: // showLocalFingerprintIn
                        {
                            showLocalFingerprintIn.read();
                            boolean show = (boolean) uii.dataOwner.options.getOrDefault("TLS.SHOW_LOCAL_FINGERPRINT", true);
                            if (show) {
                                Dialogs.notifyDialog(this, "Security: Local fingerprint", "An incoming connection has paused, presumably for fingerprint verification.\nThe local TLS fingerprint is:\n" + uii.dataOwner.tlsContext.sha256Fingerprint);
                            }
                            break;
                        }
                    }
                    //TODO Make efficient
                    final HashMap<UUID, Summary> scopy = new HashMap<>(summarys);

                    uii.dataOwner.errOnce("//TODO Update NodeList");
                    ArrayList<NodeLine> nodeLines = new ArrayList<>();
                    for (Map.Entry<UUID, Summary> entry : scopy.entrySet()) {
                        nodeLines.add(new NodeLine(entry.getValue()));
                    }
                    int sorting = (int) uii.dataOwner.options.getOrDefault("NodeList.SORT_BY_(TIMESTAMP|ID|SUMMARY)", 0);
                    switch (sorting) {
                        case 0: // Timestamp
                            Collections.sort(nodeLines, (o1, o2) -> -Long.compare(o1.summary.timestamp, o2.summary.timestamp));
                            break;
                        case 1: // Id
                            Collections.sort(nodeLines, (o1, o2) -> MeUtils.compare(o1.summary.id+"", o2.summary.id+""));
                            break;
                        case 2: // Summary
                            Collections.sort(nodeLines, (o1, o2) -> MeUtils.compare(o1.summary.summary, o2.summary.summary));
                            break;
                    }
                    binder00.nodeLines.clear();
                    binder00.nodeLines.addAll(nodeLines);
                    binder00.nodeLinesChanged.trigger();
                }
            }).start();

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
        Log.i(TAG, "Received start id " + startId + ": " + intent);
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_DESTROY:
                    die();
                    break;
                case ACTION_RESTART:
                    // https://stackoverflow.com/a/38750878/513038
                    AlarmManager am = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
                    Intent serviceIntent = new Intent(this, MainActivity.class);
                    PendingIntent pi = PendingIntent.getActivity(this, 7, serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+1000, pi);
                    die();
                    break;
            }
        }
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
        //TODO Close more cleanly than this
        Log.d(TAG, "System.exit(0)");
        System.exit(0);
    }

    /**
     * Show a notification while this service is running.
     */
    // https://android.googlesource.com/platform/development/+/master/samples/ApiDemos/src/com/example/android/apis/app/StatusBarNotifications.java
    // https://stackoverflow.com/a/47533338/513038
    private void showNotification() {
        String channelId;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createNotificationChannel("lancopy_net_service", "LanCopy Communication Service");
        } else {
            // If earlier version channel ID is not used
            // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
            channelId = "";
        }

        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.local_service_started);

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0); //TODO set

        mBinder.uii.dataOwner.errOnce("//TODO Set swipe-down action on notification");

        Intent destroyIntent = new Intent(this, LanCopyService.class);
        destroyIntent.setAction(ACTION_DESTROY);
        //destroyIntent.putExtra(EXTRA_NOTIFICATION_ID, 0);
        PendingIntent destroyPendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            destroyPendingIntent = PendingIntent.getForegroundService(this, 0, destroyIntent, 0);
        } else {
            destroyPendingIntent = PendingIntent.getService(this, 0, destroyIntent, 0);
        }

        Intent restartIntent = new Intent(this, LanCopyService.class);
        restartIntent.setAction(ACTION_RESTART);
        //restartIntent.putExtra(EXTRA_NOTIFICATION_ID, 0);
        PendingIntent restartPendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            restartPendingIntent = PendingIntent.getForegroundService(this, 0, restartIntent, 0);
        } else {
            restartPendingIntent = PendingIntent.getService(this, 0, restartIntent, 0);
        }

        // Set the info for the views that show in the notification panel.
        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setOngoing(true)
                .setSmallIcon(android.R.drawable.stat_sys_upload_done)  //TODO set
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle("LanCopy")  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .setCategory(Notification.CATEGORY_SERVICE)
                .addAction(android.R.drawable.ic_delete, "Exit", destroyPendingIntent)
                .addAction(android.R.drawable.ic_menu_rotate, "Restart", restartPendingIntent)
                .build();

        // Send the notification.
        startForeground(R.string.local_service_started, notification);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(String channelId, String channelName) {
        NotificationChannel chan = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE);
        //chan.setLightColor(Color.BLUE);
        //chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(chan);
        return channelId;
    }

    private void die() {
        mNM.cancel(R.string.local_service_started);
        stopForeground(true);
        stopSelf();
        // Tell the user we stopped.
        Toast.makeText(this, "LanCopy destroyed", Toast.LENGTH_SHORT).show();
        //TODO Close more cleanly than this
        Log.d(TAG, "LanCopyService terminating");
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}