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
    private TextInputEditText etDeviceName, etToggleCommand;
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
        etToggleCommand = findViewById(R.id.etToggleCommand);
        switchDeviceState = findViewById(R.id.switchDeviceState);
    }

    private void loadDeviceData() {
        if (device != null) {
            etDeviceName.setText(device.getName());
            etToggleCommand.setText(device.getToggleCommand());
            switchDeviceState.setChecked(device.isOn());
        }
    }

    private void setupListeners() {
        Button btnCancel = findViewById(R.id.btnCancel);
        Button btnSave = findViewById(R.id.btnSave);

        btnCancel.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> saveDeviceSettings());
    }

    private void saveDeviceSettings() {
        String name = etDeviceName.getText().toString().trim();
        String toggleCommand = etToggleCommand.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Please enter device name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (toggleCommand.isEmpty()) {
            Toast.makeText(getContext(), "Please enter toggle command", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update device
        device.setName(name);
        device.setToggleCommand(toggleCommand);
        device.setOn(switchDeviceState.isChecked());

        if (listener != null) {
            listener.onDeviceSettingsSaved(device);
        }

        dismiss();
    }
}