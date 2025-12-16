package com.ossoftware.ossmartcontrol;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

public class DeviceSettingsDialog extends Dialog {

    private DeviceModel device;
    private OnDeviceSettingsListener listener;
    private TextInputEditText etDeviceName, etCommandOn, etCommandOff;
    private MaterialSwitch switchDeviceState;

    public interface OnDeviceSettingsListener {
        void onDeviceSettingsSaved(DeviceModel device);
    }

    public DeviceSettingsDialog(@NonNull Context context, DeviceModel device, OnDeviceSettingsListener listener) {
        super(context);
        this.device = device;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_device_settings);

        initializeViews();
        loadDeviceData();
        setupListeners();
    }

    private void initializeViews() {
        etDeviceName = findViewById(R.id.etDeviceName);
        etCommandOn = findViewById(R.id.etCommandOn);
        etCommandOff = findViewById(R.id.etCommandOff);
        switchDeviceState = findViewById(R.id.switchDeviceState);

        Button btnCancel = findViewById(R.id.btnCancel);
        Button btnSave = findViewById(R.id.btnSave);

        btnCancel.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> saveDeviceSettings());
    }

    private void loadDeviceData() {
        if (device != null) {
            etDeviceName.setText(device.getName());
            etCommandOn.setText(device.getCommandOn());
            etCommandOff.setText(device.getCommandOff());
            switchDeviceState.setChecked(device.isOn());
        }
    }

    private void saveDeviceSettings() {
        String name = etDeviceName.getText().toString().trim();
        String commandOn = etCommandOn.getText().toString().trim();
        String commandOff = etCommandOff.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Please enter device name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (commandOn.isEmpty()) {
            Toast.makeText(getContext(), "Please enter ON command", Toast.LENGTH_SHORT).show();
            return;
        }

        if (commandOff.isEmpty()) {
            Toast.makeText(getContext(), "Please enter OFF command", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update device
        device.setName(name);
        device.setCommandOn(commandOn);
        device.setCommandOff(commandOff);
        device.setOn(switchDeviceState.isChecked());

        if (listener != null) {
            listener.onDeviceSettingsSaved(device);
        }

        dismiss();
    }

    private void setupListeners() {
        // Optional: Add any additional listeners
    }
}