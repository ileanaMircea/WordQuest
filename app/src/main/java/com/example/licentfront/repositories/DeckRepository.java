// /repositories/DeckRepository.java
package com.example.licentfront.repositories;

import com.example.licentfront.models.Deck;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DeckRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    public interface DecksCallback {
        void onSuccess(List<Deck> decks);
        void onFailure(Exception e);
    }

    public interface SimpleCallback {
        void onComplete(boolean success);
    }

    public void getDecks(DecksCallback callback) {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("user-decks").document(uid).collection("decks")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Deck> decks = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot) {
                        String title = doc.getString("title");
                        Long day = doc.getLong("day");
                        String id = doc.getId();
                        String language = doc.getString("language");
                        if (title != null && day != null) {
                            decks.add(new Deck(id, title, language, day.intValue()));
                        }
                    }
                    callback.onSuccess(decks);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void addDeck(Map<String, Object> deckData, SimpleCallback callback) {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("user-decks").document(uid).collection("decks")
                .add(deckData)
                .addOnSuccessListener(documentReference -> callback.onComplete(true))
                .addOnFailureListener(e -> callback.onComplete(false));
    }

    public void deleteDeck(String deckId, SimpleCallback callback) {
        String uid = mAuth.getCurrentUser().getUid();
        // get all flashcards
        db.collection("user-decks").document(uid).collection("decks")
                .document(deckId).collection("flashcards")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    //delete each flashcard
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        document.getReference().delete();
                    }
                    //after deleting flashcards, delete the deck
                    db.collection("user-decks").document(uid).collection("decks")
                            .document(deckId)
                            .delete()
                            .addOnSuccessListener(aVoid -> callback.onComplete(true))
                            .addOnFailureListener(e -> callback.onComplete(false));
                })
                .addOnFailureListener(e -> callback.onComplete(false));
    }
    public interface DeckCallback {
        void onSuccess(String title, String language);
        void onFailure(Exception e);
    }

    public void getDeck(String deckId, DeckCallback callback) {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("user-decks").document(uid).collection("decks").document(deckId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String title = documentSnapshot.getString("title");
                        String language = documentSnapshot.getString("language");
                        callback.onSuccess(title != null ? title : "", language != null ? language : "");
                    } else {
                        callback.onFailure(new Exception("Deck not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }
    public void updateDeck(String deckId, Map<String, Object> updates, SimpleCallback callback) {
        String uid = mAuth.getCurrentUser().getUid();
        db.collection("user-decks").document(uid).collection("decks").document(deckId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onComplete(true))
                .addOnFailureListener(e -> callback.onComplete(false));
    }


}
