package com.example.licentfront.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.example.licentfront.R;

public class PracticeSummaryActivity extends BaseActivity {

    private static final String TAG = "PracticeSummary";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice_summary);

        long practiceStartTime = getIntent().getLongExtra("practice_start_time", 0);
        long practiceEndTime = System.currentTimeMillis();

        Log.d(TAG, "Practice start time: " + practiceStartTime);
        Log.d(TAG, "Practice end time: " + practiceEndTime);

        if (practiceStartTime == 0) {
            Log.w(TAG, "Practice start time is 0, showing default message");
            TextView completionMessage = findViewById(R.id.completion_message);
            completionMessage.setText("You finished this practice session!");
        } else {
            long sessionDurationMs = practiceEndTime - practiceStartTime;
            long totalSeconds = sessionDurationMs / 1000;
            long minutes = totalSeconds / 60;
            long seconds = totalSeconds % 60;

            Log.d(TAG, "Session duration: " + sessionDurationMs + "ms = " + totalSeconds + "s = " + minutes + "m " + seconds + "s");

            TextView completionMessage = findViewById(R.id.completion_message);

            String message;
            if (minutes > 0) {
                message = String.format("You finished this practice session in %d minutes %d seconds", minutes, seconds);
            } else {
                message = String.format("You finished this practice session in %d seconds", seconds);
            }
            completionMessage.setText(message);
        }

        Button continueButton = findViewById(R.id.continue_button);

        continueButton.setOnClickListener(v -> {
            Log.d(TAG, "Continue button clicked");

            PracticeBackActivity.forceResetStats();
            PracticeFrontActivity.resetForNewSession();

            Intent intent = new Intent(PracticeSummaryActivity.this, DecksActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }
}