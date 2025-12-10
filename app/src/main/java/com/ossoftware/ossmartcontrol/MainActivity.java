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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ai.bongotech.bt.BongoBT;

public class MainActivity extends AppCompatActivity {

    private MaterialSwitch switchFan, switchLight1, switchLight2, switchLight3;
    private ImageView btnBluetooth;
    private Button buttonConnect;
    private TextView txtStatus;
    private LinearLayout connectionStatus;

    private BongoBT bongoBT;
    private Set<String> discoveredDevices = new HashSet<>();
    private String connectedMac = "";
    private String connectedDeviceName = "";

    private Dialog bluetoothDialog;
    private LinearLayout dialogDeviceListLayout;
    private TextView txtSearchStatus;
    private TextView txtScanStatus;
    private Button btnRefresh;
    private Button btnClose;
    private ImageView loadingAnimation;

    private final Handler scanHandler = new Handler();
    private static final long SCAN_PERIOD = 15000; // 15 seconds
    private boolean isScanning = false;
    private int scanTimeRemaining = 15;

    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 100;

    // Runnable for scan timer
    private final Runnable scanTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (scanTimeRemaining > 0 && isScanning) {
                txtScanStatus.setText("Scanning... " + scanTimeRemaining + "s remaining");
                scanTimeRemaining--;
                scanHandler.postDelayed(this, 1000);
            } else if (isScanning) {
                stopBluetoothScan();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        bongoBT = new BongoBT(this);

        // Setup click listeners
        btnBluetooth.setOnClickListener(v -> showBluetoothDialog());
        buttonConnect.setOnClickListener(v -> showBluetoothDialog());

        // Set up switch listeners
        setupSwitchListeners();

        // Check Bluetooth permissions
        checkBluetoothPermissions();
    }

    private void initializeViews() {
        btnBluetooth = findViewById(R.id.btnBluetooth);
        buttonConnect = findViewById(R.id.buttonConnect);
        txtStatus = findViewById(R.id.txtStatus);
        connectionStatus = findViewById(R.id.connectionStatus);

        switchFan = findViewById(R.id.switchFan);
        switchLight1 = findViewById(R.id.switchLight1);
        switchLight2 = findViewById(R.id.switchLight2);
        switchLight3 = findViewById(R.id.switchLight3);

        // Initially hide status
        connectionStatus.setVisibility(View.GONE);
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
    private void showBluetoothDialog() {
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

        dialogDeviceListLayout = bluetoothDialog.findViewById(R.id.deviceListLayout);
        txtSearchStatus = bluetoothDialog.findViewById(R.id.txtSearchStatus);
        txtScanStatus = bluetoothDialog.findViewById(R.id.txtScanStatus);
        btnRefresh = bluetoothDialog.findViewById(R.id.btnRefresh);
        btnClose = bluetoothDialog.findViewById(R.id.btnClose);
        loadingAnimation = bluetoothDialog.findViewById(R.id.loadingAnimation);

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
    }

    /* =====================
       START BLUETOOTH SCAN (USING BONGOBT)
       ===================== */
    private void startBluetoothScan() {
        // Update UI for scanning
        startScanningAnimation();
        txtScanStatus.setText("Scanning... 15s remaining");
        txtScanStatus.setTextColor(Color.BLUE);
        btnRefresh.setText("Stop Scanning");
        btnRefresh.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_red_dark));

        // Clear previous devices (except already connected)
        clearNonConnectedDevices();

        // Reset timer
        scanTimeRemaining = 15;
        isScanning = true;

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
                    }
                });
            }

            @Override
            public void onFinished(ArrayList<HashMap<String, String>> arrayList) {
                runOnUiThread(() -> {
                    // Add any devices that might have been missed
                    if (arrayList != null) {
                        for (HashMap<String, String> device : arrayList) {
                            String name = device.get("name");
                            String mac = device.get("mac");
                            if (mac != null && !discoveredDevices.contains(mac)) {
                                discoveredDevices.add(mac);
                                String displayName = (name != null && !name.isEmpty()) ? name : "Unknown Device";
                                addDeviceToDialog(displayName, mac);
                            }
                        }
                    }

                    stopScanningAnimation();
                    scanHandler.removeCallbacks(scanTimerRunnable);

                    if (discoveredDevices.isEmpty()) {
                        txtScanStatus.setText("No devices found");
                        txtScanStatus.setTextColor(Color.RED);
                    } else {
                        txtScanStatus.setText("Scan completed");
                        txtScanStatus.setTextColor(Color.GREEN);
                    }
                    updateDeviceCount();
                });
            }

            @Override
            public void onError(String errorReason) {
                runOnUiThread(() -> {
                    stopScanningAnimation();
                    scanHandler.removeCallbacks(scanTimerRunnable);
                    isScanning = false;

                    txtScanStatus.setText("Error: " + errorReason);
                    txtScanStatus.setTextColor(Color.RED);

                    btnRefresh.setText("Start Scanning");
                    btnRefresh.setBackgroundTintList(ContextCompat.getColorStateList(MainActivity.this, android.R.color.holo_blue_dark));

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
       STOP BLUETOOTH SCAN
       ===================== */
    private void stopBluetoothScan() {
        // Note: BongoBT doesn't seem to have a method to stop scanning
        // If it does, you would call it here: bongoBT.stopSearch()

        stopScanningAnimation();
        scanHandler.removeCallbacks(scanTimerRunnable);
        isScanning = false;

        btnRefresh.setText("Start Scanning");
        btnRefresh.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_blue_dark));

        if (discoveredDevices.isEmpty()) {
            txtScanStatus.setText("Scan complete. No devices found");
            txtScanStatus.setTextColor(Color.RED);
        } else {
            txtScanStatus.setText("Scan complete");
            txtScanStatus.setTextColor(Color.GREEN);
        }

        updateDeviceCount();
    }

    /* =====================
       START SCANNING ANIMATION
       ===================== */
    private void startScanningAnimation() {
        loadingAnimation.setVisibility(View.VISIBLE);

        // Rotate animation
        loadingAnimation.animate()
                .rotationBy(360)
                .setDuration(1000)
                .setInterpolator(null)
                .withEndAction(() -> {
                    if (isScanning) {
                        startScanningAnimation(); // Loop animation
                    } else {
                        loadingAnimation.clearAnimation();
                    }
                })
                .start();
    }

    /* =====================
       STOP SCANNING ANIMATION
       ===================== */
    private void stopScanningAnimation() {
        loadingAnimation.setVisibility(View.GONE);
        loadingAnimation.clearAnimation();
    }

    /* =====================
       UPDATE DEVICE COUNT
       ===================== */
    private void updateDeviceCount() {
        int count = discoveredDevices.size();
        txtSearchStatus.setText("Found " + count + " device(s)");
    }

    /* =====================
       CLEAR NON-CONNECTED DEVICES
       ===================== */
    private void clearNonConnectedDevices() {
        // Remove all views except those that represent connected device
        for (int i = dialogDeviceListLayout.getChildCount() - 1; i >= 0; i--) {
            View deviceView = dialogDeviceListLayout.getChildAt(i);
            if (deviceView.findViewById(R.id.deviceMac) != null) {
                TextView deviceMac = deviceView.findViewById(R.id.deviceMac);
                String mac = deviceMac.getText().toString();

                // Keep only the connected device
                if (!mac.equals(connectedMac)) {
                    dialogDeviceListLayout.removeViewAt(i);
                    discoveredDevices.remove(mac);
                }
            }
        }
        updateDeviceCount();
    }

    /* =====================
       ADD DEVICE TO DIALOG
       ===================== */
    private void addDeviceToDialog(String name, String mac) {
        // Check if device already exists in the list
        for (int i = 0; i < dialogDeviceListLayout.getChildCount(); i++) {
            View existingView = dialogDeviceListLayout.getChildAt(i);
            if (existingView.findViewById(R.id.deviceMac) != null) {
                TextView existingMac = existingView.findViewById(R.id.deviceMac);
                if (existingMac.getText().toString().equals(mac)) {
                    return; // Device already in list
                }
            }
        }

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
        updateDeviceCount();
    }

    /* =====================
       CONNECT DEVICE (USING BONGOBT)
       ===================== */
    private void connectDevice(String name, String mac) {
        // Stop scanning when connecting
        stopBluetoothScan();

        txtScanStatus.setText("Connecting to " + name + "...");
        txtScanStatus.setTextColor(Color.parseColor("#FF9800")); // Orange color

        bongoBT.connectTo(mac, new BongoBT.BtConnectListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onConnected() {
                runOnUiThread(() -> {
                    connectedMac = mac;
                    connectedDeviceName = name;

                    // Update UI in dialog
                    updateDialogDeviceList();
                    txtScanStatus.setText("Connected to " + name);
                    txtScanStatus.setTextColor(Color.GREEN);

                    // Update main activity status
                    updateConnectionStatus();

                    // Get connected device info
                    BluetoothDevice device = bongoBT.getConnectedDevice();
                    if (device != null) {
                        String deviceName = device.getName();
                        String deviceMac = device.getAddress();
                        Toast.makeText(MainActivity.this,
                                "Connected to: " + deviceName + " (" + deviceMac + ")",
                                Toast.LENGTH_SHORT).show();
                    }

                    // Close dialog after delay
                    new Handler().postDelayed(() -> {
                        if (bluetoothDialog != null && bluetoothDialog.isShowing()) {
                            bluetoothDialog.dismiss();
                        }
                    }, 1000);
                });
            }

            @Override
            public void onReceived(String message) {
                runOnUiThread(() -> {
                    // Handle incoming messages from device
                    handleDeviceMessage(message);
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

                    // Update dialog UI
                    if (bluetoothDialog != null && bluetoothDialog.isShowing()) {
                        updateDialogDeviceList();
                        txtScanStatus.setText("Connection failed: " + reason);
                        txtScanStatus.setTextColor(Color.RED);
                    }

                    // Update main activity status
                    updateConnectionStatus();

                    Toast.makeText(MainActivity.this,
                            "Connection failed: " + reason,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /* =====================
       UPDATE DIALOG DEVICE LIST
       ===================== */
    private void updateDialogDeviceList() {
        if (dialogDeviceListLayout == null) return;

        for (int i = 0; i < dialogDeviceListLayout.getChildCount(); i++) {
            View deviceView = dialogDeviceListLayout.getChildAt(i);
            if (deviceView.findViewById(R.id.deviceMac) != null) {
                TextView deviceMac = deviceView.findViewById(R.id.deviceMac);
                Button btnConnect = deviceView.findViewById(R.id.btnConnect);

                String mac = deviceMac.getText().toString();

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
            }
        }
        updateDeviceCount();
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

                // Enable switches
                enableSwitches(true);
            } else {
                connectionStatus.setVisibility(View.GONE);

                // Disable switches
                enableSwitches(false);
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
       SEND COMMAND TO DEVICE (USING BONGOBT)
       ===================== */
    private void sendCommand(String command) {
        if (!connectedMac.isEmpty()) {
            // Using sendCommand() as per BongoBT documentation
            bongoBT.sendCommand(command);
            Toast.makeText(this, "Command sent: " + command, Toast.LENGTH_SHORT).show();
        }
    }

    /* =====================
       HANDLE DEVICE MESSAGES
       ===================== */
    private void handleDeviceMessage(String message) {
        // Parse and handle messages from Bluetooth device
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
                    }
                });
            }
        }
    }

    /* =====================
       CHECK PERMISSIONS
       ===================== */
    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    /* =====================
       CHECK BLUETOOTH PERMISSIONS
       ===================== */
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