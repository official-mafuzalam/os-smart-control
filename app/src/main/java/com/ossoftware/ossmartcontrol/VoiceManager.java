package com.ossoftware.ossmartcontrol;

import android.content.Intent;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceManager {

    private static final String TAG = "VoiceManager";

    private MainActivity activity;
    private TextView txtListeningStatus;
    private VoiceCommandParser commandParser;

    // Activity result launcher for speech recognition
    private ActivityResultLauncher<Intent> speechRecognitionLauncher;

    public interface VoiceResultListener {
        void onVoiceCommandRecognized(String command);

        void onVoiceError(String error);
    }

    private VoiceResultListener voiceResultListener;

    public VoiceManager(MainActivity activity, TextView statusTextView) {
        this.activity = activity;
        this.txtListeningStatus = statusTextView;
        this.commandParser = new VoiceCommandParser();

        // Initialize the activity result launcher
        initializeSpeechRecognitionLauncher();
    }

    private void initializeSpeechRecognitionLauncher() {
        speechRecognitionLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    int resultCode = result.getResultCode();
                    Intent data = result.getData();

                    if (resultCode == AppCompatActivity.RESULT_OK && data != null) {
                        ArrayList<String> results = data.getStringArrayListExtra(
                                RecognizerIntent.EXTRA_RESULTS);

                        if (results != null && !results.isEmpty()) {
                            String spokenText = results.get(0).trim();
                            Log.d(TAG, "✅ Speech recognized: " + spokenText);

                            updateStatus("✅ Processing: " + spokenText, android.R.color.holo_green_dark);

                            // Add log for voice command - fixed access
                            activity.runOnUiThread(() -> {
                                if (activity.logManager != null) {
                                    activity.logManager.addLog("Voice heard: " + spokenText,
                                            LogManager.LogType.INFO, "Voice");
                                }
                            });

                            // Process the recognized text
                            String command = commandParser.parseCommand(spokenText);
                            Log.d(TAG, "🔧 Parsed command: " + command);

                            if (voiceResultListener != null) {
                                voiceResultListener.onVoiceCommandRecognized(command);
                            }

                            // Hide status after 2 seconds
                            activity.runOnUiThread(() -> {
                                new android.os.Handler().postDelayed(() -> {
                                    if (txtListeningStatus != null) {
                                        txtListeningStatus.setVisibility(android.view.View.GONE);
                                    }
                                }, 2000);
                            });

                        } else {
                            Log.w(TAG, "❌ No speech results found");
                            handleNoResults();
                        }
                    } else {
                        Log.w(TAG, "❌ Speech recognition cancelled or failed (result code: " + resultCode + ")");
                        handleNoResults();
                    }
                }
        );
    }

    public void setVoiceResultListener(VoiceResultListener listener) {
        this.voiceResultListener = listener;
    }

    public void startListening() {
        // Check if speech recognition is available
        if (!isSpeechRecognitionAvailable()) {
            Log.e(TAG, "Speech recognition not available");
            if (voiceResultListener != null) {
                voiceResultListener.onVoiceError("Speech recognition not available");
            }
            return;
        }

        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your command...");
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);

            // Start speech recognition using the launcher
            speechRecognitionLauncher.launch(intent);
            Log.d(TAG, "🎤 Started speech recognition activity");

            updateStatus("🎤 Listening...", android.R.color.holo_green_dark);

        } catch (Exception e) {
            Log.e(TAG, "❌ Error starting speech recognition: " + e.getMessage(), e);
            showToast("Error: " + e.getMessage());
            updateStatus("❌ Error", android.R.color.holo_red_dark);

            if (voiceResultListener != null) {
                voiceResultListener.onVoiceError("Error starting speech recognition: " + e.getMessage());
            }
        }
    }

    private boolean isSpeechRecognitionAvailable() {
        // Check if recognizer intent is supported
        return activity.getPackageManager().queryIntentActivities(
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0).size() > 0;
    }

    private void handleNoResults() {
        updateStatus("❌ No speech recognized", android.R.color.holo_red_dark);

        // Log the error - fixed access
        activity.runOnUiThread(() -> {
            if (activity.logManager != null) {
                activity.logManager.addLog("Voice: No speech recognized",
                        LogManager.LogType.ERROR, "Voice");
            }
        });

        if (voiceResultListener != null) {
            voiceResultListener.onVoiceError("No speech recognized");
        }

        activity.runOnUiThread(() -> {
            new android.os.Handler().postDelayed(() -> {
                if (txtListeningStatus != null) {
                    txtListeningStatus.setVisibility(android.view.View.GONE);
                }
            }, 2000);
        });
    }

    private void updateStatus(String text, int color) {
        if (txtListeningStatus != null) {
            activity.runOnUiThread(() -> {
                txtListeningStatus.setVisibility(android.view.View.VISIBLE);
                txtListeningStatus.setText(text);
                txtListeningStatus.setTextColor(activity.getResources().getColor(color));
            });
        }
    }

    private void showToast(String message) {
        activity.runOnUiThread(() ->
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show());
    }

    public void destroy() {
        // Nothing to destroy in this implementation
    }
}