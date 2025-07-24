package com.example.licentfront.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.licentfront.R;
import com.example.licentfront.models.Deck;
import com.example.licentfront.repositories.DeckRepository;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class DragChooseDeck extends BaseActivity {

    private LinearLayout deckContainer;
    private DeckRepository deckRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drag_choose_deck);

        deckContainer = findViewById(R.id.deck_container);
        deckRepository = new DeckRepository();

        loadUserDecks();

        findViewById(R.id.back_btn).setOnClickListener(v -> finish());
    }

    private void loadUserDecks() {
        deckRepository.getDecks(new DeckRepository.DecksCallback() {
            @Override
            public void onSuccess(List<Deck> decks) {
                for (Deck deck : decks) {
                    MaterialButton deckButton = new MaterialButton(DragChooseDeck.this);
                    deckButton.setText(deck.getTitle() + " (" + deck.getLanguage() + ")");
                    deckButton.setCornerRadius(28);
                    deckButton.setBackgroundTintList(getResources().getColorStateList(R.color.dark_green, null));

                    float scale = getResources().getDisplayMetrics().density;
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            (int) (60 * scale)
                    );
                    params.setMargins(0, 0, 0, (int) (16 * scale));
                    deckButton.setLayoutParams(params);

                    deckButton.setOnClickListener(v -> {
                        Intent intent = new Intent(DragChooseDeck.this, DragGameActivity.class);
                        intent.putExtra("deck_id", deck.getId());
                        intent.putExtra("deck_title", deck.getTitle());
                        intent.putExtra("deck_language", deck.getLanguage());
                        startActivity(intent);
                    });

                    deckContainer.addView(deckButton);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(DragChooseDeck.this, "Failed to load decks", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
