package com.example.licentfront.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.licentfront.R;

public class AuthenticationActivity extends AppCompatActivity {
    private Button logInBtn;
    private Button signUpBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_authentication);
        logInBtn=findViewById(R.id.log_in_btn);
        signUpBtn=findViewById(R.id.sign_up_btn);

        logInBtn.setOnClickListener(v->{
            Intent intent=new Intent(AuthenticationActivity.this, LogInActivity.class);
            startActivity(intent);
        });

        signUpBtn.setOnClickListener(v->{
            Intent intent=new Intent(AuthenticationActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }
}