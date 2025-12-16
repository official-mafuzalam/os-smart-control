package com.ossoftware.ossmartcontrol;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

public class AddSwitchesDialog extends Dialog {

    private EditText etSwitchCount;
    private Button btnCancel, btnCreate;
    private OnSwitchesCreatedListener listener;

    public interface OnSwitchesCreatedListener {
        void onSwitchesCreated(int count);
    }

    public AddSwitchesDialog(@NonNull Context context, OnSwitchesCreatedListener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_add_switches);

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        etSwitchCount = findViewById(R.id.etSwitchCount);
        btnCancel = findViewById(R.id.btnCancel);
        btnCreate = findViewById(R.id.btnCreate);
    }

    private void setupListeners() {
        btnCancel.setOnClickListener(v -> dismiss());

        btnCreate.setOnClickListener(v -> {
            String countStr = etSwitchCount.getText().toString().trim();

            if (countStr.isEmpty()) {
                Toast.makeText(getContext(), "Please enter number of switches", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int count = Integer.parseInt(countStr);

                if (count < 1) {
                    Toast.makeText(getContext(), "Minimum 1 switch required", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (count > 20) {
                    Toast.makeText(getContext(), "Maximum 20 switches allowed", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (listener != null) {
                    listener.onSwitchesCreated(count);
                }

                dismiss();

            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Please enter a valid number", Toast.LENGTH_SHORT).show();
            }
        });
    }
}