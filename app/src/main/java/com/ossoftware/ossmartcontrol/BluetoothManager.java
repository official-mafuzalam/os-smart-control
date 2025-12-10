package com.ossoftware.ossmartcontrol;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import ai.bongotech.bt.BongoBT;

public class BluetoothManager {

    private Context context;
    private BongoBT bongoBT;
    private Set<String> discoveredDevices;
    private String connectedMac;
    private String connectedDeviceName;

    private BluetoothListener bluetoothListener;

    public interface BluetoothListener {
        void onDeviceFound(String name, String mac);

        void onScanStarted();

        void onScanFinished(int deviceCount);

        void onScanError(String error);

        void onConnected(String deviceName, String mac);

        void onDisconnected();

        void onConnectionError(String error);

        void onMessageReceived(String message);
    }

    public BluetoothManager(Context context, BluetoothListener listener) {
        this.context = context;
        this.bluetoothListener = listener;
        this.bongoBT = new BongoBT(context);
        this.discoveredDevices = new HashSet<>();
        this.connectedMac = "";
        this.connectedDeviceName = "";
    }

    public void startScanning() {
        discoveredDevices.clear();

        if (bluetoothListener != null) {
            bluetoothListener.onScanStarted();
        }

        bongoBT.searchDevices(new BongoBT.BtDiscoveryListener() {
            @Override
            public void onStarted() {
                // Scan started
            }

            @Override
            public void onDeviceAdded(String name, String mac) {
                if (!discoveredDevices.contains(mac)) {
                    discoveredDevices.add(mac);
                    if (bluetoothListener != null) {
                        bluetoothListener.onDeviceFound(name, mac);
                    }
                }
            }

            @Override
            public void onFinished(ArrayList<HashMap<String, String>> arrayList) {
                if (bluetoothListener != null) {
                    bluetoothListener.onScanFinished(discoveredDevices.size());
                }
            }

            @Override
            public void onError(String errorReason) {
                if (bluetoothListener != null) {
                    bluetoothListener.onScanError(errorReason);
                }
            }
        });
    }

    public void connectToDevice(String name, String mac) {
        bongoBT.connectTo(mac, new BongoBT.BtConnectListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onConnected() {
                connectedMac = mac;
                connectedDeviceName = name;

                if (bluetoothListener != null) {
                    bluetoothListener.onConnected(name, mac);
                }
            }

            @Override
            public void onReceived(String message) {
                if (bluetoothListener != null) {
                    bluetoothListener.onMessageReceived(message);
                }
            }

            @Override
            public void onError(String reason) {
                connectedMac = "";
                connectedDeviceName = "";

                if (bluetoothListener != null) {
                    bluetoothListener.onConnectionError(reason);
                }
            }
        });
    }

    public void sendCommand(String command) {
        if (!connectedMac.isEmpty()) {
            bongoBT.sendCommand(command);
        }
    }

    public void disconnect() {
        if (bongoBT != null) {
            bongoBT.disconnect();
        }
        connectedMac = "";
        connectedDeviceName = "";

        if (bluetoothListener != null) {
            bluetoothListener.onDisconnected();
        }
    }

    public boolean isConnected() {
        return !connectedMac.isEmpty();
    }

    public String getConnectedDeviceName() {
        return connectedDeviceName;
    }

    public String getConnectedMac() {
        return connectedMac;
    }

    public BluetoothDevice getConnectedDevice() {
        return bongoBT.getConnectedDevice();
    }

    public void clearDiscoveredDevices() {
        discoveredDevices.clear();
    }
}