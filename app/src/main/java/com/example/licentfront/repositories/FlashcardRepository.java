package com.example.licentfront.repositories;

import com.example.licentfront.models.Flashcard;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlashcardRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    public interface FlashcardsCallback {
        void onSuccess(List<Flashcard> flashcards);
        void onFailure(Exception e);
    }

    public interface SimpleCallback {
        void onComplete(boolean success);
    }
    public interface FlashcardCallback {
        void onSuccess(String back, String example);
        void onFailure(Exception e);
    }
    public interface EligibleCardsCallback {
        void onSuccess(List<String> eligibleCardIds);
        void onFailure(Exception e);
    }

    public void getEligibleCards(String deckId, EligibleCardsCallback callback) {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("user-decks").document(uid).collection("decks").document(deckId)
                .collection("flashcards")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> eligibleCardIds = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Long nextReviewTime = doc.getLong("nextReviewTime");
                        if (nextReviewTime == null || nextReviewTime <= System.currentTimeMillis()) {
                            eligibleCardIds.add(doc.getId());
                        }
                    }
                    callback.onSuccess(eligibleCardIds);
                })
                .addOnFailureListener(callback::onFailure);
    }
    public void getFlashcard(String deckId, String cardId, FlashcardCallback callback) {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("user-decks").document(uid).collection("decks").document(deckId)
                .collection("flashcards").document(cardId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String back = documentSnapshot.getString("back");
                        String example = documentSnapshot.getString("example");
                        callback.onSuccess(back, example);
                    } else {
                        callback.onFailure(new Exception("Card not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void getFlashcards(String deckId, FlashcardsCallback callback) {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("user-decks").document(uid).collection("decks").document(deckId)
                .collection("flashcards")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Flashcard> flashcards = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot) {
                        String front = doc.getString("front");
                        String back = doc.getString("back");
                        String example = doc.getString("example");
                        String id=doc.getId();
                        if (front != null && back != null) {
                            Flashcard card = new Flashcard(front, back, example);
                            card.setId(id);
                            flashcards.add(card);
                        }
                    }
                    callback.onSuccess(flashcards);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void addFlashcard(String deckId, Map<String, Object> flashcardData, SimpleCallback callback) {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("user-decks").document(uid).collection("decks").document(deckId)
                .collection("flashcards")
                .add(flashcardData)
                .addOnSuccessListener(documentReference -> callback.onComplete(true))
                .addOnFailureListener(e -> callback.onComplete(false));
    }

    public void deleteFlashcard(String deckId, Flashcard flashcard, SimpleCallback callback) {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("user-decks").document(uid).collection("decks").document(deckId)
                .collection("flashcards")
                .whereEqualTo("front", flashcard.getFront())
                .whereEqualTo("back", flashcard.getBack())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            document.getReference().delete()
                                    .addOnSuccessListener(aVoid -> callback.onComplete(true))
                                    .addOnFailureListener(e -> callback.onComplete(false));
                        }
                    } else {
                        callback.onComplete(false);
                    }
                })
                .addOnFailureListener(e -> callback.onComplete(false));
    }

    public void updateNextReview(String deckId, String cardId, String difficulty, SimpleCallback callback) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("user-decks").document(uid)
                .collection("decks").document(deckId)
                .collection("flashcards").document(cardId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        callback.onComplete(false);
                        return;
                    }

                    Flashcard flashcard = documentSnapshot.toObject(Flashcard.class);
                    if (flashcard == null) {
                        callback.onComplete(false);
                        return;
                    }

                    double ef = flashcard.getEaseFactor();
                    int rep = flashcard.getRepetition();
                    int interval = flashcard.getInterval();

                    //map difficulty to SM-2 quality score
                    //EF = ease factor (cat de usor a fost sa ti l amintesti)
                    //rep = de cate ori ai raspuns corect
                    //interval = in cate zile vei revizita cardul

                    int quality;
                    switch (difficulty.toLowerCase()) {
                        case "very hard": quality = 0; break;
                        case "hard": quality = 2; break;
                        case "medium": quality = 3; break;
                        case "easy": quality = 4; break;
                        case "very easy": quality = 5; break;
                        default: quality = 4; break;
                    }

                    if (quality < 3) {
                        rep = 0;
                        interval = 1;
                    } else {
                        if (rep == 0)
                            interval = 1;
                        else if (rep == 1)
                            interval = 6;
                        else interval = (int) Math.round(interval * ef);

                        ef = ef + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
                        if (ef < 1.3)
                            ef = 1.3;
                        rep += 1;
                    }

                    long nextReviewTime = System.currentTimeMillis() + interval * 24L * 60 * 60 * 1000; //acum + "interval" zile

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("easeFactor", ef);
                    updates.put("repetition", rep);
                    updates.put("interval", interval);
                    updates.put("nextReviewTime", nextReviewTime);

                    db.collection("user-decks").document(uid)
                            .collection("decks").document(deckId)
                            .collection("flashcards").document(cardId)
                            .update(updates)
                            .addOnSuccessListener(aVoid -> callback.onComplete(true))
                            .addOnFailureListener(e -> callback.onComplete(false));
                })
                .addOnFailureListener(e -> callback.onComplete(false));
    }

    public void updateFlashcardById(String deckId, String cardId, Map<String, Object> updatedData, SimpleCallback callback) {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("user-decks").document(uid).collection("decks").document(deckId)
                .collection("flashcards").document(cardId)
                .update(updatedData)
                .addOnSuccessListener(aVoid -> callback.onComplete(true))
                .addOnFailureListener(e -> callback.onComplete(false));
    }


}
