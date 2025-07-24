package com.example.licentfront.repositories;

import android.util.Log;

import com.example.licentfront.models.Book;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class BookRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String collectionPath = "books";

    public interface BookListCallback {
        void onSuccess(List<Book> books);
        void onFailure(Exception e);
    }

    public interface SimpleCallback {
        void onComplete(boolean success);
    }

    public void getBooksByLanguage(String language, BookListCallback callback) {
        db.collection(collectionPath)
                .whereEqualTo("language", language)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Book> books = new ArrayList<>();
                    int count = 0;
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Book book = doc.toObject(Book.class);
                        if (book != null) {
                            count++;
                            book.setId(doc.getId());
                            books.add(book);
                        }
                    }
                    Log.d("BookRepository", "Books fetched for language " + language + ": " + count);
                    callback.onSuccess(books);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void addBook(Book book, SimpleCallback callback) {
        db.collection(collectionPath)
                .add(book)
                .addOnSuccessListener(docRef -> callback.onComplete(true))
                .addOnFailureListener(e -> callback.onComplete(false));
    }

    public void deleteBook(String bookId, SimpleCallback callback) {
        db.collection(collectionPath)
                .document(bookId)
                .delete()
                .addOnSuccessListener(unused -> callback.onComplete(true))
                .addOnFailureListener(e -> callback.onComplete(false));
    }

    public void updateBook(String bookId, Book updatedBook, SimpleCallback callback) {
        db.collection(collectionPath)
                .document(bookId)
                .set(updatedBook)
                .addOnSuccessListener(unused -> callback.onComplete(true))
                .addOnFailureListener(e -> callback.onComplete(false));
    }
}
