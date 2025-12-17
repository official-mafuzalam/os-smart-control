package com.ossoftware.ossmartcontrol;

import org.json.JSONException;
import org.json.JSONObject;

public class DeviceModel {
    private String id;
    private String name;
    private String toggleCommand; // Changed from commandOn/commandOff
    private boolean isOn;
    private int index;

    // Default constructor
    public DeviceModel() {
    }

    public DeviceModel(String id, String name, String toggleCommand) {
        this.id = id;
        this.name = name;
        this.toggleCommand = toggleCommand;
        this.isOn = false;
        this.index = 0;
    }

    public DeviceModel(int index, String name, String toggleCommand) {
        this.id = "SWITCH_" + index;
        this.name = name;
        this.toggleCommand = toggleCommand;
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

    // Get toggle command
    public String getToggleCommand() {
        return toggleCommand;
    }

    public void setToggleCommand(String toggleCommand) {
        this.toggleCommand = toggleCommand;
    }

    // For backward compatibility
    public String getCommandOn() {
        return toggleCommand;
    }

    public String getCommandOff() {
        return toggleCommand;
    }

    public void setCommandOn(String commandOn) {
        this.toggleCommand = commandOn;
    }

    public void setCommandOff(String commandOff) {
        this.toggleCommand = commandOff;
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
        return toggleCommand;
    }

    // Convert to JSON string
    public String toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("name", name);
            json.put("toggleCommand", toggleCommand);
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

            // Handle both old format (commandOn/commandOff) and new format (toggleCommand)
            if (json.has("toggleCommand")) {
                device.setToggleCommand(json.optString("toggleCommand", ""));
            } else {
                // For backward compatibility
                device.setToggleCommand(json.optString("commandOn", ""));
            }

            device.setOn(json.optBoolean("isOn", false));
            device.setIndex(json.optInt("index", 0));
            return device;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}