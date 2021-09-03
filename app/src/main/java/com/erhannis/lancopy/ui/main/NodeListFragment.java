package com.erhannis.lancopy.ui.main;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
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
import com.erhannis.lancopy.data.BinaryData;
import com.erhannis.lancopy.data.Data;
import com.erhannis.lancopy.data.ErrorData;
import com.erhannis.lancopy.data.FilesData;
import com.erhannis.lancopy.data.NoData;
import com.erhannis.lancopy.data.TextData;
import com.erhannis.lancopy.databinding.FragmentNodeListBinding;
import com.erhannis.lancopy.refactor.Summary;
import com.erhannis.mathnstuff.Pair;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
        final NodeListAdapter adapter = new NodeListAdapter(new ArrayList<>(), nodeLine -> {
            pullFromNode(nodeLine);
        });
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

    private void pullFromNode(NodeLine nl) {
        if (nl != null) {
            ProgressDialog pd = new ProgressDialog(getActivity());
            pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            pd.setTitle("Pulling data...");
            pd.show();

            new Thread(() -> { // Android didn't like me doing networking on main thread
                try {
                    //TODO This is not airtight; drag-n-drop still works, for instance
                    Pair<String, InputStream> result = lcs.uii.dataCall.call(lcs.uii.adCall.call(nl.summary.id).comms);
                    Data data;
                    if (result == null) {
                        data = new ErrorData("Node could not be reached");
                    } else {
                        switch (result.a) {
                            case "text/plain":
                                data = TextData.deserialize(result.b);
                                break;
                            case "application/octet-stream":
                                data = BinaryData.deserialize(result.b);
                                break;
                            case "lancopy/files":
//                            data = FilesData.deserialize(result.b, filename -> {
//                                File f = new File(filename);
//                                fileSaveChooser.setSelectedFile(f);
//                                if (fileSaveChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
//                                    return fileSaveChooser.getSelectedFile();
//                                } else {
//                                    return null;
//                                }
//                            });
//                            break;
                                throw new RuntimeException("not yet implemented");
                            case "lancopy/nodata":
                                data = NoData.deserialize(result.b);
                                break;
                            default:
                                data = new ErrorData("Unhandled MIME: " + result.a);
                                break;
                        }
                        try {
                            result.b.close();
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                    //System.out.println("rx data: " + data);
                    ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    if (data instanceof TextData) {
                        lcs.loadedText.set(((TextData) data).text);
                        ClipData clip = ClipData.newPlainText("pulled TextData", ((TextData) data).text);
                        clipboard.setPrimaryClip(clip);
                    } else if (data instanceof ErrorData) {
                        lcs.loadedText.set(((ErrorData) data).text);
                        ClipData clip = ClipData.newPlainText("pulled ErrorData", ((ErrorData) data).text);
                        clipboard.setPrimaryClip(clip);
                    } else if (data instanceof BinaryData) {
//                    if (fileOpenChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
//                        File f = fileOpenChooser.getSelectedFile();
//                        FileUtils.copyInputStreamToFile(((BinaryData) data).stream, f);
//                        taLoadedData.setText(((BinaryData) data).toString());
//                        try {
//                            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(f.getParentFile().getAbsolutePath()), null);
//                        } catch (Throwable t) {
//                            // Nevermind
//                        }
//                    } else {
//                        throw new RuntimeException("File save canceled");
//                    }
                        throw new IOException();
                    } else if (data instanceof FilesData) {
                        FilesData fd = ((FilesData) data);
                        lcs.loadedText.set(fd.toLongString());
                        try {
                            ClipData clip = ClipData.newPlainText("pulled FilesData", fd.files[0].getParentFile().getAbsolutePath());
                            clipboard.setPrimaryClip(clip);
                        } catch (Throwable t) {
                            // Nevermind
                        }
                        //System.err.println("//TODO Save files");
                    } else if (data instanceof NoData) {
                        lcs.loadedText.set(((NoData) data).toString());
                        ClipData clip = ClipData.newPlainText("pulled NoData", ((NoData) data).toString());
                        clipboard.setPrimaryClip(clip);
                    } else {
                        throw new RuntimeException("Unhandled data type");
                    }
                } catch (IOException ex) {
                    Log.e(TAG, "Error deserializing pulled data", ex);
                    lcs.loadedText.set("ERROR: " + ex.getMessage());
                } finally {
                    pd.dismiss();
                }
            }).start();
            /**/
        }
    }
}