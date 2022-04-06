package com.erhannis.lancopy.ui.main;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.erhannis.android.ekandroid.ui.OptionsFragment;
import com.erhannis.lancopy.AndroidQRComm;
import com.erhannis.lancopy.MyApplication;
import com.erhannis.lancopy.R;
import com.erhannis.lancopy.refactor2.CommChannel;

public class QRActivity extends AppCompatActivity {
    private static final String TAG_QR_FRAGMENT = "qr_fragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);

        FragmentManager fm = getSupportFragmentManager();
        QRFragment f = (QRFragment) fm.findFragmentByTag(TAG_QR_FRAGMENT);
        if (f == null) {
            // (Optionally do some processing or w/e here)
            f = new QRFragment((CommChannel) MyApplication.HORRIBLE_SINGLETON.get(getIntent().getStringExtra(AndroidQRComm.TOKEN_KEY)));
            fm.beginTransaction().add(f, TAG_QR_FRAGMENT).commit();
        }
        // (Optionally save a reference to the fragment here)
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.fcvQr, f);
        ft.commit();
    }
}