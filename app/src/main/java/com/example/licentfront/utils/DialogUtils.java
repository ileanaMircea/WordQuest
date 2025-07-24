package com.example.licentfront.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.licentfront.R;
import com.example.licentfront.repositories.FlashcardRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.example.licentfront.utils.ReadingUtils.getLanguageCode;

public class DialogUtils {

    private static Dialog loadingDialog;

    public static void showLoading(Context context) {
        loadingDialog = new Dialog(context);
        loadingDialog.setContentView(R.layout.loading_dialog);
        loadingDialog.setCancelable(false);
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        loadingDialog.show();
    }

    public static void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    public static void showDialog(Activity activity, int layoutResId, DialogConfigurator dialogConfigurator) {
        Dialog dialog = new Dialog(activity);
        dialog.setContentView(layoutResId);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            WindowManager.LayoutParams layoutParams = dialog.getWindow().getAttributes();
            layoutParams.width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.85);
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(layoutParams);
        }

        dialogConfigurator.configure(dialog);

        dialog.show();
    }

    public static void showDeleteDialog(Activity activity, String itemType, Runnable onDeleteConfirmed) {
        showDialog(activity, R.layout.dialog_delete, dialog -> {
            TextView message = dialog.findViewById(R.id.mes_delete);
            Button cancelBtn = dialog.findViewById(R.id.cancel_btn);
            Button yesBtn = dialog.findViewById(R.id.yes_btn);

            message.setText("Are you sure you want to delete this " + itemType + "?");

            cancelBtn.setOnClickListener(v -> dialog.dismiss());
            yesBtn.setOnClickListener(v -> {
                dialog.dismiss();
                onDeleteConfirmed.run();
            });
        });
    }

    public interface OnCardAddedListener {
        void onCardAdded(String front, String back);
    }

    public static void showAddCardDialog(Context context, String deckId, String initialWord,
                                         boolean showLanguageSpinner, boolean showTranslateButton,
                                         boolean showGenerateExampleButton, String bookLanguage,
                                         String userNativeLanguage, OnCardAddedListener listener) {

        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_add_card);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        WindowManager.LayoutParams layoutParams = dialog.getWindow().getAttributes();
        layoutParams.width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.85);
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setAttributes(layoutParams);

        // Get dialog views
        ImageView backBtn = dialog.findViewById(R.id.dialog_back);
        EditText frontEditText = dialog.findViewById(R.id.front_new);
        EditText backEditText = dialog.findViewById(R.id.back_new);
        EditText exampleEditText = dialog.findViewById(R.id.example_sentence);
        Button generateBtn = dialog.findViewById(R.id.btn_generate_example);
        Button translateBtn = dialog.findViewById(R.id.btn_translate);
        Button saveBtn = dialog.findViewById(R.id.btn_save);
        Spinner languageSpinner = dialog.findViewById(R.id.spinner_language);

        if (initialWord != null && !initialWord.trim().isEmpty()) {
            frontEditText.setText(initialWord);
        }

        if (showLanguageSpinner && languageSpinner != null) {
            languageSpinner.setVisibility(android.view.View.VISIBLE);
            ArrayAdapter<String> langAdapter = new ArrayAdapter<>(context,
                    android.R.layout.simple_spinner_item,
                    Arrays.asList("English", "German", "French", "Spanish", "Italian",
                            "Polish", "Dutch", "Portuguese", "Romanian", "Czech"));
            langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            languageSpinner.setAdapter(langAdapter);
        } else if (languageSpinner != null) {
            languageSpinner.setVisibility(android.view.View.GONE);
        }

        if (showTranslateButton && translateBtn != null) {
            translateBtn.setVisibility(android.view.View.VISIBLE);
            translateBtn.setOnClickListener(v -> {
                String front = frontEditText.getText().toString().trim();
                if (front.isEmpty()) {
                    Toast.makeText(context, "Enter a word to translate", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Use the user's native language for translation
                String targetLanguage = userNativeLanguage != null && !userNativeLanguage.isEmpty()
                        ? userNativeLanguage : "English";

                AIUtils.translateWordToNativeLanguage(front, targetLanguage, backEditText, context);
            });
        } else if (translateBtn != null) {
            translateBtn.setVisibility(android.view.View.GONE);
        }

        if (showGenerateExampleButton && generateBtn != null) {
            generateBtn.setVisibility(android.view.View.VISIBLE);
            generateBtn.setOnClickListener(v -> {
                String word = frontEditText.getText().toString().trim();
                if (word.isEmpty()) {
                    Toast.makeText(context, "Enter a word first", Toast.LENGTH_SHORT).show();
                    return;
                }
                AIUtils.generateExampleSentence(word, exampleEditText, context);
            });
        } else if (generateBtn != null) {
            generateBtn.setVisibility(android.view.View.GONE);
        }

        backBtn.setOnClickListener(v -> dialog.dismiss());

        saveBtn.setOnClickListener(v -> {
            String front = frontEditText.getText().toString().trim();
            String back = backEditText.getText().toString().trim();
            String example = exampleEditText != null ? exampleEditText.getText().toString().trim() : "";

            if (front.isEmpty() || back.isEmpty()) {
                Toast.makeText(context, "Please fill in both fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (showLanguageSpinner) {
                // save to Firestore directly -->read book activity
                saveToFirestore(context, deckId, front, back, example, languageSpinner, dialog, listener);
            } else {
                //edit decks activity
                saveWithRepository(context, deckId, front, back, example, dialog, listener);
            }
        });

        dialog.show();
    }

    // Overloaded method for backward compatibility (for edit decks activity)
    public static void showAddCardDialog(Context context, String deckId, String initialWord,
                                         boolean showLanguageSpinner, boolean showTranslateButton,
                                         boolean showGenerateExampleButton, String bookLanguage,
                                         OnCardAddedListener listener) {
        // For backward compatibility, fetch user's native language from Firebase
        fetchUserNativeLanguageAndShowDialog(context, deckId, initialWord, showLanguageSpinner,
                showTranslateButton, showGenerateExampleButton,
                bookLanguage, listener);
    }

    private static void fetchUserNativeLanguageAndShowDialog(Context context, String deckId, String initialWord,
                                                             boolean showLanguageSpinner, boolean showTranslateButton,
                                                             boolean showGenerateExampleButton, String bookLanguage,
                                                             OnCardAddedListener listener) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("user-data").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String userNativeLanguage = "English"; // Default fallback
                    if (documentSnapshot.exists()) {
                        String nativeLanguage = documentSnapshot.getString("nativeLanguage");
                        if (nativeLanguage != null && !nativeLanguage.isEmpty()) {
                            userNativeLanguage = nativeLanguage;
                        }
                    }

                    // Now show the dialog with the fetched native language
                    showAddCardDialog(context, deckId, initialWord, showLanguageSpinner,
                            showTranslateButton, showGenerateExampleButton,
                            bookLanguage, userNativeLanguage, listener);
                })
                .addOnFailureListener(e -> {
                    Log.e("DialogUtils", "Error fetching user native language: " + e.getMessage());
                    // Use English as fallback on error
                    showAddCardDialog(context, deckId, initialWord, showLanguageSpinner,
                            showTranslateButton, showGenerateExampleButton,
                            bookLanguage, "English", listener);
                });
    }

    private static void saveToFirestore(Context context, String deckId, String front, String back,
                                        String example, Spinner languageSpinner, Dialog dialog,
                                        OnCardAddedListener listener) {
        if (deckId == null) {
            Toast.makeText(context, "No deck selected. Please create a deck first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedLang = "";
        if (languageSpinner != null && languageSpinner.getSelectedItem() != null) {
            selectedLang = getLanguageCode(languageSpinner.getSelectedItem().toString());
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> flashcardData = new HashMap<>();
        flashcardData.put("front", front);
        flashcardData.put("back", back);
        flashcardData.put("example", example);
        flashcardData.put("language", selectedLang);
        flashcardData.put("easeFactor", 2.5);
        flashcardData.put("repetition", 0);
        flashcardData.put("interval", 0);
        flashcardData.put("nextReviewTime", System.currentTimeMillis());

        db.collection("user-decks").document(uid)
                .collection("decks").document(deckId)
                .collection("flashcards")
                .add(flashcardData)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(context, "Added to deck", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    if (listener != null) {
                        listener.onCardAdded(front, back);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("DialogUtils", "Failed to add flashcard: " + e.getMessage());
                    Toast.makeText(context, "Failed to add word", Toast.LENGTH_SHORT).show();
                });
    }

    private static void saveWithRepository(Context context, String deckId, String front, String back,
                                           String example, Dialog dialog, OnCardAddedListener listener) {
        Map<String, Object> flashcardData = new HashMap<>();
        flashcardData.put("front", front);
        flashcardData.put("back", back);
        flashcardData.put("example", example);

        FlashcardRepository flashcardRepository = new FlashcardRepository();
        flashcardRepository.addFlashcard(deckId, flashcardData, success -> {
            if (success) {
                Toast.makeText(context, "Flashcard added", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                if (listener != null) {
                    listener.onCardAdded(front, back);
                }
            } else {
                Toast.makeText(context, "Failed to add flashcard", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void showEditDecksAddCardDialog(Context context, String deckId, OnCardAddedListener listener) {
        showAddCardDialog(context, deckId, "", true, true, true, "", listener);
    }

    public static void showReadBookAddCardDialog(Context context, String deckId, String selectedWord,
                                                 String bookLanguage, String userNativeLanguage, OnCardAddedListener listener) {
        showAddCardDialog(context, deckId, selectedWord, true, true, true, bookLanguage, userNativeLanguage, listener);
    }

    public interface DialogConfigurator {
        void configure(Dialog dialog);
    }
}