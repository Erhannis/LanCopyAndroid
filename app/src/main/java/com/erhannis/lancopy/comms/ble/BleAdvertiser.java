package com.erhannis.lancopy.comms.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertisingSet;
import android.bluetooth.le.AdvertisingSetCallback;
import android.bluetooth.le.AdvertisingSetParameters;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.erhannis.lancopy.DataOwner;
import com.erhannis.lancopy.MyApplication;
import com.erhannis.mathnstuff.MeUtils;
import com.welie.blessed.BluetoothCentralManager;
import com.welie.blessed.BluetoothCentralManagerCallback;
import com.welie.blessed.BluetoothPeripheral;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class BleAdvertiser {
    private static final String TAG = "BleAdvertiser";

    private final DataOwner dataOwner;

    public BleAdvertiser(DataOwner dataOwner) {
        this.dataOwner = dataOwner;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    void example1() {
        BluetoothLeAdvertiser advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();

        AdvertisingSetParameters parameters =  (new AdvertisingSetParameters.Builder())
                .setLegacyMode(true) // True by default, but set here as a reminder.
                .setConnectable(true)
                .setInterval(AdvertisingSetParameters.INTERVAL_HIGH)
                .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
                .build();

        AdvertiseData data = (new AdvertiseData.Builder()).setIncludeDeviceName(true).build();

        AdvertisingSet[] currentAdvertisingSet = {null};
        AdvertisingSetCallback callback = new AdvertisingSetCallback() {
            @Override
            public void onAdvertisingSetStarted(AdvertisingSet advertisingSet, int txPower, int status) {
                Log.i(TAG, "onAdvertisingSetStarted(): txPower:" + txPower + " , status: "
                        + status);
                currentAdvertisingSet[0] = advertisingSet;
            }

            @Override
            public void onAdvertisingDataSet(AdvertisingSet advertisingSet, int status) {
                Log.i(TAG, "onAdvertisingDataSet() :status:" + status);
            }

            @Override
            public void onScanResponseDataSet(AdvertisingSet advertisingSet, int status) {
                Log.i(TAG, "onScanResponseDataSet(): status:" + status);
            }

            @Override
            public void onAdvertisingSetStopped(AdvertisingSet advertisingSet) {
                Log.i(TAG, "onAdvertisingSetStopped():");
            }
        };

        //advertiser.startAdvertisingSet(parameters, data, null, null, null, callback);

        // After onAdvertisingSetStarted callback is called, you can modify the
        // advertising data and scan response data:
//        currentAdvertisingSet[0].setAdvertisingData(new AdvertiseData.Builder().
//                setIncludeDeviceName(true).setIncludeTxPowerLevel(true).build());
//        // Wait for onAdvertisingDataSet callback...
//        currentAdvertisingSet[0].setScanResponseData(new
//                AdvertiseData.Builder().addServiceUuid(new ParcelUuid(UUID.randomUUID())).build());
        // Wait for onScanResponseDataSet callback...

        // When done with the advertising:
        advertiser.stopAdvertisingSet(callback);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void testAdvertise() {
        Log.d(TAG, "--> testAdvertise");
        BluetoothLeAdvertiser advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
        advertiser.startAdvertising(new AdvertiseSettings.Builder()
                        .setConnectable(true)
                        .setTimeout(0)
                        .setTxPowerLevel((int) dataOwner.options.getOrDefault("BleAdvertiser.TX_POWER(0-3)", 3))
                        .setAdvertiseMode(0) //TODO Optionize?
                        .build(),
                new AdvertiseData.Builder()
                        //.addServiceUuid(new ParcelUuid(DataOwner.LANCOPY_SERVICE))
                        .addServiceData(new ParcelUuid(DataOwner.LANCOPY_SERVICE), new byte[]{-1,-2})
                        //.addServiceData(new ParcelUuid(DataOwner.LANCOPY_SERVICE), MeUtils.asBytes(dataOwner.ID))
                        .setIncludeDeviceName(false)
                        .setIncludeTxPowerLevel(false)
                        .build(),
                new AdvertiseCallback() {
                    @Override
                    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                        super.onStartSuccess(settingsInEffect);
                        Log.d(TAG, "onStartSuccess " + settingsInEffect);
                    }

                    @Override
                    public void onStartFailure(int errorCode) {
                        super.onStartFailure(errorCode);
                        Log.d(TAG, "onStartFailure " + errorCode);
                    }
                });
        Log.d(TAG, "<-- testAdvertise");
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void testScan() {
        final BluetoothCentralManagerCallback bluetoothCentralManagerCallback = new BluetoothCentralManagerCallback() {
            @Override
            public void onDiscoveredPeripheral(BluetoothPeripheral peripheral, ScanResult scanResult) {
                Log.d(TAG, "onDiscoveredPeripheral " + peripheral + " " + scanResult);
                if ("65:AB:0F:7D:7E:F7".equals(peripheral.getAddress())){
                    Log.d(TAG, "Found phone!");
                }
//                central.stopScan();
//                central.connectPeripheral(peripheral, peripheralCallback);
            }
        };

// Create BluetoothCentral and receive callbacks on the main thread
        BluetoothCentralManager central = new BluetoothCentralManager((MyApplication.getContext()), bluetoothCentralManagerCallback, new Handler(Looper.getMainLooper()));

// Scan for peripherals with a certain service UUID
        Log.d(TAG, "btuuid " + uuidToBtUuid(DataOwner.LANCOPY_SERVICE));
        //central.scanForPeripheralsWithServices(new UUID[]{uuidToBtUuid(DataOwner.LANCOPY_SERVICE)});
        central.scanForPeripheralsUsingFilters(List.of(new ScanFilter.Builder().setServiceData(new ParcelUuid(DataOwner.LANCOPY_SERVICE), new byte[16], new byte[16]).build()));
        //central.scanForPeripherals();
    }

    private static final UUID BASE_BT_UUID = UUID.fromString("00000000-0000-1000-8000-00805f9b34fb");
    public static UUID uuidToBtUuid(UUID uuid) {
        return new UUID(BASE_BT_UUID.getMostSignificantBits() | (uuid.getMostSignificantBits() & 0x0000FFFF00000000L), BASE_BT_UUID.getLeastSignificantBits());
    }
}
