package com.example.licentfront.repositories;

import android.util.Log;

import com.example.licentfront.models.Book;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserLibraryRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String USERS_COLLECTION = "users";
    private static final String USER_BOOKS_SUBCOLLECTION = "userBooks";

    public interface BookListCallback {
        void onSuccess(List<Book> books);
        void onFailure(Exception e);
    }

    public interface SimpleCallback {
        void onComplete(boolean success);
    }

    public interface ProgressCallback {
        void onComplete(boolean success);
    }

    // get books by language
    public void getUserBooks(String userId, String language, BookListCallback callback) {
        db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(USER_BOOKS_SUBCOLLECTION)
                .whereEqualTo("language", language)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Book> books = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Book book = doc.toObject(Book.class);
                        if (book != null) {
                            book.setId(doc.getId());
                            books.add(book);
                        }
                    }
                    Log.d("UserLibraryRepository", "User books fetched: " + books.size());
                    callback.onSuccess(books);
                })
                .addOnFailureListener(e -> {
                    Log.e("UserLibraryRepository", "Error fetching user books", e);
                    callback.onFailure(e);
                });
    }

    // add to user's library
    public void addBookToLibrary(String userId, Book book, SimpleCallback callback) {
        Map<String, Object> userBook = new HashMap<>();
        userBook.put("title", book.getTitle());
        userBook.put("author", book.getAuthor());
        userBook.put("language", book.getLanguage());
        userBook.put("content", book.getContent());
        userBook.put("totalPages", book.getTotalPages());
        userBook.put("currentPage", 0);
        userBook.put("isCompleted", false);
        userBook.put("originalBookId", book.getId());
        userBook.put("dateAdded", System.currentTimeMillis());

        db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(USER_BOOKS_SUBCOLLECTION)
                .add(userBook)
                .addOnSuccessListener(docRef -> {
                    Log.d("UserLibraryRepository", "Book added to user library: " + docRef.getId());
                    callback.onComplete(true);
                })
                .addOnFailureListener(e -> {
                    Log.e("UserLibraryRepository", "Error adding book to library", e);
                    callback.onComplete(false);
                });
    }
    public void removeBookFromLibrary(String userId, String bookId, SimpleCallback callback) {
        db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(USER_BOOKS_SUBCOLLECTION)
                .document(bookId)
                .delete()
                .addOnSuccessListener(unused -> {
                    Log.d("UserLibraryRepository", "Book removed from library");
                    callback.onComplete(true);
                })
                .addOnFailureListener(e -> {
                    Log.e("UserLibraryRepository", "Error removing book", e);
                    callback.onComplete(false);
                });
    }

    public void updateReadingProgress(String userId, String bookId, int currentPage, ProgressCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("currentPage", currentPage);
        updates.put("lastReadTime", System.currentTimeMillis());

        db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(USER_BOOKS_SUBCOLLECTION)
                .document(bookId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    Log.d("UserLibraryRepository", "Progress updated: page " + currentPage);
                    callback.onComplete(true);
                })
                .addOnFailureListener(e -> {
                    Log.e("UserLibraryRepository", "Error updating progress", e);
                    callback.onComplete(false);
                });
    }

    public void markBookAsCompleted(String userId, String bookId, ProgressCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isCompleted", true);
        updates.put("completedTime", System.currentTimeMillis());

        db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(USER_BOOKS_SUBCOLLECTION)
                .document(bookId)
                .update(updates)
                .addOnSuccessListener(unused -> {
                    Log.d("UserLibraryRepository", "Book marked as completed");
                    callback.onComplete(true);
                })
                .addOnFailureListener(e -> {
                    Log.e("UserLibraryRepository", "Error marking book as completed", e);
                    callback.onComplete(false);
                });
    }

    public void isBookInLibrary(String userId, String originalBookId, SimpleCallback callback) {
        db.collection(USERS_COLLECTION)
                .document(userId)
                .collection(USER_BOOKS_SUBCOLLECTION)
                .whereEqualTo("originalBookId", originalBookId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    boolean exists = !snapshot.isEmpty();
                    callback.onComplete(exists);
                })
                .addOnFailureListener(e -> {
                    Log.e("UserLibraryRepository", "Error checking if book exists", e);
                    callback.onComplete(false);
                });
    }
}