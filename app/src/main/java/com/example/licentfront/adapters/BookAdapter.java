package com.example.licentfront.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.licentfront.R;
import com.example.licentfront.models.Book;

import java.util.List;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookViewHolder> {
    private List<Book> books;
    private boolean isMyLibrary;
    private OnBookActionListener listener;

    public interface OnBookActionListener {
        void onAddToLibrary(Book book);
        void onRemoveFromLibrary(Book book);
        void onReadBook(Book book);
    }

    public BookAdapter(List<Book> books, boolean isMyLibrary, OnBookActionListener listener) {
        this.books = books;
        this.isMyLibrary = isMyLibrary;
        this.listener = listener;
    }

    public void updateBooks(List<Book> newBooks) {
        this.books = newBooks;
        notifyDataSetChanged();
    }

    public void setMyLibraryMode(boolean isMyLibrary) {
        this.isMyLibrary = isMyLibrary;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_book_card, parent, false);
        return new BookViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        Book book = books.get(position);
        holder.title.setText(book.getTitle());
        holder.author.setText(book.getAuthor());

        if (isMyLibrary) {
            holder.actionButton.setImageResource(R.drawable.dialog_delete);
            holder.actionButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemoveFromLibrary(book);
                }
            });

            // indicator progres
            if (book.getCurrentPage() > 0) {
                holder.progressText.setVisibility(View.VISIBLE);
                holder.progressText.setText("Page " + book.getCurrentPage() + "/" + book.getTotalPages());
            } else {
                holder.progressText.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReadBook(book);
                }
            });
        } else {
            holder.actionButton.setImageResource(R.drawable.plus_icon);
            holder.actionButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddToLibrary(book);
                }
            });
            holder.progressText.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return books.size();
    }
    static class BookViewHolder extends RecyclerView.ViewHolder {
        TextView title, author, progressText;
        ImageButton actionButton;
        public BookViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.book_title);
            author = itemView.findViewById(R.id.book_author);
            actionButton = itemView.findViewById(R.id.action_button);
            progressText = itemView.findViewById(R.id.progress_text);
        }
    }
}