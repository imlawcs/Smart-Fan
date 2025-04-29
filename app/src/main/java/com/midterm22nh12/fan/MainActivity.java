package com.midterm22nh12.fan;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.midterm22nh12.fan.R;

import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends Activity {

    // UI components
    private Button btnAuto, btnOn1, btnOn2, btnToggle, btnConnect;
    private TextView statusText, temperatureText;

    // State variables
    private boolean isOn = false;
    private boolean isAutoMode = false;
    private boolean isConnected = false;

    // Bluetooth components
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic characteristic;
    private static final String DEVICE_NAME = "HMSoft"; // Hoặc tên HM-10 của bạn
    private static final UUID SERVICE_UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB");
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");
    private static final long SCAN_PERIOD = 10000;

    // Permission request code
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;
    private static final String TAG = "MainActivity";
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        statusText = findViewById(R.id.status);
        temperatureText = findViewById(R.id.temperature);
        btnAuto = findViewById(R.id.btnAuto);
        btnOn1 = findViewById(R.id.btnOn1);
        btnOn2 = findViewById(R.id.btnOn2);
        btnToggle = findViewById(R.id.btnToggle);
        btnConnect = findViewById(R.id.btnConnect);

// Initialize Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        }

// Check and request permissions
        checkAndRequestPermissions();

// Set up button listeners
        btnConnect.setOnClickListener(v -> {
            if (!isConnected) {
                Log.d(TAG, "Attempting to connect to HMSoft...");
                startScan();
            } else {
                Log.d(TAG, "Attempting to disconnect from HMSoft...");
                disconnectFromBluetooth();
            }
        });

        btnAuto.setOnClickListener(v -> {
            isAutoMode = !isAutoMode;
            if (isAutoMode) {
                sendCommand("AUTO\n");
                statusText.setText(R.string.auto_mode_on);
                btnAuto.setBackgroundResource(R.drawable.rounded_button);
            } else {
                sendCommand("MANUAL\n");
                handler.postDelayed(() -> sendCommand("OFF\n"), 100);
                statusText.setText(R.string.auto_mode_off);
                btnToggle.setText(R.string.off);
                btnToggle.setBackgroundResource(R.drawable.circle_button_off);
                btnAuto.setBackgroundResource(R.drawable.rounded_button_gray);
                isOn = false;
            }
        });

        btnOn1.setOnClickListener(v -> {
            if (!isAutoMode) {
                sendCommand("MANUAL\n");
                handler.postDelayed(() -> sendCommand("ON1\n"), 100);
                btnToggle.setText(R.string.on);
                btnToggle.setBackgroundResource(R.drawable.circle_button_on);
                btnAuto.setBackgroundResource(R.drawable.rounded_button_gray);
                isOn = true;
            }
        });

        btnOn2.setOnClickListener(v -> {
            if (!isAutoMode) {
                sendCommand("MANUAL\n");
                handler.postDelayed(() -> sendCommand("ON2\n"), 100);
                btnToggle.setText(R.string.on);
                btnToggle.setBackgroundResource(R.drawable.circle_button_on);
                btnAuto.setBackgroundResource(R.drawable.rounded_button_gray);
                isOn = true;
            }
        });

        btnToggle.setOnClickListener(v -> {
            if (!isAutoMode) {
                if (isOn) {
                    sendCommand("MANUAL\n");
                    handler.postDelayed(() -> sendCommand("OFF\n"), 100);
                    btnToggle.setText(R.string.off);
                    btnToggle.setBackgroundResource(R.drawable.circle_button_off);
                    isOn = false;
                } else {
                    sendCommand("MANUAL\n");
                    handler.postDelayed(() -> sendCommand("ON1\n"), 100);
                    btnToggle.setText(R.string.on);
                    btnToggle.setBackgroundResource(R.drawable.circle_button_on);
                    isOn = true;
                }
            }
        });
    }

    private void checkAndRequestPermissions() {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT);
                Log.d(TAG, "Requesting BLUETOOTH_CONNECT permission");
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN);
                Log.d(TAG, "Requesting BLUETOOTH_SCAN permission");
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
            Log.d(TAG, "Requesting ACCESS_FINE_LOCATION permission");
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            Log.d(TAG, "Requesting ACCESS_COARSE_LOCATION permission");
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), REQUEST_BLUETOOTH_PERMISSIONS);
            Log.d(TAG, "Requesting permissions: " + permissionsToRequest.toString());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                statusText.setText(R.string.permissions_granted);
                Log.d(TAG, "Permissions granted");
            } else {
                statusText.setText(R.string.need_permissions_to_connect);
                Log.d(TAG, "Permissions denied");
                Toast.makeText(this, R.string.please_grant_permissions, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        }
    }

    private void startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            statusText.setText(R.string.bluetooth_not_available);
            Log.d(TAG, "Bluetooth is not available or not enabled");
            Toast.makeText(this, R.string.please_enable_bluetooth, Toast.LENGTH_LONG).show();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)) {
            statusText.setText(R.string.need_permission_to_scan);
            checkAndRequestPermissions();
            return;
        }

        statusText.setText(R.string.scanning_devices);
        Log.d(TAG, "Starting BLE scan...");
        try {
            ScanCallback localScanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    BluetoothDevice device = result.getDevice();
                    if (device.getName() != null && device.getName().equals(DEVICE_NAME)) {
                        Log.d(TAG, "Found HMSoft: " + device.getName());
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                                    && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                                statusText.setText(R.string.need_permission_to_scan);
                                checkAndRequestPermissions();
                                return;
                            }
                            bluetoothLeScanner.stopScan(this);
                        } catch (SecurityException e) {
                            statusText.setText(R.string.need_permission_to_scan);
                            Log.d(TAG, "SecurityException in stopScan: " + e.getMessage());
                            checkAndRequestPermissions();
                            return;
                        }
                        connectToDevice(device);
                    }
                }
            };

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                        && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    statusText.setText(R.string.need_permission_to_scan);
                    checkAndRequestPermissions();
                    return;
                }
                bluetoothLeScanner.startScan(localScanCallback);
            } catch (SecurityException e) {
                statusText.setText(R.string.need_permission_to_scan);
                Log.d(TAG, "SecurityException in startScan: " + e.getMessage());
                checkAndRequestPermissions();
                return;
            }

            handler.postDelayed(() -> {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                            && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                        statusText.setText(R.string.need_permission_to_scan);
                        checkAndRequestPermissions();
                        return;
                    }
                    bluetoothLeScanner.stopScan(localScanCallback);
                    if (!isConnected) {
                        statusText.setText(R.string.device_not_found);
                        Log.d(TAG, "Scan stopped, HMSoft not found");
                        Toast.makeText(MainActivity.this, R.string.device_not_found, Toast.LENGTH_LONG).show();
                    }
                } catch (SecurityException e) {
                    statusText.setText(R.string.need_permission_to_scan);
                    Log.d(TAG, "SecurityException in stopScan: " + e.getMessage());
                    checkAndRequestPermissions();
                }
            }, SCAN_PERIOD);
        } catch (SecurityException e) {
            statusText.setText(R.string.need_permission_to_scan);
            Log.d(TAG, "SecurityException in startScan: " + e.getMessage());
            checkAndRequestPermissions();
        }
    }

    private void connectToDevice(BluetoothDevice device) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            statusText.setText(R.string.need_permission_to_connect);
            checkAndRequestPermissions();
            return;
        }

        statusText.setText(String.format(getString(R.string.connecting_to), DEVICE_NAME));
        Log.d(TAG, "Connecting to " + device.getName());
        try {
            bluetoothGatt = device.connectGatt(this, false, gattCallback);
        } catch (SecurityException e) {
            statusText.setText(R.string.need_permission_to_connect);
            Log.d(TAG, "SecurityException in connectGatt: " + e.getMessage());
            checkAndRequestPermissions();
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int gattStatus, int newState) {
            super.onConnectionStateChange(gatt, gattStatus, newState);
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                runOnUiThread(() -> {
                    statusText.setText(String.format(getString(R.string.connected_to), DEVICE_NAME));
                    isConnected = true;
                    btnConnect.setText(R.string.disconnect);
                    btnConnect.setBackgroundResource(R.drawable.rounded_button_disconnect);
                    Log.d(TAG, "Connected to HMSoft");
                });
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                            && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        runOnUiThread(() -> statusText.setText(R.string.need_permission_to_connect));
                        checkAndRequestPermissions();
                        return;
                    }
                    gatt.discoverServices();
                } catch (SecurityException e) {
                    runOnUiThread(() -> statusText.setText(R.string.need_permission_to_connect));
                    Log.d(TAG, "SecurityException in discoverServices: " + e.getMessage());
                    checkAndRequestPermissions();
                }
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                runOnUiThread(() -> {
                    statusText.setText(R.string.disconnected);
                    temperatureText.setText("Temperature: --.- °C");
                    isConnected = false;
                    isAutoMode = false;
                    isOn = false;
                    btnConnect.setText(R.string.connect_to_hmsoft);
                    btnConnect.setBackgroundResource(R.drawable.rounded_button);
                    btnAuto.setBackgroundResource(R.drawable.circle_button_off);
                    btnToggle.setText(R.string.off);
                    btnToggle.setBackgroundResource(R.drawable.circle_button_off);
                    Log.d(TAG, "Disconnected from HMSoft");
                });
                closeGatt();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int serviceStatus) {
            super.onServicesDiscovered(gatt, serviceStatus);
            if (serviceStatus == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                if (service != null) {
                    characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
                    if (characteristic != null) {
                        Log.d(TAG, "Found characteristic FFE1");
                        // Kích hoạt thông báo
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                                    && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                runOnUiThread(() -> statusText.setText(R.string.need_permission_to_connect));
                                checkAndRequestPermissions();
                                return;
                            }
                            gatt.setCharacteristicNotification(characteristic, true);
                            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                            if (descriptor != null) {
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                gatt.writeDescriptor(descriptor);
                                Log.d(TAG, "Enabled notifications for characteristic FFE1");
                            }
                        } catch (SecurityException e) {
                            runOnUiThread(() -> statusText.setText(R.string.need_permission_to_connect));
                            Log.d(TAG, "SecurityException in enabling notifications: " + e.getMessage());
                            checkAndRequestPermissions();
                        }
                    } else {
                        runOnUiThread(() -> statusText.setText(R.string.characteristic_not_found));
                        Log.d(TAG, "Characteristic FFE1 not found");
                    }
                } else {
                    runOnUiThread(() -> statusText.setText(R.string.service_not_found));
                    Log.d(TAG, "Service FFE0 not found");
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int writeStatus) {
            super.onCharacteristicWrite(gatt, characteristic, writeStatus);
            if (writeStatus == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Command sent successfully");
            } else {
                runOnUiThread(() -> statusText.setText(R.string.command_failed));
                Log.d(TAG, "Failed to send command");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                        && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    runOnUiThread(() -> statusText.setText(R.string.need_permission_to_connect));
                    checkAndRequestPermissions();
                    return;
                }
                String data = new String(characteristic.getValue());
                Log.d(TAG, "Received data: " + data);
                if (data.startsWith("TEMP:")) {
                    String tempValue = data.replace("TEMP:", "").trim();
                    runOnUiThread(() -> temperatureText.setText("Temperature: " + tempValue + " °C"));
                }
            } catch (SecurityException e) {
                runOnUiThread(() -> statusText.setText(R.string.need_permission_to_connect));
                Log.d(TAG, "SecurityException in onCharacteristicChanged: " + e.getMessage());
                checkAndRequestPermissions();
            }
        }

         

         


    
    private void sendCommand(String command) {
        if (isConnected && characteristic != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                statusText.setText(R.string.need_permission_to_connect);
                checkAndRequestPermissions();
                return;
            }
            characteristic.setValue(command);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                        && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    statusText.setText(R.string.need_permission_to_connect);
                    checkAndRequestPermissions();
                    return;
                }
                bluetoothGatt.writeCharacteristic(characteristic);
                if (!isAutoMode || command.equals("AUTO\n")) {
                    statusText.setText(String.format(getString(R.string.command_sent), command.trim()));
            Log.d(TAG, "Sent command: " + command.trim());
                }
            } catch (SecurityException e) {
                statusText.setText(R.string.need_permission_to_connect);
                Log.d(TAG, "SecurityException in writeCharacteristic: " + e.getMessage());
                checkAndRequestPermissions();
            }
        } else {
            statusText.setText(R.string.not_connected);
            Log.d(TAG, "Cannot send command: Not connected");
        }
    }

ate void disconnectFromBluetooth() {
        if (bluetoothGatt != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                statusText.setText(R.string.need_permission_to_connect);
                checkAndRequestPermissions();
                return;
            }
            try {
                bluetoothGatt.disconnect();
            } catch (SecurityException e) {
                statusText.setText(R.string.need_permission_to_connect);
                Log.d(TAG, "SecurityException in disconnect: " + e.getMessage());
                checkAndRequestPermissions();
            }
        }
    }

    private void closeGatt() {
        if (bluetoothGatt != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                statusText.setText(R.string.need_permission_to_connect);
                checkAndRequestPermissions();
                return;
            }
            try {
                bluetoothGatt.close();
                bluetoothGatt = null;
                characteristic = null;
            } catch (SecurityException e) {
                statusText.setText(R.string.need_permission_to_connect);
                Log.d(TAG, "SecurityException in closeGatt: " + e.getMessage());
                checkAndRequestPermissions();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeGatt();
        Log.d(TAG, "onDestroy: Resources closed");
    }
}
