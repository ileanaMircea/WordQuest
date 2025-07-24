package com.example.licentfront.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.licentfront.R;
import com.example.licentfront.models.Flashcard;
import com.example.licentfront.repositories.FlashcardRepository;
import com.example.licentfront.utils.DialogUtils;
import com.example.licentfront.repositories.FlashcardRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlashcardAdapter extends RecyclerView.Adapter<FlashcardAdapter.FlashcardViewHolder> {
    private List<Flashcard> flashcardList;
    private Context context;
    private FlashcardRepository flashcardRepository;
    private String deckId;

    public FlashcardAdapter(List<Flashcard> flashcardList, Context context, String deckId) {
        this.flashcardList = flashcardList;
        this.context = context;
        this.flashcardRepository = new FlashcardRepository();
        this.deckId = deckId;
    }


    @NonNull
    @Override
    public FlashcardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_flashcard, parent, false);
        return new FlashcardViewHolder(view);
    }

    private void flipCard(View visibleSide, View hiddenSide) {
        visibleSide.animate()
                .rotationY(90)
                .setDuration(350)
                .withEndAction(() -> {
                    visibleSide.setVisibility(View.GONE);
                    visibleSide.setRotationY(0);
                    hiddenSide.setVisibility(View.VISIBLE);
                    hiddenSide.setRotationY(-90);
                    hiddenSide.animate().rotationY(0).setDuration(150).start();
                })
                .start();
    }

    @Override
    public void onBindViewHolder(@NonNull FlashcardViewHolder holder, int position) {
        Flashcard flashcard = flashcardList.get(position);

        holder.front.setText(flashcard.getFront());
        holder.back.setText(flashcard.getBack());

        if (flashcard.isFlipped()) {
            holder.cardFront.setVisibility(View.GONE);
            holder.cardBack.setVisibility(View.VISIBLE);
        } else {
            holder.cardFront.setVisibility(View.VISIBLE);
            holder.cardBack.setVisibility(View.GONE);
        }

        holder.flipBtnFront.setOnClickListener(v -> {
            saveIfChanged(holder, position, true);
        });

        holder.flipBtnBack.setOnClickListener(v -> {
            saveIfChanged(holder, position, false);
        });

        holder.trashcanFront.setOnClickListener(v -> showDeleteDialog(position));
        holder.trashcanBack.setOnClickListener(v -> showDeleteDialog(position));
    }

    private void saveIfChanged(FlashcardViewHolder holder, int position, boolean toBack) {
        String newFront = holder.front.getText().toString().trim();
        String newBack = holder.back.getText().toString().trim();
        Flashcard oldCard = flashcardList.get(position);

        boolean textChanged = !oldCard.getFront().equals(newFront) || !oldCard.getBack().equals(newBack);

        if (textChanged) {
            Map<String, Object> updatedData = new HashMap<>();
            updatedData.put("front", newFront);
            updatedData.put("back", newBack);

            flashcardRepository.updateFlashcardById(deckId, oldCard.getId(), updatedData, success -> {
                if (success) {
                    oldCard.setFront(newFront);
                    oldCard.setBack(newBack);
                    oldCard.setFlipped(toBack);
                    notifyItemChanged(position);
                    Toast.makeText(context, "Flashcard updated", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            oldCard.setFlipped(toBack);
            notifyItemChanged(position);
            if (toBack) {
                flipCard(holder.cardFront, holder.cardBack);
            } else {
                flipCard(holder.cardBack, holder.cardFront);
            }
        }
    }

    private void showDeleteDialog(int position) {
        DialogUtils.showDeleteDialog((Activity) context, "flashcard", () -> {
            Flashcard flashcard = flashcardList.get(position);
            flashcardRepository.deleteFlashcard(deckId, flashcard, success -> {
                if (success) {
                    flashcardList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, flashcardList.size());
                } else {
                    Toast.makeText(context, "Failed to delete flashcard", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public int getItemCount() {
        return flashcardList.size();
    }

    public static class FlashcardViewHolder extends RecyclerView.ViewHolder {
        EditText front, back;
        View cardFront, cardBack, cardContainer;
        Button flipBtnFront, flipBtnBack;
        ImageView trashcanFront, trashcanBack;

        public FlashcardViewHolder(@NonNull View itemView) {
            super(itemView);
            front = itemView.findViewById(R.id.edit_front);
            back = itemView.findViewById(R.id.edit_back);
            cardFront = itemView.findViewById(R.id.card_front);
            cardBack = itemView.findViewById(R.id.card_back_edit_);
            cardContainer = itemView.findViewById(R.id.card_container);
            flipBtnFront = itemView.findViewById(R.id.flip_btn_front);
            flipBtnBack = itemView.findViewById(R.id.flip_btn_back);
            trashcanFront = itemView.findViewById(R.id.trashcan);
            trashcanBack = itemView.findViewById(R.id.trashcan_back);
        }
    }
}
