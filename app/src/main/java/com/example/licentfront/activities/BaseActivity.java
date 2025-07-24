package com.example.licentfront.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.licentfront.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawableResource(R.drawable.default_background);
    }


    @Override
    public void setContentView(int layoutResID) {
        ViewGroup fullView = (ViewGroup) getLayoutInflater().inflate(R.layout.activity_base, null);
        FrameLayout container = fullView.findViewById(R.id.activity_container);
        getLayoutInflater().inflate(layoutResID, container, true);
        super.setContentView(fullView);

        setupBottomNav();
        setupBackButton();
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        if (this instanceof ProfileActivity) {
            bottomNav.setSelectedItemId(R.id.nav_profile);
        } else if (this instanceof LibraryActivity) {
            bottomNav.setSelectedItemId(R.id.nav_read);
        } else if (this instanceof DecksActivity) {
            bottomNav.setSelectedItemId(R.id.nav_decks);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_profile && !(this instanceof ProfileActivity)) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_read && !(this instanceof LibraryActivity)) {
                startActivity(new Intent(this, LibraryActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_decks && !(this instanceof DecksActivity)) {
                startActivity(new Intent(this, DecksActivity.class));
                finish();
                return true;
            }

            return true;
        });
    }

    private void setupBackButton() {
        ImageView backButton = findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> onBackIconPressed());
        }
    }

    protected void onBackIconPressed() {

    }
}
