package com.example.licentfront.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.licentfront.R;
import com.example.licentfront.adapters.FlashcardAdapter;
import com.example.licentfront.models.Flashcard;
import com.example.licentfront.repositories.DeckRepository;
import com.example.licentfront.repositories.FlashcardRepository;
import com.example.licentfront.utils.AIUtils;
import com.example.licentfront.utils.DialogUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditDecksActivity extends BaseActivity {

    private RecyclerView flashcardRecyclerView;
    private FlashcardAdapter adapter;
    private List<Flashcard> flashcardList;
    private FlashcardRepository flashcardRepository;
    private DeckRepository deckRepository;
    private Button addNewCardBtn;
    private ImageView deleteDeckBtn;
    private String deckId;
    private CardView saveLangBtn,saveTitleBtn;
    private EditText languageEdit,titleEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_decks);

        deckId = getIntent().getStringExtra("deck_id");
        flashcardList = new ArrayList<>();
        adapter = new FlashcardAdapter(flashcardList, this, deckId);

        flashcardRecyclerView = findViewById(R.id.flashcard_recycler);
        flashcardRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        flashcardRecyclerView.setAdapter(adapter);

        flashcardRepository = new FlashcardRepository();
        deckRepository = new DeckRepository();

        deleteDeckBtn = findViewById(R.id.delete_deck);
        deleteDeckBtn.setOnClickListener(v -> {
            DialogUtils.showDeleteDialog(this, "deck", () -> {
                deckRepository.deleteDeck(deckId, success -> {
                    if (success) {
                        Toast.makeText(this, "Deck deleted!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, DecksActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to delete deck.", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        addNewCardBtn = findViewById(R.id.add_flashcard_btn);
        addNewCardBtn.setOnClickListener(v -> showAddCardDialog());
        //save changes to deck logic
        saveLangBtn = findViewById(R.id.save_language_btn);
        saveTitleBtn = findViewById(R.id.save_title_btn);
        languageEdit = findViewById(R.id.language_edit_btn);
        titleEdit = findViewById(R.id.title_edit_btn);

        saveLangBtn.setOnClickListener(v -> {
            String newLanguage = languageEdit.getText().toString().trim();
            if (!newLanguage.isEmpty()) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("language", newLanguage);
                deckRepository.updateDeck(deckId, updates, success -> {
                    if (success) {
                        Toast.makeText(this, "Language updated successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to update language", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        saveTitleBtn.setOnClickListener(v -> {
            String newTitle = titleEdit.getText().toString().trim();
            if (!newTitle.isEmpty()) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("title", newTitle);
                deckRepository.updateDeck(deckId, updates, success -> {
                    if (success) {
                        Toast.makeText(this, "Title updated successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to update title", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        loadDeckDetails();
        loadFlashcards();
    }

    @Override
    protected void onBackIconPressed() {
        Intent intent = new Intent(this, DecksActivity.class);
        startActivity(intent);
        finish();
    }
    private void loadDeckDetails() {
        deckRepository.getDeck(deckId, new DeckRepository.DeckCallback() {
            @Override
            public void onSuccess(String title, String language) {
                EditText languageEdit = findViewById(R.id.language_edit_btn);
                EditText titleEdit = findViewById(R.id.title_edit_btn);

                languageEdit.setText(language);
                titleEdit.setText(title);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(EditDecksActivity.this, "Failed to load deck details.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFlashcards() {
        flashcardRepository.getFlashcards(deckId, new FlashcardRepository.FlashcardsCallback() {
            @Override
            public void onSuccess(List<Flashcard> flashcards) {
                flashcardList.clear();
                flashcardList.addAll(flashcards);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("EditDecksActivity", "Failed to load flashcards", e);
            }
        });
    }
    private void showAddCardDialog() {
        DialogUtils.showEditDecksAddCardDialog(this, deckId, (front, back) -> {
            flashcardList.add(new Flashcard(front, back));
            adapter.notifyItemInserted(flashcardList.size() - 1);
        });
    }

}
