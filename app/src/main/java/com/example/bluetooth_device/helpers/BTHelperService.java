package com.example.bluetooth_device.helpers;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Switch;

import androidx.annotation.Nullable;

import com.example.bluetooth_device.enums.BluetoothConnectionStates;
import com.example.bluetooth_device.views.MainActivity;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import static android.nfc.NfcAdapter.EXTRA_DATA;

public class BTHelperService extends Service {
    public static final String DEVICE_SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b";
    public static final String CHAR_HEART_BEAT_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8";
    public static final String CHAR_TEMP_UUID = "5c6ea9f3-b9a5-4ceb-a2d7-ffcbbeb29abb";
    public static final String CHAR_SPO_UUID = "f0b6957d-1d77-4ec8-9755-6c08090fe3a6";

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_CONNECTING =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTING";
    public final static String ACTION_GATT_DISCONNECTING =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTING";
    public final static String ACTION_GATT_SERVICE_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICE_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String ACTION_CHARA_SUCCESS =
            "com.example.bluetooth.le.ACTION_CHARA_SUCCESS";

    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    public final static  String EXTRA_DATA_TYPE = "com.example.bluetooth.le.EXTRA_DATA_TYPE";
    public final static  String EXTRA_HEART_BEAT = "com.example.bluetooth.le.EXTRA_HEART_BEAT";
    public final static String EXTRA_TEMP ="com.example.bluetooth.le.EXTRA_TEMP";
    public final static String EXTRA_SPO = "com.example.bluetooth.le.EXTRA_SPO";


    private final IBinder mBinder = new LocalBinder();

    private BluetoothConnectionStates connectionState = BluetoothConnectionStates.DISCONNECTED;

    private BluetoothGatt btGatt;
    private BluetoothAdapter bluetoothAdapter;

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            Log.d("####### char update",characteristic.getValue().toString());
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            if(status!=BluetoothGatt.GATT_SUCCESS){
                Log.d("#####", "status not success");
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if(status!=BluetoothGatt.GATT_SUCCESS){
                Log.d("#####", "status not success");
            }else{
                broadcastUpdate(ACTION_CHARA_SUCCESS);
            }
        }



        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d("###### serviceDisc", String.valueOf(status));
            broadcastUpdate(ACTION_GATT_SERVICE_DISCOVERED);
        }


        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            switch (newState) {
                case BluetoothGatt.STATE_CONNECTED:
                    Log.d("##### connected", "discovering services");
                    connectionState = BluetoothConnectionStates.CONNECTED;
                    broadcastUpdate(ACTION_GATT_CONNECTED);
                    gatt.discoverServices();
                    break;
                case BluetoothGatt.STATE_DISCONNECTED:
                    connectionState = BluetoothConnectionStates.DISCONNECTED;
                    broadcastUpdate(ACTION_GATT_DISCONNECTED);
                    break;
                case BluetoothGatt.STATE_CONNECTING:
                    connectionState = BluetoothConnectionStates.CONNECTING;
                    broadcastUpdate(ACTION_GATT_CONNECTING);
                    break;
                case BluetoothGatt.STATE_DISCONNECTING:
                    connectionState = BluetoothConnectionStates.DISCONNECTING;
                    broadcastUpdate(ACTION_GATT_DISCONNECTING);
                    break;
            }
        }

    };

    public  void disconnectDevice(){
        if(this.connectionState == BluetoothConnectionStates.CONNECTED)
        this.btGatt.disconnect();
    }

    public BluetoothGattService getSupportedServices() {
        if (btGatt == null) {
            return null;
        }
        return btGatt.getService(UUID.fromString(DEVICE_SERVICE_UUID));
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (btGatt == null) {
            Log.w("#######", "BluetoothGatt not initialized");
            return;
        }
        if(!btGatt.readCharacteristic(characteristic)){
            Log.d("#######", "Cannot read charact");
        }
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,boolean enabled) {
        if (btGatt == null) {
            Log.w("######", "BluetoothGatt not initialized");
            return;
        }
        btGatt.setCharacteristicNotification(characteristic, enabled);

        // This is specific to Heart Rate Measurement.
//        if (CHAR_HEART_BEAT_UUID.equals(characteristic.getUuid().toString())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptors().get(0);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            btGatt.writeDescriptor(descriptor);
//        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    private void close() {
        if (btGatt == null) {
            return;
        }
        btGatt.close();
        btGatt = null;
    }

    public BluetoothConnectionStates getConnectionState() {
        return connectionState;
    }


    public boolean initialize() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e("#########", "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    public boolean connect(final String address) {
        if (bluetoothAdapter == null || address == null) {
            Log.w("#######", "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        try {
            final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            // connect to the GATT server on the device
            btGatt = device.connectGatt(this, false, mGattCallback);
            return true;
        } catch (IllegalArgumentException exception) {
            Log.w("########", "Device not found with provided address.  Unable to connect.");
            return false;
        }
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        int flag = characteristic.getProperties();
        int format = -1;
        if ((flag & 0x01) != 0) {
            format = BluetoothGattCharacteristic.FORMAT_UINT16;
        } else {
            format = BluetoothGattCharacteristic.FORMAT_UINT8;
        }


        // This is special handling for the Heart Rate Measurement profile. Data
        // parsing is carried out as per profile specifications.
        if (CHAR_HEART_BEAT_UUID.equals(characteristic.getUuid().toString())) {

            final float heartRate =  ByteBuffer.wrap(characteristic.getValue()).order(ByteOrder.LITTLE_ENDIAN).getFloat();
            Log.d("#######","Received heart rate: "+ heartRate);
            intent.putExtra(EXTRA_DATA, heartRate);
            intent.putExtra(EXTRA_DATA_TYPE,EXTRA_HEART_BEAT);
            sendBroadcast(intent);
        } else if(CHAR_TEMP_UUID.equals(characteristic.getUuid().toString())) {
            final float temp =  ByteBuffer.wrap(characteristic.getValue()).order(ByteOrder.LITTLE_ENDIAN).getFloat();;
            Log.d("#######", "Received Temp rate: "+temp);
            intent.putExtra(EXTRA_DATA, temp);
            intent.putExtra(EXTRA_DATA_TYPE,EXTRA_TEMP);
            sendBroadcast(intent);

        }else if(CHAR_SPO_UUID.equals(characteristic.getUuid().toString())){
            final float spo =  ByteBuffer.wrap(characteristic.getValue()).order(ByteOrder.LITTLE_ENDIAN).getFloat();;
            Log.d("#######", "Received SPO rate: "+ spo);
            intent.putExtra(EXTRA_DATA, spo);
            intent.putExtra(EXTRA_DATA_TYPE,EXTRA_SPO);
            sendBroadcast(intent);
        }
    }


    public class LocalBinder extends Binder {
        public BTHelperService getService() {
            return BTHelperService.this;
        }
    }

}
