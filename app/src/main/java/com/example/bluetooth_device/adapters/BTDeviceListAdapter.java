package com.example.bluetooth_device.adapters;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import java.util.Objects;

public class BTDeviceListAdapter extends ArrayAdapter<BTDeviceListAdapter.BTDeviceModel> {
    public BTDeviceListAdapter(@NonNull Context context, int resource) {
        super(context, resource);
    }

     public static class BTDeviceModel{
        final BluetoothDevice device;


         public BTDeviceModel(BluetoothDevice device) {
             this.device = device;
         }

         @Override
         public String toString() {
             return device.getName();
         }

         public BluetoothDevice getDevice() {
             return device;
         }

         @Override
         public boolean equals(Object o) {
             if (this == o) return true;
             if (o == null || getClass() != o.getClass()) return false;
             BTDeviceModel that = (BTDeviceModel) o;
             return Objects.equals(device, that.device);
         }

         @Override
         public int hashCode() {
             return Objects.hash(device);
         }
     }
}
