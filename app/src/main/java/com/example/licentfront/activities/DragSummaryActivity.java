package com.example.licentfront.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.licentfront.R;

public class DragSummaryActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drag_summary);

        int totalWords = getIntent().getIntExtra("total_words", 0);
        long startTime = getIntent().getLongExtra("start_time", 0);
        long endTime = System.currentTimeMillis();

        long durationMillis = endTime - startTime;
        double durationSeconds = durationMillis / 1000.0;

        TextView resultText = findViewById(R.id.result_text);

        if (durationSeconds < 60) {
            resultText.setText("You snapped " + totalWords + " words in " +
                    (int) durationSeconds + " seconds!");
        } else {
            double wordsPerMinute = totalWords / (durationSeconds / 60.0);
            resultText.setText(String.format("You snapped %.0f words per minute!", wordsPerMinute));
        }
    }
}
