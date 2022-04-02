package com.example.bluetooth_device.views;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.example.bluetooth_device.R;
import com.example.bluetooth_device.adapters.BTDeviceListAdapter;
import com.example.bluetooth_device.enums.BluetoothConnectionStates;
import com.example.bluetooth_device.helpers.BTHelperService;


import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final ArrayList<BluetoothGattCharacteristic> characteristics = new ArrayList<>();
    private BTDeviceListAdapter btSelectArrayAdapter;
    private AlertDialog.Builder btSelectDialog;
    private  AlertDialog btAlertDialog;

    private LottieAnimationView lottieAnimationView;
    private BTHelperService helper;
    private TextView tvConnectStatus;
    private TextView tvHeartBeat;
    private TextView tvSpo;
    private TextView tvTemperature;


    private final String[] permissions = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_PRIVILEGED,
            Manifest.permission.ACCESS_FINE_LOCATION,
    };

    private BluetoothAdapter bluetoothAdapter;
    private ActivityResultLauncher<Intent> someActivityResultLauncher;
    private boolean isScanning = false;


    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BTHelperService.ACTION_GATT_CONNECTED.equals(action)) {
                MainActivity.this.onConnectedToDevice();
            } else if (BTHelperService.ACTION_GATT_DISCONNECTED.equals(action)) {
                MainActivity.this.onDisconnectedFromDevice();
            }else  if(BTHelperService.ACTION_GATT_DISCONNECTING.equals(action)){
                MainActivity.this.onDisconnecting();
            }else if(BTHelperService.ACTION_GATT_CONNECTING.equals(action)){
                MainActivity.this.onConnecting();
            }else if(BTHelperService.ACTION_GATT_SERVICE_DISCOVERED.equals(action)){
                MainActivity.this.discoverServices();
            }else if(BTHelperService.ACTION_DATA_AVAILABLE.equals(action)){

                    switch (intent.getStringExtra(BTHelperService.EXTRA_DATA_TYPE)) {
                        case BTHelperService.EXTRA_HEART_BEAT:
                            tvHeartBeat.setText(intent.getStringExtra(BTHelperService.EXTRA_DATA));
                            break;
                        case BTHelperService.EXTRA_TEMP:
                            tvTemperature.setText(intent.getStringExtra(BTHelperService.EXTRA_DATA));
                            break;
                        case BTHelperService.EXTRA_SPO:
                            tvSpo.setText(intent.getStringExtra(BTHelperService.EXTRA_DATA));
                            break;
                    }

            }else if(BTHelperService.ACTION_CHARA_SUCCESS.equals(action)) {
                if (characteristics.size() > 0) {
                    characteristics.remove(0);
                    if (characteristics.size() > 0) {
                        helper.setCharacteristicNotification(characteristics.get(0), true);
                        helper.readCharacteristic(characteristics.get(0));
                    }
                }
            }
        }
    };

    private final ScanCallback btScanCallback = new ScanCallback() {

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for (ScanResult result : results) {
                MainActivity.this.onDeviceFound(result.getDevice());
            }
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            MainActivity.this.onDeviceFound(result.getDevice());
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            MainActivity.this.onScanFailed(errorCode);
        }


    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BTHelperService btHelperService = ((BTHelperService.LocalBinder)service).getService();
            if(!btHelperService.initialize()){
                Log.d("#####","Could not initialise the service");
                finish();
            }
            helper = btHelperService;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            helper = null;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        someActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {

                        MainActivity.this.startScan();
                    }else{
                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this, "Please enable the bluetooth", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });

        bluetoothAdapter  = BluetoothAdapter.getDefaultAdapter();
        btSelectArrayAdapter = new BTDeviceListAdapter(MainActivity.this, android.R.layout.select_dialog_singlechoice);


        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(MainActivity.this, "Bluetooth LE is not supported on this device", Toast.LENGTH_SHORT).show();
            finish();
        }

        initIds();

        checkAndReqPermissions();

        Intent serviceIntent = new Intent(this,BTHelperService.class);
        if(!bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)){
            Log.d("#########","Couldn't bind the service");
        }
    }

    public void startScan() {
        this.isScanning = true;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvConnectStatus.setText("Scanning for devices");
                // Added Dummy data here
                tvHeartBeat.setText("80");
                tvTemperature.setText("37");
                tvSpo.setText("96");
            }
        });
        bluetoothAdapter.getBluetoothLeScanner().startScan(btScanCallback);
    }

    public void stopScan() {
        this.isScanning = false;
        bluetoothAdapter.getBluetoothLeScanner().stopScan(btScanCallback);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvConnectStatus.setText("Tap to connect");
            }
        });
        // Added for dummy data
        resetFields();
    }


    public boolean connectDevice(String address) {
        stopScan();
        if (bluetoothAdapter == null || address == null) {
            Log.w("####### ", "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        try {
            final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            helper.connect(device.getAddress());
        } catch (IllegalArgumentException exception) {
            Log.w("#######", "Device not found with provided address.");
            return false;
        }

        return true;
    }


    private void discoverServices(){
        if(helper == null) return;
        BluetoothGattService service = helper.getSupportedServices();

        if(service==null) return;

        characteristics.clear();
        characteristics.addAll(service.getCharacteristics());

        helper.setCharacteristicNotification(characteristics.get(0), true);
        helper.readCharacteristic(characteristics.get(0));

    }
    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
//        if (helper != null) {
//            final boolean result = helper.connect(address);
//            Log.d("#####", "Connect request result=" + result);
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(gattUpdateReceiver);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BTHelperService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BTHelperService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BTHelperService.ACTION_GATT_CONNECTING);
        intentFilter.addAction(BTHelperService.ACTION_GATT_DISCONNECTING);
        intentFilter.addAction(BTHelperService.ACTION_GATT_SERVICE_DISCOVERED);
        intentFilter.addAction(BTHelperService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BTHelperService.ACTION_CHARA_SUCCESS);
        return intentFilter;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }



    private void checkAndReqPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int REQUEST_CODE = 22;
            requestPermissions(permissions, REQUEST_CODE);
        }
    }

    private void initIds() {
        lottieAnimationView = findViewById(R.id.bluetooth_connecting);
        tvConnectStatus = findViewById(R.id.tv_tap_to_connect);
        tvHeartBeat = findViewById(R.id.tv_hb_value);
        tvSpo = findViewById(R.id.tv_spo_value);
        tvTemperature = findViewById(R.id.tv_temp_value);
    }


    public void exitFromApp(View view) {
        this.finishAffinity();
    }

    public void onLottieClick(View view) {
        Log.d("############", helper.getConnectionState().toString());
        if (helper.getConnectionState() == BluetoothConnectionStates.DISCONNECTED) {
            if(!bluetoothAdapter.isEnabled()){
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                someActivityResultLauncher.launch(enableBtIntent);
            }else {
                if(!this.isScanning) {
                    this.startScan();
                }else{
                    this.stopScan();
                }
            }
        } else if (helper.getConnectionState() == BluetoothConnectionStates.CONNECTED) {
            helper.disconnectDevice();
        }
    }

    public void showSelectDeviceDialog() {
//        builderSingle.setIcon(R.drawable.ic_launcher);
        if(btAlertDialog !=null && btAlertDialog.isShowing()){
            return;
        }
        btSelectDialog = new AlertDialog.Builder(MainActivity.this);
        btSelectDialog.setTitle("Select Device");

        btSelectDialog.setNegativeButton("cancel", (dialog, which) -> {
            dialog.dismiss();
            MainActivity.this.stopScan();
        });

        btSelectDialog.setAdapter(btSelectArrayAdapter, (dialog, which) -> this.connectDevice(btSelectArrayAdapter.getItem(which).getDevice().getAddress()));
        btAlertDialog = btSelectDialog.show();
    }

    public void resetFields() {
        runOnUiThread(() -> {
            tvHeartBeat.setText("0");
            tvSpo.setText("0");
            tvTemperature.setText("0");
        });
    }

    public void onConnectedToDevice() {
        this.isScanning = false;

        runOnUiThread(() -> {
            tvConnectStatus.setText("Connected");
            lottieAnimationView.pauseAnimation();

        });
    }

    public void onDisconnectedFromDevice() {
        runOnUiThread(() -> {
            tvConnectStatus.setText("Disconnected");
            lottieAnimationView.pauseAnimation();

        });
    }

    public void onConnecting() {
        resetFields();
        runOnUiThread(() -> {
            tvConnectStatus.setText("Connecting... Please wait...");
            lottieAnimationView.playAnimation();
        });

    }

    public void onDisconnecting() {
        runOnUiThread(() -> {
            tvConnectStatus.setText("Disconnecting... Please wait...");
            lottieAnimationView.playAnimation();
        });

    }

    public void onDeviceFound(BluetoothDevice device) {
        showSelectDeviceDialog();
        if(device== null || device.getAddress().isEmpty() ){
            return;
        }
        if (btSelectArrayAdapter.getPosition(new BTDeviceListAdapter.BTDeviceModel(device)) < 0) {
            btSelectArrayAdapter.add(new BTDeviceListAdapter.BTDeviceModel(device));
            btSelectArrayAdapter.notifyDataSetChanged();
        }
    }

    public void onScanFailed(int errorCode) {
        this.isScanning = false;
        Log.d("#######", "Scan failed");
    }

}
