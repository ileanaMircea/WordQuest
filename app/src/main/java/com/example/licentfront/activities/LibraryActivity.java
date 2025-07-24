package com.example.licentfront.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.licentfront.R;
import com.example.licentfront.adapters.BookAdapter;
import com.example.licentfront.models.Book;
import com.example.licentfront.repositories.BookRepository;
import com.example.licentfront.repositories.UserLibraryRepository;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class LibraryActivity extends BaseActivity {

    private RecyclerView booksRecyclerView;
    private Spinner languageSpinner;
    private MaterialButton btnMyLibrary, btnAllBooks;
    private ProgressBar progressBar;
    private LinearLayout emptyState;
    private TextView emptyMessage;

    private BookRepository bookRepository;
    private UserLibraryRepository userLibraryRepository;
    private BookAdapter bookAdapter;

    private boolean isMyLibraryMode = true;
    private String currentLanguage = "";
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        initViews();
        initRepositories();
        setupRecyclerView();
        setupSpinner();
        setupToggleButtons();

        //get user
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "Please log in to continue", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        booksRecyclerView = findViewById(R.id.books_recycler_view);
        languageSpinner = findViewById(R.id.language_spinner);
        btnMyLibrary = findViewById(R.id.btn_my_library);
        btnAllBooks = findViewById(R.id.btn_all_books);
        progressBar = findViewById(R.id.progress_bar);
        emptyState = findViewById(R.id.empty_state);
        emptyMessage = findViewById(R.id.empty_message);
    }

    private void initRepositories() {
        bookRepository = new BookRepository();
        userLibraryRepository = new UserLibraryRepository();
    }

    private void setupRecyclerView() {
        booksRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        bookAdapter = new BookAdapter(new ArrayList<>(), isMyLibraryMode, new BookAdapter.OnBookActionListener() {
            @Override
            public void onAddToLibrary(Book book) {
                addBookToLibrary(book);
            }
            @Override
            public void onRemoveFromLibrary(Book book) {
                removeBookFromLibrary(book);
            }
            @Override
            public void onReadBook(Book book) {
                openBookReader(book);
            }
        });
        booksRecyclerView.setAdapter(bookAdapter);
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> langAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.language_options,
                android.R.layout.simple_spinner_item
        );
        langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(langAdapter);
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentLanguage = parent.getItemAtPosition(position).toString();
                loadBooks();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupToggleButtons() {
        btnMyLibrary.setOnClickListener(v -> {
            if (!isMyLibraryMode) {
                switchToMyLibrary();
            }
        });

        btnAllBooks.setOnClickListener(v -> {
            if (isMyLibraryMode) {
                switchToAllBooks();
            }
        });
    }

    private void switchToMyLibrary() {
        isMyLibraryMode = true;
        updateToggleButtonsUI();
        bookAdapter.setMyLibraryMode(true);
        emptyMessage.setText("No books in your library.\nAdd some from 'All Books'!");
        loadBooks();
    }

    private void switchToAllBooks() {
        isMyLibraryMode = false;
        updateToggleButtonsUI();
        bookAdapter.setMyLibraryMode(false);
        emptyMessage.setText("No books available for this language.");
        loadBooks();
    }

    private void updateToggleButtonsUI() {
        if (isMyLibraryMode) {
            btnMyLibrary.setBackgroundTintList(getColorStateList(R.color.dark_green));
            btnMyLibrary.setTextColor(getColor(R.color.white));
            btnAllBooks.setBackgroundTintList(getColorStateList(R.color.gray));
            btnAllBooks.setTextColor(getColor(R.color.black));
        } else {
            btnMyLibrary.setBackgroundTintList(getColorStateList(R.color.gray));
            btnMyLibrary.setTextColor(getColor(R.color.black));
            btnAllBooks.setBackgroundTintList(getColorStateList(R.color.dark_green));
            btnAllBooks.setTextColor(getColor(R.color.white));
        }
    }

    private void loadBooks() {
        if (currentLanguage.isEmpty()) return;
        showLoading(true);
        if (isMyLibraryMode) {
            loadUserLibraryBooks();
        } else {
            loadAllBooks();
        }
    }

    private void loadUserLibraryBooks() {
        userLibraryRepository.getUserBooks(currentUserId, currentLanguage, new UserLibraryRepository.BookListCallback() {
            @Override
            public void onSuccess(List<Book> books) {
                showLoading(false);
                updateBooksList(books);
            }

            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                Toast.makeText(LibraryActivity.this, "Failed to load your library", Toast.LENGTH_SHORT).show();
                updateBooksList(new ArrayList<>());
            }
        });
    }

    private void loadAllBooks() {
        bookRepository.getBooksByLanguage(currentLanguage, new BookRepository.BookListCallback() {
            @Override
            public void onSuccess(List<Book> books) {
                showLoading(false);
                updateBooksList(books);
            }

            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                Toast.makeText(LibraryActivity.this, "Failed to load books", Toast.LENGTH_SHORT).show();
                updateBooksList(new ArrayList<>());
            }
        });
    }

    private void addBookToLibrary(Book book) {
        userLibraryRepository.addBookToLibrary(currentUserId, book, new UserLibraryRepository.SimpleCallback() {
            @Override
            public void onComplete(boolean success) {
                if (success) {
                    Toast.makeText(LibraryActivity.this, "Book added to your library!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LibraryActivity.this, "Failed to add book", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void removeBookFromLibrary(Book book) {
        userLibraryRepository.removeBookFromLibrary(currentUserId, book.getId(), new UserLibraryRepository.SimpleCallback() {
            @Override
            public void onComplete(boolean success) {
                if (success) {
                    Toast.makeText(LibraryActivity.this, "Book removed from library", Toast.LENGTH_SHORT).show();
                    loadBooks(); //refresh list
                } else {
                    Toast.makeText(LibraryActivity.this, "Failed to remove book", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void openBookReader(Book book) {
        Intent intent = new Intent(LibraryActivity.this, ReadBookActivity.class);
        intent.putExtra("book_id", book.getId());
        intent.putExtra("selected_language", currentLanguage);
        startActivity(intent);
    }

    private void updateBooksList(List<Book> books) {
        bookAdapter.updateBooks(books);

        if (books.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            booksRecyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            booksRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            emptyState.setVisibility(View.GONE);
        }
    }
}