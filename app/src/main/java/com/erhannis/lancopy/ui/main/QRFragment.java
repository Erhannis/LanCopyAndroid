package com.erhannis.lancopy.ui.main;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.erhannis.android.ekandroid.Misc;
import com.erhannis.android.ekandroid.R;
import com.erhannis.lancopy.refactor2.CommChannel;
import com.erhannis.mathnstuff.FactoryHashMap;
import com.erhannis.mathnstuff.Stringable;
import com.erhannis.mathnstuff.utils.Factory;
import com.erhannis.mathnstuff.utils.Options;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;

public class QrFragment extends Fragment {
    public final CommChannel channel;

    private final Handler handler = Misc.startHandlerThread();

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public QrFragment() {
        this(null);
        //TODO Throw exception?
    }

    public QrFragment(CommChannel channel) {
        this.channel = channel;
        //reload(); // Can't reload until view created
        this.setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_qr, container, false);

        FragmentTransaction ft = this.getActivity().getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fcvTop, listFragment);
        ft.commit();

        this.tvMessage.setText(message);

        this.btnReload.setOnClickListener(v -> {
            reload();
        });

        Misc.runOnUiThread(() -> {
            System.out.println("--> ocv.rui.reload");
            reload();
            System.out.println("<-- ocv.rui.reload");
        });

        return view;
    }

    /**
     * Run on UI thread
     */
    public void reload() {
        ArrayList<Stringable<Map.Entry<String, Object>>> items = new ArrayList<>();
        FactoryHashMap<Object, HashSet<String>> reverse = new FactoryHashMap<>(new Factory<Object, HashSet<String>>() {
            @Override
            public HashSet<String> construct(Object input) {
                return new HashSet<String>();
            }
        });
        //Collections.sort
        for (Entry<String, Object> e : options.getRecentEntries()) {
            Stringable<Entry<String, Object>> s = new Stringable<Entry<String, Object>>(e, e.getKey() + " : " + e.getValue());
            items.add(s);
            reverse.get(s.val.getValue()).add(s.val.getKey());
        }
        for (Entry<Object, HashSet<String>> e : reverse.entrySet()) {
            if (e.getValue().size() > 1) {
                System.err.println("Conflict on " + e.getKey() + " with " + StringUtils.join(e.getValue()));
            }
        }
        listFragment.setList(items);
    }

    private void select(Entry<String, Object> entry) {
        Entry<String,Object> e = entry;
        if (e.getValue() != null) {
            if (e.getValue() instanceof Boolean) {
                String newValue = Dialogs.stringInputDialog(this.getActivity(), e.getKey() + " (boolean)", ""+e.getValue());
                if (newValue != null) {
                    boolean v = false;
                    if ("true".equals(newValue.toLowerCase())) {
                        v = true;
                    } else if ("false".equals(newValue.toLowerCase())) {
                        v = false;
                    } else {
                        Misc.showToast(this.getActivity(), "Error, please enter true or false");
                        return;
                    }
                    options.set(e.getKey(), v);
                }
            } else if (e.getValue() instanceof String) {
                String newValue = Dialogs.stringInputDialog(this.getActivity(), e.getKey() + " (string)", ""+e.getValue());
                if (newValue != null) {
                    options.set(e.getKey(), newValue);
                }
            } else if (e.getValue() instanceof Character) {
                String newValue = Dialogs.stringInputDialog(this.getActivity(), e.getKey() + " (character)", ""+e.getValue());
                if (newValue != null) {
                    if (newValue.length() != 1) {
                        Misc.showToast(this.getActivity(), "Error, please enter a single character");
                        return;
                    }
                    options.set(e.getKey(), newValue.charAt(0));
                }
            } else if (e.getValue() instanceof Integer) {
                String newValue = Dialogs.stringInputDialog(this.getActivity(), e.getKey() + " (integer)", ""+e.getValue());
                if (newValue != null) {
                    int v = 0;
                    try {
                        v = Integer.parseInt(newValue);
                    } catch (NumberFormatException ex) {
                        Misc.showToast(this.getActivity(), "Error, please enter an integer (max 32 bit)");
                        return;
                    }
                    options.set(e.getKey(), v);
                }
            } else if (e.getValue() instanceof Long) {
                String newValue = Dialogs.stringInputDialog(this.getActivity(), e.getKey() + " (long integer)", ""+e.getValue());
                if (newValue != null) {
                    long v = 0;
                    try {
                        v = Long.parseLong(newValue);
                    } catch (NumberFormatException ex) {
                        Misc.showToast(this.getActivity(), "Error, please enter an integer (max 64 bit)");
                        return;
                    }
                    options.set(e.getKey(), v);
                }
            } else if (e.getValue() instanceof Float) {
                String newValue = Dialogs.stringInputDialog(this.getActivity(), e.getKey() + " (float)", ""+e.getValue());
                if (newValue != null) {
                    float v = 0;
                    try {
                        v = Float.parseFloat(newValue);
                    } catch (NumberFormatException ex) {
                        Misc.showToast(this.getActivity(), "Error, please enter a float");
                        return;
                    }
                    options.set(e.getKey(), v);
                }
            } else if (e.getValue() instanceof Double) {
                String newValue = Dialogs.stringInputDialog(this.getActivity(), e.getKey() + " (double)", ""+e.getValue());
                if (newValue != null) {
                    double v = 0;
                    try {
                        v = Double.parseDouble(newValue);
                    } catch (NumberFormatException ex) {
                        Misc.showToast(this.getActivity(), "Error, please enter a double (floating point number)");
                        return;
                    }
                    options.set(e.getKey(), v);
                }
            } else {
                Misc.showToast(this.getActivity(), "Sorry, unhandled datatype");
                return;
            }
            if ((Boolean)options.getOrDefault("OptionsFrame.AUTOSAVE_OPTIONS", true)) {
                try {
                    Options.saveOptions(options, optionsFilename);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    Misc.showToast(this.getActivity(), "Failed to save options to disk.");
                }
            }
            Misc.runOnUiThread(() -> {
                reload();
            });
        } else {
            Misc.showToast(this.getActivity(), "Sorry, null value; can't infer datatype");
            return;
        }
    }

    private void delete(Entry<String, Object> entry) {
        if (Dialogs.confirmDialog(this.getContext(), "Delete option?", "Delete option, potentially resetting it back to default?")) {
            options.remove(entry.getKey());
            if ((Boolean)options.getOrDefault("OptionsFrame.AUTOSAVE_OPTIONS", true)) {
                try {
                    Options.saveOptions(options, optionsFilename);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    Misc.showToast(this.getActivity(), "Failed to save options to disk.");
                }
            }
            Misc.runOnUiThread(() -> {
                reload();
            });
        }
    }
}
