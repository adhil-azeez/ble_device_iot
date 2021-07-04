package com.example.bluetooth_device.interfaces;


import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.example.bluetooth_device.enums.BluetoothConnectionStates;

public interface IBTHelper {

    void startScan();
    void stopScan();
    void connectDevice(BluetoothDevice device, Context context);
    void disConnectDevice();
    BluetoothConnectionStates getConnectionState();

}
