package com.example.licentfront.activities;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.licentfront.R;
import com.example.licentfront.models.Flashcard;
import com.example.licentfront.repositories.FlashcardRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

public class DragGameActivity extends BaseActivity {

    private LinearLayout foreignColumn;
    private LinearLayout translationColumn;

    private final List<Flashcard> allFlashcards = new ArrayList<>();
    private final Deque<Flashcard> activeFlashcards = new ArrayDeque<>();
    private int currentIndex = 0;
    private static final int MAX_PAIRS = 5;
    private long sessionStartTime;
    private int totalMatched = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drag_game);
        allFlashcards.clear();
        activeFlashcards.clear();
        currentIndex = 0;
        foreignColumn = findViewById(R.id.foreign_column);
        translationColumn = findViewById(R.id.translation_column);
        sessionStartTime = System.currentTimeMillis();

        findViewById(R.id.back_btn).setOnClickListener(v -> finish());

        String deckId = getIntent().getStringExtra("deck_id");
        if (deckId == null) {
            Toast.makeText(this, "Missing deck", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        loadFlashcards(deckId);
    }

    private void loadNextBatch() {
        foreignColumn.removeAllViews();
        translationColumn.removeAllViews();
        activeFlashcards.clear();

        while (activeFlashcards.size() < MAX_PAIRS && currentIndex < allFlashcards.size()) {
            activeFlashcards.add(allFlashcards.get(currentIndex++));
        }

        List<Flashcard> fronts = new ArrayList<>(activeFlashcards);
        List<Flashcard> backs = new ArrayList<>(activeFlashcards);
        Collections.shuffle(fronts);
        Collections.shuffle(backs);

        for (Flashcard f : fronts) {
            foreignColumn.addView(createDraggableWord(f.getFront()));
        }

        for (Flashcard f : backs) {
            translationColumn.addView(createDropTarget(f.getBack(), f.getFront()));
        }
    }

    private void loadFlashcards(String deckId) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        new FlashcardRepository().getFlashcards(deckId, new FlashcardRepository.FlashcardsCallback() {
            @Override
            public void onSuccess(List<Flashcard> cards) {
                if (cards.size() < MAX_PAIRS) {
                    Toast.makeText(DragGameActivity.this, "Not enough flashcards", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                Collections.shuffle(cards);
                allFlashcards.addAll(cards);
                currentIndex = 0;
                loadNextBatch();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(DragGameActivity.this, "Failed to load flashcards", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private TextView createDraggableWord(String word) {
        TextView textView = new TextView(this);
        textView.setText(word);
        textView.setTextSize(20f);
        textView.setTextColor(getColor(R.color.black));
        textView.setBackground(ContextCompat.getDrawable(this, R.drawable.drag_word));
        textView.setPadding(32, 32, 32, 32);
        textView.setGravity(Gravity.CENTER);

        try {
            textView.setTypeface(getResources().getFont(R.font.istokweb_bold));
        } catch (Exception e) {
            textView.setTypeface(null, android.graphics.Typeface.BOLD);
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 24);
        textView.setLayoutParams(params);

        textView.setOnLongClickListener(v -> {
            ClipData data = ClipData.newPlainText("word", word);
            View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
            v.startDragAndDrop(data, shadowBuilder, v, 0);
            return true;
        });

        return textView;
    }

    private TextView createDropTarget(String expectedTranslation, String expectedFront) {
        TextView target = new TextView(this);
        target.setText(expectedTranslation);
        target.setTextSize(20f);
        target.setTextColor(Color.BLACK);
        target.setBackground(ContextCompat.getDrawable(this, R.drawable.drag_match));
        target.setPadding(32, 32, 32, 32);
        target.setGravity(Gravity.CENTER);

        try {
            target.setTypeface(getResources().getFont(R.font.istokweb_bold));
        } catch (Exception e) {
            target.setTypeface(null, android.graphics.Typeface.BOLD);
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 24);
        target.setLayoutParams(params);

        target.setOnDragListener((v, event) -> {
            if (event.getAction() == DragEvent.ACTION_DROP) {
                String draggedText = event.getClipData().getItemAt(0).getText().toString();

                if (draggedText.equals(expectedFront)) {
                    Toast.makeText(this, "Corect!", Toast.LENGTH_SHORT).show();
                    totalMatched++;
                    Flashcard matchedCard = null;
                    for (Flashcard f : activeFlashcards) {
                        if (f.getFront().equals(expectedFront)) {
                            matchedCard = f;
                            break;
                        }
                    }

                    if (matchedCard != null) {
                        activeFlashcards.remove(matchedCard);
                    }

                    foreignColumn.removeView(findDraggedView(draggedText));
                    translationColumn.removeView(v);

                    if (currentIndex < allFlashcards.size()) {
                        Flashcard next = allFlashcards.get(currentIndex++);
                        activeFlashcards.add(next);
                        foreignColumn.addView(createDraggableWord(next.getFront()));
                        translationColumn.addView(createDropTarget(next.getBack(), next.getFront()));
                    }

                    if (activeFlashcards.isEmpty() && currentIndex >= allFlashcards.size()) {
                        goToSummary();
                    }

                    return true;
                } else {
                    Toast.makeText(this, "Încearcă din nou!", Toast.LENGTH_SHORT).show();
                }
            }
            return true;
        });

        return target;
    }

    private View findDraggedView(String text) {
        for (int i = 0; i < foreignColumn.getChildCount(); i++) {
            View child = foreignColumn.getChildAt(i);
            if (child instanceof TextView && ((TextView) child).getText().toString().equals(text)) {
                return child;
            }
        }
        return null;
    }

    private void goToSummary() {
        Intent intent = new Intent(this, DragSummaryActivity.class);
        intent.putExtra("total_words", totalMatched);
        intent.putExtra("start_time", sessionStartTime);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        allFlashcards.clear();
        activeFlashcards.clear();
        currentIndex = 0;
    }
}