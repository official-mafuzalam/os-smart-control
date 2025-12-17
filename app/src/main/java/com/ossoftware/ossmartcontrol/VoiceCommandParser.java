package com.ossoftware.ossmartcontrol;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VoiceCommandParser {

    // Command mapping
    private Map<String, String> commandMap;

    public VoiceCommandParser() {
        initializeCommandMap();
    }

    private void initializeCommandMap() {
        commandMap = new HashMap<>();

        // Light control commands (for up to 8 switches)
        for (int i = 1; i <= 8; i++) {
            String num = String.valueOf(i);
            String word = getNumberWord(i);

            // Turn on commands - multiple variations
            commandMap.put("turn on light " + word, "LIGHT" + num + "_ON");
            commandMap.put("turn on light " + num, "LIGHT" + num + "_ON");
            commandMap.put("switch on light " + word, "LIGHT" + num + "_ON");
            commandMap.put("light " + word + " on", "LIGHT" + num + "_ON");
            commandMap.put("light " + num + " on", "LIGHT" + num + "_ON");
            commandMap.put("on light " + word, "LIGHT" + num + "_ON");
            commandMap.put("on light " + num, "LIGHT" + num + "_ON");
            commandMap.put("enable light " + word, "LIGHT" + num + "_ON");
            commandMap.put("power on light " + word, "LIGHT" + num + "_ON");

            // Turn off commands - multiple variations
            commandMap.put("turn off light " + word, "LIGHT" + num + "_OFF");
            commandMap.put("turn off light " + num, "LIGHT" + num + "_OFF");
            commandMap.put("switch off light " + word, "LIGHT" + num + "_OFF");
            commandMap.put("light " + word + " off", "LIGHT" + num + "_OFF");
            commandMap.put("light " + num + " off", "LIGHT" + num + "_OFF");
            commandMap.put("off light " + word, "LIGHT" + num + "_OFF");
            commandMap.put("off light " + num, "LIGHT" + num + "_OFF");
            commandMap.put("disable light " + word, "LIGHT" + num + "_OFF");
            commandMap.put("power off light " + word, "LIGHT" + num + "_OFF");

            // Toggle commands
            commandMap.put("toggle light " + word, "LIGHT" + num + "_TOGGLE");
            commandMap.put("toggle light " + num, "LIGHT" + num + "_TOGGLE");
            commandMap.put("switch light " + word, "LIGHT" + num + "_TOGGLE");
            commandMap.put("switch light " + num, "LIGHT" + num + "_TOGGLE");
        }

        // Group commands
        commandMap.put("turn on all lights", "ALL_LIGHTS_ON");
        commandMap.put("all lights on", "ALL_LIGHTS_ON");
        commandMap.put("lights on", "ALL_LIGHTS_ON");
        commandMap.put("turn on every light", "ALL_LIGHTS_ON");
        commandMap.put("switch on all lights", "ALL_LIGHTS_ON");

        commandMap.put("turn off all lights", "ALL_LIGHTS_OFF");
        commandMap.put("all lights off", "ALL_LIGHTS_OFF");
        commandMap.put("lights off", "ALL_LIGHTS_OFF");
        commandMap.put("turn off every light", "ALL_LIGHTS_OFF");
        commandMap.put("switch off all lights", "ALL_LIGHTS_OFF");

        // Status commands
        commandMap.put("what is the status", "STATUS");
        commandMap.put("get status", "STATUS");
        commandMap.put("check status", "STATUS");
        commandMap.put("show status", "STATUS");
        commandMap.put("current status", "STATUS");
        commandMap.put("status report", "STATUS");

        // Help command
        commandMap.put("help", "HELP");
        commandMap.put("show help", "HELP");
        commandMap.put("what can i say", "HELP");
        commandMap.put("available commands", "HELP");
        commandMap.put("list commands", "HELP");

        // Test commands
        commandMap.put("test", "TEST");
        commandMap.put("test connection", "TEST");
        commandMap.put("check connection", "TEST");
    }

    private String getNumberWord(int number) {
        switch (number) {
            case 1:
                return "one";
            case 2:
                return "two";
            case 3:
                return "three";
            case 4:
                return "four";
            case 5:
                return "five";
            case 6:
                return "six";
            case 7:
                return "seven";
            case 8:
                return "eight";
            default:
                return String.valueOf(number);
        }
    }

    public String parseCommand(String spokenText) {
        // Convert to lowercase and remove extra spaces
        spokenText = spokenText.toLowerCase().trim().replaceAll("\\s+", " ");

        // First, try exact match
        if (commandMap.containsKey(spokenText)) {
            return commandMap.get(spokenText);
        }

        // Try partial matches
        for (Map.Entry<String, String> entry : commandMap.entrySet()) {
            String key = entry.getKey();
            if (spokenText.contains(key)) {
                return entry.getValue();
            }
        }

        // Try to extract light number from phrases using regex
        Pattern pattern = Pattern.compile("(light|lamp|switch|device)\\s*(\\d+|one|two|three|four|five|six|seven|eight)");
        Matcher matcher = pattern.matcher(spokenText);

        if (matcher.find()) {
            String numberWord = matcher.group(2);
            int lightNumber = wordToNumber(numberWord);

            if (lightNumber > 0 && lightNumber <= 8) {
                if (spokenText.contains("on") || spokenText.contains("open") ||
                        spokenText.contains("start") || spokenText.contains("enable") ||
                        spokenText.contains("power on")) {
                    return "LIGHT" + lightNumber + "_ON";
                } else if (spokenText.contains("off") || spokenText.contains("close") ||
                        spokenText.contains("stop") || spokenText.contains("disable") ||
                        spokenText.contains("power off")) {
                    return "LIGHT" + lightNumber + "_OFF";
                } else if (spokenText.contains("toggle") || spokenText.contains("switch")) {
                    return "LIGHT" + lightNumber + "_TOGGLE";
                }
            }
        }

        // Check for "all lights" commands
        if (spokenText.contains("all light") || spokenText.contains("every light") ||
                spokenText.contains("all the light")) {
            if (spokenText.contains("on") || spokenText.contains("enable")) {
                return "ALL_LIGHTS_ON";
            } else if (spokenText.contains("off") || spokenText.contains("disable")) {
                return "ALL_LIGHTS_OFF";
            }
        }

        return "UNKNOWN_COMMAND";
    }

    private int wordToNumber(String word) {
        switch (word.toLowerCase()) {
            case "1":
            case "one":
                return 1;
            case "2":
            case "two":
                return 2;
            case "3":
            case "three":
                return 3;
            case "4":
            case "four":
                return 4;
            case "5":
            case "five":
                return 5;
            case "6":
            case "six":
                return 6;
            case "7":
            case "seven":
                return 7;
            case "8":
            case "eight":
                return 8;
            default:
                try {
                    return Integer.parseInt(word);
                } catch (NumberFormatException e) {
                    return -1;
                }
        }
    }

    public Map<String, String> getAvailableCommands() {
        return new HashMap<>(commandMap);
    }
}