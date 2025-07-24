package com.example.licentfront.utils;

import android.content.Intent;

import com.example.licentfront.activities.DecksActivity;
import com.example.licentfront.activities.ProfileActivity;
import com.example.licentfront.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.app.Activity;

public class NavigationUtils {
    public static void setupBottomNavigation(BottomNavigationView bottomNav, Activity activity, int selectedItemId) {
        bottomNav.setSelectedItemId(selectedItemId);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_profile && selectedItemId != R.id.nav_profile) {
                activity.startActivity(new Intent(activity, ProfileActivity.class));
                return true;
            } else if (id == R.id.nav_decks && selectedItemId != R.id.nav_decks) {
                activity.startActivity(new Intent(activity, DecksActivity.class));
                return true;
            } else if (id == R.id.nav_read && selectedItemId != R.id.nav_read) {
                //activity.startActivity(new Intent(activity, LibraryActivity.class));
                return true;
            }
            return false;
        });
    }
}