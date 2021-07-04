package com.example.bluetooth_device.helpers;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.bluetooth_device.enums.BluetoothConnectionStates;
import com.example.bluetooth_device.interfaces.IBTHelper;
import com.example.bluetooth_device.interfaces.IBluetoothConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BTHelperService extends Service implements IBTHelper {
    public static final String DEVICE_SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b";
    public static final String CHAR_HEART_BEAT_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8";
    public static final String CHAR_TEMP_UUID = "5c6ea9f3-b9a5-4ceb-a2d7-ffcbbeb29abb";
    public static final String CHAR_SPO_UUID = "f0b6957d-1d77-4ec8-9755-6c08090fe3a6";


    private final IBinder mBinder = new LocalBinder();
    private BluetoothAdapter bluetoothAdapter;


    private BluetoothConnectionStates connectionState = BluetoothConnectionStates.DISCONNECTED;
    private ScanCallback btScanCallback = new ScanCallback() {

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for (ScanResult result : results) {
                iBluetoothConnection.onDeviceFound(result.getDevice());
            }
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            iBluetoothConnection.onDeviceFound(result.getDevice());
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            iBluetoothConnection.onScanFailed(errorCode);
        }
    };
    private IBluetoothConnection iBluetoothConnection;

    private BluetoothDevice device;

    private BluetoothGatt btGatt;

    List<BluetoothGattCharacteristic> chars = new ArrayList<>();

    private GattListener mGattCallback;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        disConnectDevice();
        return super.onUnbind(intent);
    }

    @Override
    public BluetoothConnectionStates getConnectionState() {
        return connectionState;
    }

    @Override
    public void startScan() {
        bluetoothAdapter.getBluetoothLeScanner().startScan(btScanCallback);
    }

    @Override
    public void stopScan() {
        bluetoothAdapter.getBluetoothLeScanner().stopScan(btScanCallback);
    }

    @Override
    public void connectDevice(BluetoothDevice device, Context context) {
        stopScan();
        if (this.device != null && this.device.getAddress().equals(device.getAddress())) {
            btGatt.connect();
        } else if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
            iBluetoothConnection.onPairing();
            device.createBond();
        } else {
            btGatt = device.connectGatt(context, false, mGattCallback);
            this.device = device;
        }

    }

    public void init(IBluetoothConnection iBluetoothConnection) {
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.iBluetoothConnection = iBluetoothConnection;
        this.mGattCallback = new GattListener(iBluetoothConnection);
    }

    @Override
    public void disConnectDevice() {
        iBluetoothConnection.onDisconnecting();
        if (this.btGatt != null) {
            this.btGatt.disconnect();
            this.btGatt.close();
            this.btGatt = null;
        }
        iBluetoothConnection.onDisconnectedFromDevice();
    }

    public class LocalBinder extends Binder {
        public BTHelperService getService() {
            return BTHelperService.this;
        }
    }

    public class GattListener extends  BluetoothGattCallback{

        final IBluetoothConnection iBluetoothConnection;

        public GattListener(IBluetoothConnection iBluetoothConnection) {
            this.iBluetoothConnection = iBluetoothConnection;
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            int flag = characteristic.getProperties();
            int format = -1;


            if (characteristic.getUuid().toString().equals(CHAR_HEART_BEAT_UUID)) {
                if ((flag & 0x01) != 0) {
                    format = BluetoothGattCharacteristic.FORMAT_UINT16;
//                    Log.d("#####", "Heart rate format UINT16.");
                } else {
                    format = BluetoothGattCharacteristic.FORMAT_UINT8;
//                    Log.d("#####", "Heart rate format UINT8.");
                }
                int value = characteristic.getIntValue(format, 0);
//                Log.d("###### onCharChanged", value + "");
                iBluetoothConnection.onHeartBeatReceived(value);
            } else if (characteristic.getUuid().toString().equals(CHAR_TEMP_UUID)) {
                float value = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 0);
                iBluetoothConnection.onTempReceived(value);
            } else if (characteristic.getUuid().toString().equals(CHAR_SPO_UUID)) {
                float value = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 0);
                iBluetoothConnection.onSpoReceived(value);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d("###### onCharRead", characteristic.getUuid().toString());
            Log.d("######### onCharRead", "status "+status);

            if (btGatt.setCharacteristicNotification(characteristic, true)) {
                Log.d("#####", "Charact Notif success");
            } else {
                Log.d("#####", "Charact Notif failure");
            }
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(characteristic.getDescriptors().get(0).getUuid());
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            if (btGatt.writeDescriptor(descriptor)) {
                Log.d("#####", "descriptor Notif success for " + descriptor.getUuid().toString());
            } else {
                Log.d("#####", "descriptor Notif failure");
            }
//            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
//            if (btGatt.writeDescriptor(descriptor)) {
//                Log.d("#####", "descriptor INDIC success");
//            } else {
//                Log.d("#####", "descriptor INDIC failure");
//            }

            if(status == BluetoothGatt.GATT_SUCCESS) {
                chars.remove(chars.size() - 1);
                if (chars.size() > 0) {
                    reqCharacteristic();
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d("###### serviceDisc", String.valueOf(status));
            BluetoothGattService testService = gatt.getService(UUID.fromString(DEVICE_SERVICE_UUID));
            if (testService != null) {
                chars.clear();
                chars.addAll(testService.getCharacteristics());
                reqCharacteristic();
//                btGatt.readCharacteristic(testService.getCharacteristics().get(0));
            }
        }

        private void reqCharacteristic(){
            if(btGatt.readCharacteristic(chars.get(chars.size()-1))){
                Log.d("#######","car read success");
            }else{
                Log.d("#######","car read failed");

            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d("###### onDescRead", descriptor.toString());
        }


        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            switch (newState) {
                case BluetoothGatt.STATE_CONNECTED:
                    Log.d("##### connectd", "discovering services");
                    connectionState = BluetoothConnectionStates.CONNECTED;
                    iBluetoothConnection.onConnectedToDevice();
                    gatt.discoverServices();
                    break;
                case BluetoothGatt.STATE_DISCONNECTED:
                    connectionState = BluetoothConnectionStates.DISCONNECTED;
                    iBluetoothConnection.onDisconnectedFromDevice();
                    break;
                case BluetoothGatt.STATE_CONNECTING:
                    connectionState = BluetoothConnectionStates.CONNECTING;
                    iBluetoothConnection.onConnecting();
                    break;
                case BluetoothGatt.STATE_DISCONNECTING:
                    connectionState = BluetoothConnectionStates.DISCONNECTING;
                    iBluetoothConnection.onDisconnecting();
                    break;
            }
        }

    }
}
