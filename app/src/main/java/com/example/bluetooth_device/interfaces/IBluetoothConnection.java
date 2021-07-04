package com.example.bluetooth_device.interfaces;

import android.bluetooth.BluetoothDevice;

import com.example.bluetooth_device.enums.BluetoothConnectionStates;

public interface IBluetoothConnection {
     String ACTION_ON_CONNECTED_TO_DEVICE = "com.bluetooth.iot.action_connected_device";
     String ACTION_ON_DISCONNECTED = "com.bluetooth.iot.on_disconnected";
     String ACTION_ON_CONNECTING = "com.bluetooth.iot.on_connecting";
     String ACTION_ON_PAIRING = "com.bluetooth.iot.on_pairing";
     String ACTION_ON_DISCONNECTING = "com.bluetooth.iot.on_disconnecting";


     void onConnectedToDevice();
     void onDisconnectedFromDevice();
     void onConnecting();
     void onDisconnecting();
     void onPairing();

     void onDeviceFound(BluetoothDevice device);
     void onScanFailed(int errorCode);


     void onHeartBeatReceived(int val);
     void onSpoReceived(float val);
     void onTempReceived(float val);
}
