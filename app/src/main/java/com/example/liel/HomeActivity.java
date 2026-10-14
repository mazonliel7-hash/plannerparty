package com.example.liel;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout partiesContainer;
    private TextView emptyPartiesText;

    private ListenerRegistration partiesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        partiesContainer = findViewById(R.id.partiesContainer);
        emptyPartiesText = findViewById(R.id.emptyPartiesText);

        FirebaseUser user = auth.getCurrentUser();

        // מוודאים שיש משתמש מחובר
        if (user == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // כפתור יצירת מסיבה
        findViewById(R.id.createPartyButton)
                .setOnClickListener(v -> showCreatePartyDialog());

        // טעינת המסיבות של המשתמש
        loadParties(user.getUid());
    }

    // חלון יצירת מסיבה
    private void showCreatePartyDialog() {

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(40, 10, 40, 10);
        form.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        EditText partyNameInput = new EditText(this);
        partyNameInput.setHint("שם המסיבה");

        EditText dateInput = new EditText(this);
        dateInput.setHint("תאריך המסיבה");
        dateInput.setFocusable(false);
        dateInput.setClickable(true);

        EditText locationInput = new EditText(this);
        locationInput.setHint("מיקום המסיבה");

        form.addView(partyNameInput);
        form.addView(dateInput);
        form.addView(locationInput);

        // בחירת תאריך מתוך לוח שנה
        dateInput.setOnClickListener(v -> {

            Calendar calendar = Calendar.getInstance();

            DatePickerDialog datePicker = new DatePickerDialog(
                    HomeActivity.this,
                    (view, year, month, day) -> {
                        String date = String.format(
                                Locale.getDefault(),
                                "%02d/%02d/%04d",
                                day,
                                month + 1,
                                year
                        );

                        dateInput.setText(date);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            datePicker.show();
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("🎉 יצירת מסיבה חדשה")
                .setView(form)
                .setPositiveButton("שמירה", null)
                .setNegativeButton("ביטול", null)
                .create();

        dialog.setOnShowListener(d -> {

            Button saveButton =
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            saveButton.setOnClickListener(v -> {

                String partyName = partyNameInput
                        .getText().toString().trim();

                String partyDate = dateInput
                        .getText().toString().trim();

                String partyLocation = locationInput
                        .getText().toString().trim();

                if (partyName.isEmpty() || partyDate.isEmpty()) {
                    Toast.makeText(
                            HomeActivity.this,
                            "נא למלא שם ותאריך למסיבה",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                FirebaseUser user = auth.getCurrentUser();

                if (user == null) {
                    Toast.makeText(
                            HomeActivity.this,
                            "יש להתחבר מחדש",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                // הנתונים שישמרו ב-Firestore
                Map<String, Object> party = new HashMap<>();
                party.put("name", partyName);
                party.put("date", partyDate);
                party.put("location", partyLocation);
                party.put("ownerId", user.getUid());
                party.put("createdAt", FieldValue.serverTimestamp());

                // שמירה תחת המשתמש המחובר
                db.collection("users")
                        .document(user.getUid())
                        .collection("parties")
                        .add(party)
                        .addOnSuccessListener(documentReference -> {

                            Toast.makeText(
                                    HomeActivity.this,
                                    "המסיבה נשמרה בהצלחה! 💗",
                                    Toast.LENGTH_SHORT
                            ).show();

                            dialog.dismiss();

                        })
                        .addOnFailureListener(e -> {

                            Toast.makeText(
                                    HomeActivity.this,
                                    "שמירת המסיבה נכשלה: "
                                            + e.getLocalizedMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        });
            });
        });

        dialog.show();
    }

    // טעינת המסיבות מתוך Firebase
    private void loadParties(String userId) {

        partiesListener = db.collection("users")
                .document(userId)
                .collection("parties")
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        Toast.makeText(
                                HomeActivity.this,
                                "לא ניתן לטעון מסיבות",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    if (snapshots == null) {
                        return;
                    }

                    partiesContainer.removeAllViews();

                    boolean isEmpty = snapshots.isEmpty();

                    emptyPartiesText.setVisibility(
                            isEmpty ? View.VISIBLE : View.GONE
                    );

                    for (DocumentSnapshot document
                            : snapshots.getDocuments()) {

                        String name = document.getString("name");
                        String date = document.getString("date");
                        String location = document.getString("location");

                        if (name == null) {
                            name = "מסיבה ללא שם";
                        }

                        if (date == null) {
                            date = "לא הוגדר תאריך";
                        }

                        if (location == null || location.isEmpty()) {
                            location = "לא הוגדר מיקום";
                        }

                        TextView partyCard = new TextView(this);

                        partyCard.setText(
                                name
                                        + "\nתאריך: " + date
                                        + "\nמיקום: " + location
                        );

                        partyCard.setTextSize(17);
                        partyCard.setTextColor(0xFFB92D68);
                        partyCard.setTypeface(
                                null,
                                Typeface.BOLD
                        );
                        partyCard.setGravity(Gravity.RIGHT);
                        partyCard.setPadding(30, 25, 30, 25);
                        partyCard.setBackgroundResource(
                                R.drawable.edit_text_background
                        );

                        LinearLayout.LayoutParams params =
                                new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                );

                        params.bottomMargin = 20;

                        partyCard.setLayoutParams(params);
                        partiesContainer.addView(partyCard);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (partiesListener != null) {
            partiesListener.remove();
        }
    }
}