package com.example.liel;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PartyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_party);

        TextView partyNameText =
                findViewById(R.id.partyNameText);

        TextView partyDateText =
                findViewById(R.id.partyDateText);

        TextView partyLocationText =
                findViewById(R.id.partyLocationText);

        String name =
                getIntent().getStringExtra("partyName");

        String date =
                getIntent().getStringExtra("partyDate");

        String location =
                getIntent().getStringExtra("partyLocation");

        partyNameText.setText(
                name == null ? "המסיבה שלי" : name
        );

        partyDateText.setText(
                "📅 תאריך: " +
                        (date == null ? "לא הוגדר" : date)
        );

        partyLocationText.setText(
                "📍 מיקום: " +
                        (location == null || location.isEmpty()
                                ? "לא הוגדר"
                                : location)
        );

        Button backButton =
                findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> finish());

        // נחבר את הכפתורים למסכים שלהם בשלבים הבאים
        findViewById(R.id.guestsTasksButton)
                .setOnClickListener(v ->
                        Toast.makeText(
                                this,
                                "נחבר את ניהול האורחים והמשימות בשלב הבא",
                                Toast.LENGTH_SHORT
                        ).show()
                );

        findViewById(R.id.budgetButton)
                .setOnClickListener(v ->
                        Toast.makeText(
                                this,
                                "נחבר את ניהול התקציב בשלב הבא",
                                Toast.LENGTH_SHORT
                        ).show()
                );

        findViewById(R.id.aiButton)
                .setOnClickListener(v ->
                        Toast.makeText(
                                this,
                                "נחבר את העוזרת החכמה בהמשך",
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }
}