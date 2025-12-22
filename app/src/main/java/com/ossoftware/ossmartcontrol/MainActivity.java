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

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements BluetoothManager.BluetoothListener,
        DeviceDialogManager.DeviceDialogListener,
        DeviceSettingsDialog.OnDeviceSettingsListener,
        AddSwitchesDialog.OnSwitchesCreatedListener,
        SwitchGridAdapter.OnSwitchClickListener,
        VoiceManager.VoiceResultListener {

    // UI Components
    private TextView txtStatus;
    private LinearLayout connectionStatus;
    private GridView switchesGrid;
    private TextView txtListeningStatus;
    private MaterialCardView cardStatus;

    // Logs UI
    private LinearLayout logsContainer;
    private TextView txtEmptyLogs;
    private TextView txtLogStats;
    private Button btnClearLogs;

    // Managers
    private BluetoothManager bluetoothManager;
    public LogManager logManager; // Changed to public for VoiceManager access
    private DeviceDialogManager dialogManager;
    private PreferencesManager preferencesManager;
    private VoiceManager voiceManager;

    // Device data
    private Map<String, DeviceModel> devices;
    private List<DeviceModel> switchList;
    private SwitchGridAdapter gridAdapter;

    // Current device being edited
    private String currentEditingDeviceId;

    // Permissions
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 100;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Setup ActionBar/Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Smart Home Control");
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.success));

        initializeViews();

        // Initialize permission launcher
        initializePermissionLauncher();

        // Initialize managers
        bluetoothManager = new BluetoothManager(this, this);
        logManager = new LogManager(this, logsContainer, txtEmptyLogs, txtLogStats);
        dialogManager = new DeviceDialogManager(this, this);

        // Initialize voice manager
        voiceManager = new VoiceManager(this, txtListeningStatus, cardStatus);
        voiceManager.setVoiceResultListener(this);

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

    private void initializePermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // Permission granted, start voice recognition
                        startVoiceRecognition();
                    } else {
                        // Permission denied
                        showSafeToast("Voice permission denied. Voice control won't work.");
                    }
                }
        );
    }

    private void startVoiceRecognition() {
        if (!bluetoothManager.isConnected()) {
            showSafeToast("Please connect to a Bluetooth device first");
            return;
        }

        // Start voice recognition
        voiceManager.startListening();
        logManager.addLog("Voice recognition started", LogManager.LogType.INFO, "");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        MenuItem connectBluetooth = menu.add("Connect Device");
        MenuItem editSwitch = menu.add("Edit Switches");
        MenuItem voiceHelp = menu.add("Voice Commands");
        voiceHelp.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        editSwitch.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        connectBluetooth.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        connectBluetooth.setOnMenuItemClickListener(item -> {
            showBluetoothDialog();
            return true;
        });
        editSwitch.setOnMenuItemClickListener(menuItem -> {
            showAddSwitchesDialog();
            return true;
        });
        voiceHelp.setOnMenuItemClickListener(item -> {
            showVoiceCommandsHelp();
            return true;
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_voice) {
            toggleVoiceControl();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initializeViews() {
        // Main controls
        txtStatus = findViewById(R.id.txtStatus);
        connectionStatus = findViewById(R.id.connectionStatus);
        switchesGrid = findViewById(R.id.switchesGrid);

        // Voice control views
        cardStatus = findViewById(R.id.cardStatus);
        txtListeningStatus = findViewById(R.id.txtListeningStatus);

        // Logs
        logsContainer = findViewById(R.id.logsContainer);
        txtEmptyLogs = findViewById(R.id.txtEmptyLogs);
        txtLogStats = findViewById(R.id.txtLogStats);
        btnClearLogs = findViewById(R.id.btnClearLogs);

        // Initially hide status
        connectionStatus.setVisibility(View.GONE);
        cardStatus.setVisibility(View.GONE);
        txtListeningStatus.setVisibility(View.GONE);
    }

    private void setupButtonListeners() {
        btnClearLogs.setOnClickListener(v -> logManager.clearLogs());

    }

    // Voice control methods
    private void toggleVoiceControl() {
        if (!bluetoothManager.isConnected()) {
            showSafeToast("Please connect to a Bluetooth device first");
            return;
        }

        // Check and request voice permission
        if (!checkVoicePermissions()) {
            requestVoicePermissions();
            return;
        }

        startVoiceRecognition();
    }

    // Handle voice command results
    @Override
    public void onVoiceCommandRecognized(String command) {
        runOnUiThread(() -> {
            if (command.equals("UNKNOWN_COMMAND")) {
                showSafeToast("Command not recognized. Try 'Turn on light one' or 'All lights off'");
                logManager.addLog("Voice: Unknown command", LogManager.LogType.INFO, "");
                return;
            }

            logManager.addLog("Voice command: " + command, LogManager.LogType.SENT, "");

            // Process the command
            processVoiceCommand(command);
        });
    }

    @Override
    public void onVoiceError(String error) {
        runOnUiThread(() -> {
            showSafeToast("Voice error: " + error);
            logManager.addLog("Voice error: " + error, LogManager.LogType.ERROR, "");
        });
    }

    private void processVoiceCommand(String command) {
        if (!bluetoothManager.isConnected()) {
            showSafeToast("Not connected to Bluetooth device");
            return;
        }

        // Handle different command types
        if (command.startsWith("LIGHT") && command.endsWith("_ON")) {
            handleLightCommand(command, true);
        } else if (command.startsWith("LIGHT") && command.endsWith("_OFF")) {
            handleLightCommand(command, false);
        } else if (command.startsWith("LIGHT") && command.endsWith("_TOGGLE")) {
            handleToggleCommand(command);
        } else if (command.equals("ALL_LIGHTS_ON")) {
            handleAllLightsCommand(true);
        } else if (command.equals("ALL_LIGHTS_OFF")) {
            handleAllLightsCommand(false);
        } else if (command.equals("STATUS")) {
            sendCommandToDevice("STATUS");
            showSafeToast("Requesting status");
            logManager.addLog("Voice: Request status",
                    LogManager.LogType.SENT,
                    bluetoothManager.getConnectedDeviceName());
        } else if (command.equals("HELP")) {
            showVoiceCommandsHelp();
        } else if (command.startsWith("SET_TEMP_")) {
            String temp = command.replace("SET_TEMP_", "");
            sendCommandToDevice("SET_TEMP_" + temp);
            showSafeToast("Setting temperature to " + temp + "°C");
            logManager.addLog("Voice: Set temperature to " + temp + "°C",
                    LogManager.LogType.SENT,
                    bluetoothManager.getConnectedDeviceName());
        } else {
            // Send the raw command to Arduino
            sendCommandToDevice(command);
            showSafeToast("Sending command: " + command);
            logManager.addLog("Voice: " + command,
                    LogManager.LogType.SENT,
                    bluetoothManager.getConnectedDeviceName());
        }
    }

    private void handleLightCommand(String command, boolean turnOn) {
        String lightNumber = command.replace("LIGHT", "").replace(turnOn ? "_ON" : "_OFF", "");
        try {
            int lightIndex = Integer.parseInt(lightNumber) - 1;
            if (lightIndex >= 0 && lightIndex < switchList.size()) {
                DeviceModel device = switchList.get(lightIndex);
                device.setOn(turnOn);
                gridAdapter.updateSwitchState(lightIndex, turnOn);

                sendCommandToDevice(command);
                showSafeToast("Turning " + (turnOn ? "on" : "off") + " light " + lightNumber);

                logManager.addLog("Voice: " + command,
                        LogManager.LogType.SENT,
                        bluetoothManager.getConnectedDeviceName());
            }
        } catch (NumberFormatException e) {
            showSafeToast("Invalid light number");
        }
    }

    private void handleToggleCommand(String command) {
        String lightNumber = command.replace("LIGHT", "").replace("_TOGGLE", "");
        try {
            int lightIndex = Integer.parseInt(lightNumber) - 1;
            if (lightIndex >= 0 && lightIndex < switchList.size()) {
                DeviceModel device = switchList.get(lightIndex);
                boolean newState = !device.isOn();
                device.setOn(newState);
                gridAdapter.updateSwitchState(lightIndex, newState);

                sendCommandToDevice("LIGHT" + lightNumber + "_TOGGLE");
                showSafeToast("Toggling light " + lightNumber);

                logManager.addLog("Voice: Toggle light " + lightNumber,
                        LogManager.LogType.SENT,
                        bluetoothManager.getConnectedDeviceName());
            }
        } catch (NumberFormatException e) {
            showSafeToast("Invalid light number");
        }
    }

    private void handleAllLightsCommand(boolean turnOn) {
        for (int i = 0; i < switchList.size(); i++) {
            switchList.get(i).setOn(turnOn);
            gridAdapter.updateSwitchState(i, turnOn);
        }

        // Send command for each light
        for (int i = 1; i <= switchList.size(); i++) {
            sendCommandToDevice("LIGHT" + i + (turnOn ? "_ON" : "_OFF"));
        }

        showSafeToast("Turning " + (turnOn ? "on" : "off") + " all lights");
        logManager.addLog("Voice: " + (turnOn ? "All lights on" : "All lights off"),
                LogManager.LogType.SENT,
                bluetoothManager.getConnectedDeviceName());
    }

    private void showVoiceCommandsHelp() {
        StringBuilder helpText = new StringBuilder();
        helpText.append("Available Voice Commands:\n\n");

        helpText.append("• Turn on/off light [1-8]\n");
        helpText.append("• Toggle light [1-8]\n");
        helpText.append("• All lights on/off\n");
        helpText.append("• Get status\n");
        helpText.append("• Set temperature [number]\n");
        helpText.append("• Movie mode\n");
        helpText.append("• Sleep mode\n");
        helpText.append("• Good night\n");

        // Create a dialog to show help
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Voice Commands Help")
                .setMessage(helpText.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    // Voice permissions methods
    private boolean checkVoicePermissions() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestVoicePermissions() {
        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
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
            showSafeToast("Number of switches is already " + newCount);
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

        showSafeToast("Updated to " + newCount + " switches");
    }

    private void addNewSwitches(int newCount) {
        int currentCount = switchList.size();

        for (int i = currentCount + 1; i <= newCount; i++) {
            DeviceModel device = new DeviceModel(
                    i,
                    "Switch " + i,
                    "LIGHT" + i + "_TOGGLE"
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

    @Override
    public void onSwitchClick(int position, DeviceModel device) {
        // Toggle switch state locally first
        boolean newState = !device.isOn();
        device.setOn(newState);

        // Update UI immediately for better responsiveness
        gridAdapter.updateSwitchState(position, newState);

        if (bluetoothManager.isConnected()) {
            // Send the toggle command
            String command = device.getToggleCommand();
            sendCommandToDevice(command);

            // Log the command
            logManager.addLog("Sending: " + command, LogManager.LogType.SENT,
                    bluetoothManager.getConnectedDeviceName());
        } else {
            showSafeToast("Please connect to a device first");
            // Revert UI change if not connected
            device.setOn(!newState);
            gridAdapter.updateSwitchState(position, !newState);
        }
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

            showSafeToast("Switch settings saved");
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
                    "LIGHT" + i + "_TOGGLE"
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

        showSafeToast("4 default switches created");
    }

    /* =====================
       SEND COMMAND TO DEVICE
       ===================== */
    private void sendCommandToDevice(String command) {
        if (bluetoothManager.isConnected()) {
            bluetoothManager.sendCommand(command);
        } else {
            showSafeToast("Please connect to a device first");
        }
    }

    /* =====================
       HANDLE DEVICE MESSAGES
       ===================== */
    private void handleDeviceMessage(String message) {
        message = message.trim();

        // Handle status messages from device (LIGHT1:ON, LIGHT1:OFF)
        if (message.contains(":")) {
            String[] parts = message.split(":");
            if (parts.length == 2) {
                String deviceName = parts[0].trim();
                String state = parts[1].trim();

                boolean isOn = state.equals("ON");

                runOnUiThread(() -> {
                    // Update corresponding switch
                    updateSwitchFromDevice(deviceName, isOn);

                    // Log the state change
                    logManager.addLog(deviceName + " is now " + state,
                            LogManager.LogType.RECEIVED,
                            bluetoothManager.getConnectedDeviceName());
                });
            }
        }
        // Handle toggle acknowledgement messages
        else if (message.contains("toggled") || message.contains("Toggled")) {
            // Extract device number from message
            parseToggleAckMessage(message);
        }
        // Handle status request response
        else if (message.contains("Status:") || message.contains("L1=") || message.contains("L2=") || message.contains("L3=")) {
            parseStatusResponse(message);
        }
        // Handle BT/IR received messages
        else if (message.contains("BT Received:") || message.contains("IR Received:")) {
            // Just log these messages
            logManager.addLog(message, LogManager.LogType.INFO,
                    bluetoothManager.getConnectedDeviceName());
        }
        // Handle error messages
        else if (message.startsWith("ERROR:") || message.contains("Unknown")) {
            logManager.addLog(message, LogManager.LogType.ERROR,
                    bluetoothManager.getConnectedDeviceName());
        }
        // Handle HELP command
        else if (message.contains("Bluetooth Commands")) {
            logManager.addLog("HELP received", LogManager.LogType.INFO,
                    bluetoothManager.getConnectedDeviceName());
        }
        // For any other messages, just log them
        else if (!message.isEmpty()) {
            logManager.addLog("Received: " + message, LogManager.LogType.INFO,
                    bluetoothManager.getConnectedDeviceName());
        }
    }

    private void parseToggleAckMessage(String message) {
        try {
            // Extract device number from messages like:
            // "Light 2 toggled"
            // "IR: Toggled Light 1"

            String lowerMessage = message.toLowerCase();
            String[] words = lowerMessage.split("\\s+");

            int deviceNumber = -1;

            // Look for patterns like "light 1" or "light 2"
            for (int i = 0; i < words.length - 1; i++) {
                if (words[i].equals("light") && i + 1 < words.length) {
                    try {
                        deviceNumber = Integer.parseInt(words[i + 1]);
                        break;
                    } catch (NumberFormatException e) {
                        // Try to parse word like "1" from "light1"
                        String word = words[i + 1];
                        if (word.matches("light\\d+")) {
                            deviceNumber = Integer.parseInt(word.replace("light", ""));
                            break;
                        }
                    }
                }
            }

            if (deviceNumber > 0) {
                // Don't update UI here - wait for LIGHTX:ON/OFF message
                // Just log it
                logManager.addLog("Device acknowledged: " + message,
                        LogManager.LogType.INFO,
                        bluetoothManager.getConnectedDeviceName());
            }
        } catch (Exception e) {
            logManager.addLog("Error parsing toggle ack: " + e.getMessage(),
                    LogManager.LogType.ERROR, "");
        }
    }

    private void updateSwitchFromDevice(String deviceName, boolean isOn) {
        // Extract device number from device name (e.g., LIGHT1 -> 1)
        if (deviceName.startsWith("LIGHT")) {
            try {
                String numberStr = deviceName.substring(5); // Remove "LIGHT"
                int deviceNumber = Integer.parseInt(numberStr);

                // Find and update the switch
                for (int i = 0; i < switchList.size(); i++) {
                    DeviceModel device = switchList.get(i);
                    if (device.getIndex() == deviceNumber) {
                        device.setOn(isOn);
                        gridAdapter.updateSwitchState(i, isOn);

                        // Save state to preferences
                        saveSwitchesToPreferences();
                        break;
                    }
                }

            } catch (NumberFormatException e) {
                // Invalid device number
                logManager.addLog("Invalid device name: " + deviceName,
                        LogManager.LogType.ERROR, "");
            }
        }
    }

    private void parseStatusResponse(String statusMessage) {
        // Format: Status: L1=ON  L2=OFF  L3=ON
        // OR: L1=ON L2=OFF L3=ON
        try {
            // Remove "Status:" if present
            String cleanMessage = statusMessage.replace("Status:", "").trim();

            // Split by spaces
            String[] parts = cleanMessage.split("\\s+");

            for (String part : parts) {
                if (part.contains("=")) {
                    String[] keyValue = part.split("=");
                    if (keyValue.length == 2) {
                        String key = keyValue[0].trim();
                        String value = keyValue[1].trim();

                        boolean isOn = value.equals("ON");

                        // Map L1, L2, L3 to device numbers
                        int deviceNumber = 0;
                        if (key.equals("L1")) deviceNumber = 1;
                        else if (key.equals("L2")) deviceNumber = 2;
                        else if (key.equals("L3")) deviceNumber = 3;

                        if (deviceNumber > 0) {
                            updateSwitchFromDevice("LIGHT" + deviceNumber, isOn);
                        }
                    }
                }
            }

            // Log the status update
            logManager.addLog("Status updated: " + statusMessage,
                    LogManager.LogType.INFO,
                    bluetoothManager.getConnectedDeviceName());

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
            showSafeToast("Please grant Bluetooth permissions first");
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
        showSafeToast("Scan error: " + error);
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
            showSafeToast("Connected to: " + connectedName);

            // Request initial status from Arduino
            new Handler().postDelayed(() -> {
                if (bluetoothManager.isConnected()) {
                    sendCommandToDevice("STATUS");
                    logManager.addLog("Requesting initial status", LogManager.LogType.INFO, connectedName);
                }
            }, 1000);
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
        logManager.addLog("Disconnected from device", LogManager.LogType.INFO, "");

        // Reset all switches to off when disconnected
        runOnUiThread(() -> {
            for (int i = 0; i < switchList.size(); i++) {
                switchList.get(i).setOn(false);
            }
            gridAdapter.updateAllSwitches(switchList);
            showSafeToast("Disconnected. All switches reset to OFF.");
        });
    }

    @Override
    public void onConnectionError(String error) {
        connectionStatus.setVisibility(View.GONE);

        if (dialogManager.isDialogShowing()) {
            dialogManager.updateScanStatus("Connection failed: " + error, android.R.color.holo_red_dark);
        }

        logManager.addLog("Connection failed: " + error, LogManager.LogType.ERROR, "");
        showSafeToast("Connection failed: " + error);
    }

    @Override
    public void onMessageReceived(String message) {
        // Handle each line separately (Arduino might send multiple lines)
        String[] lines = message.split("\n");
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                handleDeviceMessage(line);
            }
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
                showSafeToast("Bluetooth permissions are required to use this feature");
            } else {
                // Permissions granted, show dialog
                showBluetoothDialog();
            }
        }
        // Remove the REQUEST_VOICE_PERMISSIONS check since we're using ActivityResultLauncher now
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

        // Clean up voice manager
        if (voiceManager != null) {
            voiceManager.destroy();
        }
    }

    // Safe toast method to prevent SystemUI crashes
    private void showSafeToast(String message) {
        try {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // If toast fails, log it instead
            logManager.addLog("Toast failed: " + message, LogManager.LogType.ERROR, "");
        }
    }
}