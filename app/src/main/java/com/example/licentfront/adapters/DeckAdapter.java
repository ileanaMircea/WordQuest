package com.example.licentfront.adapters;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.licentfront.activities.EditDecksActivity;
import com.example.licentfront.R;
import com.example.licentfront.activities.PracticeFrontActivity;
import com.example.licentfront.models.Deck;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class DeckAdapter extends RecyclerView.Adapter<DeckAdapter.DeckViewHolder> {
    private List<Deck> deckList;
    private Context context;

    public DeckAdapter(List<Deck> deckList, Context context) {
        this.deckList = deckList;
        this.context = context;
    }

    @NonNull
    @Override
    public DeckViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_deck, parent, false);
        return new DeckViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeckViewHolder holder, int position) {
        Deck deck = deckList.get(position);
        holder.title.setText(deck.getTitle());
        holder.language.setText(deck.getLanguage());
        holder.day.setText("Day " + deck.getDay());

        //handle edit button
        holder.editBtn.setOnClickListener(v->{
            Intent intent=new Intent(context, EditDecksActivity.class);
            intent.putExtra("deck_id",deck.getId());
            intent.putExtra("deck_title",deck.getTitle());
            context.startActivity(intent);
        });

        //handle practice button
        holder.practiceBtn.setOnClickListener(v -> {
            String deckId = deck.getId();
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            FirebaseFirestore.getInstance()
                    .collection("user-decks").document(uid)
                    .collection("decks").document(deckId)
                    .collection("flashcards")
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Long nextReview = doc.getLong("nextReviewTime");
                            if (nextReview == null || nextReview <= System.currentTimeMillis()) {
                                String cardId = doc.getId();
                                Intent intent = new Intent(context, PracticeFrontActivity.class);
                                intent.putExtra("deck_id", deckId);
                                intent.putExtra("card_id", cardId);
                                context.startActivity(intent);
                                return;
                            }
                        }
                        Toast.makeText(context, "No cards ready for practice.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to load flashcards.", Toast.LENGTH_SHORT).show();
                    });
        });

    }

    @Override
    public int getItemCount() {
        return deckList.size();
    }

    public static class DeckViewHolder extends RecyclerView.ViewHolder {
        TextView title, day, language;
        Button practiceBtn, editBtn;

        public DeckViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.deck_title);
            day = itemView.findViewById(R.id.front_edit_label);
            language=itemView.findViewById(R.id.deck_language);
            practiceBtn = itemView.findViewById(R.id.practice_btn);
            editBtn = itemView.findViewById(R.id.edit_btn);
        }
    }
}

