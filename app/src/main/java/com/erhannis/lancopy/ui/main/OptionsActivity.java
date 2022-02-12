package com.erhannis.lancopy.ui.main;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;

import com.erhannis.android.ekandroid.ui.ListFragment;
import com.erhannis.android.ekandroid.ui.OptionsFragment;
import com.erhannis.lancopy.R;
import com.erhannis.mathnstuff.utils.Options;

import java.util.Arrays;

public class OptionsActivity extends AppCompatActivity {
    private static final String TAG_OPTIONS_FRAGMENT = "options_fragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);

        FragmentManager fm = getSupportFragmentManager();
        OptionsFragment f = (OptionsFragment) fm.findFragmentByTag(TAG_OPTIONS_FRAGMENT);
        if (f == null) {
            // (Optionally do some processing or w/e here)
            f = new OptionsFragment(new Options());
            fm.beginTransaction().add(f, TAG_OPTIONS_FRAGMENT).commit();
        }
        // (Optionally save a reference to the fragment here)
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.fragmentContainerView, f);
        ft.commit();
    }
}