package com.erhannis.lancopy;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.multidex.MultiDexApplication;

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
}
