package com.erhannis.lancopy;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import com.erhannis.lancopy.data.BinaryData;
import com.erhannis.lancopy.data.Data;
import com.erhannis.lancopy.data.ErrorData;
import com.erhannis.mathnstuff.MeUtils;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;

import org.kamranzafar.jtar.TarEntry;
import org.kamranzafar.jtar.TarHeader;
import org.kamranzafar.jtar.TarOutputStream;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.Objects;
import java.util.function.Function;

public class ContentData extends Data {
    private static final String TAG = "ContentData";
    private static final Charset UTF8 = Charset.forName("UTF-8");

    public final Uri[] uris;

    public ContentData(Uri... uris) {
        this.uris = uris;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof ContentData)) {
            return false;
        }
        return Objects.deepEquals(this.uris, ((ContentData)obj).uris);
    }

    @Override
    public int hashCode() {
        return Objects.hash("ContentData", Arrays.hashCode(uris));
    }

    @Override
    public String getMime(boolean external) {
        if (external) {
            switch (uris.length) {
                case 0:
                    return "application/octet-stream";
                case 1:
                    return MyApplication.getContext().getContentResolver().getType(uris[0]);
                default:
                    return "application/octet-stream";
            }
        } else {
            return "lancopy/files";
        }
    }

    @Override
    public String toString() {
        return "[content] {" + MeUtils.join(", ", uris) + "}";
    }

    public String toLongString() {
        String[] subtexts = new String[this.uris.length + 1];
        subtexts[0] = "[content]";
        if (this.uris.length > 0) {
            try {
                subtexts[0] = "[content]\n" + this.uris[0] + "\n----";
            } catch (Throwable var3) {
            }
        }

        for(int i = 0; i < this.uris.length; ++i) {
            subtexts[i + 1] = this.uris[i] + " (" + getSize(this.uris[i], -1) + ")";
        }

        return MeUtils.join("\n", subtexts);
    }

    private static class PathedUri {
        public final String path;
        public final Uri uri;

        public PathedUri(String path, Uri uri) {
            this.path = path;
            this.uri = uri;
        }
    }

    private static String getFilename(Uri uri, String def) {
        try {
            Cursor returnCursor = MyApplication.getContext().getContentResolver().query(uri, null, null, null, null);
            int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            returnCursor.moveToFirst();
            return returnCursor.getString(nameIndex);
        } catch (Throwable t) {
            Log.e(TAG, "Error getting filename", t);
            return def;
        }
    }

    private static long getSize(Uri uri, long def) {
        try {
            Cursor returnCursor = MyApplication.getContext().getContentResolver().query(uri, null, null, null, null);
            int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
            returnCursor.moveToFirst();
            return returnCursor.getLong(sizeIndex);
        } catch (Throwable t) {
            Log.e(TAG, "Error getting file size", t);
            return def;
        }
    }

    private static InputStream getIS(Uri uri) throws FileNotFoundException {
        return MyApplication.getContext().getContentResolver().openInputStream(uri);
    }

    private static boolean isDirectory(Uri uri) {
        //TODO Do
        return false;
    }

    private static TarHeader getTarHeader(PathedUri pu) {
        Cursor returnCursor = MyApplication.getContext().getContentResolver().query(pu.uri, null, null, null, null);
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        String filename = returnCursor.getString(nameIndex);
        long size = returnCursor.getLong(sizeIndex);
        return TarHeader.createHeader(pu.path + "/" + filename, size, System.currentTimeMillis()/1000L, false, 0777);
    }

    @Override
    public InputStream serialize(boolean external) {
        if (uris.length == 1 && !isDirectory(uris[0])) {
            try {
                // This is a little cluttered
                byte[] filenameBytes = getFilename(uris[0], "0").getBytes(UTF8);
                if (external) {
                    return getIS(uris[0]);
                } else {
                    return new SequenceInputStream(new ByteArrayInputStream(Bytes.concat(
                            Ints.toByteArray(1), // Not yet really used
                            Ints.toByteArray(filenameBytes.length),
                            filenameBytes)),
                            getIS(uris[0]));
                }
            } catch (FileNotFoundException ex) {
                Log.e(TAG, "Error serializing files", ex);
                return new ErrorData("Error serializing files: " + ex.getMessage()).serialize(external); //TODO Wrong mime type
            }
        }

        return MeUtils.incrementalStream(os -> {
            try {
                Date date = new Date();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                byte[] filenameBytes = (dateFormat.format(date)+".tar").getBytes(UTF8);
                if (!external) {
                    MeUtils.pipeInputStreamToOutputStream(new ByteArrayInputStream(Bytes.concat(
                            Ints.toByteArray(1),
                            Ints.toByteArray(filenameBytes.length),
                            filenameBytes)), os);
                }

                LinkedList<PathedUri> pending = new LinkedList<>();
                for (Uri uri : uris) {
                    pending.add(new PathedUri("", uri));
                }

                try (TarOutputStream out = new TarOutputStream(os)) {
                    while (!pending.isEmpty()) {
                        PathedUri pu = pending.pop();
                        if (isDirectory(pu.uri)) {
                            throw new RuntimeException("Directories not yet handled");
//                            for (File f : pf.file.listFiles()) {
//                                PathedFile subfile = new PathedFile(pf.path + "/" + pf.file.getName(), f);
//                                pending.add(subfile);
//                            }
//                            continue;
                        }
                        out.putNextEntry(new TarEntry(getTarHeader(pu)));
                        BufferedInputStream origin = new BufferedInputStream(getIS(pu.uri));
                        int count;
                        byte data[] = new byte[2048];

                        while ((count = origin.read(data)) != -1) {
                            out.write(data, 0, count);
                        }

                        out.flush();
                        origin.close();
                    }
                }
            } catch (IOException e) {
                //TODO Not really an error; probably don't even need to log the trace
                Log.e(TAG, "FilesData serialize stream aborted", e);
            }
        });
    }

    public static Data deserialize(InputStream stream, Context ctx, Function<String, Uri> filePicker) {
        System.out.println("--> ContentData deserialize");
        try {
            int fileCount = Ints.fromByteArray(MeUtils.readNBytes(stream, 4));
            Uri[] files = new Uri[fileCount];
            for (int i = 0; i < fileCount; i++) {
                int filenameLen = Ints.fromByteArray(MeUtils.readNBytes(stream, 4));
                String filename = new String(MeUtils.readNBytes(stream, filenameLen), UTF8);

                Uri uri = filePicker.apply(filename);
                if (uri == null) {
                    throw new RuntimeException("File save canceled");
                }

                //TODO Wait until all files named?
                //TODO Uh...this copies the entire remaining stream.  Incompatible with "multiple files".

                OutputStream output = ctx.getContentResolver().openOutputStream(uri);

                MeUtils.pipeInputStreamToOutputStream(stream, output);
                stream.close();
                output.flush();
                output.close();

                files[i] = uri;
            }
            System.out.println("Done deserializing files");
            System.out.println("<-- ContentData deserialize");
            return new ContentData(files);
        } catch (Throwable t) {
            Log.e(TAG, "Error deserializing to ContentData", t);
            System.out.println("<-- ContentData deserialize");
            return new ErrorData("Error deserializing files: " + t);
        }
    }
}
