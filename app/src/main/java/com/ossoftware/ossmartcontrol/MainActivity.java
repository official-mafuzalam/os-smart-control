package com.ossoftware.ossmartcontrol;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements BluetoothManager.BluetoothListener,
        DeviceDialogManager.DeviceDialogListener {

    // UI Components
    private ImageView btnBluetooth;
    private TextView txtStatus;
    private MaterialSwitch switchFan, switchLight1, switchLight2, switchLight3;
    private LinearLayout connectionStatus;

    // Logs UI
    private LinearLayout logsContainer;
    private TextView txtEmptyLogs;
    private TextView txtLogStats;
    private Button btnClearLogs;

    // Managers
    private BluetoothManager bluetoothManager;
    private LogManager logManager;
    private DeviceDialogManager dialogManager;

    // Permissions
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();

        // Initialize managers
        bluetoothManager = new BluetoothManager(this, this);
        logManager = new LogManager(this, logsContainer, txtEmptyLogs, txtLogStats);
        dialogManager = new DeviceDialogManager(this, this);

        setupSwitchListeners();
        setupButtonListeners();

        // Check Bluetooth permissions
        checkBluetoothPermissions();
    }

    private void initializeViews() {
        // Main controls
        btnBluetooth = findViewById(R.id.btnBluetooth);
        txtStatus = findViewById(R.id.txtStatus);
        connectionStatus = findViewById(R.id.connectionStatus);

        // Switches
        switchFan = findViewById(R.id.switchFan);
        switchLight1 = findViewById(R.id.switchLight1);
        switchLight2 = findViewById(R.id.switchLight2);
        switchLight3 = findViewById(R.id.switchLight3);

        // Logs
        logsContainer = findViewById(R.id.logsContainer);
        txtEmptyLogs = findViewById(R.id.txtEmptyLogs);
        txtLogStats = findViewById(R.id.txtLogStats);
        btnClearLogs = findViewById(R.id.btnClearLogs);

        // Initially hide status and disable switches
        connectionStatus.setVisibility(View.GONE);
        enableSwitches(false);
    }

    private void setupButtonListeners() {
        btnBluetooth.setOnClickListener(v -> showBluetoothDialog());
        btnClearLogs.setOnClickListener(v -> logManager.clearLogs());
    }

    private void setupSwitchListeners() {
        switchFan.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bluetoothManager.isConnected()) {
                // Convert to Arduino format: FAN:ON -> FAN_ON
                String command = isChecked ? "FAN:ON" : "FAN:OFF";
                sendCommand(command);
            } else {
                Toast.makeText(this, "Please connect to a device first", Toast.LENGTH_SHORT).show();
                switchFan.setChecked(false);
            }
        });

        switchLight1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bluetoothManager.isConnected()) {
                // Convert to Arduino format: LIGHT1:ON -> LIGHT1_ON
                String command = isChecked ? "LIGHT1:ON" : "LIGHT1:OFF";
                sendCommand(command);
            } else {
                Toast.makeText(this, "Please connect to a device first", Toast.LENGTH_SHORT).show();
                switchLight1.setChecked(false);
            }
        });

        switchLight2.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bluetoothManager.isConnected()) {
                // Convert to Arduino format: LIGHT2:ON -> LIGHT2_ON
                String command = isChecked ? "LIGHT2:ON" : "LIGHT2:OFF";
                sendCommand(command);
            } else {
                Toast.makeText(this, "Please connect to a device first", Toast.LENGTH_SHORT).show();
                switchLight2.setChecked(false);
            }
        });

        switchLight3.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (bluetoothManager.isConnected()) {
                // Convert to Arduino format: LIGHT3:ON -> LIGHT3_ON
                String command = isChecked ? "LIGHT3:ON" : "LIGHT3:OFF";
                sendCommand(command);
            } else {
                Toast.makeText(this, "Please connect to a device first", Toast.LENGTH_SHORT).show();
                switchLight3.setChecked(false);
            }
        });
    }

    /* =====================
       CONVERT TO ARDUINO FORMAT
       ===================== */
    private String convertToArduinoFormat(String command) {
        if (command.contains(":")) {
            String[] parts = command.split(":");
            if (parts.length == 2) {
                String device = parts[0].toUpperCase();
                String state = parts[1].toUpperCase();

                // Convert DEVICE:STATE to DEVICE_STATE format
                return device + "_" + state;
            }
        }
        return command; // Return as-is if no colon found
    }

    /* =====================
       SEND COMMAND TO DEVICE
       ===================== */
    private void sendCommand(String command) {
        if (bluetoothManager.isConnected()) {
            // Convert command to Arduino format
            String arduinoCommand = convertToArduinoFormat(command);

            bluetoothManager.sendCommand(arduinoCommand);

            // Log the actual command sent to Arduino
            logManager.addLog("Command sent: " + arduinoCommand, LogManager.LogType.SENT,
                    bluetoothManager.getConnectedDeviceName());
            Toast.makeText(this, "Command sent: " + arduinoCommand, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Please connect to a device first", Toast.LENGTH_SHORT).show();
        }
    }

    /* =====================
       HANDLE DEVICE MESSAGES
       ===================== */
    private void handleDeviceMessage(String message) {
        // Parse and handle messages from device
        // Arduino sends back LIGHT1:ON or LIGHT1:OFF format
        if (message.contains(":")) {
            String[] parts = message.split(":");
            if (parts.length == 2) {
                String device = parts[0];
                String state = parts[1];

                boolean isOn = state.equals("ON");

                runOnUiThread(() -> {
                    switch (device) {
                        case "FAN":
                            switchFan.setChecked(isOn);
                            break;
                        case "LIGHT1":
                            switchLight1.setChecked(isOn);
                            break;
                        case "LIGHT2":
                            switchLight2.setChecked(isOn);
                            break;
                        case "LIGHT3":
                            switchLight3.setChecked(isOn);
                            break;
                        case "STATUS":
                            // Handle status message
                            // Format: STATUS:L1=1:L2=0:FAN=1
                            parseStatusMessage(message);
                            break;
                    }
                });
            }
        }
        // Also handle error messages from Arduino
        else if (message.startsWith("ERROR:")) {
            logManager.addLog(message, LogManager.LogType.ERROR,
                    bluetoothManager.getConnectedDeviceName());
        }
        // Handle ALL:ON or ALL:OFF responses
        else if (message.equals("ALL:ON") || message.equals("ALL:OFF")) {
            boolean isOn = message.equals("ALL:ON");
            runOnUiThread(() -> {
                switchFan.setChecked(isOn);
                switchLight1.setChecked(isOn);
                switchLight2.setChecked(isOn);
                switchLight3.setChecked(isOn);
            });
        }
    }

    /* =====================
       PARSE STATUS MESSAGE FROM ARDUINO
       ===================== */
    private void parseStatusMessage(String statusMessage) {
        // Format: STATUS:L1=1:L2=0:FAN=1
        try {
            String[] parts = statusMessage.split(":");
            for (String part : parts) {
                if (part.contains("=")) {
                    String[] keyValue = part.split("=");
                    if (keyValue.length == 2) {
                        String key = keyValue[0];
                        int value = Integer.parseInt(keyValue[1]);
                        boolean isOn = value == 1;

                        runOnUiThread(() -> {
                            switch (key) {
                                case "L1":
                                    switchLight1.setChecked(isOn);
                                    break;
                                case "L2":
                                    switchLight2.setChecked(isOn);
                                    break;
                                case "FAN":
                                    switchFan.setChecked(isOn);
                                    break;
                                case "L3":
                                    switchLight3.setChecked(isOn);
                                    break;
                            }
                        });
                    }
                }
            }
        } catch (Exception e) {
            logManager.addLog("Error parsing status: " + e.getMessage(),
                    LogManager.LogType.ERROR, "");
        }
    }

    /* =====================
       BLUETOOTH DIALOG
       ===================== */
    public void showBluetoothDialog() {
        // Check permissions first
        if (!checkPermissions()) {
            checkBluetoothPermissions();
            Toast.makeText(this, "Please grant Bluetooth permissions first", Toast.LENGTH_SHORT).show();
            return;
        }

        dialogManager.showDialog();
    }

    /* =====================
       DEVICE DIALOG LISTENER
       ===================== */
    @Override
    public void onScanRequested() {
        bluetoothManager.clearDiscoveredDevices();
        logManager.addLog("Scanning started", LogManager.LogType.INFO, "");
        bluetoothManager.startScanning();
    }

    @Override
    public void onStopScanRequested() {
        // Nothing needed here as BluetoothManager handles its own scanning
    }

    @Override
    public void onDeviceConnectRequested(String name, String mac) {
        dialogManager.updateScanStatus("Connecting to " + name + "...", android.R.color.holo_orange_dark);
        logManager.addLog("Connecting to " + name + "...", LogManager.LogType.INFO, "");
        bluetoothManager.connectToDevice(name, mac);
    }

    /* =====================
       BLUETOOTH LISTENER
       ===================== */
    @Override
    public void onDeviceFound(String name, String mac) {
        dialogManager.addDevice(name, mac);
        logManager.addLog("Found device: " + name, LogManager.LogType.INFO, "");
    }

    @Override
    public void onScanStarted() {
        // UI already updated by dialog manager
    }

    @Override
    public void onScanFinished(int deviceCount) {
        dialogManager.stopScanning();
        logManager.addLog("Scanning finished. Found " + deviceCount + " devices", LogManager.LogType.INFO, "");
    }

    @Override
    public void onScanError(String error) {
        dialogManager.stopScanning();
        dialogManager.updateScanStatus("Error: " + error, android.R.color.holo_red_dark);
        logManager.addLog("Scan error: " + error, LogManager.LogType.ERROR, "");
        Toast.makeText(this, "Scan error: " + error, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnected(String deviceName, String mac) {
        // Update UI
        connectionStatus.setVisibility(View.VISIBLE);
        txtStatus.setText("Connected to: " + deviceName);
        txtStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));

        // Enable switches
        enableSwitches(true);

        // Get connected device info
        BluetoothDevice device = bluetoothManager.getConnectedDevice();
        if (device != null) {
            String connectedName = device.getName();
            logManager.addLog("Connected to: " + connectedName, LogManager.LogType.INFO, "");
            Toast.makeText(this, "Connected to: " + connectedName, Toast.LENGTH_SHORT).show();

            // Send STATUS command to sync switch states
            sendCommand("STATUS");
        }

        // Close dialog after delay
        new android.os.Handler().postDelayed(() -> {
            if (dialogManager.isDialogShowing()) {
                dialogManager.dismissDialog();
            }
        }, 1500);
    }

    @Override
    public void onDisconnected() {
        connectionStatus.setVisibility(View.GONE);
        enableSwitches(false);
    }

    @Override
    public void onConnectionError(String error) {
        connectionStatus.setVisibility(View.GONE);
        enableSwitches(false);

        if (dialogManager.isDialogShowing()) {
            dialogManager.updateScanStatus("Connection failed: " + error, android.R.color.holo_red_dark);
        }

        logManager.addLog("Connection failed: " + error, LogManager.LogType.ERROR, "");
        Toast.makeText(this, "Connection failed: " + error, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onMessageReceived(String message) {
        handleDeviceMessage(message);
        logManager.addLog("Received: " + message, LogManager.LogType.RECEIVED,
                bluetoothManager.getConnectedDeviceName());
        // Toast message is now optional - can comment out if too many toasts
        // Toast.makeText(this, "Received: " + message, Toast.LENGTH_SHORT).show();
    }

    private void enableSwitches(boolean enable) {
        switchFan.setEnabled(enable);
        switchLight1.setEnabled(enable);
        switchLight2.setEnabled(enable);
        switchLight3.setEnabled(enable);

        if (!enable) {
            // Reset switches when disconnected
            switchFan.setChecked(false);
            switchLight1.setChecked(false);
            switchLight2.setChecked(false);
            switchLight3.setChecked(false);
        }
    }

    /* =====================
       PERMISSIONS
       ===================== */

    private boolean checkPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void checkBluetoothPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    REQUEST_BLUETOOTH_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                Toast.makeText(this, "Bluetooth permissions are required to use this feature", Toast.LENGTH_LONG).show();
            } else {
                // Permissions granted, show dialog
                showBluetoothDialog();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clean up managers
        if (bluetoothManager != null) {
            bluetoothManager.disconnect();
        }

        if (dialogManager != null) {
            dialogManager.dismissDialog();
        }
    }
}