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

        // Create 4 default switches with toggle commands
        for (int i = 1; i <= 4; i++) {
            DeviceModel device = new DeviceModel(
                    i,
                    "Switch " + i,
                    "LIGHT" + i + "_TOGGLE"
            );
            devices.put(device.getId(), device);
        }

//        DeviceModel deviceModel = new DeviceModel(
//                5,
//                "HELP SWITCH",
//                "HELP"
//        );
//        devices.put(deviceModel.getId(), deviceModel);
//
//        DeviceModel deviceModel2 = new DeviceModel(
//                6,
//                "STATUS SWITCH",
//                "STATUS"
//        );
//        devices.put(deviceModel2.getId(), deviceModel2);
//
//        DeviceModel deviceModel3 = new DeviceModel(
//                7,
//                "ALL_ON SWITCH",
//                "ALL_ON"
//        );
//        devices.put(deviceModel3.getId(), deviceModel3);
//
//        DeviceModel deviceModel4 = new DeviceModel(
//                8,
//                "ALL_OFF SWITCH",
//                "ALL_OFF"
//        );
//        devices.put(deviceModel4.getId(), deviceModel4);

        saveDevices(devices);
    }
}