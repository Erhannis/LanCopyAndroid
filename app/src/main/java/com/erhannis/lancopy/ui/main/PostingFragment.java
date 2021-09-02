package com.erhannis.lancopy.ui.main;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.erhannis.lancopy.ContentData;
import com.erhannis.lancopy.LanCopyService;
import com.erhannis.lancopy.R;
import com.erhannis.lancopy.data.BinaryData;
import com.erhannis.lancopy.data.Data;
import com.erhannis.lancopy.data.FilesData;
import com.erhannis.lancopy.data.NoData;
import com.erhannis.lancopy.data.TextData;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;

import static android.app.Activity.RESULT_OK;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PostingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PostingFragment extends LCFragment {
    private static final String TAG = "PostingFragment";

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public PostingFragment() {
        // Required empty public constructor
        //TODO Add loop checkbox?
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment PostingFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PostingFragment newInstance(String param1, String param2) {
        PostingFragment fragment = new PostingFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    private EditText etPosted;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        ClipboardManager clipboard = (ClipboardManager)getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        View view = inflater.inflate(R.layout.fragment_posting, container, false);
        view.findViewById(R.id.btnPostClipboard).setOnClickListener(v -> {
            ClipData cd = clipboard.getPrimaryClip();
            if (cd.getItemCount() > 0) {
                if (cd.getItemCount() > 1) {
                    toast("Too many clipboard items: " + cd.getItemCount());
                }
                ClipData.Item ci = cd.getItemAt(0);
                if (ci.getIntent() != null) {
                    //TODO ???
                    System.out.println("Got Intent");
                } else if (ci.getUri() != null) {
                    //TODO Paste file
                    System.out.println("Got URI");
                } else if (ci.getHtmlText() != null) {
                    setData(new TextData(ci.getHtmlText()));
                } else if (ci.getText() != null) {
                    setData(new TextData(ci.getText()+"")); //TODO Kinda janky conversion
                }
            } else {
                setData(new NoData());
            }
        });
        view.findViewById(R.id.btnPostFiles).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            try {
                startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"),FILE_SELECT_CODE);
            } catch (Exception ex) {
                Log.e(TAG, "Error creating file chooser", ex);
            }
        });
        view.findViewById(R.id.btnPostFolder).setOnClickListener(v -> {
            Log.e(TAG, "//TODO ERROR: folders not yet supported");
            toast("ERROR: folders not yet supported");
//            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
//            //intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // Doesn't work
//            try {
//                startActivityForResult(Intent.createChooser(intent, "Select a Folder to Upload"),FILE_SELECT_CODE);
//            } catch (Exception ex) {
//                Log.e(TAG, "Error creating file chooser", ex);
//            }
        });
        this.etPosted = ((EditText)view.findViewById(R.id.etPosted));
        return view;
    }

    private void setData(Data data) {
        etPosted.setText("" + data);
        lcs.uii.newDataOut.write(data);
    }





    // https://stackoverflow.com/a/36129285/513038

    private static final int FILE_SELECT_CODE = 1524;

    public static String getPath(Context context, Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // DocumentProvider
            if (DocumentsContract.isDocumentUri(context, uri)) {
                // ExternalStorageProvider
                if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }
                    // TODO handle non-primary volumes
                }
                // DownloadsProvider
                else if (isDownloadsDocument(uri)) {
                    final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                    return getDataColumn(context, contentUri, null, null);
                }
                // MediaProvider
                else if (isMediaDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }
                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{split[1]};
                    return getDataColumn(context, contentUri, selection, selectionArgs);
                }
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Activity act = getActivity();
        if (requestCode == FILE_SELECT_CODE) {
            if (resultCode == RESULT_OK) {
                try {
                    ArrayList<Uri> uris = new ArrayList<>();
                    {
                        Uri uri = data.getData();
                        if (uri != null) {
                            uris.add(uri);
                        }
                        ClipData cd = data.getClipData();
                        if (cd != null) {
                            for (int i = 0; i < cd.getItemCount(); i++) {
                                ClipData.Item item = cd.getItemAt(i);
                                if (item != null && item.getUri() != null) {
                                    uris.add(item.getUri());
                                }
                            }
                        }
                    }
                    if (!uris.isEmpty()) {
                        setData(new ContentData(uris.toArray(new Uri[0])));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}