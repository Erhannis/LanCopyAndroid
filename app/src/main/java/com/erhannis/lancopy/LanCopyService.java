package com.erhannis.lancopy;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.erhannis.lancopy.refactor.Advertisement;
import com.erhannis.lancopy.refactor.Comm;
import com.erhannis.lancopy.refactor.LanCopyNet;
import com.erhannis.lancopy.refactor.Summary;
import com.erhannis.lancopy.ui.main.NodeLine;
import com.erhannis.mathnstuff.MeUtils;
import com.erhannis.mathnstuff.Pair;
import com.erhannis.mathnstuff.utils.Observable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import jcsp.lang.Alternative;
import jcsp.lang.Guard;
import jcsp.lang.ProcessManager;

public class LanCopyService extends Service {
    private static final String TAG = "LanCopyService";

    public class LocalBinder extends Binder {
        public final LanCopyNet.UiInterface uii;
        public final CopyOnWriteArrayList<NodeLine> nodeLines = new CopyOnWriteArrayList<>();
        //TODO Make better
        public final Observable<Object> nodeLinesChanged = new Observable<Object>(true);

        public LocalBinder(LanCopyNet.UiInterface uii) {
            this.uii = uii;
        }
    }

    private NotificationManager mNM;
    private final LocalBinder mBinder;
    private ConcurrentHashMap<Comm, Boolean> commStatus = new ConcurrentHashMap<>();

    public LanCopyService() {
        LocalBinder binder0 = null;
        try {
            System.out.println("lancopy init");
            LanCopyNet.UiInterface uii = LanCopyNet.startNet();
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
                Alternative alt = new Alternative(new Guard[]{uii.adIn, uii.commStatusIn, uii.summaryIn});
                HashMap<String, Summary> summarys = new HashMap<>();
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
                    }
                    //TODO Make efficient
                    final HashMap<String, Summary> scopy = new HashMap<>(summarys);

                    uii.dataOwner.errOnce("//TODO Update NodeList");
                    ArrayList<NodeLine> nodeLines = new ArrayList<>();
                    for (Map.Entry<String, Summary> entry : scopy.entrySet()) {
                        nodeLines.add(new NodeLine(entry.getValue()));
                    }
                    int sorting = (int) uii.dataOwner.options.getOrDefault("NodeList.SORT_BY_(TIMESTAMP|ID|SUMMARY)", 0);
                    switch (sorting) {
                        case 0: // Timestamp
                            Collections.sort(nodeLines, (o1, o2) -> -Long.compare(o1.summary.timestamp, o2.summary.timestamp));
                            break;
                        case 1: // Id
                            Collections.sort(nodeLines, (o1, o2) -> MeUtils.compare(o1.summary.id, o2.summary.id));
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
        System.exit(0);
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