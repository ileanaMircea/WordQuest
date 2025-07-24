package com.example.licentfront.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;

import com.example.licentfront.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class PracticeFrontActivity extends BaseActivity {

    private static final String TAG = "PracticeFront";
    private static boolean isFirstCard = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_practice_front);

        String deckId = getIntent().getStringExtra("deck_id");
        String cardId = getIntent().getStringExtra("card_id");

        // Reset stats only for the first card of a new practice session
        if (isFirstCard) {
            Log.d(TAG, "First card detected, resetting stats for new practice session");
            PracticeBackActivity.forceResetStats();
            PracticeBackActivity.setPracticeStartTime(System.currentTimeMillis());
            isFirstCard = false;
            Log.d(TAG, "Stats reset complete");
        } else {
            Log.d(TAG, "Continuing practice session - not first card");
        }

        TextView frontTextView = findViewById(R.id.front_text);
        Button nextButton = findViewById(R.id.flip_button);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Load and display the flashcard
        loadFlashcard(db, uid, deckId, cardId, frontTextView);

        // Set up the flip button
        nextButton.setOnClickListener(v -> {
            Log.d(TAG, "Flip button clicked, going to back of card: " + cardId);
            Intent intent = new Intent(this, PracticeBackActivity.class);
            intent.putExtra("deck_id", deckId);
            intent.putExtra("card_id", cardId);
            startActivity(intent);
            finish();
        });
    }

    private void loadFlashcard(FirebaseFirestore db, String uid, String deckId, String cardId, TextView frontTextView) {
        db.collection("user-decks").document(uid).collection("decks").document(deckId)
                .collection("flashcards").document(cardId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long nextReviewTime = documentSnapshot.getLong("nextReviewTime");
                        long now = System.currentTimeMillis();

                        if (nextReviewTime == null || nextReviewTime <= now) {
                            String front = documentSnapshot.getString("front");
                            frontTextView.setText(front != null ? front : "No content");
                            Log.d(TAG, "Card loaded successfully: " + cardId);
                        } else {
                            Log.d(TAG, "Card not due for review, skipping: " + cardId);
                            skipToNextDueCard(deckId, cardId);
                        }
                    } else {
                        Log.w(TAG, "Card not found: " + cardId);
                        frontTextView.setText("Card not found.");
                        skipToNextDueCard(deckId, cardId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading card: " + cardId, e);
                    frontTextView.setText("Error loading card.");
                    skipToNextDueCard(deckId, cardId);
                });
    }

    private void skipToNextDueCard(String deckId, String currentCardId) {
        Log.d(TAG, "Attempting to skip to next due card from: " + currentCardId);

        List<String> eligibleCardIds = PracticeBackActivity.getEligibleCardIds();

        if (eligibleCardIds == null || eligibleCardIds.isEmpty()) {
            Log.d(TAG, "No eligible cards available, ending practice session");
            resetForNewSession();
            finish();
            return;
        }

        // If this is the only card and it's not due, end the session
        if (eligibleCardIds.size() == 1 && eligibleCardIds.get(0).equals(currentCardId)) {
            Log.d(TAG, "Only one card available and it's not due, ending practice session");
            resetForNewSession();
            finish();
            return;
        }

        int currentIndex = eligibleCardIds.indexOf(currentCardId);
        int nextIndex;

        if (currentIndex == -1) {
            nextIndex = 0;
            Log.d(TAG, "Current card not in eligible list, starting from beginning");
        } else if (currentIndex + 1 >= eligibleCardIds.size()) {
            nextIndex = 0;
            Log.d(TAG, "At end of eligible cards, wrapping to beginning");
        } else {
            nextIndex = currentIndex + 1;
            Log.d(TAG, "Moving to next card in sequence");
        }

        String nextCardId = eligibleCardIds.get(nextIndex);
        Log.d(TAG, "Going to next card: " + nextCardId);

        Intent intent = new Intent(this, PracticeFrontActivity.class);
        intent.putExtra("deck_id", deckId);
        intent.putExtra("card_id", nextCardId);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called, isFinishing: " + isFinishing());
    }

    public static void resetForNewSession() {
        isFirstCard = true;
        Log.d(TAG, "Session flag reset - next card will be treated as first card");
    }

    public static boolean isFirstCard() {
        return isFirstCard;
    }
}