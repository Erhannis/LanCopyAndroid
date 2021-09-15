package com.erhannis.lancopy;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import com.erhannis.lancopy.ui.main.MainFragment;

public class MainActivity extends AppCompatActivity {
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            lcs = (LanCopyService.LocalBinder)service;
            //toast("Connected to LanCopy service");
            MainActivity.this.setTitle(lcs.uii.dataOwner.ID+"");
        }

        public void onServiceDisconnected(ComponentName className) {
            lcs = null;
            //toast("Disconnected from LanCopy service");
        }
    };

    public LanCopyService.LocalBinder lcs;
    public boolean mIsBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getSystemService(Context.AUDIO_SERVICE);
        setContentView(R.layout.main_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, MainFragment.newInstance())
                    .commitNow();
        }
        mIsBound = bindService(new Intent(this, LanCopyService.class), mConnection, Context.BIND_ABOVE_CLIENT | Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT);
    }
}