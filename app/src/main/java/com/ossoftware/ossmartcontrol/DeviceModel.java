package com.ossoftware.ossmartcontrol;

import org.json.JSONException;
import org.json.JSONObject;

public class DeviceModel {
    private String id;
    private String name;
    private String commandOn;
    private String commandOff;
    private boolean isOn;
    private int index; // For dynamic switches

    // Default constructor
    public DeviceModel() {
    }

    public DeviceModel(String id, String name, String commandOn, String commandOff) {
        this.id = id;
        this.name = name;
        this.commandOn = commandOn;
        this.commandOff = commandOff;
        this.isOn = false;
        this.index = 0;
    }

    public DeviceModel(int index, String name, String commandOn, String commandOff) {
        this.id = "SWITCH_" + index;
        this.name = name;
        this.commandOn = commandOn;
        this.commandOff = commandOff;
        this.isOn = false;
        this.index = index;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCommandOn() {
        return commandOn;
    }

    public void setCommandOn(String commandOn) {
        this.commandOn = commandOn;
    }

    public String getCommandOff() {
        return commandOff;
    }

    public void setCommandOff(String commandOff) {
        this.commandOff = commandOff;
    }

    public boolean isOn() {
        return isOn;
    }

    public void setOn(boolean on) {
        isOn = on;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getCurrentCommand() {
        return isOn ? commandOn : commandOff;
    }

    // Convert to JSON string
    public String toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("name", name);
            json.put("commandOn", commandOn);
            json.put("commandOff", commandOff);
            json.put("isOn", isOn);
            json.put("index", index);
            return json.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return "{}";
        }
    }

    // Create from JSON string
    public static DeviceModel fromJson(String jsonStr) {
        try {
            JSONObject json = new JSONObject(jsonStr);
            DeviceModel device = new DeviceModel();
            device.setId(json.optString("id", ""));
            device.setName(json.optString("name", ""));
            device.setCommandOn(json.optString("commandOn", ""));
            device.setCommandOff(json.optString("commandOff", ""));
            device.setOn(json.optBoolean("isOn", false));
            device.setIndex(json.optInt("index", 0));
            return device;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}