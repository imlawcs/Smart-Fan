package com.midterm22nh12.fan;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.midterm22nh12.fan.R;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;
import android.Manifest;

public class MainActivity extends Activity {

    // UI components
    private Button btnAuto, btnOn1, btnOn2, btnToggle, btnConnect;
    private TextView status;

    // State variables
    private boolean isOn = false;       // Toggle button state (ON/OFF)
    private boolean isAutoMode = false; // AUTO mode state
    private boolean isConnected = false;// Bluetooth connection state

    // Bluetooth components
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private static final String DEVICE_NAME = "HMSoft"; // HM-10 device name
    private static final UUID BT_MODULE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // SPP UUID

    // Permission request code
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        status = findViewById(R.id.status);
        btnAuto = findViewById(R.id.btnAuto);
        btnOn1 = findViewById(R.id.btnOn1);
        btnOn2 = findViewById(R.id.btnOn2);
        btnToggle = findViewById(R.id.btnToggle);
        btnConnect = findViewById(R.id.btnConnect);

        // Initialize Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Check and request permissions
        checkAndRequestPermissions();

        // Set up button listeners
        btnConnect.setOnClickListener(v -> {
            if (!isConnected) {
                Log.d(TAG, "Attempting to connect to HMSoft...");
                connectToBluetooth();
            } else {
                Log.d(TAG, "Attempting to disconnect from HMSoft...");
                disconnectFromBluetooth();
            }
        });

        btnAuto.setOnClickListener(v -> {
            isAutoMode = !isAutoMode;
            if (isAutoMode) {
                sendCommand("AUTO\n");
                status.setText("AUTO Mode ON");
                btnAuto.setBackgroundResource(R.drawable.rounded_button);
            } else {
                sendCommand("OFF\n");
                status.setText("AUTO Mode OFF");
                btnToggle.setText("OFF");
                btnToggle.setBackgroundResource(R.drawable.circle_button_off);
                isOn = false;
            }
        });

        btnOn1.setOnClickListener(v -> {
            isAutoMode = false;
            sendCommand("ON1\n");
            btnToggle.setText("ON");
            btnToggle.setBackgroundResource(R.drawable.circle_button_on);
            isOn = true;
        });

        btnOn2.setOnClickListener(v -> {
            isAutoMode = false;
            sendCommand("ON2\n");
            btnToggle.setText("ON");
            btnToggle.setBackgroundResource(R.drawable.circle_button_on);
            isOn = true;
        });

        btnToggle.setOnClickListener(v -> {
            isAutoMode = false;
            if (isOn) {
                sendCommand("OFF\n");
                btnToggle.setText("OFF");
                btnToggle.setBackgroundResource(R.drawable.circle_button_off);
                isOn = false;
            } else {
                sendCommand("ON1\n");
                btnToggle.setText("ON");
                btnToggle.setBackgroundResource(R.drawable.circle_button_on);
                isOn = true;
            }
        });
    }

    private void checkAndRequestPermissions() {
        ArrayList<String> permissionsToRequest = new ArrayList<>();

        // Request BLUETOOTH_CONNECT and BLUETOOTH_SCAN only on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT);
                Log.d(TAG, "Requesting BLUETOOTH_CONNECT permission");
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN);
                Log.d(TAG, "Requesting BLUETOOTH_SCAN permission");
            }
        } else {
            Log.d(TAG, "Android version < 12, using BLUETOOTH and BLUETOOTH_ADMIN permissions");
        }

        // Request location permissions (required for Bluetooth scanning on Android 10+)
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
        } else {
            Log.d(TAG, "All permissions granted, attempting to connect...");
            connectToBluetooth();
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
                status.setText("Quyền đã được cấp");
                Log.d(TAG, "Permissions granted, attempting to connect...");
                connectToBluetooth();
            } else {
                status.setText("Cần cấp quyền để kết nối");
                Log.d(TAG, "Permissions denied");
                boolean shouldShowRationale = false;
                for (String permission : permissions) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                        shouldShowRationale = true;
                        break;
                    }
                }
                if (!shouldShowRationale) {
                    Toast.makeText(this, "Quyền bị từ chối. Vui lòng cấp quyền trong Cài đặt > Ứng dụng > Quyền", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Vui lòng cấp quyền để tiếp tục", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void connectToBluetooth() {
        // Check if Bluetooth is available and enabled
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            status.setText("Bluetooth không khả dụng");
            Log.d(TAG, "Bluetooth is not available or not enabled");
            Toast.makeText(this, "Vui lòng bật Bluetooth", Toast.LENGTH_LONG).show();
            return;
        }

        // Check permissions for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                status.setText("Không có quyền Bluetooth");
                Log.d(TAG, "Bluetooth permission not granted");
                checkAndRequestPermissions();
                return;
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                status.setText("Không có quyền quét Bluetooth");
                Log.d(TAG, "Bluetooth scan permission not granted");
                checkAndRequestPermissions();
                return;
            }
        }

        // Check location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            status.setText("Không có quyền Vị trí");
            Log.d(TAG, "Location permission not granted");
            checkAndRequestPermissions();
            return;
        }

        // Look for HMSoft in paired devices
        boolean deviceFound = false;
        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
            if (device.getName() != null && device.getName().equals(DEVICE_NAME)) {
                deviceFound = true;
                Log.d(TAG, "Found HMSoft in paired devices");
                try {
                    bluetoothSocket = device.createRfcommSocketToServiceRecord(BT_MODULE_UUID);
                    Log.d(TAG, "Attempting to connect to HMSoft...");
                    bluetoothSocket.connect();
                    outputStream = bluetoothSocket.getOutputStream();
                    runOnUiThread(() -> {
                        status.setText("Đã kết nối tới " + DEVICE_NAME);
                        isConnected = true;
                        btnConnect.setText("Ngắt kết nối");
                        btnConnect.setBackgroundResource(R.drawable.rounded_button_disconnect);
                        Log.d(TAG, "Connected successfully, button text updated to 'Ngắt kết nối'");
                    });
                } catch (IOException e) {
                    runOnUiThread(() -> {
                        status.setText("Kết nối thất bại");
                        Log.d(TAG, "Connection failed: " + e.getMessage());
                        Toast.makeText(this, "Kết nối thất bại. Vui lòng kiểm tra HMSoft.", Toast.LENGTH_LONG).show();
                    });
                    closeResources();
                    e.printStackTrace();
                }
                return;
            }
        }

        if (!deviceFound) {
            runOnUiThread(() -> {
                status.setText("HMSoft chưa được ghép nối");
                Log.d(TAG, "HMSoft not found in paired devices");
                Toast.makeText(this, "Vui lòng ghép nối với HMSoft trong Cài đặt Bluetooth", Toast.LENGTH_LONG).show();
            });
        }
    }

    private void disconnectFromBluetooth() {
        closeResources();
        runOnUiThread(() -> {
            status.setText("Đã ngắt kết nối");
            isConnected = false;
            btnConnect.setText("Kết nối tới HMSoft");
            btnConnect.setBackgroundResource(R.drawable.rounded_button);
            Log.d(TAG, "Disconnected successfully, button text updated to 'Kết nối tới HMSoft'");

            // Automatically enable AUTO mode after disconnect
            isAutoMode = true;
            sendCommand("AUTO\n");
            status.setText("AUTO Mode ON (Đã ngắt kết nối)");
            btnAuto.setBackgroundResource(R.drawable.rounded_button);
        });
    }

    private void sendCommand(String command) {
        if (outputStream != null && isConnected) {
            try {
                outputStream.write(command.getBytes());
                if (!isAutoMode) {
                    status.setText("Đã gửi: " + command.trim());
                    Log.d(TAG, "Sent command: " + command.trim());
                }
            } catch (IOException e) {
                status.setText("Gửi lệnh thất bại");
                Log.d(TAG, "Failed to send command: " + e.getMessage());
                closeResources();
                e.printStackTrace();
            }
        } else {
            status.setText("Chưa kết nối");
            Log.d(TAG, "Cannot send command: Not connected");
        }
    }

    private void closeResources() {
        try {
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
                Log.d(TAG, "OutputStream closed successfully");
            }
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
                bluetoothSocket = null;
                Log.d(TAG, "BluetoothSocket closed successfully");
            }
        } catch (IOException e) {
            Log.d(TAG, "Failed to close resources: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeResources();
        Log.d(TAG, "onDestroy: Resources closed");
    }
}