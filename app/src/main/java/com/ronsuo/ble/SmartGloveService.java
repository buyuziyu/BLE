package com.ronsuo.ble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.util.UUID;

/**
 * Created by Oxygen on 2016/9/2.
 */
public class SmartGloveService extends Service {

    private static final String DEVICE_NAME = "Dmart Glove";
    private static final UUID SMART_GLOVE_SERVICE_UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private static final UUID SMART_GLOVE_CHARACTERISTIC_UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private Handler mHandler;
    DeviceListener mListener;
    DataListener mDataListener;
    private String mBluetoothDeviceAddress;

    private final IBinder mBinder = new LocalBinder();

    public interface DeviceListener {
        void onNewDevice(String address);
        void onScanStateChanged(boolean scaning);
    }

    public interface DataListener {
        void onData(byte[] data);
        void onConnectStateChanged(boolean connected);
    }

    public class LocalBinder extends Binder {
        SmartGloveService getService() {
            return SmartGloveService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported.", Toast.LENGTH_SHORT).show();
        }
    }

    public void startLeScan(DeviceListener listener) {
        mListener = listener;
        Log.i("Oxygen", "--------->startLeScan");
        mBluetoothAdapter.startLeScan(leScanCallback);
    }

    public void stopLeScan() {
        mBluetoothAdapter.stopLeScan(leScanCallback);
    }

    public boolean connectToDevice(String address, DataListener listener) {
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d("Oxygen", "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w("Oxygen", "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d("Oxygen", "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mDataListener = listener;
        return true;
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("Oxygen", "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i("Oxygen", "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
                mDataListener.onConnectStateChanged(true);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("Oxygen", "Disconnected from GATT server.");
                mDataListener.onConnectStateChanged(false);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(SMART_GLOVE_SERVICE_UUID);
                if (service != null) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(SMART_GLOVE_CHARACTERISTIC_UUID);
                    if (characteristic != null) {
                        gatt.setCharacteristicNotification(characteristic, true);
                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        mBluetoothGatt.writeDescriptor(descriptor);
                    }
                }
            } else {
                Log.w("Oxygen", "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //mDataListener.onData(characteristic.getValue());
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            mDataListener.onData(characteristic.getValue());
        }
    };

    BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            String name = device.getName();
            if (name != null && name.equals(DEVICE_NAME)) {
                if (mListener != null) {
                    mListener.onNewDevice(device.getAddress());
                }
            }
        }
    };
}
