package com.ossoftware.ossmartcontrol;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements BluetoothManager.BluetoothListener,
        DeviceDialogManager.DeviceDialogListener,
        DeviceSettingsDialog.OnDeviceSettingsListener,
        AddSwitchesDialog.OnSwitchesCreatedListener {

    // UI Components
    private ImageView btnBluetooth;
    private TextView txtStatus;
    private LinearLayout connectionStatus;
    private LinearLayout switchesContainer;
    private Button btnAddSwitches;

    // Logs UI
    private LinearLayout logsContainer;
    private TextView txtEmptyLogs;
    private TextView txtLogStats;
    private Button btnClearLogs;

    // Managers
    private BluetoothManager bluetoothManager;
    private LogManager logManager;
    private DeviceDialogManager dialogManager;
    private PreferencesManager preferencesManager;

    // Device data
    private Map<String, DeviceModel> devices;
    private List<DeviceModel> switchList;

    // Current device being edited
    private String currentEditingDeviceId;

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

        // Initialize preferences manager
        preferencesManager = new PreferencesManager(this);
        devices = preferencesManager.loadDevices();
        switchList = new ArrayList<>();

        setupButtonListeners();
        loadSwitchesFromPreferences();

        // Check Bluetooth permissions
        checkBluetoothPermissions();
    }

    private void initializeViews() {
        // Main controls
        btnBluetooth = findViewById(R.id.btnBluetooth);
        txtStatus = findViewById(R.id.txtStatus);
        connectionStatus = findViewById(R.id.connectionStatus);
        switchesContainer = findViewById(R.id.switchesContainer);
        btnAddSwitches = findViewById(R.id.btnAddSwitches);

        // Logs
        logsContainer = findViewById(R.id.logsContainer);
        txtEmptyLogs = findViewById(R.id.txtEmptyLogs);
        txtLogStats = findViewById(R.id.txtLogStats);
        btnClearLogs = findViewById(R.id.btnClearLogs);

        // Initially hide status
        connectionStatus.setVisibility(View.GONE);
    }

    private void setupButtonListeners() {
        btnBluetooth.setOnClickListener(v -> showBluetoothDialog());
        btnClearLogs.setOnClickListener(v -> logManager.clearLogs());
        btnAddSwitches.setOnClickListener(v -> showAddSwitchesDialog());
    }

    private void showAddSwitchesDialog() {
        AddSwitchesDialog dialog = new AddSwitchesDialog(this, this);
        dialog.show();
    }

    @Override
    public void onSwitchesCreated(int count) {
        createSwitches(count);
    }

    private void createSwitches(int count) {
        // Clear existing switches
        switchesContainer.removeAllViews();
        switchList.clear();

        // Create new switches
        for (int i = 1; i <= count; i++) {
            DeviceModel device = new DeviceModel(
                    i,
                    "Switch " + i,
                    "SWITCH" + i + "_ON",
                    "SWITCH" + i + "_OFF"
            );

            switchList.add(device);

            // Create switch view
            View switchView = LayoutInflater.from(this).inflate(R.layout.item_switch, switchesContainer, false);

            TextView txtSwitchName = switchView.findViewById(R.id.txtSwitchName);
            RelativeLayout btnSwitchToggle = switchView.findViewById(R.id.btnSwitchToggle);
            TextView txtSwitchState = switchView.findViewById(R.id.txtSwitchState);

            txtSwitchName.setText(device.getName());
            updateSwitchUI(btnSwitchToggle, txtSwitchState, device.isOn());

            final int switchIndex = i;
            final DeviceModel currentDevice = device;

            // Toggle on click
            btnSwitchToggle.setOnClickListener(v -> {
                boolean newState = !currentDevice.isOn();
                currentDevice.setOn(newState);
                updateSwitchUI(btnSwitchToggle, txtSwitchState, newState);

                if (bluetoothManager.isConnected()) {
                    String command = newState ? currentDevice.getCommandOn() : currentDevice.getCommandOff();
                    sendCommandToDevice(command, "SWITCH" + switchIndex);
                }
            });

            // Long click for settings
            switchView.setOnLongClickListener(v -> {
                showSwitchSettingsDialog(currentDevice);
                return true;
            });

            switchesContainer.addView(switchView);
        }

        // Save to preferences
        saveSwitchesToPreferences();

        Toast.makeText(this, "Created " + count + " switches", Toast.LENGTH_SHORT).show();
    }

    private void updateSwitchUI(RelativeLayout switchButton, TextView stateText, boolean isOn) {
        if (isOn) {
            switchButton.setBackgroundResource(R.drawable.bg_switch_on);
            stateText.setText("ON");
        } else {
            switchButton.setBackgroundResource(R.drawable.bg_switch_off);
            stateText.setText("OFF");
        }
    }

    private void showSwitchSettingsDialog(DeviceModel device) {
        currentEditingDeviceId = device.getId();

        DeviceSettingsDialog dialog = new DeviceSettingsDialog(this, device, this);
        dialog.show();
    }

    @Override
    public void onDeviceSettingsSaved(DeviceModel updatedDevice) {
        if (currentEditingDeviceId != null) {
            // Update in list
            for (int i = 0; i < switchList.size(); i++) {
                if (switchList.get(i).getId().equals(currentEditingDeviceId)) {
                    switchList.set(i, updatedDevice);
                    break;
                }
            }

            // Update UI
            updateSwitchInUI(updatedDevice);

            // Save to preferences
            saveSwitchesToPreferences();

            Toast.makeText(this, "Switch settings saved", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSwitchInUI(DeviceModel device) {
        for (int i = 0; i < switchesContainer.getChildCount(); i++) {
            View switchView = switchesContainer.getChildAt(i);
            TextView txtSwitchName = switchView.findViewById(R.id.txtSwitchName);
            RelativeLayout btnSwitchToggle = switchView.findViewById(R.id.btnSwitchToggle);
            TextView txtSwitchState = switchView.findViewById(R.id.txtSwitchState);

            if (txtSwitchName.getText().toString().equals(device.getName())) {
                txtSwitchName.setText(device.getName());
                updateSwitchUI(btnSwitchToggle, txtSwitchState, device.isOn());
                break;
            }
        }
    }

    private void saveSwitchesToPreferences() {
        Map<String, DeviceModel> allDevices = new HashMap<>(devices);

        // Clear existing switches
        for (String key : allDevices.keySet().toArray(new String[0])) {
            if (key.startsWith("SWITCH_")) {
                allDevices.remove(key);
            }
        }

        // Add current switches
        for (DeviceModel device : switchList) {
            allDevices.put(device.getId(), device);
        }

        preferencesManager.saveDevices(allDevices);
        devices = allDevices;
    }

    private void loadSwitchesFromPreferences() {
        // Clear existing
        switchList.clear();

        // Find all switches
        for (Map.Entry<String, DeviceModel> entry : devices.entrySet()) {
            if (entry.getKey().startsWith("SWITCH_")) {
                switchList.add(entry.getValue());
            }
        }

        // Sort by index
        switchList.sort((d1, d2) -> Integer.compare(d1.getIndex(), d2.getIndex()));

        // If no switches found, create 4 by default
        if (switchList.isEmpty()) {
            createSwitches(4);
        } else {
            // Recreate UI from loaded switches
            recreateSwitchesUI();
        }
    }

    private void recreateSwitchesUI() {
        switchesContainer.removeAllViews();

        for (DeviceModel device : switchList) {
            View switchView = LayoutInflater.from(this).inflate(R.layout.item_switch, switchesContainer, false);

            TextView txtSwitchName = switchView.findViewById(R.id.txtSwitchName);
            RelativeLayout btnSwitchToggle = switchView.findViewById(R.id.btnSwitchToggle);
            TextView txtSwitchState = switchView.findViewById(R.id.txtSwitchState);

            txtSwitchName.setText(device.getName());
            updateSwitchUI(btnSwitchToggle, txtSwitchState, device.isOn());

            final DeviceModel currentDevice = device;
            final int switchIndex = device.getIndex();

            // Toggle on click
            btnSwitchToggle.setOnClickListener(v -> {
                boolean newState = !currentDevice.isOn();
                currentDevice.setOn(newState);
                updateSwitchUI(btnSwitchToggle, txtSwitchState, newState);

                if (bluetoothManager.isConnected()) {
                    String command = newState ? currentDevice.getCommandOn() : currentDevice.getCommandOff();
                    sendCommandToDevice(command, "SWITCH" + switchIndex);
                }

                // Save state
                saveSwitchesToPreferences();
            });

            // Long click for settings
            switchView.setOnLongClickListener(v -> {
                showSwitchSettingsDialog(currentDevice);
                return true;
            });

            switchesContainer.addView(switchView);
        }
    }

    /* =====================
       SEND COMMAND TO DEVICE
       ===================== */
    private void sendCommandToDevice(String command, String deviceId) {
        if (bluetoothManager.isConnected()) {
            bluetoothManager.sendCommand(command);

            // Log the command
            logManager.addLog("Command sent: " + command, LogManager.LogType.SENT,
                    bluetoothManager.getConnectedDeviceName());

            Toast.makeText(this, "Command sent: " + command, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Please connect to a device first", Toast.LENGTH_SHORT).show();
        }
    }

    /* =====================
       HANDLE DEVICE MESSAGES
       ===================== */
    private void handleDeviceMessage(String message) {
        // Handle messages from device
        if (message.contains(":")) {
            String[] parts = message.split(":");
            if (parts.length == 2) {
                String device = parts[0];
                String state = parts[1];

                boolean isOn = state.equals("ON");

                runOnUiThread(() -> {
                    // Check if it's one of our switches
                    for (DeviceModel switchDevice : switchList) {
                        if (device.equals("SWITCH" + switchDevice.getIndex())) {
                            switchDevice.setOn(isOn);
                            updateSwitchStateInUI(switchDevice.getIndex(), isOn);
                            break;
                        }
                    }
                });
            }
        }
        // Handle error messages
        else if (message.startsWith("ERROR:")) {
            logManager.addLog(message, LogManager.LogType.ERROR,
                    bluetoothManager.getConnectedDeviceName());
        }
    }

    private void updateSwitchStateInUI(int switchIndex, boolean isOn) {
        for (int i = 0; i < switchesContainer.getChildCount(); i++) {
            View switchView = switchesContainer.getChildAt(i);
            TextView txtSwitchName = switchView.findViewById(R.id.txtSwitchName);

            if (txtSwitchName.getText().toString().equals("Switch " + switchIndex)) {
                RelativeLayout btnSwitchToggle = switchView.findViewById(R.id.btnSwitchToggle);
                TextView txtSwitchState = switchView.findViewById(R.id.txtSwitchState);
                updateSwitchUI(btnSwitchToggle, txtSwitchState, isOn);

                // Update in list
                for (DeviceModel device : switchList) {
                    if (device.getIndex() == switchIndex) {
                        device.setOn(isOn);
                        break;
                    }
                }

                break;
            }
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

        // Get connected device info
        BluetoothDevice device = bluetoothManager.getConnectedDevice();
        if (device != null) {
            String connectedName = device.getName();
            logManager.addLog("Connected to: " + connectedName, LogManager.LogType.INFO, "");
            Toast.makeText(this, "Connected to: " + connectedName, Toast.LENGTH_SHORT).show();
        }

        // Close dialog after delay
        new Handler().postDelayed(() -> {
            if (dialogManager.isDialogShowing()) {
                dialogManager.dismissDialog();
            }
        }, 1500);
    }

    @Override
    public void onDisconnected() {
        connectionStatus.setVisibility(View.GONE);
    }

    @Override
    public void onConnectionError(String error) {
        connectionStatus.setVisibility(View.GONE);

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