
package com.example.liel;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PartyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_party);

        TextView partyNameText = findViewById(R.id.partyNameText);
        TextView partyDateText = findViewById(R.id.partyDateText);
        TextView partyLocationText = findViewById(R.id.partyLocationText);

        String name = getIntent().getStringExtra("partyName");
        String date = getIntent().getStringExtra("partyDate");
        String location = getIntent().getStringExtra("partyLocation");

        partyNameText.setText(name == null ? "המסיבה שלי" : name);

        partyDateText.setText("📅 תאריך: " +
                (date == null ? "לא הוגדר" : date));

        partyLocationText.setText("📍 מיקום: " +
                (location == null || location.isEmpty()
                        ? "לא הוגדר" : location));

        TextView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        findViewById(R.id.guestsTasksButton).setOnClickListener(v -> {

            Intent intent = new Intent(
                    PartyActivity.this,
                    GuestsTasksActivity.class
            );

            intent.putExtra(
                    "partyId",
                    getIntent().getStringExtra("partyId")
            );

            intent.putExtra(
                    "partyName",
                    getIntent().getStringExtra("partyName")
            );

            startActivity(intent);
        });

        // מעבר למסך ניהול התקציב
        findViewById(R.id.budgetButton).setOnClickListener(v -> {

            Intent intent = new Intent(
                    PartyActivity.this,
                    BudgetActivity.class
            );

            intent.putExtra(
                    "partyId",
                    getIntent().getStringExtra("partyId")
            );

            startActivity(intent);
        });

        findViewById(R.id.aiButton).setOnClickListener(v ->
                Toast.makeText(
                        this,
                        "נחבר את העוזרת החכמה בהמשך",
                        Toast.LENGTH_SHORT
                ).show()
        );
    }
}
