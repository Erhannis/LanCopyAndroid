package com.erhannis.lancopy;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;

import com.erhannis.lancopy.ui.main.MainFragment;
import com.erhannis.lancopy.ui.main.OptionsActivity;
import com.erhannis.lancopy.ui.main.QRActivity;

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
        setContentView(R.layout.main_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, MainFragment.newInstance())
                    .commitNow();
        }
        mIsBound = bindService(new Intent(this, LanCopyService.class), mConnection, Context.BIND_ABOVE_CLIENT | Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Options...").setOnMenuItemClickListener(menuItem -> {
            Intent intent = new Intent(this, OptionsActivity.class);
            startActivity(intent);
            return true;
        });
        menu.add("QR...").setOnMenuItemClickListener(menuItem -> {
            Intent intent = new Intent(this, QRActivity.class);
            startActivity(intent);
            return true;
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        return super.onPrepareOptionsMenu(menu);
    }
}