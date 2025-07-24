package com.example.licentfront.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.licentfront.R;

public class GamesLibraryActivity extends BaseActivity {

    private Button gameBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_games_library);
        gameBtn=findViewById(R.id.word_snap_btn);
        gameBtn.setOnClickListener(v->{
            Intent intent=new Intent(GamesLibraryActivity.this, DragChooseDeck.class);
            startActivity(intent);
        });
    }
}