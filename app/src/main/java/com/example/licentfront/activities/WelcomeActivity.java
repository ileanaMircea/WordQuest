package com.example.licentfront.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.example.licentfront.R;

public class WelcomeActivity extends AppCompatActivity {
    private static final int splash_duration=3000; //3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        new Handler().postDelayed(()->{
            Intent intent=new Intent(WelcomeActivity.this, AuthenticationActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_down,R.anim.slide_out_up);
            finish();
        },splash_duration);
    }
}