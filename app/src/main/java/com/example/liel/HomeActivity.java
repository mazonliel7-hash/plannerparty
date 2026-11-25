package com.example.liel;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
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

        // שלב 6: בקשת הרשאה להתראות באנדרואיד 13 ומעלה
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        101
                );
            }
        }

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        partiesContainer = findViewById(R.id.partiesContainer);
        emptyPartiesText = findViewById(R.id.emptyPartiesText);

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        findViewById(R.id.createPartyButton)
                .setOnClickListener(v -> showCreatePartyDialog());

        loadParties(user.getUid());
    }

    // יצירת מסיבה חדשה
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

                String partyName =
                        partyNameInput.getText().toString().trim();

                String partyDate =
                        dateInput.getText().toString().trim();

                String partyLocation =
                        locationInput.getText().toString().trim();

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

                Map<String, Object> party = new HashMap<>();
                party.put("name", partyName);
                party.put("date", partyDate);
                party.put("location", partyLocation);
                party.put("ownerId", user.getUid());
                party.put("createdAt", FieldValue.serverTimestamp());

                db.collection("users")
                        .document(user.getUid())
                        .collection("parties")
                        .add(party)
                        .addOnSuccessListener(documentReference -> {

                            // תזמון תזכורת ליום לפני המסיבה
                            PartyReminder.schedule(
                                    HomeActivity.this,
                                    documentReference.getId(),
                                    partyName,
                                    partyDate
                            );

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

    // טעינת המסיבות וחלוקה למסיבות קרובות ולעבר
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

                    if (snapshots.isEmpty()) {
                        emptyPartiesText.setVisibility(View.VISIBLE);
                        return;
                    }

                    emptyPartiesText.setVisibility(View.GONE);

                    List<DocumentSnapshot> upcomingParties =
                            new ArrayList<>();

                    List<DocumentSnapshot> pastParties =
                            new ArrayList<>();

                    Date today = getToday();

                    for (DocumentSnapshot document :
                            snapshots.getDocuments()) {

                        String dateString = document.getString("date");
                        Date partyDate = parseDate(dateString);

                        if (partyDate == null ||
                                !partyDate.before(today)) {

                            upcomingParties.add(document);

                        } else {

                            pastParties.add(document);
                        }
                    }

                    // מסיבות קרובות: מהקרובה ביותר לרחוקה ביותר
                    Collections.sort(
                            upcomingParties,
                            (a, b) -> compareDates(a, b, false)
                    );

                    // מסיבות שעברו: מהאחרונה לישנה ביותר
                    Collections.sort(
                            pastParties,
                            (a, b) -> compareDates(a, b, true)
                    );

                    if (!upcomingParties.isEmpty()) {

                        addSectionTitle("🎉 המסיבות הקרובות");

                        for (DocumentSnapshot document :
                                upcomingParties) {

                            addPartyCard(document);
                        }
                    }

                    if (!pastParties.isEmpty()) {

                        addSectionTitle("📁 מסיבות שעברו");

                        for (DocumentSnapshot document : pastParties) {

                            addPartyCard(document);
                        }
                    }
                });
    }

    // קבלת התאריך של היום ללא שעה
    private Date getToday() {

        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTime();
    }

    // המרת תאריך מהפורמט שנשמר ב-Firebase
    private Date parseDate(String dateString) {

        if (dateString == null || dateString.isEmpty()) {
            return null;
        }

        SimpleDateFormat format =
                new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        format.setLenient(false);

        try {
            return format.parse(dateString);
        } catch (ParseException e) {
            return null;
        }
    }

    // השוואת תאריכים לצורך מיון
    private int compareDates(
            DocumentSnapshot first,
            DocumentSnapshot second,
            boolean reverse) {

        Date firstDate = parseDate(first.getString("date"));
        Date secondDate = parseDate(second.getString("date"));

        if (firstDate == null && secondDate == null) {
            return 0;
        }

        if (firstDate == null) {
            return 1;
        }

        if (secondDate == null) {
            return -1;
        }

        return reverse
                ? secondDate.compareTo(firstDate)
                : firstDate.compareTo(secondDate);
    }

    // כותרת של כל אזור
    private void addSectionTitle(String title) {

        TextView sectionTitle = new TextView(this);

        sectionTitle.setText(title);
        sectionTitle.setTextSize(21);
        sectionTitle.setTextColor(0xFFB92D68);
        sectionTitle.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );
        sectionTitle.setGravity(Gravity.RIGHT);
        sectionTitle.setPadding(5, 30, 5, 15);
        sectionTitle.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        partiesContainer.addView(sectionTitle);
    }

    // יצירת כרטיס מסיבה
    private void addPartyCard(DocumentSnapshot document) {

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

        final String savedName = name;
        final String savedDate = date;
        final String savedLocation = location;
        final String savedPartyId = document.getId();

        LinearLayout cardRow = new LinearLayout(this);
        cardRow.setOrientation(LinearLayout.HORIZONTAL);
        cardRow.setGravity(Gravity.CENTER_VERTICAL);
        cardRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        rowParams.bottomMargin = 20;
        cardRow.setLayoutParams(rowParams);

        // פרטי המסיבה
        TextView partyCard = new TextView(this);

        partyCard.setText(
                savedName
                        + "\nתאריך: " + savedDate
                        + "\nמיקום: " + savedLocation
        );

        partyCard.setTextSize(17);
        partyCard.setTextColor(0xFFB92D68);
        partyCard.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );
        partyCard.setGravity(Gravity.RIGHT);
        partyCard.setPadding(30, 25, 30, 25);
        partyCard.setBackgroundResource(
                R.drawable.edit_text_background
        );

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        partyCard.setLayoutParams(cardParams);
        partyCard.setClickable(true);

        // פתיחת המסיבה גם אם התאריך שלה עבר
        partyCard.setOnClickListener(v -> {

            Intent intent = new Intent(
                    HomeActivity.this,
                    PartyActivity.class
            );

            intent.putExtra("partyId", savedPartyId);
            intent.putExtra("partyName", savedName);
            intent.putExtra("partyDate", savedDate);
            intent.putExtra("partyLocation", savedLocation);

            startActivity(intent);
        });

        // כפתור מחיקה
        TextView deleteButton = new TextView(this);

        deleteButton.setText("מחיקה");
        deleteButton.setTextSize(14);
        deleteButton.setTextColor(0xFFB92D68);
        deleteButton.setGravity(Gravity.CENTER);
        deleteButton.setPadding(15, 20, 15, 20);
        deleteButton.setBackgroundResource(
                R.drawable.edit_text_background
        );

        LinearLayout.LayoutParams deleteParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        deleteParams.setMargins(10, 0, 0, 0);
        deleteButton.setLayoutParams(deleteParams);

        deleteButton.setOnClickListener(v ->
                confirmDeleteParty(document)
        );

        cardRow.addView(partyCard);
        cardRow.addView(deleteButton);

        partiesContainer.addView(cardRow);
    }

    // אישור לפני מחיקת מסיבה וכל הנתונים שלה
    private void confirmDeleteParty(DocumentSnapshot document) {

        String name = document.getString("name");

        if (name == null) {
            name = "המסיבה";
        }

        new AlertDialog.Builder(this)
                .setTitle("מחיקת מסיבה")
                .setMessage(
                        "את בטוחה שברצונך למחוק את \""
                                + name + "\"?\n\n"
                                + "גם רשימת האורחים והמשימות של המסיבה יימחקו."
                )
                .setPositiveButton("כן, למחוק", (dialog, which) -> {

                    FirebaseUser user = auth.getCurrentUser();

                    if (user == null) {
                        Toast.makeText(
                                this,
                                "יש להתחבר מחדש",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    String userId = user.getUid();
                    String partyId = document.getId();

                    // ביטול התזכורת של המסיבה שנמחקת
                    PartyReminder.cancel(this, partyId);

                    // כתובת המסיבה ב-Firebase
                    com.google.firebase.firestore.DocumentReference partyRef =
                            db.collection("users")
                                    .document(userId)
                                    .collection("parties")
                                    .document(partyId);

                    // קודם טוענים את האורחים
                    partyRef.collection("guests")
                            .get()
                            .addOnSuccessListener(guests -> {

                                // לאחר מכן טוענים את המשימות
                                partyRef.collection("tasks")
                                        .get()
                                        .addOnSuccessListener(tasks -> {

                                            List<Task<Void>> deleteTasks =
                                                    new ArrayList<>();

                                            // מחיקת האורחים
                                            for (DocumentSnapshot guest : guests) {
                                                deleteTasks.add(
                                                        guest.getReference().delete()
                                                );
                                            }

                                            // מחיקת המשימות
                                            for (DocumentSnapshot task : tasks) {
                                                deleteTasks.add(
                                                        task.getReference().delete()
                                                );
                                            }

                                            // מחכים שכל המחיקות יסתיימו
                                            Tasks.whenAll(deleteTasks)
                                                    .addOnSuccessListener(unused -> {

                                                        // מחיקת המסיבה עצמה
                                                        partyRef.delete()
                                                                .addOnSuccessListener(result ->
                                                                        Toast.makeText(
                                                                                this,
                                                                                "המסיבה וכל הנתונים שלה נמחקו 💗",
                                                                                Toast.LENGTH_SHORT
                                                                        ).show()
                                                                )
                                                                .addOnFailureListener(e ->
                                                                        showDeleteError()
                                                                );
                                                    })
                                                    .addOnFailureListener(e ->
                                                            showDeleteError()
                                                    );
                                        })
                                        .addOnFailureListener(e ->
                                                showDeleteError()
                                        );
                            })
                            .addOnFailureListener(e ->
                                    showDeleteError()
                            );
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    // הודעת שגיאה במקרה שהמחיקה נכשלה
    private void showDeleteError() {

        Toast.makeText(
                this,
                "לא הצלחנו למחוק את המסיבה. נסי שוב.",
                Toast.LENGTH_LONG
        ).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (partiesListener != null) {
            partiesListener.remove();
        }
    }
}
