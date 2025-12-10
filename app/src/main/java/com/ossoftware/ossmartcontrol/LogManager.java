package com.ossoftware.ossmartcontrol;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogManager {

    private Context context;
    private LinearLayout logsContainer;
    private TextView txtEmptyLogs;
    private TextView txtLogStats;

    private List<LogEntry> logEntries;
    private int sentCount = 0;
    private int receivedCount = 0;
    private int errorCount = 0;
    private int infoCount = 0;

    public enum LogType {
        SENT, RECEIVED, ERROR, INFO
    }

    public class LogEntry {
        LogType type;
        String message;
        String deviceName;
        String timestamp;

        LogEntry(LogType type, String message, String deviceName) {
            this.type = type;
            this.message = message;
            this.deviceName = deviceName;
            this.timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        }
    }

    public LogManager(Context context, LinearLayout logsContainer, TextView txtEmptyLogs, TextView txtLogStats) {
        this.context = context;
        this.logsContainer = logsContainer;
        this.txtEmptyLogs = txtEmptyLogs;
        this.txtLogStats = txtLogStats;
        this.logEntries = new ArrayList<>();
    }

    public void addLog(String message, LogType type, String deviceName) {
        LogEntry logEntry = new LogEntry(type, message, deviceName);
        logEntries.add(0, logEntry); // Add to top

        // Update counts
        switch (type) {
            case SENT:
                sentCount++;
                break;
            case RECEIVED:
                receivedCount++;
                break;
            case ERROR:
                errorCount++;
                break;
            case INFO:
                infoCount++;
                break;
        }

        // Add log to UI
        addLogToUI(logEntry);

        updateEmptyState();
        updateStats();
    }

    private void addLogToUI(LogEntry logEntry) {
        // Inflate log item layout
        View logView = LayoutInflater.from(context).inflate(R.layout.item_log, null);

        TextView logIcon = logView.findViewById(R.id.logIcon);
        TextView logMessage = logView.findViewById(R.id.logMessage);
        TextView logTime = logView.findViewById(R.id.logTime);
        TextView logDevice = logView.findViewById(R.id.logDevice);

        logMessage.setText(logEntry.message);
        logTime.setText(logEntry.timestamp);

        // Set icon and background based on type
        switch (logEntry.type) {
            case SENT:
                logIcon.setText("→");
                logIcon.setBackgroundResource(R.drawable.log_icon_bg_sent);
                break;
            case RECEIVED:
                logIcon.setText("←");
                logIcon.setBackgroundResource(R.drawable.log_icon_bg_received);
                break;
            case ERROR:
                logIcon.setText("!");
                logIcon.setBackgroundResource(R.drawable.log_icon_bg_error);
                break;
            case INFO:
                logIcon.setText("i");
                logIcon.setBackgroundResource(R.drawable.log_icon_bg_info);
                break;
        }

        // Show device info for received messages
        if (logEntry.deviceName != null && !logEntry.deviceName.isEmpty()) {
            logDevice.setText("From: " + logEntry.deviceName);
            logDevice.setVisibility(View.VISIBLE);
        } else {
            logDevice.setVisibility(View.GONE);
        }

        // Add to logs container at the top
        logsContainer.addView(logView, 0);
    }

    private void updateEmptyState() {
        if (txtEmptyLogs != null) {
            txtEmptyLogs.setVisibility(logEntries.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void updateStats() {
        if (txtLogStats != null) {
            int total = logEntries.size();
            txtLogStats.setText(String.format("Total: %d | Sent: %d | Received: %d | Errors: %d | Info: %d",
                    total, sentCount, receivedCount, errorCount, infoCount));
        }
    }

    public void clearLogs() {
        logEntries.clear();
        logsContainer.removeAllViews();
        sentCount = 0;
        receivedCount = 0;
        errorCount = 0;
        infoCount = 0;

        updateEmptyState();
        updateStats();
    }

    public int getTotalLogs() {
        return logEntries.size();
    }
}