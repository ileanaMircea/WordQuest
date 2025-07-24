package com.example.licentfront.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;
import com.example.licentfront.BuildConfig;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.*;

public class AIUtils {

    private static final String OPENAI_API_KEY = BuildConfig.API_KEY;
    private static final String ENDPOINT = "https://api.openai.com/v1/chat/completions";

    public static void generateExampleSentence(String word, EditText targetField, android.content.Context context) {
        OkHttpClient client = new OkHttpClient();

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", "gpt-3.5-turbo");
            jsonBody.put("messages", new org.json.JSONArray()
                    .put(new JSONObject().put("role", "system").put("content", "You are a language assistant. Create only the example sentence using the given word. Do not include any explanations, greetings, or additional text. Respond with ONLY the sentence."))
                    .put(new JSONObject().put("role", "user").put("content", "Create a simple example sentence using the word \"" + word + "\" in its original language. Only return the sentence, nothing else.")));

            jsonBody.put("max_tokens", 30);
            jsonBody.put("temperature", 0.3);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(ENDPOINT)
                .header("Authorization", "Bearer " + OPENAI_API_KEY)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            final Handler handler = new Handler(Looper.getMainLooper());

            @Override
            public void onFailure(Call call, IOException e) {
                handler.post(() -> Toast.makeText(context, "Failed to connect to AI", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "null";
                    String errorMsg = "Code: " + response.code() + "\nBody: " + errorBody;

                    handler.post(() -> {
                        Toast.makeText(context, "AI Error: " + response.code(), Toast.LENGTH_LONG).show();
                        Log.e("AIUtils", "Response not successful:\n" + errorMsg);
                    });
                    return;
                }

                try {
                    JSONObject json = new JSONObject(response.body().string());
                    String reply = json
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");

                    String cleanedReply = cleanResponse(reply.trim());
                    handler.post(() -> targetField.setText(cleanedReply));
                } catch (Exception e) {
                    handler.post(() -> Toast.makeText(context, "Failed to parse AI response", Toast.LENGTH_SHORT).show());
                    Log.e("AIUtils", "Parsing error", e);
                }
            }

        });
    }

    private static String cleanResponse(String response) {
        response = response.replaceAll("^(Sure,?\\s*|Here\\s+(is|are)\\s+|Here's\\s+)", "");
        response = response.replaceAll("^(an?\\s+)?example\\s+sentence:?\\s*", "");
        response = response.replaceAll("\\.$", ""); // Remove trailing period if it's the only punctuation
        response = response.replaceAll("^[\"']|[\"']$", ""); // Remove quotes

        if (!response.matches(".*[.!?]$")) {
            response += ".";
        }

        return response.trim();
    }

    public static void translateWordToNativeLanguage(String word, String nativeLanguage, EditText backField, Context context) {
        OkHttpClient client = new OkHttpClient();
        word = word.trim().replaceAll("[^\\p{L}]", "");

        if (nativeLanguage == null || nativeLanguage.isEmpty()) {
            nativeLanguage = "English";
        }

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", "gpt-3.5-turbo");
            jsonBody.put("messages", new org.json.JSONArray()
                    .put(new JSONObject().put("role", "user").put("content", "Translate the word '" + word + "' into " + nativeLanguage + ". Respond with ONLY the translated word and nothing else.")));
            jsonBody.put("max_tokens", 15);
            jsonBody.put("temperature", 0);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to build request", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(ENDPOINT)
                .header("Authorization", "Bearer " + OPENAI_API_KEY)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            final Handler handler = new Handler(Looper.getMainLooper());

            @Override
            public void onFailure(Call call, IOException e) {
                handler.post(() -> Toast.makeText(context, "AI translation failed", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    handler.post(() -> Toast.makeText(context, "Error: " + response.code(), Toast.LENGTH_SHORT).show());
                    return;
                }

                try {
                    JSONObject json = new JSONObject(response.body().string());
                    String translated = json
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");

                    handler.post(() -> backField.setText(translated.trim()));
                } catch (Exception e) {
                    handler.post(() -> Toast.makeText(context, "Failed to parse response", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    @Deprecated
    public static void translateWordToEnglish(String word, EditText backField, Context context) {
        translateWordToNativeLanguage(word, "English", backField, context);
    }
}