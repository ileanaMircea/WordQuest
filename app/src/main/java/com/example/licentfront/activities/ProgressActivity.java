package com.example.licentfront.activities;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.licentfront.R;
import com.example.licentfront.utils.NavigationUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProgressActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private TextView totalDecksText, totalFlashcardsText, booksStartedText, booksCompletedText;
    private TextView practiceSessionsText, studyTimeText, currentStreakText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        NavigationUtils.setupBottomNavigation(bottomNav, this, R.id.nav_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();

        if (currentUser != null) {
            loadProgressData();
        }
    }

    private void initializeViews() {
        totalDecksText = findViewById(R.id.total_decks_text);
        totalFlashcardsText = findViewById(R.id.total_flashcards_text);
        booksStartedText = findViewById(R.id.books_started_text);
        booksCompletedText = findViewById(R.id.books_completed_text);
        practiceSessionsText = findViewById(R.id.practice_sessions_text);
        studyTimeText = findViewById(R.id.study_time_text);
        currentStreakText = findViewById(R.id.current_streak_text);
    }

    private void loadProgressData() {
        String userId = currentUser.getUid();
        loadUserStats(userId);
        loadDecksProgress(userId);
        loadBooksProgress(userId);
    }

    private void loadUserStats(String userId) {
        db.collection("user-data").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long totalPracticeSessions = documentSnapshot.getLong("totalPracticeSessions");
                        Long totalStudyTime = documentSnapshot.getLong("totalStudyTime");

                        Map<String, Object> dailyPracticeStreak = (Map<String, Object>) documentSnapshot.get("dailyPracticeStreak");

                        practiceSessionsText.setText(String.valueOf(totalPracticeSessions != null ? totalPracticeSessions : 0));

                        if (totalStudyTime != null) {
                            long hours = totalStudyTime / (1000 * 60 * 60);
                            long minutes = (totalStudyTime % (1000 * 60 * 60)) / (1000 * 60);
                            studyTimeText.setText(hours + "h " + minutes + "m");
                        } else {
                            studyTimeText.setText("0h 0m");
                        }

                        // Calculate current streak from dailyPracticeStreak map
                        int currentStreak = calculateCurrentStreak(dailyPracticeStreak);
                        currentStreakText.setText(String.valueOf(currentStreak));
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading user stats", Toast.LENGTH_SHORT).show();
                });
    }

    private int calculateCurrentStreak(Map<String, Object> dailyPracticeStreak) {
        if (dailyPracticeStreak == null || dailyPracticeStreak.isEmpty()) {
            return 0;
        }

        // Get today's date in the same format as stored in Firebase
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();

        int streak = 0;
        String currentDate = dateFormat.format(calendar.getTime());

        if (!dailyPracticeStreak.containsKey(currentDate)) {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            currentDate = dateFormat.format(calendar.getTime());
        }

        // numara zilele consecutiv ptr streak
        while (dailyPracticeStreak.containsKey(currentDate)) {
            Object streakValue = dailyPracticeStreak.get(currentDate);
            if (streakValue instanceof Number) {
                int dayStreak = ((Number) streakValue).intValue();
                if (dayStreak > 0) {
                    streak = Math.max(streak, dayStreak);
                    calendar.add(Calendar.DAY_OF_MONTH, -1);
                    currentDate = dateFormat.format(calendar.getTime());
                } else {
                    break; //s-a dus streak ul
                }
            } else {
                break;
            }
        }

        return streak;
    }

    private void loadDecksProgress(String userId) {
        db.collection("user-decks").document(userId).collection("decks")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalDecks = querySnapshot.size();
                    totalDecksText.setText(String.valueOf(totalDecks));
                    countTotalFlashcards(userId, querySnapshot);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading decks progress", Toast.LENGTH_SHORT).show();
                });
    }

    private void countTotalFlashcards(String userId, com.google.firebase.firestore.QuerySnapshot decks) {
        int[] totalFlashcards = {0};
        int[] decksProcessed = {0};
        int totalDecks = decks.size();

        if (totalDecks == 0) {
            totalFlashcardsText.setText("0");
            return;
        }

        for (com.google.firebase.firestore.DocumentSnapshot deckDoc : decks.getDocuments()) {
            String deckId = deckDoc.getId();

            db.collection("user-decks").document(userId).collection("decks")
                    .document(deckId).collection("flashcards")
                    .get()
                    .addOnSuccessListener(flashcardSnapshot -> {
                        totalFlashcards[0] += flashcardSnapshot.size();
                        decksProcessed[0]++;

                        if (decksProcessed[0] == totalDecks) {
                            totalFlashcardsText.setText(String.valueOf(totalFlashcards[0]));
                        }
                    })
                    .addOnFailureListener(e -> {
                        decksProcessed[0]++;
                        if (decksProcessed[0] == totalDecks) {
                            totalFlashcardsText.setText(String.valueOf(totalFlashcards[0]));
                        }
                    });
        }
    }

    private void loadBooksProgress(String userId) {
        db.collection("user-books").document(userId).collection("books")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int booksStarted = 0;
                    int booksCompleted = 0;

                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Boolean isStarted = doc.getBoolean("isStarted");
                        Boolean isCompleted = doc.getBoolean("isCompleted");

                        if (isStarted != null && isStarted) {
                            booksStarted++;
                        }
                        if (isCompleted != null && isCompleted) {
                            booksCompleted++;
                        }
                    }

                    booksStartedText.setText(String.valueOf(booksStarted));
                    booksCompletedText.setText(String.valueOf(booksCompleted));
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading books progress", Toast.LENGTH_SHORT).show();
                });
    }
}