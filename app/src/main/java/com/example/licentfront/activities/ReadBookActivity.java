package com.example.licentfront.activities;

import static com.example.licentfront.utils.ReadingUtils.getLanguageCode;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.licentfront.R;
import com.example.licentfront.models.Book;
import com.example.licentfront.repositories.UserLibraryRepository;
import com.example.licentfront.utils.AIUtils;
import com.example.licentfront.utils.DialogUtils;
import com.example.licentfront.utils.ReadingUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReadBookActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String bookId;
    private String userBookId;
    private TextView bookTextView;
    private Button nextPageBtn, prevPageBtn;
    private UserLibraryRepository repository;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final int WORDS_PER_PAGE = 150;
    private List<String> words = new ArrayList<>();
    private int currentPage = 0;
    private String bookLanguage = "";
    private String selectedDeckId = null;
    private boolean isBookLoaded = false;
    private int totalPages = 0;
    private String userNativeLanguage = "English";
    private long readingStartTime = 0;
    private long totalSessionTime = 0;
    private boolean isBookStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_book);

        findViewById(R.id.back_btn).setOnClickListener(v -> finish());

        bookTextView = findViewById(R.id.book_text);
        nextPageBtn = findViewById(R.id.next_page_btn);
        prevPageBtn = findViewById(R.id.prev_page_btn);
        repository = new UserLibraryRepository();

        db = FirebaseFirestore.getInstance();
        bookId = getIntent().getStringExtra("book_id");
        userBookId = getIntent().getStringExtra("user_book_id"); // Get user book ID
        bookLanguage = getIntent().getStringExtra("selected_language");

        if (bookId == null || bookId.isEmpty()) {
            Log.e("ReadBookActivity", "Book ID is null or empty");
            Toast.makeText(this, "Invalid book ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (userBookId == null || userBookId.isEmpty()) {
            userBookId = db.collection("user-books").document().getId();
        }

        loadUserNativeLanguage();

        Log.d("ReadBookActivity", "Starting to fetch book from Firestore");
        loadBook();
        setupNavigationButtons();
    }

    private void loadUserNativeLanguage() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("user-data").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String nativeLanguage = documentSnapshot.getString("nativeLanguage");
                        if (nativeLanguage != null && !nativeLanguage.isEmpty()) {
                            userNativeLanguage = nativeLanguage;
                            Log.d("ReadBookActivity", "User native language loaded: " + userNativeLanguage);
                        } else {
                            userNativeLanguage = "English";
                            Log.d("ReadBookActivity", "No native language found, using default: English");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ReadBookActivity", "Error loading user native language: " + e.getMessage());
                    userNativeLanguage = "English";
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        readingStartTime = System.currentTimeMillis();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (readingStartTime > 0) {
            long sessionTime = System.currentTimeMillis() - readingStartTime;
            totalSessionTime += sessionTime;

            if (userBookId != null) {
                ReadingUtils.updateReadingTime(userBookId, sessionTime);
            }
        }

        if (isBookLoaded && userBookId != null) {
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            repository.updateReadingProgress(currentUserId, bookId, currentPage, success -> {
                Log.d("ReadBookActivity", "Progress saved on pause: " + success);
            });

            ReadingUtils.updateReadingProgress(userBookId, currentPage, totalPages);
        }
    }

    private void setupNavigationButtons() {
        nextPageBtn.setOnClickListener(v -> {
            if (!isBookLoaded) return;

            if ((currentPage + 1) * WORDS_PER_PAGE < words.size()) {
                currentPage++;
                showPage(currentPage);

                if (userBookId != null) {
                    ReadingUtils.updateReadingProgress(userBookId, currentPage, totalPages);
                }
            } else {
                Toast.makeText(this, "Congratulations! You've finished the book!", Toast.LENGTH_LONG).show();
                markBookAsCompleted();
            }
        });

        prevPageBtn.setOnClickListener(v -> {
            if (!isBookLoaded) return;

            if (currentPage > 0) {
                currentPage--;
                showPage(currentPage);

                if (userBookId != null) {
                    ReadingUtils.updateReadingProgress(userBookId, currentPage, totalPages);
                }
            } else {
                Toast.makeText(this, "Beginning of book", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadBook() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users").document(currentUserId)
                .collection("userBooks").document(bookId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d("ReadBookActivity", "Firestore success callback triggered");
                    Log.d("ReadBookActivity", "Document exists: " + documentSnapshot.exists());

                    if (documentSnapshot.exists()) {
                        Log.d("ReadBookActivity", "Document data: " + documentSnapshot.getData());
                        processBookDocument(documentSnapshot, true);
                    } else {
                        Log.e("ReadBookActivity", "Document does not exist in userBooks, trying main books collection");
                        tryMainBooksCollection();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ReadBookActivity", "Firestore error in userBooks: " + e.getMessage(), e);
                    tryMainBooksCollection();
                });
    }

    private void showPage(int pageIndex) {
        Log.d("ReadBookActivity", "showPage called with index: " + pageIndex);

        if (words.isEmpty()) {
            Log.e("ReadBookActivity", "Words list is empty");
            return;
        }

        int start = pageIndex * WORDS_PER_PAGE;
        int end = Math.min(start + WORDS_PER_PAGE, words.size());

        Log.d("ReadBookActivity", "Page range: " + start + " to " + end);

        if (start >= words.size()) {
            Log.e("ReadBookActivity", "Start index exceeds words size");
            return;
        }

        List<String> pageWords = words.subList(start, end);
        String pageContent = String.join(" ", pageWords);

        Log.d("ReadBookActivity", "Page content length: " + pageContent.length());

        displayInteractiveText(pageContent);
        updateNavigationButtons();
        updateReadingProgress(pageIndex);

        markBookAsStartedIfNeeded();
    }

    private void markBookAsStartedIfNeeded() {
        if (!isBookStarted && userBookId != null && bookId != null) {
            isBookStarted = true;
            ReadingUtils.markBookAsStarted(userBookId, bookId);
            Log.d("ReadBookActivity", "Book marked as started");
        }
    }

    private void updateNavigationButtons() {
        prevPageBtn.setEnabled(currentPage > 0);
        nextPageBtn.setEnabled((currentPage + 1) * WORDS_PER_PAGE < words.size());
    }

    private void updateReadingProgress(int pageIndex) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        repository.updateReadingProgress(currentUserId, bookId, pageIndex, success -> {
            if (success) {
                Log.d("ReadBookActivity", "Reading progress updated to page " + pageIndex);
            } else {
                Log.e("ReadBookActivity", "Failed to update reading progress");
            }
        });
    }

    private void markBookAsCompleted() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        repository.markBookAsCompleted(currentUserId, bookId, success -> {
            if (success) {
                Log.d("ReadBookActivity", "Book marked as completed successfully");
            } else {
                Log.e("ReadBookActivity", "Failed to mark book as completed");
            }
        });

        if (userBookId != null) {
            ReadingUtils.markBookAsCompleted(userBookId);
        }
    }

    private void displayInteractiveText(String content) {
        if (content == null || content.trim().isEmpty()) {
            Log.e("ReadBookActivity", "Content is null or empty");
            bookTextView.setText("No content available");
            return;
        }

        executor.execute(() -> {
            try {
                SpannableString spannableString = new SpannableString(content);
                String[] localWords = content.split("\\s+");

                int currentIndex = 0;
                for (String word : localWords) {
                    if (word.trim().isEmpty()) continue;

                    int start = content.indexOf(word, currentIndex);
                    if (start == -1) continue;
                    int end = start + word.length();
                    currentIndex = end;

                    final String selectedWord = word.replaceAll("[^\\w]", "").toLowerCase();

                    if (!selectedWord.isEmpty()) {
                        ClickableSpan span = new ClickableSpan() {
                            @Override
                            public void onClick(@NonNull View widget) {
                                mainHandler.post(() -> showAddCardDialog(selectedWord));
                            }

                            @Override
                            public void updateDrawState(@NonNull android.text.TextPaint ds) {
                                super.updateDrawState(ds);
                                ds.setColor(Color.BLACK);
                                ds.setUnderlineText(false);
                            }
                        };
                        spannableString.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }

                mainHandler.post(() -> {
                    bookTextView.setText(spannableString);
                    bookTextView.setMovementMethod(LinkMovementMethod.getInstance());
                    bookTextView.setHighlightColor(Color.TRANSPARENT);
                    Log.d("ReadBookActivity", "Text displayed successfully");
                });
            } catch (Exception e) {
                Log.e("ReadBookActivity", "Error in displayInteractiveText: " + e.getMessage(), e);
                mainHandler.post(() -> {
                    bookTextView.setText(content);
                });
            }
        });
    }

    private void showAddCardDialog(String word) {
        DialogUtils.showReadBookAddCardDialog(this, selectedDeckId, word, bookLanguage, userNativeLanguage,
                (front, back) -> {
                    Log.d("ReadBookActivity", "Card added: " + front + " -> " + back);
                });
    }

    private void tryMainBooksCollection() {
        Log.d("ReadBookActivity", "Trying main books collection");

        db.collection("books").document(bookId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d("ReadBookActivity", "Main books collection - Document exists: " + documentSnapshot.exists());

                    if (documentSnapshot.exists()) {
                        Log.d("ReadBookActivity", "Document data from main collection: " + documentSnapshot.getData());
                        processBookDocument(documentSnapshot, false);
                    } else {
                        Log.e("ReadBookActivity", "Book not found in either collection");
                        Toast.makeText(this, "Book not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ReadBookActivity", "Error in main books collection: " + e.getMessage(), e);
                    Toast.makeText(this, "Error loading book: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void processBookDocument(com.google.firebase.firestore.DocumentSnapshot documentSnapshot, boolean isFromUserLibrary) {
        Book book = documentSnapshot.toObject(Book.class);
        if (book != null) {
            Log.d("ReadBookActivity", "Book object created successfully");

            if (book.getContent() != null && !book.getContent().trim().isEmpty()) {
                words = Arrays.asList(book.getContent().split("\\s+"));
                totalPages = (int) Math.ceil((double) words.size() / WORDS_PER_PAGE);
                isBookLoaded = true;

                Log.d("ReadBookActivity", "Words count: " + words.size());
                Log.d("ReadBookActivity", "Total pages: " + totalPages);

                if (!isFromUserLibrary && userBookId != null) {
                    String title = book.getTitle() != null ? book.getTitle() : "Unknown Title";
                    String author = book.getAuthor() != null ? book.getAuthor() : "Unknown Author";
                    String language = book.getLanguage() != null ? book.getLanguage() : bookLanguage;

                    ReadingUtils.initializeBookInLibrary(userBookId, bookId, title, author, language);
                }

                try {
                    ReadingUtils.populateDeckSpinner(this, new Spinner(this), db, bookLanguage, id -> selectedDeckId = id);
                } catch (Exception e) {
                    Log.e("ReadBookActivity", "Error initializing deck spinner: " + e.getMessage());
                }

                if (isFromUserLibrary) {
                    loadSavedProgress();
                } else {
                    showPage(currentPage);
                }
            } else {
                Log.e("ReadBookActivity", "Book content is null or empty");
                Toast.makeText(this, "Book content not found", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e("ReadBookActivity", "Failed to convert document to Book object");
            Toast.makeText(this, "Error parsing book data", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadSavedProgress() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users").document(currentUserId)
                .collection("userBooks").document(bookId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long savedPageLong = documentSnapshot.getLong("currentPage");
                        if (savedPageLong != null && savedPageLong > 0) {
                            currentPage = savedPageLong.intValue();
                            Log.d("ReadBookActivity", "Loaded saved progress: page " + currentPage);
                        }
                    }
                    showPage(currentPage);
                })
                .addOnFailureListener(e -> {
                    Log.e("ReadBookActivity", "Error loading saved progress: " + e.getMessage());
                    showPage(currentPage);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}