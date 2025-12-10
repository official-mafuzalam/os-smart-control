package com.ossoftware.ossmartcontrol;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.util.HashSet;
import java.util.Set;

public class DeviceDialogManager {

    private Context context;
    private Dialog bluetoothDialog;
    private LinearLayout dialogDeviceListLayout;
    private TextView txtScanStatus;
    private TextView txtSearchStatus;
    private ImageView loadingAnimation;
    private Button btnRefresh;

    private Set<String> discoveredDevices = new HashSet<>();
    private boolean isScanning = false;
    private int scanTimeRemaining = 15;
    private final Handler scanHandler = new Handler();

    private DeviceDialogListener dialogListener;

    public interface DeviceDialogListener {
        void onScanRequested();

        void onStopScanRequested();

        void onDeviceConnectRequested(String name, String mac);
    }

    public DeviceDialogManager(Context context, DeviceDialogListener listener) {
        this.context = context;
        this.dialogListener = listener;
    }

    public void showDialog() {
        bluetoothDialog = new Dialog(context);
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
                startScanning();
            } else {
                stopScanning();
            }
        });

        btnClose.setOnClickListener(v -> {
            stopScanning();
            dismissDialog();
        });

        // Show dialog
        bluetoothDialog.show();
    }

    public void startScanning() {
        if (isScanning) return;

        // Clear device list
        dialogDeviceListLayout.removeAllViews();
        discoveredDevices.clear();
        updateDeviceCount();

        // Update UI for scanning
        loadingAnimation.setVisibility(View.VISIBLE);
        txtScanStatus.setText("Scanning... 15s remaining");
        txtScanStatus.setTextColor(Color.BLUE);
        btnRefresh.setText("Stop Scanning");
        btnRefresh.setBackgroundTintList(ContextCompat.getColorStateList(context, android.R.color.holo_red_dark));

        // Reset timer
        scanTimeRemaining = 15;
        isScanning = true;

        // Start animation
        startScanningAnimation();

        // Start timer
        startScanTimer();

        // Notify listener
        if (dialogListener != null) {
            dialogListener.onScanRequested();
        }

        // Auto-stop after 15 seconds
        scanHandler.postDelayed(() -> {
            if (isScanning) {
                stopScanning();
            }
        }, 15000);
    }

    public void stopScanning() {
        isScanning = false;
        scanHandler.removeCallbacksAndMessages(null);
        stopScanningAnimation();

        if (btnRefresh != null) {
            btnRefresh.setText("Scan Again");
            btnRefresh.setBackgroundTintList(ContextCompat.getColorStateList(context, android.R.color.holo_blue_dark));
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

    public void addDevice(String name, String mac) {
        if (discoveredDevices.contains(mac)) return;

        discoveredDevices.add(mac);
        addDeviceToDialog(name, mac);
        updateDeviceCount();
    }

    public void updateScanStatus(String status, int colorResId) {
        if (txtScanStatus != null) {
            txtScanStatus.setText(status);
            txtScanStatus.setTextColor(ContextCompat.getColor(context, colorResId));
        }
    }

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

    private void stopScanningAnimation() {
        if (loadingAnimation != null) {
            loadingAnimation.setVisibility(View.GONE);
            loadingAnimation.clearAnimation();
        }
    }

    private void startScanTimer() {
        scanHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (scanTimeRemaining > 0 && isScanning) {
                    txtScanStatus.setText("Scanning... " + scanTimeRemaining + "s remaining");
                    scanTimeRemaining--;
                    scanHandler.postDelayed(this, 1000);
                }
            }
        }, 1000);
    }

    private void addDeviceToDialog(String name, String mac) {
        // Inflate device item layout
        View deviceView = LayoutInflater.from(context).inflate(R.layout.item_device, null);

        TextView deviceName = deviceView.findViewById(R.id.deviceName);
        TextView deviceMac = deviceView.findViewById(R.id.deviceMac);
        Button btnConnect = deviceView.findViewById(R.id.btnConnect);

        deviceName.setText(name);
        deviceMac.setText(mac);

        btnConnect.setText("Connect");
        btnConnect.setEnabled(true);
        btnConnect.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark));
        btnConnect.setTextColor(Color.WHITE);

        // Set click listener
        btnConnect.setOnClickListener(v -> {
            if (dialogListener != null) {
                dialogListener.onDeviceConnectRequested(name, mac);
                btnConnect.setText("Connecting...");
                btnConnect.setEnabled(false);
            }
        });

        // Add to dialog layout
        dialogDeviceListLayout.addView(deviceView);
    }

    private void updateDeviceCount() {
        if (txtSearchStatus != null) {
            int count = discoveredDevices.size();
            txtSearchStatus.setText("Found " + count + " device(s)");
        }
    }

    public void dismissDialog() {
        if (bluetoothDialog != null && bluetoothDialog.isShowing()) {
            bluetoothDialog.dismiss();
        }
    }

    public boolean isDialogShowing() {
        return bluetoothDialog != null && bluetoothDialog.isShowing();
    }
}