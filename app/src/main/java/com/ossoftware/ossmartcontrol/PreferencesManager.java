package com.ossoftware.ossmartcontrol;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class PreferencesManager {

    private static final String PREF_NAME = "SmartHomePrefs";
    private static final String KEY_DEVICES = "devices";

    private SharedPreferences sharedPreferences;
    private Gson gson;

    public PreferencesManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    // Save all devices
    public void saveDevices(Map<String, DeviceModel> devices) {
        String json = gson.toJson(devices);
        sharedPreferences.edit().putString(KEY_DEVICES, json).apply();
    }

    // Load all devices
    public Map<String, DeviceModel> loadDevices() {
        String json = sharedPreferences.getString(KEY_DEVICES, "");

        if (json.isEmpty()) {
            return createDefaultDevices();
        }

        Type type = new TypeToken<HashMap<String, DeviceModel>>() {
        }.getType();
        Map<String, DeviceModel> devices = gson.fromJson(json, type);

        if (devices == null) {
            return createDefaultDevices();
        }

        return devices;
    }

    // Save single device
    public void saveDevice(String deviceId, DeviceModel device) {
        Map<String, DeviceModel> devices = loadDevices();
        devices.put(deviceId, device);
        saveDevices(devices);
    }

    // Load single device
    public DeviceModel loadDevice(String deviceId) {
        Map<String, DeviceModel> devices = loadDevices();
        return devices.get(deviceId);
    }

    // Create default devices
    private Map<String, DeviceModel> createDefaultDevices() {
        Map<String, DeviceModel> devices = new HashMap<>();

        // Default devices with Arduino format commands
        devices.put("FAN", new DeviceModel("FAN", "Ceiling Fan", "FAN_ON", "FAN_OFF"));
        devices.put("LIGHT1", new DeviceModel("LIGHT1", "Living Room Light", "LIGHT1_ON", "LIGHT1_OFF"));
        devices.put("LIGHT2", new DeviceModel("LIGHT2", "Bedroom Light", "LIGHT2_ON", "LIGHT2_OFF"));
        devices.put("LIGHT3", new DeviceModel("LIGHT3", "Kitchen Light", "LIGHT3_ON", "LIGHT3_OFF"));

        saveDevices(devices);
        return devices;
    }
}