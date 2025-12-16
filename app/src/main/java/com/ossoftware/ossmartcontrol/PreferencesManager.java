package com.ossoftware.ossmartcontrol;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PreferencesManager {

    private static final String PREF_NAME = "SmartHomePrefs";
    private static final String KEY_DEVICES = "devices";
    private static final String KEY_FIRST_RUN = "first_run";

    private SharedPreferences sharedPreferences;

    public PreferencesManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Check if first run
        if (sharedPreferences.getBoolean(KEY_FIRST_RUN, true)) {
            // Create default switches for first run
            createDefaultSwitches();
            sharedPreferences.edit().putBoolean(KEY_FIRST_RUN, false).apply();
        }
    }

    // Save all devices
    public void saveDevices(Map<String, DeviceModel> devices) {
        try {
            JSONObject devicesJson = new JSONObject();
            for (Map.Entry<String, DeviceModel> entry : devices.entrySet()) {
                devicesJson.put(entry.getKey(), entry.getValue().toJson());
            }
            sharedPreferences.edit().putString(KEY_DEVICES, devicesJson.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Load all devices
    public Map<String, DeviceModel> loadDevices() {
        String jsonStr = sharedPreferences.getString(KEY_DEVICES, "");

        if (jsonStr.isEmpty()) {
            return new HashMap<>();
        }

        try {
            Map<String, DeviceModel> devices = new HashMap<>();
            JSONObject devicesJson = new JSONObject(jsonStr);

            Iterator<String> keys = devicesJson.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String deviceJsonStr = devicesJson.getString(key);
                DeviceModel device = DeviceModel.fromJson(deviceJsonStr);
                if (device != null) {
                    devices.put(key, device);
                }
            }

            return devices;
        } catch (JSONException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    // Create default switches
    private void createDefaultSwitches() {
        Map<String, DeviceModel> devices = new HashMap<>();

        // Create 4 default switches
        for (int i = 1; i <= 4; i++) {
            DeviceModel device = new DeviceModel(
                    i,
                    "Switch " + i,
                    "SWITCH" + i + "_ON",
                    "SWITCH" + i + "_OFF"
            );
            devices.put(device.getId(), device);
        }

        saveDevices(devices);
    }
}