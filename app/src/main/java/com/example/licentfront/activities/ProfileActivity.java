package com.example.licentfront.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.licentfront.R;
import com.example.licentfront.utils.NavigationUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends BaseActivity {

    private Button myDecksBtn, readBtn, gamesBtn, editProfileBtn;
    private ImageView profileImageView;
    private TextView nameTextView, languageTextView;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private FirebaseFirestore db;
    private String userId;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        profileImageView = findViewById(R.id.profile_img);
        nameTextView = findViewById(R.id.name_tw);
        languageTextView = findViewById(R.id.language_tw);
        editProfileBtn = findViewById(R.id.edit_profile_btn2);

        initializeActivityLaunchers();

        editProfileBtn.setOnClickListener(v -> showEditProfileDialog());

        if (user != null) {
            loadUserProfile();
        }
        setupButtons();
    }

    private void initializeActivityLaunchers() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            uploadImageToFirebase(imageUri);
                        }
                    }
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        if (extras != null) {
                            Bitmap imageBitmap = (Bitmap) extras.get("data");
                            if (imageBitmap != null) {
                                uploadBitmapToFirebase(imageBitmap);
                            }
                        }
                    }
                });

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openCamera();
                    } else {
                        Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Profile")
                .setItems(new String[]{"Edit Name", "Edit Native Language", "Change Profile Picture"}, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showEditNameDialog();
                            break;
                        case 1:
                            showEditLanguageDialog();
                            break;
                        case 2:
                            showImagePickerDialog();
                            break;
                    }
                })
                .show();
    }

    private void showEditNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Name");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(nameTextView.getText().toString());
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                updateUserField("nickname", newName);
                nameTextView.setText(newName);
            } else {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showEditLanguageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Native Language");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        String currentText = languageTextView.getText().toString();
        String languageOnly = currentText.replace(" Native Speaker", "").trim();
        input.setText(languageOnly);

        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newLanguage = input.getText().toString().trim();
            if (!newLanguage.isEmpty()) {
                updateUserField("nativeLanguage", newLanguage);
                languageTextView.setText(newLanguage + " Native Speaker");
            } else {
                Toast.makeText(this, "Language cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateUserField(String fieldName, String value) {
        if (userId == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put(fieldName, value);

        db.collection("user-data").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Profile Picture")
                .setItems(new String[]{"Choose from Gallery", "Take Photo", "Remove Photo"}, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            openGallery();
                            break;
                        case 1:
                            checkCameraPermissionAndOpen();
                            break;
                        case 2:
                            removeProfilePicture();
                            break;
                    }
                })
                .show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(intent);
        } else {
            Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadImageToFirebase(Uri imageUri) {
        if (userId == null) return;

        profileImageView.setAlpha(0.5f);

        StorageReference profileImageRef = storageRef.child("profile_images/" + userId + ".jpg");

        profileImageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    profileImageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        saveProfileImageUrl(downloadUri.toString());
                        loadProfileImage(downloadUri.toString());
                        profileImageView.setAlpha(1.0f);
                        Toast.makeText(this, "Profile picture updated successfully", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    profileImageView.setAlpha(1.0f);
                    Toast.makeText(this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadBitmapToFirebase(Bitmap bitmap) {
        if (userId == null) return;

        profileImageView.setAlpha(0.5f);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] data = baos.toByteArray();

        StorageReference profileImageRef = storageRef.child("profile_images/" + userId + ".jpg");

        profileImageRef.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> {
                    profileImageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        saveProfileImageUrl(downloadUri.toString());
                        loadProfileImage(downloadUri.toString());
                        profileImageView.setAlpha(1.0f);
                        Toast.makeText(this, "Profile picture updated successfully", Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    profileImageView.setAlpha(1.0f);
                    Toast.makeText(this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveProfileImageUrl(String imageUrl) {
        if (userId == null) return;

        db.collection("user-data").document(userId)
                .update("profileImageUrl", imageUrl)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save profile image URL", Toast.LENGTH_SHORT).show();
                });
    }

    private void removeProfilePicture() {
        if (userId == null) return;

        StorageReference profileImageRef = storageRef.child("profile_images/" + userId + ".jpg");
        profileImageRef.delete()
                .addOnSuccessListener(aVoid -> {
                    db.collection("user-data").document(userId)
                            .update("profileImageUrl", null)
                            .addOnSuccessListener(aVoid1 -> {
                                profileImageView.setImageResource(R.drawable.ic_default_profile);
                                Toast.makeText(this, "Profile picture removed", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to remove profile picture", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadUserProfile() {
        db.collection("user-data").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String nickname = documentSnapshot.getString("nickname");
                        // Changed from "native language" to "nativeLanguage"
                        String nativeLanguage = documentSnapshot.getString("nativeLanguage");
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                        nameTextView.setText(nickname != null ? nickname : "No nickname");

                        if (nativeLanguage != null && !nativeLanguage.isEmpty()) {
                            languageTextView.setText(nativeLanguage + " Native Speaker");
                        } else {
                            languageTextView.setText("English Native Speaker");
                        }

                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            loadProfileImage(profileImageUrl);
                        } else {
                            profileImageView.setImageResource(R.drawable.ic_default_profile);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show();
                });
    }
    private void loadProfileImage(String imageUrl) {
        Glide.with(this)
                .load(imageUrl)
                .transform(new CircleCrop())
                .placeholder(R.drawable.ic_default_profile)
                .error(R.drawable.ic_default_profile)
                .into(profileImageView);
    }

    private void setupButtons() {
        myDecksBtn = findViewById(R.id.decks_profile_btn);
        myDecksBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, DecksActivity.class);
            startActivity(intent);
        });

        readBtn = findViewById(R.id.read_profile_btn);
        readBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, LibraryActivity.class);
            startActivity(intent);
        });

        gamesBtn = findViewById(R.id.games_profile_btn);
        gamesBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, GamesLibraryActivity.class);
            startActivity(intent);
        });

        Button progressBtn = findViewById(R.id.progress_profile_btn);
        progressBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, ProgressActivity.class);
            startActivity(intent);
        });
    }
}