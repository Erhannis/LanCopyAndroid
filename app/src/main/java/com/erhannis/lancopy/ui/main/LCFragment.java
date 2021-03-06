package com.erhannis.lancopy.ui.main;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.erhannis.lancopy.LanCopyService;

public abstract class LCFragment extends Fragment implements ServiceConnection {
    private static final String TAG = "LCFragment";

    public LanCopyService.LocalBinder lcs;

    public void onServiceConnected(ComponentName className, IBinder service) {
        Log.d(TAG, "onServiceConnected");
        lcs = (LanCopyService.LocalBinder)service;
        //toast("Connected to LanCopy service");
    }

    public void onServiceDisconnected(ComponentName className) {
        Log.d(TAG, "onServiceDisconnected");
        lcs = null;
        //toast("Disconnected from LanCopy service");
    }

    public boolean mIsBound = false;
    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIsBound = getActivity().bindService(new Intent(getActivity(), LanCopyService.class), this, Context.BIND_ABOVE_CLIENT | Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mIsBound) {
            getActivity().unbindService(this);
            mIsBound = false;
        }
    }

    protected void toast(final String msg) {
        getActivity().runOnUiThread(() -> {
            System.out.println("toast: " + msg);
            Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG).show();
        });
    }
}
