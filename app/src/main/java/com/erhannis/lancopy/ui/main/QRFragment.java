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
import com.erhannis.lancopy.R;
import com.erhannis.lancopy.refactor2.CommChannel;
import com.erhannis.lancopy.refactor2.qr.QRCommChannel;
import com.erhannis.lancopy.refactor2.qr.QRProcess;
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
import java.util.Random;
import java.util.StringJoiner;

import jcsp.helpers.JcspUtils;
import jcsp.helpers.NameParallel;
import jcsp.lang.AltingChannelInput;
import jcsp.lang.Any2OneChannel;
import jcsp.lang.CSProcess;
import jcsp.lang.CSTimer;
import jcsp.lang.Channel;
import jcsp.lang.ChannelOutput;
import jcsp.lang.ProcessManager;
import jcsp.util.InfiniteBuffer;
import jcsp.util.OverWriteOldestBuffer;

public class QRFragment extends Fragment {
    public final CommChannel channel;

    private final Handler handler = Misc.startHandlerThread();

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public QRFragment() {
        this(null);
        //TODO Throw exception?
    }

    public QRFragment(CommChannel channel) {
        this.channel = channel;
        //reload(); // Can't reload until view created
        this.setRetainInstance(true);

        Webcam webcam = Webcam.getDefault();
        Dimension[] ds = webcam.getViewSizes();
        webcam.setViewSize(ds[ds.length - 1]);
        webcam.open();
        jSplitPane1.setLeftComponent(new WebcamPanel(webcam));
        jSplitPane1.setDividerLocation(100);


        //DO Resize/scale
        final ImagePanel qrImagePanel = new ImagePanel();
        jSplitPane2.setRightComponent(qrImagePanel);
        qrImagePanel.invalidate();


        Any2OneChannel<byte[]> txBytesChannel = Channel.<byte[]>any2one(5);
        AltingChannelInput<byte[]> txBytesIn = txBytesChannel.in();
        ChannelOutput<byte[]> txBytesOut = JcspUtils.logDeadlock(txBytesChannel.out());

        // The buffering is a little non-standard, but I think this is right

        Any2OneChannel<byte[]> rxBytesChannel = Channel.<byte[]>any2one(new InfiniteBuffer<>(), 5);
        AltingChannelInput<byte[]> rxBytesIn = rxBytesChannel.in();
        ChannelOutput<byte[]> rxBytesOut = JcspUtils.logDeadlock(rxBytesChannel.out());

        Any2OneChannel<byte[]> txQrChannel = Channel.<byte[]>any2one(new InfiniteBuffer<>(), 5);
        AltingChannelInput<byte[]> txQrIn = txQrChannel.in();
        ChannelOutput<byte[]> txQrOut = JcspUtils.logDeadlock(txQrChannel.out());

        Any2OneChannel<byte[]> rxQrChannel = Channel.<byte[]>any2one(5);
        AltingChannelInput<byte[]> rxQrIn = rxQrChannel.in();
        ChannelOutput<byte[]> rxQrOut = JcspUtils.logDeadlock(rxQrChannel.out());

        Any2OneChannel<String> statusChannel = Channel.<String> any2one(new OverWriteOldestBuffer<>(1), 5);
        AltingChannelInput<String> statusIn = statusChannel.in();
        ChannelOutput<String> statusOut = JcspUtils.logDeadlock(statusChannel.out());


        String[] lastStatus = {"None"};

        this.channel = new QRCommChannel(dataOwner, txBytesOut, rxBytesIn, comm);
        new ProcessManager(new NameParallel(new CSProcess[]{
                new QRProcess(txBytesIn, txQrOut, rxBytesOut, rxQrIn, statusOut),

                () -> {
                    Thread.currentThread().setName("QR Camera loop");
                    CSTimer timer = new CSTimer();
                    while (true) {
                        if (!webcam.isOpen()) {
                            timer.sleep(200);
                            taStatus.setText(lastStatus[0] + "\n" + "camera not open");
                            continue;
                        }
                        BufferedImage image = webcam.getImage();
                        if (image == null) {
                            taStatus.setText(lastStatus[0] + "\n" + "no image");
                            continue;
                        }

                        Result result = null;
                        try {
                            result = new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image))));
                            taStatus.setText(lastStatus[0] + "\n" + "qr visible");
                        } catch (NotFoundException e) {
                            taStatus.setText(lastStatus[0] + "\n" + "qr not visible 1");
                            continue;
                        } catch (ChecksumException ex) {
                            taStatus.setText(lastStatus[0] + "\n" + "qr not visible 2");
                            continue;
                        } catch (FormatException ex) {
                            taStatus.setText(lastStatus[0] + "\n" + "qr not visible 3");
                            continue;
                        }

                        if (result != null) {
                            ArrayList<byte[]> segments = (ArrayList<byte[]>) result.getResultMetadata().get(ResultMetadataType.BYTE_SEGMENTS);
                            if (segments.size() > 1) {
                                System.err.println("Got more than one segment in a qr code!!! What does it mean?!?");
                            }
                            for (byte[] segment : segments) {
                                System.out.println("QR <-RXb " + Arrays.toString(segment));
                                System.out.println("QR <-RXc " + new String(segment, CHARSET));

                                rxQrOut.write(segment);
                            }
                        }
                    }
                },

                () -> {
                    Thread.currentThread().setName("QR handler loop");
                    Random r = new Random();
                    CSTimer jitterTimer = new CSTimer();
                    // The jitter is because sometimes the reader would get stuck, and slightly moving the image unsticks it
                    jitterTimer.setAlarm(jitterTimer.read()+((Long) dataOwner.options.getOrDefault("Comms.qr.JITTER.INTERVAL",200L)));
                    BufferedImage lastTxQr = null;

                    Alternative alt = new Alternative(new Guard[]{txQrIn, statusIn, jitterTimer});
                    while (true) {
                        switch (alt.priSelect()) {
                            case 0: { // txQrIn
                                byte[] qr = txQrIn.read();
                                System.out.println("QR TXb-> " + Arrays.toString(qr));
                                System.out.println("QR TXc-> " + new String(qr, CHARSET));
                                try {
                                    BufferedImage bi = getQR(qr);
                                    qrImagePanel.setImage(bi);
                                    lastTxQr = bi;
                                    long INTERVAL = (Long) dataOwner.options.getOrDefault("Comms.qr.JITTER.INTERVAL",200L);
                                    jitterTimer.setAlarm(jitterTimer.read()+INTERVAL);
                                } catch (WriterException ex) {
                                    Logger.getLogger(SwingQRCommFrame.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                break;
                            }
                            case 1: { // statusIn
                                lastStatus[0] = statusIn.read();
                                break;
                            }
                            case 2: { // jitterTimer
                                if (lastTxQr != null) {
                                    double X = (Double) dataOwner.options.getOrDefault("Comms.qr.JITTER.X",100.0);
                                    double Y = (Double) dataOwner.options.getOrDefault("Comms.qr.JITTER.Y",100.0);
                                    boolean ROTATE = (Boolean) dataOwner.options.getOrDefault("Comms.qr.JITTER.ROTATE",true);
                                    BufferedImage jittered = transform(lastTxQr, ROTATE ? r.nextInt(4) * Math.PI / 2 : 0, (r.nextDouble()*X)-(X/2), (r.nextDouble()*Y)-(Y/2));
                                    qrImagePanel.setImage(jittered);
                                }
                                long INTERVAL = (Long) dataOwner.options.getOrDefault("Comms.qr.JITTER.INTERVAL",200L);
                                jitterTimer.setAlarm(jitterTimer.read()+INTERVAL);
                                break;
                            }
                        }
                    }
                }
        })).start();

        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                webcam.close();
                rxQrOut.poison(10);
                txQrIn.poison(10);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_qr, container, false);

        FragmentTransaction ft = this.getActivity().getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fcvTop, listFragment);
        ft.commit();

        //DO Set UI callbacks etc.

        return view;
    }
}
