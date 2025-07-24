package com.example.licentfront.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.licentfront.R;
import com.google.firebase.auth.FirebaseAuth;

public class LogInActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button logInButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        mAuth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.email_input);
        passwordEditText = findViewById(R.id.password_input);
        logInButton = findViewById(R.id.sign_btn);

        logInButton.setOnClickListener(v -> logInUser());
    }

    private void logInUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completează toate câmpurile!", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Autentificare reușită!", Toast.LENGTH_SHORT).show();
                        Intent intent=new Intent(LogInActivity.this, ProfileActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "Autentificare eșuată: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
