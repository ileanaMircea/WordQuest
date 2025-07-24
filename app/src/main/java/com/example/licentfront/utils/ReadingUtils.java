package com.example.licentfront.utils;

import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ReadingUtils {

    private static FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static void populateLanguageSpinner(Context context, Spinner spinner) {
        List<String> languages = List.of(
                "English", "German", "French", "Spanish", "Italian",
                "Polish", "Dutch", "Portuguese", "Romanian", "Czech"
        );

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    public static String getLanguageCode(String language) {
        switch (language.toLowerCase()) {
            case "english": return "en";
            case "french": return "fr";
            case "german": return "de";
            case "spanish": return "es";
            case "italian": return "it";
            case "polish": return "pl";
            case "dutch": return "nl";
            case "portuguese": return "pt";
            case "romanian": return "ro";
            case "czech": return "cs";
            default: return "en";
        }
    }

    public static void populateDeckSpinner(Context context, Spinner dummySpinner, FirebaseFirestore db, String autoSelectLanguage, java.util.function.Consumer<String> callback) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("user-decks").document(uid).collection("decks")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String language = doc.getString("language");
                        String docId = doc.getId();

                        if (language != null && language.equalsIgnoreCase(autoSelectLanguage)) {
                            callback.accept(docId);
                            return;
                        }
                    }

                    Toast.makeText(context, "No deck found for language: " + autoSelectLanguage, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Failed to load decks", Toast.LENGTH_SHORT).show());
    }

    // ===== BOOK PROGRESS TRACKING METHODS =====

    /**
     * Mark a book as started when user opens it for the first time
     */
    public static void markBookAsStarted(String userBookId, String bookId) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userId == null) return;

        long currentTime = System.currentTimeMillis();

        Map<String, Object> updates = new HashMap<>();
        updates.put("isStarted", true);
        updates.put("startedDate", currentTime);
        updates.put("lastReadTime", currentTime);

        // Update user-books collection
        db.collection("user-books").document(userId).collection("books").document(userBookId)
                .update(updates)
                .addOnFailureListener(e -> {
                    // If document doesn't exist, create it
                    updates.put("bookId", bookId);
                    updates.put("currentPage", 0);
                    updates.put("isCompleted", false);
                    updates.put("totalReadingTime", 0);

                    db.collection("user-books").document(userId).collection("books").document(userBookId)
                            .set(updates);
                });

        // Update user statistics
        db.collection("user-data").document(userId)
                .update("totalBooksStarted", FieldValue.increment(1))
                .addOnFailureListener(e -> {
                    // If user document doesn't exist, create it
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("totalBooksStarted", 1);
                    userData.put("totalBooksCompleted", 0);
                    userData.put("totalPracticeSessions", 0);
                    userData.put("totalStudyTime", 0);
                    userData.put("lastLoginTime", currentTime);

                    db.collection("user-data").document(userId).set(userData);
                });
    }

    /**
     * Mark a book as completed when user finishes reading it
     */
    public static void markBookAsCompleted(String userBookId) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userId == null) return;

        long currentTime = System.currentTimeMillis();

        Map<String, Object> updates = new HashMap<>();
        updates.put("isCompleted", true);
        updates.put("completedDate", currentTime);
        updates.put("lastReadTime", currentTime);

        // Update user-books collection
        db.collection("user-books").document(userId).collection("books").document(userBookId)
                .update(updates);

        // Update user statistics
        db.collection("user-data").document(userId)
                .update("totalBooksCompleted", FieldValue.increment(1));
    }

    /**
     * Update reading progress when user navigates through pages
     */
    public static void updateReadingProgress(String userBookId, int currentPage, int totalPages) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userId == null) return;

        long currentTime = System.currentTimeMillis();

        Map<String, Object> updates = new HashMap<>();
        updates.put("currentPage", currentPage);
        updates.put("totalPages", totalPages);
        updates.put("lastReadTime", currentTime);

        // Check if book is completed
        if (currentPage >= totalPages) {
            updates.put("isCompleted", true);
            updates.put("completedDate", currentTime);

            // Update user completed books count
            db.collection("user-data").document(userId)
                    .update("totalBooksCompleted", FieldValue.increment(1));
        }

        // Update user-books collection
        db.collection("user-books").document(userId).collection("books").document(userBookId)
                .update(updates);
    }

    /**
     * Update total reading time spent on a book
     */
    public static void updateReadingTime(String userBookId, long additionalTime) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userId == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("totalReadingTime", FieldValue.increment(additionalTime));
        updates.put("lastReadTime", System.currentTimeMillis());

        // Update user-books collection
        db.collection("user-books").document(userId).collection("books").document(userBookId)
                .update(updates);

        // Update user total study time
        db.collection("user-data").document(userId)
                .update("totalStudyTime", FieldValue.increment(additionalTime));
    }

    /**
     * Initialize a new book in user's library with starting values
     */
    public static void initializeBookInLibrary(String userBookId, String bookId, String title, String author, String language) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userId == null) return;

        long currentTime = System.currentTimeMillis();

        Map<String, Object> bookData = new HashMap<>();
        bookData.put("bookId", bookId);
        bookData.put("title", title);
        bookData.put("author", author);
        bookData.put("language", language);
        bookData.put("currentPage", 0);
        bookData.put("totalPages", 0);
        bookData.put("isStarted", false);
        bookData.put("isCompleted", false);
        bookData.put("startedDate", 0);
        bookData.put("completedDate", 0);
        bookData.put("lastReadTime", 0);
        bookData.put("totalReadingTime", 0);
        bookData.put("addedToLibraryDate", currentTime);

        db.collection("user-books").document(userId).collection("books").document(userBookId)
                .set(bookData);
    }
}