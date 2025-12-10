package com.ossoftware.ossmartcontrol;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LogsFragment extends Fragment {

    private RecyclerView recyclerViewLogs;
    private LogAdapter logAdapter;
    private TextView txtEmptyLogs;
    private TextView txtLogStats;
    private Button btnClearLogs;

    private List<LogEntry> logEntries = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_logs, container, false);

        recyclerViewLogs = view.findViewById(R.id.recyclerViewLogs);
        txtEmptyLogs = view.findViewById(R.id.txtEmptyLogs);
        txtLogStats = view.findViewById(R.id.txtLogStats);
        btnClearLogs = view.findViewById(R.id.btnClearLogs);

        // Setup RecyclerView
        logAdapter = new LogAdapter(logEntries);
        recyclerViewLogs.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewLogs.setAdapter(logAdapter);

        btnClearLogs.setOnClickListener(v -> clearLogs());

        updateEmptyState();
        updateStats();

        return view;
    }

    public void addLog(String message, String type, String deviceName) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                LogEntry.LogType logType;
                switch (type) {
                    case "SENT":
                        logType = LogEntry.LogType.SENT;
                        break;
                    case "RECEIVED":
                        logType = LogEntry.LogType.RECEIVED;
                        break;
                    case "ERROR":
                        logType = LogEntry.LogType.ERROR;
                        break;
                    case "INFO":
                    default:
                        logType = LogEntry.LogType.INFO;
                        break;
                }

                LogEntry logEntry = new LogEntry(logType, message, deviceName);
                logEntries.add(0, logEntry); // Add to top
                logAdapter.notifyItemInserted(0);

                updateEmptyState();
                updateStats();

                // Scroll to top
                recyclerViewLogs.scrollToPosition(0);
            });
        }
    }

    private void updateEmptyState() {
        if (txtEmptyLogs != null) {
            txtEmptyLogs.setVisibility(logEntries.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void updateStats() {
        if (txtLogStats != null) {
            int total = logEntries.size();
            int sent = 0, received = 0, error = 0, info = 0;

            for (LogEntry entry : logEntries) {
                switch (entry.getType()) {
                    case SENT: sent++; break;
                    case RECEIVED: received++; break;
                    case ERROR: error++; break;
                    case INFO: info++; break;
                }
            }

            txtLogStats.setText(String.format("Total: %d | Sent: %d | Received: %d | Errors: %d | Info: %d",
                    total, sent, received, error, info));
        }
    }

    private void clearLogs() {
        logEntries.clear();
        if (logAdapter != null) {
            logAdapter.notifyDataSetChanged();
        }
        updateEmptyState();
        updateStats();
    }

    // Log Entry class
    public static class LogEntry {
        public enum LogType {
            SENT, RECEIVED, ERROR, INFO
        }

        private LogType type;
        private String message;
        private String deviceName;
        private String timestamp;

        public LogEntry(LogType type, String message, String deviceName) {
            this.type = type;
            this.message = message;
            this.deviceName = deviceName;
            this.timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        }

        public LogType getType() { return type; }
        public String getMessage() { return message; }
        public String getDeviceName() { return deviceName; }
        public String getTimestamp() { return timestamp; }
    }

    // Log Adapter class
    private class LogAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<LogAdapter.ViewHolder> {

        private List<LogEntry> logEntries;

        public LogAdapter(List<LogEntry> logEntries) {
            this.logEntries = logEntries;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_log, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            LogEntry logEntry = logEntries.get(position);

            holder.logMessage.setText(logEntry.getMessage());
            holder.logTime.setText(logEntry.getTimestamp());

            // Set icon and background based on type
            switch (logEntry.getType()) {
                case SENT:
                    holder.logIcon.setText("→");
                    holder.logIcon.setBackgroundResource(R.drawable.log_icon_bg_sent);
                    break;
                case RECEIVED:
                    holder.logIcon.setText("←");
                    holder.logIcon.setBackgroundResource(R.drawable.log_icon_bg_received);
                    break;
                case ERROR:
                    holder.logIcon.setText("!");
                    holder.logIcon.setBackgroundResource(R.drawable.log_icon_bg_error);
                    break;
                case INFO:
                    holder.logIcon.setText("i");
                    holder.logIcon.setBackgroundResource(R.drawable.log_icon_bg_info);
                    break;
            }

            // Show device info for received messages
            if (logEntry.getDeviceName() != null && !logEntry.getDeviceName().isEmpty()) {
                holder.logDevice.setText("From: " + logEntry.getDeviceName());
                holder.logDevice.setVisibility(View.VISIBLE);
            } else {
                holder.logDevice.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return logEntries.size();
        }

        class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            TextView logIcon, logMessage, logTime, logDevice;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                logIcon = itemView.findViewById(R.id.logIcon);
                logMessage = itemView.findViewById(R.id.logMessage);
                logTime = itemView.findViewById(R.id.logTime);
                logDevice = itemView.findViewById(R.id.logDevice);
            }
        }
    }
}