package com.erhannis.lancopy.ui.main;

import android.content.ComponentName;
import android.os.Bundle;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.erhannis.lancopy.MyApplication;
import com.erhannis.lancopy.R;
import com.erhannis.lancopy.databinding.FragmentNodeListBinding;
import com.erhannis.lancopy.refactor.Summary;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link NodeListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class NodeListFragment extends LCFragment {
    private static final String TAG = "NodeListFragment";

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public NodeListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment NodeListFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static NodeListFragment newInstance(String param1, String param2) {
        NodeListFragment fragment = new NodeListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate1");
        super.onCreate(savedInstanceState);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d(TAG,"onCreate2 " + mIsBound + " " + lcs);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    private NodeListAdapter nodeListAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG,"OnCreateView");
        FragmentNodeListBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_node_list, container, false);
        final NodeListAdapter adapter = new NodeListAdapter(new ArrayList<>());
        // This requires
        // - adding a layout element to activity_main.xml
        // - adding an id to the existing include element
        binding.nodeListContainer.nodeListView.setAdapter(adapter);
        this.nodeListAdapter = adapter;
        View view = binding.getRoot();
        return view;
    }

    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
        super.onServiceConnected(className, service);
        lcs.nodeLinesChanged.subscribeWithGet(o -> {
            MyApplication.runOnUiThread(() -> {
                this.nodeListAdapter.setList(new ArrayList<>(lcs.nodeLines));
            });
        });
    }
}