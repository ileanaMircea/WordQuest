package com.example.licentfront.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;

import com.example.licentfront.R;
import com.example.licentfront.repositories.FlashcardRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PracticeBackActivity extends BaseActivity {

    private static final String TAG = "PracticeBack";

    private FlashcardRepository flashcardRepository;
    private static List<String> eligibleCardIds = new ArrayList<>();
    private static boolean isShuffled = false;
    private static long practiceStartTime = 0;

    private static Map<String, Integer> sessionStats = new HashMap<>();
    private static boolean sessionStatsInitialized = false;
    private static boolean goingToSummary = false;
    private static int totalCardsAnswered = 0;

    private String deckId;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_practice_back);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if (practiceStartTime == 0) {
            practiceStartTime = System.currentTimeMillis();
            Log.d(TAG, "Practice start time set in onCreate: " + practiceStartTime);
        } else {
            Log.d(TAG, "Practice start time already set: " + practiceStartTime);
        }

        if (!sessionStatsInitialized) {
            initializeSessionStats();
            sessionStatsInitialized = true;
            Log.d(TAG, "Session stats initialized for first time: " + sessionStats.toString());
        } else {
            Log.d(TAG, "Using existing session stats: " + sessionStats.toString());
        }

        flashcardRepository = new FlashcardRepository();
        Button nextBtn = findViewById(R.id.flip_button);
        RadioGroup difficultyGroup = findViewById(R.id.difficulty_group);
        TextView backText = findViewById(R.id.back_text);

        deckId = getIntent().getStringExtra("deck_id");
        String cardId = getIntent().getStringExtra("card_id");

        Log.d(TAG, "Loading card: " + cardId + " from deck: " + deckId);

        flashcardRepository.getFlashcard(deckId, cardId, new FlashcardRepository.FlashcardCallback() {
            @Override
            public void onSuccess(String back, String example) {
                String displayText = back;
                if (example != null && !example.isEmpty()) {
                    displayText += "\n\nExample sentence: \n\n" + example;
                }
                backText.setText(displayText);
                Log.d(TAG, "Card content loaded successfully");
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to load card content", e);
                backText.setText("Error loading card.");
            }
        });

        nextBtn.setOnClickListener(v -> {
            int selectedId = difficultyGroup.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(this, "Please choose a difficulty", Toast.LENGTH_SHORT).show();
                return;
            }

            String difficulty = getDifficultyFromRadioId(selectedId);
            Log.d(TAG, "User selected difficulty: " + difficulty);

            updateSessionStats(difficulty);
            totalCardsAnswered++;

            Log.d(TAG, "Total cards answered: " + totalCardsAnswered);
            Log.d(TAG, "Current session stats after update: " + sessionStats.toString());

            String repositoryDifficulty = difficulty.toLowerCase();

            flashcardRepository.updateNextReview(deckId, cardId, repositoryDifficulty, success -> {
                if (success) {
                    Log.d(TAG, "Card review updated successfully, loading next card");
                    loadNextFlashcard(cardId);
                } else {
                    Log.e(TAG, "Failed to update card review");
                    Toast.makeText(this, "Failed to update card", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void initializeSessionStats() {
        sessionStats = new HashMap<>();
        sessionStats.put("Very easy", 0);
        sessionStats.put("Easy", 0);
        sessionStats.put("Medium", 0);
        sessionStats.put("Hard", 0);
        sessionStats.put("Very hard", 0);
        totalCardsAnswered = 0;
        Log.d(TAG, "Session stats initialized: " + sessionStats.toString());
    }

    private void updateSessionStats(String difficulty) {
        if (sessionStats.containsKey(difficulty)) {
            int currentCount = sessionStats.get(difficulty);
            sessionStats.put(difficulty, currentCount + 1);
            Log.d(TAG, "Updated " + difficulty + " count from " + currentCount + " to " + (currentCount + 1));
        } else {
            Log.e(TAG, "Difficulty key not found in session stats: " + difficulty);
            Log.e(TAG, "Available keys: " + sessionStats.keySet().toString());
            sessionStats.put(difficulty, 1);
        }

        Log.d(TAG, "Session stats after update: " + sessionStats.toString());

        int total = 0;
        for (Integer count : sessionStats.values()) {
            total += count;
        }
        Log.d(TAG, "Total responses in stats: " + total);
    }

    private String getDifficultyFromRadioId(int radioId) {
        if (radioId == R.id.very_easy) {
            return "Very easy";
        } else if (radioId == R.id.easy) {
            return "Easy";
        } else if (radioId == R.id.medium) {
            return "Medium";
        } else if (radioId == R.id.hard) {
            return "Hard";
        } else if (radioId == R.id.very_hard) {
            return "Very hard";
        } else {
            Log.w(TAG, "Unknown radio button ID: " + radioId + ", defaulting to Medium");
            return "Medium";
        }
    }

    private void loadNextFlashcard(String currentCardId) {
        Log.d(TAG, "Loading next flashcard after: " + currentCardId);

        if (eligibleCardIds.isEmpty()) {
            Log.d(TAG, "No eligible cards in memory, fetching from repository");
            flashcardRepository.getEligibleCards(deckId, new FlashcardRepository.EligibleCardsCallback() {
                @Override
                public void onSuccess(List<String> cards) {
                    Log.d(TAG, "Fetched " + cards.size() + " eligible cards");
                    eligibleCardIds.clear();
                    eligibleCardIds.addAll(cards);

                    if (!eligibleCardIds.isEmpty()) {
                        if (!isShuffled) {
                            Collections.shuffle(eligibleCardIds);
                            isShuffled = true;
                            Log.d(TAG, "Cards shuffled");
                        }

                        String nextCardId = eligibleCardIds.get(0);
                        Log.d(TAG, "Going to next card: " + nextCardId);
                        goToFront(deckId, nextCardId);
                    } else {
                        Log.d(TAG, "No more eligible cards, ending practice session");
                        endPracticeSession();
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Failed to load eligible cards", e);
                    Toast.makeText(PracticeBackActivity.this, "Failed to load flashcards", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        } else {
            int currentIndex = eligibleCardIds.indexOf(currentCardId);
            Log.d(TAG, "Current card index: " + currentIndex + " out of " + eligibleCardIds.size());

            if (currentIndex == eligibleCardIds.size() - 1) {
                Log.d(TAG, "Reached end of eligible cards, ending practice session");
                endPracticeSession();
                return;
            }

            int nextIndex = currentIndex + 1;
            String nextCardId = eligibleCardIds.get(nextIndex);
            Log.d(TAG, "Going to next card: " + nextCardId + " (index " + nextIndex + ")");
            goToFront(deckId, nextCardId);
        }
    }

    private void endPracticeSession() {
        Log.d(TAG, "=== ENDING PRACTICE SESSION ===");
        Log.d(TAG, "Final session stats: " + sessionStats.toString());
        Log.d(TAG, "Total cards answered: " + totalCardsAnswered);

        isShuffled = false;
        eligibleCardIds.clear();
        updatePracticeProgress();
        goToSummaryScreen();
    }

    private void updatePracticeProgress() {
        if (userId == null || deckId == null) return;

        long practiceEndTime = System.currentTimeMillis();
        long sessionDuration = practiceEndTime - practiceStartTime;
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        double avgDifficulty = calculateAverageDifficulty();

        Log.d(TAG, "Updating practice progress - duration: " + sessionDuration + "ms, avg difficulty: " + avgDifficulty);

        db.collection("user-decks").document(userId).collection("decks").document(deckId)
                .update(
                        "lastPracticedDate", practiceEndTime,
                        "totalPracticeSessions", FieldValue.increment(1),
                        "dailyPracticeCount." + today, FieldValue.increment(1),
                        "averageDifficulty", avgDifficulty,
                        "totalPracticeTime", FieldValue.increment(sessionDuration)
                )
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to update existing deck data, creating new", e);
                    Map<String, Object> deckData = new HashMap<>();
                    deckData.put("lastPracticedDate", practiceEndTime);
                    deckData.put("totalPracticeSessions", 1);
                    deckData.put("dailyPracticeCount", Map.of(today, 1));
                    deckData.put("averageDifficulty", avgDifficulty);
                    deckData.put("totalPracticeTime", sessionDuration);

                    db.collection("user-decks").document(userId).collection("decks").document(deckId)
                            .set(deckData);
                });

        db.collection("user-data").document(userId)
                .update(
                        "totalPracticeSessions", FieldValue.increment(1),
                        "totalStudyTime", FieldValue.increment(sessionDuration),
                        "lastLoginTime", practiceEndTime,
                        "dailyPracticeStreak." + today, FieldValue.increment(1)
                )
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to update existing user data, creating new", e);
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("totalPracticeSessions", 1);
                    userData.put("totalStudyTime", sessionDuration);
                    userData.put("lastLoginTime", practiceEndTime);
                    userData.put("dailyPracticeStreak", Map.of(today, 1));

                    db.collection("user-data").document(userId).set(userData);
                });
    }

    private double calculateAverageDifficulty() {
        Map<String, Double> difficultyValues = new HashMap<>();
        difficultyValues.put("Very easy", 1.0);
        difficultyValues.put("Easy", 2.0);
        difficultyValues.put("Medium", 3.0);
        difficultyValues.put("Hard", 4.0);
        difficultyValues.put("Very hard", 5.0);

        double totalDifficulty = 0.0;
        int totalResponses = 0;

        for (Map.Entry<String, Integer> entry : sessionStats.entrySet()) {
            String difficulty = entry.getKey();
            int count = entry.getValue();

            if (difficultyValues.containsKey(difficulty)) {
                totalDifficulty += difficultyValues.get(difficulty) * count;
                totalResponses += count;
            }
        }

        double avgDifficulty = totalResponses > 0 ? totalDifficulty / totalResponses : 3.0;
        Log.d(TAG, "Calculated average difficulty: " + avgDifficulty + " from " + totalResponses + " responses");
        return avgDifficulty;
    }

    private void goToFront(String deckId, String nextCardId) {
        Intent intent = new Intent(this, PracticeFrontActivity.class);
        intent.putExtra("deck_id", deckId);
        intent.putExtra("card_id", nextCardId);
        startActivity(intent);
        finish();
    }

    private void goToSummaryScreen() {
        Log.d(TAG, "=== PREPARING SUMMARY DATA ===");
        goingToSummary = true;

        if (sessionStats == null || sessionStats.isEmpty()) {
            Log.w(TAG, "Session stats is null or empty, this shouldn't happen");
            initializeSessionStats();
        }

        Log.d(TAG, "Final session stats before summary:");
        for (Map.Entry<String, Integer> entry : sessionStats.entrySet()) {
            Log.d(TAG, entry.getKey() + ": " + entry.getValue());
        }

        Integer veryEasy = sessionStats.getOrDefault("Very easy", 0);
        Integer easy = sessionStats.getOrDefault("Easy", 0);
        Integer medium = sessionStats.getOrDefault("Medium", 0);
        Integer hard = sessionStats.getOrDefault("Hard", 0);
        Integer veryHard = sessionStats.getOrDefault("Very hard", 0);

        int total = veryEasy + easy + medium + hard + veryHard;
        Log.d(TAG, "Calculated total responses: " + total);
        Log.d(TAG, "Tracked total cards answered: " + totalCardsAnswered);

        Intent intent = new Intent(this, PracticeSummaryActivity.class);
        intent.putExtra("very_easy", veryEasy);
        intent.putExtra("easy", easy);
        intent.putExtra("medium", medium);
        intent.putExtra("hard", hard);
        intent.putExtra("very_hard", veryHard);
        intent.putExtra("debug_total", total);

        intent.putExtra("practice_start_time", practiceStartTime);

        HashMap<String, Integer> statsBackup = new HashMap<>(sessionStats);
        intent.putExtra("stats_backup", statsBackup);

        Log.d(TAG, "Starting PracticeSummaryActivity with data:");
        Log.d(TAG, "Very easy: " + veryEasy + ", Easy: " + easy + ", Medium: " + medium +
                ", Hard: " + hard + ", Very hard: " + veryHard);
        Log.d(TAG, "Practice start time: " + practiceStartTime);

        startActivity(intent);
        finish();
    }
    public static List<String> getEligibleCardIds() {
        return new ArrayList<>(eligibleCardIds);
    }

    public static void setPracticeStartTime(long startTime) {
        practiceStartTime = startTime;
        Log.d(TAG, "Practice start time set externally: " + startTime);
    }

    public static void resetStats() {
        if (!goingToSummary) {
            Log.d(TAG, "Resetting session stats (not going to summary)");
            sessionStats.clear();
            sessionStatsInitialized = false;
            eligibleCardIds.clear();
            isShuffled = false;
            practiceStartTime = 0;
            totalCardsAnswered = 0;
        } else {
            Log.d(TAG, "Reset blocked - going to summary screen");
        }
    }

    public static void forceResetStats() {
        Log.d(TAG, "Force resetting all session data");
        sessionStats.clear();
        sessionStatsInitialized = false;
        eligibleCardIds.clear();
        isShuffled = false;
        practiceStartTime = 0;
        goingToSummary = false;
        totalCardsAnswered = 0;
    }

    public static Map<String, Integer> getCurrentStats() {
        return new HashMap<>(sessionStats);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called, goingToSummary: " + goingToSummary);
        Log.d(TAG, "Final session stats in onDestroy: " + sessionStats.toString());
    }
}