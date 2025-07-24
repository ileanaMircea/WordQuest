package com.example.licentfront.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.licentfront.R;
import com.example.licentfront.adapters.DeckAdapter;
import com.example.licentfront.models.Deck;
import com.example.licentfront.repositories.DeckRepository;
import com.example.licentfront.utils.DialogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DecksActivity extends BaseActivity {
    private RecyclerView recyclerView;
    private DeckAdapter adapter;
    private List<Deck> deckList = new ArrayList<>();
    private DeckRepository deckRepository;
    private Button addDeckBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_decks);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = findViewById(R.id.deck_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DeckAdapter(deckList, this);
        recyclerView.setAdapter(adapter);

        addDeckBtn = findViewById(R.id.add_deck_btn);
        addDeckBtn.setOnClickListener(v -> showAddDeckDialog());

        deckRepository = new DeckRepository();
        loadDecks();
    }

    @Override
    protected void onBackIconPressed() {
        Intent intent = new Intent(this, ProfileActivity.class);
        startActivity(intent);
        finish();
    }

    private void loadDecks() {
        deckRepository.getDecks(new DeckRepository.DecksCallback() {
            @Override
            public void onSuccess(List<Deck> decks) {
                deckList.clear();
                deckList.addAll(decks);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("DecksActivity", "Failed to load decks", e);
            }
        });
    }

    private void showAddDeckDialog() {
        DialogUtils.showDialog(this, R.layout.dialog_add_deck, dialog -> {
            ImageView backBtn = dialog.findViewById(R.id.dialog_back);
            backBtn.setOnClickListener(v -> dialog.dismiss());

            EditText editLanguage = dialog.findViewById(R.id.edit_language);
            EditText editTitle = dialog.findViewById(R.id.edit_title);
            Button saveBtn = dialog.findViewById(R.id.btn_save);

            saveBtn.setOnClickListener(view -> {
                String language = editLanguage.getText().toString().trim();
                String title = editTitle.getText().toString().trim();

                if (!language.isEmpty() && !title.isEmpty()) {
                    Map<String, Object> deckData = new HashMap<>();
                    deckData.put("title", title);
                    deckData.put("language", language);
                    deckData.put("day", 1);

                    deckRepository.addDeck(deckData, success -> {
                        if (success) {
                            Toast.makeText(this, "Deck saved successfully!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            loadDecks();
                        } else {
                            Toast.makeText(this, "Failed to save deck.", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }
                    });
                } else {
                    Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show();
                }
            });
            dialog.show();
        });
    }
}
