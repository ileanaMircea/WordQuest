package com.example.licentfront.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.licentfront.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {
    private EditText emailEditText, passwordEditText, confirmEditText;
    private Spinner languageSpinner;
    private Button signUpButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        emailEditText = findViewById(R.id.email_sign_input);
        passwordEditText = findViewById(R.id.password_sign_input);
        confirmEditText = findViewById(R.id.confirm_pass_input);
        languageSpinner = findViewById(R.id.language_spinner);
        signUpButton = findViewById(R.id.sign_btn);

        setupLanguageSpinner();
        signUpButton.setOnClickListener(v -> registerUser());
    }

    private void setupLanguageSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                Arrays.asList("English", "Romanian", "Spanish", "French", "German",
                        "Italian", "Portuguese", "Dutch", "Polish", "Czech"));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);

        // Set Romanian as default since your app seems to be for Romanian users
        languageSpinner.setSelection(1); // Romanian is at index 1
    }

    private void registerUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirm = confirmEditText.getText().toString().trim();
        String selectedLanguage = languageSpinner.getSelectedItem().toString();

        // Validation
        if (email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, "Completează toate câmpurile!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirm)) {
            Toast.makeText(this, "Parolele nu se potrivesc!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Parola trebuie să aibă cel puțin 6 caractere!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create user account
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Create user profile in Firestore
                            createUserProfile(user.getUid(), selectedLanguage);
                        }
                    } else {
                        handleRegistrationError(task.getException());
                    }
                });
    }

    private void createUserProfile(String userId, String nativeLanguage) {
        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("nickname", "");
        userProfile.put("nativeLanguage", nativeLanguage);
        userProfile.put("profileImageUrl", "");
        userProfile.put("totalBooksStarted", 0);
        userProfile.put("totalPracticeSessions", 0);
        userProfile.put("totalStudyTime", 0);
        userProfile.put("lastLoginTime", System.currentTimeMillis());

        db.collection("user-data").document(userId)
                .set(userProfile)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cont creat cu succes!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SignUpActivity.this, ProfileActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Eroare la crearea profilului: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SignUpActivity.this, ProfileActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
    }

    private void handleRegistrationError(Exception exception) {
        String errorMessage = "Eroare necunoscută.";

        if (exception != null && exception.getMessage() != null) {
            String message = exception.getMessage();
            if (message.contains("email address is already in use")) {
                errorMessage = "Deja există un utilizator cu acest email!";
            } else if (message.contains("weak-password")) {
                errorMessage = "Parola este prea slabă!";
            } else if (message.contains("invalid-email")) {
                errorMessage = "Adresa de email nu este validă!";
            } else {
                errorMessage = message;
            }
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }
}