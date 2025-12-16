package com.ossoftware.ossmartcontrol;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
        AddSwitchesDialog.OnSwitchesCreatedListener,
        SwitchGridAdapter.OnSwitchClickListener {

    // UI Components
    private TextView txtStatus;
    private LinearLayout connectionStatus;
    private GridView switchesGrid;
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
    private SwitchGridAdapter gridAdapter;

    // Current device being edited
    private String currentEditingDeviceId;

    // Permissions
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup ActionBar/Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Smart Home Control");
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.toolbar_blue));


        initializeViews();

        // Initialize managers
        bluetoothManager = new BluetoothManager(this, this);
        logManager = new LogManager(this, logsContainer, txtEmptyLogs, txtLogStats);
        dialogManager = new DeviceDialogManager(this, this);

        // Initialize preferences manager
        preferencesManager = new PreferencesManager(this);
        devices = preferencesManager.loadDevices();
        switchList = new ArrayList<>();

        // Initialize grid adapter
        gridAdapter = new SwitchGridAdapter(this, switchList, this);
        switchesGrid.setAdapter(gridAdapter);

        setupButtonListeners();
        loadSwitchesFromPreferences();

        // Check Bluetooth permissions
        checkBluetoothPermissions();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_bluetooth) {
            showBluetoothDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initializeViews() {
        // Main controls
        txtStatus = findViewById(R.id.txtStatus);
        connectionStatus = findViewById(R.id.connectionStatus);
        switchesGrid = findViewById(R.id.switchesGrid);
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
        btnClearLogs.setOnClickListener(v -> logManager.clearLogs());
        btnAddSwitches.setOnClickListener(v -> showAddSwitchesDialog());
    }

    private void showAddSwitchesDialog() {
        AddSwitchesDialog dialog = new AddSwitchesDialog(this, this);
        // Pre-fill with current switch count
        dialog.setCurrentCount(switchList.size());
        dialog.show();
    }

    @Override
    public void onSwitchesCreated(int count) {
        updateSwitchesCount(count);
    }

    private void updateSwitchesCount(int newCount) {
        if (newCount == switchList.size()) {
            Toast.makeText(this, "Number of switches is already " + newCount, Toast.LENGTH_SHORT).show();
            return;
        }

        if (newCount > switchList.size()) {
            // Add new switches
            addNewSwitches(newCount);
        } else {
            // Remove extra switches (keep first 'newCount' switches)
            removeExtraSwitches(newCount);
        }

        // Update grid adapter
        gridAdapter.updateAllSwitches(switchList);

        // Save to preferences
        saveSwitchesToPreferences();

        // Force refresh the grid
        switchesGrid.invalidateViews();
        switchesGrid.requestLayout();

        Toast.makeText(this, "Updated to " + newCount + " switches", Toast.LENGTH_SHORT).show();
    }

    private void addNewSwitches(int newCount) {
        int currentCount = switchList.size();

        for (int i = currentCount + 1; i <= newCount; i++) {
            DeviceModel device = new DeviceModel(
                    i,
                    "Switch " + i,
                    "SWITCH" + i + "_ON",
                    "SWITCH" + i + "_OFF"
            );

            switchList.add(device);
        }
    }

    private void removeExtraSwitches(int newCount) {
        // Keep only first 'newCount' switches
        if (newCount >= 1 && newCount <= switchList.size()) {
            // Create a new list with first 'newCount' switches
            List<DeviceModel> newList = new ArrayList<>();
            for (int i = 0; i < newCount; i++) {
                newList.add(switchList.get(i));
            }

            // Clear and add back the kept switches
            switchList.clear();
            switchList.addAll(newList);
        }
    }

    private void createSwitches(int count) {
        // Clear existing switches
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
        }

        // Update grid adapter
        gridAdapter.updateAllSwitches(switchList);

        // Save to preferences
        saveSwitchesToPreferences();

        // Force refresh the grid
        switchesGrid.invalidateViews();
        switchesGrid.requestLayout();

        Toast.makeText(this, "Created " + count + " switches", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSwitchClick(int position, DeviceModel device) {
        // Toggle switch state
        boolean newState = !device.isOn();
        device.setOn(newState);

        // Update UI
        gridAdapter.updateSwitchState(position, newState);

        if (bluetoothManager.isConnected()) {
            String command = newState ? device.getCommandOn() : device.getCommandOff();
            sendCommandToDevice(command, "SWITCH" + device.getIndex());
        }

        // Save state
        saveSwitchesToPreferences();
    }

    @Override
    public void onSwitchLongClick(int position, DeviceModel device) {
        // Show settings dialog
        showSwitchSettingsDialog(device);
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
            gridAdapter.updateAllSwitches(switchList);

            // Save to preferences
            saveSwitchesToPreferences();

            Toast.makeText(this, "Switch settings saved", Toast.LENGTH_SHORT).show();
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

        // If no switches found, create 4 by default (FIRST INSTALL)
        if (switchList.isEmpty()) {
            createDefaultSwitches();
        } else {
            // Update grid adapter
            gridAdapter.updateAllSwitches(switchList);
        }
    }

    private void createDefaultSwitches() {
        // Create 4 default switches for first-time users
        for (int i = 1; i <= 4; i++) {
            DeviceModel device = new DeviceModel(
                    i,
                    "Switch " + i,
                    "SWITCH" + i + "_ON",
                    "SWITCH" + i + "_OFF"
            );

            switchList.add(device);
        }

        // Update grid adapter
        gridAdapter.updateAllSwitches(switchList);

        // Save to preferences
        saveSwitchesToPreferences();

        // Force refresh the grid
        switchesGrid.invalidateViews();
        switchesGrid.requestLayout();

        Toast.makeText(this, "4 default switches created", Toast.LENGTH_SHORT).show();
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
                            gridAdapter.updateAllSwitches(switchList);
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