package com.ossoftware.ossmartcontrol;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import ai.bongotech.bt.BongoBT;

public class MainActivity extends AppCompatActivity {

    // UI Components
    private ImageView btnBluetooth;
    private Button buttonConnect;
    private TextView txtStatus;
    private MaterialSwitch switchFan, switchLight1, switchLight2, switchLight3;
    private LinearLayout connectionStatus;

    // Logs UI
    private LinearLayout logsContainer;
    private TextView txtEmptyLogs;
    private TextView txtLogStats;
    private Button btnClearLogs;

    // Bluetooth
    private BongoBT bongoBT;
    private Set<String> discoveredDevices = new HashSet<>();
    private String connectedMac = "";
    private String connectedDeviceName = "";

    // Dialog
    private Dialog bluetoothDialog;
    private LinearLayout dialogDeviceListLayout;
    private TextView txtScanStatus;
    private TextView txtSearchStatus;
    private ImageView loadingAnimation;
    private Button btnRefresh;

    // Scanning
    private final Handler scanHandler = new Handler();
    private static final long SCAN_PERIOD = 15000;
    private boolean isScanning = false;
    private int scanTimeRemaining = 15;

    // Logs
    private List<LogEntry> logEntries = new ArrayList<>();
    private int sentCount = 0;
    private int receivedCount = 0;
    private int errorCount = 0;
    private int infoCount = 0;

    // Permissions
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        bongoBT = new BongoBT(this);

        setupSwitchListeners();
        setupButtonListeners();

        // Check Bluetooth permissions
        checkBluetoothPermissions();
    }

    private void initializeViews() {
        // Main controls
        btnBluetooth = findViewById(R.id.btnBluetooth);
        buttonConnect = findViewById(R.id.buttonConnect);
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
        buttonConnect.setOnClickListener(v -> showBluetoothDialog());
        btnClearLogs.setOnClickListener(v -> clearLogs());
    }

    private void setupSwitchListeners() {
        switchFan.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!connectedMac.isEmpty()) {
                sendCommand("FAN:" + (isChecked ? "ON" : "OFF"));
            } else {
                Toast.makeText(this, "Please connect to a device first", Toast.LENGTH_SHORT).show();
                switchFan.setChecked(false);
            }
        });

        switchLight1.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!connectedMac.isEmpty()) {
                sendCommand("LIGHT1:" + (isChecked ? "ON" : "OFF"));
            } else {
                Toast.makeText(this, "Please connect to a device first", Toast.LENGTH_SHORT).show();
                switchLight1.setChecked(false);
            }
        });

        switchLight2.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!connectedMac.isEmpty()) {
                sendCommand("LIGHT2:" + (isChecked ? "ON" : "OFF"));
            } else {
                Toast.makeText(this, "Please connect to a device first", Toast.LENGTH_SHORT).show();
                switchLight2.setChecked(false);
            }
        });

        switchLight3.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!connectedMac.isEmpty()) {
                sendCommand("LIGHT3:" + (isChecked ? "ON" : "OFF"));
            } else {
                Toast.makeText(this, "Please connect to a device first", Toast.LENGTH_SHORT).show();
                switchLight3.setChecked(false);
            }
        });
    }

    /* =====================
       SHOW BLUETOOTH DIALOG
       ===================== */
    public void showBluetoothDialog() {
        // Check permissions first
        if (!checkPermissions()) {
            checkBluetoothPermissions();
            Toast.makeText(this, "Please grant Bluetooth permissions first", Toast.LENGTH_SHORT).show();
            return;
        }

        bluetoothDialog = new Dialog(this);
        bluetoothDialog.setContentView(R.layout.dialog_bluetooth_devices);
        bluetoothDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        bluetoothDialog.setCancelable(true);

        // Initialize dialog views
        dialogDeviceListLayout = bluetoothDialog.findViewById(R.id.deviceListLayout);
        txtScanStatus = bluetoothDialog.findViewById(R.id.txtScanStatus);
        txtSearchStatus = bluetoothDialog.findViewById(R.id.txtSearchStatus);
        loadingAnimation = bluetoothDialog.findViewById(R.id.loadingAnimation);
        btnRefresh = bluetoothDialog.findViewById(R.id.btnRefresh);
        Button btnClose = bluetoothDialog.findViewById(R.id.btnClose);

        // Clear previous list
        dialogDeviceListLayout.removeAllViews();
        discoveredDevices.clear();

        // Initialize UI state
        txtScanStatus.setText("Ready to scan");
        txtScanStatus.setTextColor(Color.BLUE);
        loadingAnimation.setVisibility(View.GONE);
        updateDeviceCount();

        // Set up button listeners
        btnRefresh.setOnClickListener(v -> {
            if (!isScanning) {
                startBluetoothScan();
            } else {
                stopBluetoothScan();
            }
        });

        btnClose.setOnClickListener(v -> {
            stopBluetoothScan();
            bluetoothDialog.dismiss();
        });

        // Show dialog
        bluetoothDialog.show();

        // Start scanning automatically
        startBluetoothScan();
    }

    /* =====================
       START BLUETOOTH SCAN
       ===================== */
    private void startBluetoothScan() {
        if (isScanning) {
            return;
        }

        // Clear device list
        dialogDeviceListLayout.removeAllViews();
        discoveredDevices.clear();
        updateDeviceCount();

        // Update UI for scanning
        loadingAnimation.setVisibility(View.VISIBLE);
        txtScanStatus.setText("Scanning... 15s remaining");
        txtScanStatus.setTextColor(Color.BLUE);
        btnRefresh.setText("Stop Scanning");
        btnRefresh.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_red_dark));

        addLog("Scanning started", LogType.INFO, "");

        // Reset timer
        scanTimeRemaining = 15;
        isScanning = true;

        // Start animation
        startScanningAnimation();

        // Start timer
        scanHandler.postDelayed(scanTimerRunnable, 1000);

        // Start scanning with BongoBT
        bongoBT.searchDevices(new BongoBT.BtDiscoveryListener() {
            @Override
            public void onStarted() {
                runOnUiThread(() -> {
                    txtScanStatus.setText("Scanning started... Discovering devices");
                });
            }

            @Override
            public void onDeviceAdded(String name, String mac) {
                runOnUiThread(() -> {
                    if (!discoveredDevices.contains(mac)) {
                        discoveredDevices.add(mac);
                        String displayName = (name != null && !name.isEmpty()) ? name : "Unknown Device";
                        addDeviceToDialog(displayName, mac);
                        updateDeviceCount();
                        addLog("Found device: " + displayName, LogType.INFO, "");
                    }
                });
            }

            @Override
            public void onFinished(ArrayList<HashMap<String, String>> arrayList) {
                runOnUiThread(() -> {
                    addLog("Scanning finished. Found " + discoveredDevices.size() + " devices", LogType.INFO, "");
                    stopBluetoothScan();
                });
            }

            @Override
            public void onError(String errorReason) {
                runOnUiThread(() -> {
                    addLog("Scan error: " + errorReason, LogType.ERROR, "");
                    stopBluetoothScan();
                    txtScanStatus.setText("Error: " + errorReason);
                    txtScanStatus.setTextColor(Color.RED);
                    Toast.makeText(MainActivity.this,
                            "Scan error: " + errorReason,
                            Toast.LENGTH_LONG).show();
                });
            }
        });

        // Auto-stop after SCAN_PERIOD
        scanHandler.postDelayed(() -> {
            if (isScanning) {
                stopBluetoothScan();
            }
        }, SCAN_PERIOD);
    }

    /* =====================
       SCAN TIMER RUNNABLE
       ===================== */
    private final Runnable scanTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (scanTimeRemaining > 0 && isScanning) {
                txtScanStatus.setText("Scanning... " + scanTimeRemaining + "s remaining");
                scanTimeRemaining--;
                scanHandler.postDelayed(this, 1000);
            }
        }
    };

    /* =====================
       STOP BLUETOOTH SCAN
       ===================== */
    private void stopBluetoothScan() {
        isScanning = false;
        scanHandler.removeCallbacks(scanTimerRunnable);
        stopScanningAnimation();

        if (btnRefresh != null) {
            btnRefresh.setText("Scan Again");
            btnRefresh.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_blue_dark));
        }

        if (txtScanStatus != null) {
            if (discoveredDevices.isEmpty()) {
                txtScanStatus.setText("Scan complete. No devices found");
                txtScanStatus.setTextColor(Color.RED);
            } else {
                txtScanStatus.setText("Scan complete");
                txtScanStatus.setTextColor(Color.GREEN);
            }
        }
    }

    /* =====================
       START SCANNING ANIMATION
       ===================== */
    private void startScanningAnimation() {
        if (loadingAnimation != null) {
            loadingAnimation.setVisibility(View.VISIBLE);
            loadingAnimation.animate()
                    .rotationBy(360)
                    .setDuration(1000)
                    .setInterpolator(null)
                    .withEndAction(() -> {
                        if (isScanning) {
                            startScanningAnimation();
                        }
                    })
                    .start();
        }
    }

    /* =====================
       STOP SCANNING ANIMATION
       ===================== */
    private void stopScanningAnimation() {
        if (loadingAnimation != null) {
            loadingAnimation.setVisibility(View.GONE);
            loadingAnimation.clearAnimation();
        }
    }

    /* =====================
       ADD DEVICE TO DIALOG
       ===================== */
    private void addDeviceToDialog(String name, String mac) {
        // Inflate device item layout
        View deviceView = LayoutInflater.from(this).inflate(R.layout.item_device, null);

        TextView deviceName = deviceView.findViewById(R.id.deviceName);
        TextView deviceMac = deviceView.findViewById(R.id.deviceMac);
        Button btnConnect = deviceView.findViewById(R.id.btnConnect);

        deviceName.setText(name);
        deviceMac.setText(mac);

        // Set button state
        if (mac.equals(connectedMac)) {
            btnConnect.setText("Connected");
            btnConnect.setEnabled(false);
            btnConnect.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            btnConnect.setTextColor(Color.WHITE);
        } else {
            btnConnect.setText("Connect");
            btnConnect.setEnabled(true);
            btnConnect.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark));
            btnConnect.setTextColor(Color.WHITE);
        }

        // Set click listener
        btnConnect.setOnClickListener(v -> {
            if (!mac.equals(connectedMac)) {
                connectDevice(name, mac);
                btnConnect.setText("Connecting...");
                btnConnect.setEnabled(false);
            }
        });

        // Add to dialog layout
        dialogDeviceListLayout.addView(deviceView);
    }

    /* =====================
       UPDATE DEVICE COUNT
       ===================== */
    private void updateDeviceCount() {
        if (txtSearchStatus != null) {
            int count = discoveredDevices.size();
            txtSearchStatus.setText("Found " + count + " device(s)");
        }
    }

    /* =====================
       CONNECT DEVICE
       ===================== */
    private void connectDevice(String name, String mac) {
        // Stop scanning when connecting
        stopBluetoothScan();

        if (txtScanStatus != null) {
            txtScanStatus.setText("Connecting to " + name + "...");
            txtScanStatus.setTextColor(Color.parseColor("#FF9800"));
        }

        addLog("Connecting to " + name + "...", LogType.INFO, "");

        bongoBT.connectTo(mac, new BongoBT.BtConnectListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onConnected() {
                runOnUiThread(() -> {
                    connectedMac = mac;
                    connectedDeviceName = name;

                    // Update main activity status
                    updateConnectionStatus();

                    // Get connected device info
                    BluetoothDevice device = bongoBT.getConnectedDevice();
                    if (device != null) {
                        String deviceName = device.getName();
                        String deviceMac = device.getAddress();
                        addLog("Connected to: " + deviceName, LogType.INFO, "");
                        Toast.makeText(MainActivity.this,
                                "Connected to: " + deviceName,
                                Toast.LENGTH_SHORT).show();
                    }

                    // Enable switches
                    enableSwitches(true);

                    // Close dialog after delay
                    new Handler().postDelayed(() -> {
                        if (bluetoothDialog != null && bluetoothDialog.isShowing()) {
                            bluetoothDialog.dismiss();
                        }
                    }, 1500);
                });
            }

            @Override
            public void onReceived(String message) {
                runOnUiThread(() -> {
                    // Handle incoming messages from device
                    handleDeviceMessage(message);
                    addLog("Received: " + message, LogType.RECEIVED, connectedDeviceName);
                    Toast.makeText(MainActivity.this,
                            "Received: " + message,
                            Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String reason) {
                runOnUiThread(() -> {
                    connectedMac = "";
                    connectedDeviceName = "";

                    // Update main activity status
                    updateConnectionStatus();

                    // Disable switches
                    enableSwitches(false);

                    addLog("Connection failed: " + reason, LogType.ERROR, "");

                    if (txtScanStatus != null) {
                        txtScanStatus.setText("Connection failed: " + reason);
                        txtScanStatus.setTextColor(Color.RED);
                    }

                    Toast.makeText(MainActivity.this,
                            "Connection failed: " + reason,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /* =====================
       UPDATE CONNECTION STATUS
       ===================== */
    private void updateConnectionStatus() {
        runOnUiThread(() -> {
            if (!connectedMac.isEmpty()) {
                connectionStatus.setVisibility(View.VISIBLE);
                txtStatus.setText("Connected to: " + connectedDeviceName);
                txtStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            } else {
                connectionStatus.setVisibility(View.GONE);
            }
        });
    }

    /* =====================
       ENABLE/DISABLE SWITCHES
       ===================== */
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
       SEND COMMAND TO DEVICE
       ===================== */
    private void sendCommand(String command) {
        if (!connectedMac.isEmpty()) {
            bongoBT.sendCommand(command);
            addLog("Command sent: " + command, LogType.SENT, connectedDeviceName);
            Toast.makeText(this, "Command sent: " + command, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Please connect to a device first", Toast.LENGTH_SHORT).show();
        }
    }

    /* =====================
       HANDLE DEVICE MESSAGES
       ===================== */
    private void handleDeviceMessage(String message) {
        runOnUiThread(() -> {
            // Parse and handle messages from device
            if (message.contains(":")) {
                String[] parts = message.split(":");
                if (parts.length == 2) {
                    String device = parts[0];
                    String state = parts[1];

                    boolean isOn = state.equals("ON");

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
                    }
                }
            }
        });
    }

    /* =====================
       LOG SYSTEM
       ===================== */

    // Log Entry class
    private enum LogType {
        SENT, RECEIVED, ERROR, INFO
    }

    private class LogEntry {
        LogType type;
        String message;
        String deviceName;
        String timestamp;

        LogEntry(LogType type, String message, String deviceName) {
            this.type = type;
            this.message = message;
            this.deviceName = deviceName;
            this.timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        }
    }

    private void addLog(String message, LogType type, String deviceName) {
        runOnUiThread(() -> {
            LogEntry logEntry = new LogEntry(type, message, deviceName);
            logEntries.add(0, logEntry); // Add to top

            // Update counts
            switch (type) {
                case SENT: sentCount++; break;
                case RECEIVED: receivedCount++; break;
                case ERROR: errorCount++; break;
                case INFO: infoCount++; break;
            }

            // Add log to UI
            addLogToUI(logEntry);

            updateEmptyState();
            updateStats();
        });
    }

    private void addLogToUI(LogEntry logEntry) {
        // Inflate log item layout
        View logView = LayoutInflater.from(this).inflate(R.layout.item_log, null);

        TextView logIcon = logView.findViewById(R.id.logIcon);
        TextView logMessage = logView.findViewById(R.id.logMessage);
        TextView logTime = logView.findViewById(R.id.logTime);
        TextView logDevice = logView.findViewById(R.id.logDevice);

        logMessage.setText(logEntry.message);
        logTime.setText(logEntry.timestamp);

        // Set icon and background based on type
        switch (logEntry.type) {
            case SENT:
                logIcon.setText("→");
                logIcon.setBackgroundResource(R.drawable.log_icon_bg_sent);
                break;
            case RECEIVED:
                logIcon.setText("←");
                logIcon.setBackgroundResource(R.drawable.log_icon_bg_received);
                break;
            case ERROR:
                logIcon.setText("!");
                logIcon.setBackgroundResource(R.drawable.log_icon_bg_error);
                break;
            case INFO:
                logIcon.setText("i");
                logIcon.setBackgroundResource(R.drawable.log_icon_bg_info);
                break;
        }

        // Show device info for received messages
        if (logEntry.deviceName != null && !logEntry.deviceName.isEmpty()) {
            logDevice.setText("From: " + logEntry.deviceName);
            logDevice.setVisibility(View.VISIBLE);
        } else {
            logDevice.setVisibility(View.GONE);
        }

        // Add to logs container at the top
        logsContainer.addView(logView, 0);
    }

    private void updateEmptyState() {
        if (txtEmptyLogs != null) {
            txtEmptyLogs.setVisibility(logEntries.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void updateStats() {
        if (txtLogStats != null) {
            int total = logEntries.size();
            txtLogStats.setText(String.format("Total: %d | Sent: %d | Received: %d | Errors: %d | Info: %d",
                    total, sentCount, receivedCount, errorCount, infoCount));
        }
    }

    private void clearLogs() {
        logEntries.clear();
        logsContainer.removeAllViews();
        sentCount = 0;
        receivedCount = 0;
        errorCount = 0;
        infoCount = 0;

        updateEmptyState();
        updateStats();
    }

    /* =====================
       PERMISSIONS
       ===================== */

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void checkBluetoothPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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

        // Stop scanning
        stopBluetoothScan();

        // Disconnect BongoBT
        if (bongoBT != null) {
            bongoBT.disconnect();
        }

        // Dismiss dialog
        if (bluetoothDialog != null && bluetoothDialog.isShowing()) {
            bluetoothDialog.dismiss();
        }

        // Remove callbacks
        scanHandler.removeCallbacksAndMessages(null);
    }
}