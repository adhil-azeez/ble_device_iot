package com.example.bluetooth_device.views;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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
import com.example.bluetooth_device.interfaces.IBTHelper;
import com.example.bluetooth_device.interfaces.IBluetoothConnection;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class MainActivity extends AppCompatActivity implements IBluetoothConnection {

    private BTDeviceListAdapter btSelectArrayAdapter;
    private AlertDialog.Builder btSelectDialog;

    private LottieAnimationView lottieAnimationView;
    private IBTHelper helper;
    private TextView tvConnectStatus;
    private TextView tvHeartBeat;
    private TextView tvSpo;
    private TextView tvTemperature;

    private final int REQUEST_CODE = 22;

    private final String[] permissions = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_PRIVILEGED,
            Manifest.permission.ACCESS_FINE_LOCATION,
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BTHelperService btHelperService = ((BTHelperService.LocalBinder)service).getService();
            btHelperService.init( MainActivity.this);
            helper = btHelperService;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            helper = null;
        }
    };

    private final BroadcastReceiver pairingBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    helper.connectDevice(device, MainActivity.this);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();

        btSelectArrayAdapter = new BTDeviceListAdapter(MainActivity.this, android.R.layout.select_dialog_singlechoice);
        btSelectDialog = new AlertDialog.Builder(MainActivity.this);

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(MainActivity.this, "Bluetooth LE is not supported on this device", Toast.LENGTH_SHORT).show();
            finish();
        }

        initIds();

        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        this.registerReceiver(pairingBroadcastReceiver, intentFilter);

        checkAndReqPermissions();

        Intent serviceIntent = new Intent(this,BTHelperService.class);
        if(!bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)){
            Log.d("#########","Couldn't bind the service");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
       Log.d("######", "requestcode "+requestCode+" resultCode "+ resultCode);
    }


    private void checkAndReqPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
        if (helper.getConnectionState() == BluetoothConnectionStates.DISCONNECTED) {
            showSelectDeviceDialog();
            helper.startScan();
            runOnUiThread(()->{
                tvConnectStatus.setText("Scanning for devices...");
            });
        } else if (helper.getConnectionState() == BluetoothConnectionStates.CONNECTED) {
            helper.disConnectDevice();
        }
    }

    public void showSelectDeviceDialog() {
//        builderSingle.setIcon(R.drawable.ic_launcher);
        btSelectDialog.setTitle("Select Device");

        btSelectDialog.setNegativeButton("cancel", (dialog, which) -> dialog.dismiss());

        btSelectDialog.setAdapter(btSelectArrayAdapter, (dialog, which) -> helper.connectDevice(btSelectArrayAdapter.getItem(which).getDevice(), this));
        btSelectDialog.show();
    }

    public void resetFields() {
        runOnUiThread(() -> {
            tvHeartBeat.setText("0");
            tvSpo.setText("0");
            tvTemperature.setText("0");
        });
    }

    @Override
    public void onConnectedToDevice() {
        runOnUiThread(() -> {
            tvConnectStatus.setText("Connected");
            lottieAnimationView.pauseAnimation();

        });
    }

    @Override
    public void onDisconnectedFromDevice() {
        runOnUiThread(() -> {
            tvConnectStatus.setText("Disconnected");
            lottieAnimationView.pauseAnimation();

        });
    }

    @Override
    public void onConnecting() {
        resetFields();
        runOnUiThread(() -> {
            tvConnectStatus.setText("Connecting... Please wait...");
            lottieAnimationView.playAnimation();
        });

    }

    @Override
    public void onDisconnecting() {
        runOnUiThread(() -> {
            tvConnectStatus.setText("Disconnecting... Please wait...");
            lottieAnimationView.playAnimation();
        });

    }

    @Override
    public void onPairing() {
        runOnUiThread(() -> tvConnectStatus.setText("Pairing... Please wait..."));
    }

    @Override
    public void onDeviceFound(BluetoothDevice device) {

        if (btSelectArrayAdapter.getPosition(new BTDeviceListAdapter.BTDeviceModel(device)) < 0) {
            btSelectArrayAdapter.add(new BTDeviceListAdapter.BTDeviceModel(device));
            btSelectArrayAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onScanFailed(int errorCode) {
        Log.d("#######", "Scan failed");
    }

    @Override
    public void onHeartBeatReceived(int val) {
        runOnUiThread(() -> tvHeartBeat.setText(String.valueOf(val)));
    }

    @Override
    public void onSpoReceived(float val) {
        NumberFormat nf = DecimalFormat.getInstance();
        nf.setMaximumFractionDigits(0);
        runOnUiThread(() -> tvSpo.setText(nf.format((val * 8) + 90)));
    }

    @Override
    public void onTempReceived(float val) {
        NumberFormat nf = DecimalFormat.getInstance();
        nf.setMaximumFractionDigits(1);
        runOnUiThread(() -> tvTemperature.setText(nf.format((val * 5) + 30)));
    }
}
