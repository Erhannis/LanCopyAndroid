package com.erhannis.lancopy;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.multidex.MultiDexApplication;

import com.erhannis.lancopy.data.FilesData;

import java.io.File;
import java.util.function.Consumer;

import static com.erhannis.mathnstuff.MeUtils.orNull;

public class MyApplication extends MultiDexApplication {
    private static Context CONTEXT = null;

    @Override
    public void onCreate() {
        super.onCreate();
        CONTEXT = this.getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, LanCopyService.class));
        } else {
            startService(new Intent(this, LanCopyService.class));
        }
    }

    public static Context getContext() {
        return CONTEXT;
    }

    public static void runOnUiThread(final Runnable r) {
        new Handler(Looper.getMainLooper()).post(() -> {
            r.run();
        });
    }

    /**
     * See {@link #getApkName(Context)}
     * @return
     * @throws PackageManager.NameNotFoundException
     */
    public static String getApkName() throws PackageManager.NameNotFoundException {
        return getApkName(CONTEXT);
    }

    /**
     * Get the apk path of this application.
     *
     * https://stackoverflow.com/a/31535681/513038
     *
     * @param context any context (e.g. an Activity or a Service)
     * @return full apk file path, or null if an exception happened (it should not happen)
     */
    public static String getApkName(Context context) throws PackageManager.NameNotFoundException {
        String packageName = context.getPackageName();
        PackageManager pm = context.getPackageManager();
        ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
        String apk = ai.publicSourceDir;
        return apk;
    }

    public static void inputDialog(Activity act, String title, String def, Consumer<String> yesHandler, Runnable cancelHandler) {
        // https://stackoverflow.com/a/10904665/513038
        AlertDialog.Builder builder = new AlertDialog.Builder(act);
        builder.setTitle(title);

        final EditText input = new EditText(act);
        input.setText(def);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                yesHandler.accept(input.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                cancelHandler.run();
            }
        });

        builder.show();
    }
}
