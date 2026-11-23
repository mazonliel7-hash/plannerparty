
package com.example.liel;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class GuestsTasksActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    private String userId;
    private String partyId;

    private LinearLayout guestsContainer;
    private LinearLayout tasksContainer;

    private TextView guestsEmptyText;
    private TextView tasksEmptyText;

    private ListenerRegistration guestsListener;
    private ListenerRegistration tasksListener;

    private final int PINK = Color.parseColor("#E83E82");
    private final int DARK_PINK = Color.parseColor("#B92D68");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guests_tasks);

        db = FirebaseFirestore.getInstance();

        FirebaseUser user = FirebaseAuth.getInstance()
                .getCurrentUser();

        if (user == null) {
            finish();
            return;
        }

        userId = user.getUid();
        partyId = getIntent().getStringExtra("partyId");

        if (partyId == null || partyId.isEmpty()) {
            Toast.makeText(this, "לא נמצאה מסיבה",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        guestsContainer = findViewById(R.id.guestsContainer);
        tasksContainer = findViewById(R.id.tasksContainer);

        guestsEmptyText = findViewById(R.id.guestsEmptyText);
        tasksEmptyText = findViewById(R.id.tasksEmptyText);

        TextView partyTitleText = findViewById(R.id.partyTitleText);

        String partyName = getIntent().getStringExtra("partyName");

        if (partyName != null && !partyName.isEmpty()) {
            partyTitleText.setText("ניהול המסיבה: " + partyName);
        }

        findViewById(R.id.addGuestButton)
                .setOnClickListener(v -> showAddDialog(true));

        findViewById(R.id.addTaskButton)
                .setOnClickListener(v -> showAddDialog(false));

        findViewById(R.id.backButton)
                .setOnClickListener(v -> finish());

        loadGuests();
        loadTasks();
    }

    private CollectionReference getPartyCollection(
            String collectionName) {

        return db.collection("users")
                .document(userId)
                .collection("parties")
                .document(partyId)
                .collection(collectionName);
    }

    // הוספת אורח או משימה
    private void showAddDialog(boolean isGuest) {

        EditText input = new EditText(this);

        input.setPadding(35, 25, 35, 25);
        input.setTextColor(DARK_PINK);
        input.setHint(isGuest ? "שם האורח" : "תיאור המשימה");
        input.setSingleLine(true);

        input.setInputType(
                InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );

        input.setTextDirection(View.TEXT_DIRECTION_RTL);
        input.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(isGuest ? "הוספת אורח 🎀" : "הוספת משימה ✨")
                .setView(input)
                .setPositiveButton("הוספה", null)
                .setNegativeButton("ביטול", null)
                .create();

        dialog.setOnShowListener(d -> {

            Button addButton =
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            addButton.setOnClickListener(v -> {

                String value = input.getText().toString().trim();

                if (value.isEmpty()) {
                    input.setError("נא למלא את השדה");
                    return;
                }

                Map<String, Object> data = new HashMap<>();

                data.put("name", value);
                data.put("createdAt", FieldValue.serverTimestamp());

                if (isGuest) {
                    data.put("attending", false);
                } else {
                    data.put("completed", false);
                }

                String collection = isGuest ? "guests" : "tasks";

                getPartyCollection(collection)
                        .add(data)
                        .addOnSuccessListener(documentReference -> {

                            Toast.makeText(
                                    this,
                                    isGuest
                                            ? "האורח נוסף בהצלחה 💗"
                                            : "המשימה נוספה בהצלחה 💗",
                                    Toast.LENGTH_SHORT
                            ).show();

                            dialog.dismiss();
                        })
                        .addOnFailureListener(e ->

                                Toast.makeText(
                                        this,
                                        "השמירה נכשלה: "
                                                + e.getLocalizedMessage(),
                                        Toast.LENGTH_LONG
                                ).show()
                        );
            });
        });

        dialog.show();
    }

    // טעינת האורחים
    private void loadGuests() {

        guestsListener = getPartyCollection("guests")
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        Toast.makeText(this,
                                "לא ניתן לטעון אורחים",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots == null) return;

                    guestsContainer.removeAllViews();

                    boolean isEmpty = snapshots.isEmpty();

                    guestsEmptyText.setVisibility(
                            isEmpty ? View.VISIBLE : View.GONE
                    );

                    int totalGuests = snapshots.size();
                    int attendingGuests = 0;

                    for (DocumentSnapshot document :
                            snapshots.getDocuments()) {

                        Boolean attending =
                                document.getBoolean("attending");

                        if (Boolean.TRUE.equals(attending)) {
                            attendingGuests++;
                        }
                    }

                    int waitingGuests =
                            totalGuests - attendingGuests;

                    addGuestSummary(
                            totalGuests,
                            attendingGuests,
                            waitingGuests
                    );

                    for (DocumentSnapshot document :
                            snapshots.getDocuments()) {

                        String name = document.getString("name");

                        if (name == null) continue;

                        LinearLayout guestRow =
                                new LinearLayout(this);

                        guestRow.setOrientation(
                                LinearLayout.HORIZONTAL
                        );

                        guestRow.setGravity(Gravity.CENTER_VERTICAL);
                        guestRow.setLayoutDirection(
                                View.LAYOUT_DIRECTION_RTL
                        );

                        guestRow.setPadding(20, 15, 20, 15);

                        guestRow.setBackgroundResource(
                                R.drawable.edit_text_background
                        );

                        LinearLayout.LayoutParams rowParams =
                                new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                );

                        rowParams.bottomMargin = 15;
                        guestRow.setLayoutParams(rowParams);

                        TextView guestName = new TextView(this);

                        guestName.setText("💗 " + name);
                        guestName.setTextSize(17);
                        guestName.setTextColor(DARK_PINK);

                        LinearLayout.LayoutParams nameParams =
                                new LinearLayout.LayoutParams(
                                        0,
                                        LinearLayout.LayoutParams.WRAP_CONTENT,
                                        1
                                );

                        guestName.setLayoutParams(nameParams);

                        CheckBox attendance = new CheckBox(this);

                        attendance.setText("מגיע/ה");
                        attendance.setTextSize(14);
                        attendance.setTextColor(DARK_PINK);

                        attendance.setButtonTintList(
                                ColorStateList.valueOf(PINK)
                        );

                        Boolean attending =
                                document.getBoolean("attending");

                        attendance.setChecked(
                                Boolean.TRUE.equals(attending)
                        );

                        attendance.setOnCheckedChangeListener(
                                (buttonView, isChecked) -> {

                                    document.getReference()
                                            .update("attending", isChecked)
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(
                                                            this,
                                                            "לא ניתן לעדכן אישור הגעה",
                                                            Toast.LENGTH_SHORT
                                                    ).show()
                                            );
                                }
                        );

                        TextView deleteButton = new TextView(this);

                        deleteButton.setText("✕");
                        deleteButton.setTextSize(20);
                        deleteButton.setTextColor(PINK);
                        deleteButton.setPadding(15, 10, 15, 10);
                        deleteButton.setGravity(Gravity.CENTER);

                        deleteButton.setOnClickListener(v ->
                                deleteGuest(document)
                        );

                        guestRow.addView(guestName);
                        guestRow.addView(attendance);
                        guestRow.addView(deleteButton);

                        guestsContainer.addView(guestRow);
                    }
                });
    }

    // סיכום הגעת האורחים
    private void addGuestSummary(
            int total,
            int attending,
            int waiting) {

        LinearLayout summaryCard = new LinearLayout(this);

        summaryCard.setOrientation(LinearLayout.VERTICAL);
        summaryCard.setGravity(Gravity.CENTER);
        summaryCard.setPadding(25, 25, 25, 25);

        summaryCard.setBackgroundResource(
                R.drawable.edit_text_background
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.bottomMargin = 20;
        summaryCard.setLayoutParams(params);

        TextView title = new TextView(this);

        title.setText("💗 סיכום הגעה");
        title.setTextSize(21);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(DARK_PINK);
        title.setGravity(Gravity.CENTER);

        TextView totalText = new TextView(this);

        totalText.setText("סה״כ מוזמנים: " + total);
        totalText.setTextSize(17);
        totalText.setTextColor(DARK_PINK);
        totalText.setGravity(Gravity.CENTER);

        TextView attendingText = new TextView(this);

        attendingText.setText("אישרו הגעה: " + attending + " ✓");
        attendingText.setTextSize(17);
        attendingText.setTextColor(PINK);
        attendingText.setGravity(Gravity.CENTER);

        TextView waitingText = new TextView(this);

        waitingText.setText("ממתינים לאישור: " + waiting);
        waitingText.setTextSize(17);
        waitingText.setTextColor(DARK_PINK);
        waitingText.setGravity(Gravity.CENTER);

        summaryCard.addView(title);
        summaryCard.addView(totalText);
        summaryCard.addView(attendingText);
        summaryCard.addView(waitingText);

        guestsContainer.addView(summaryCard);
    }

    // מחיקת אורח
    private void deleteGuest(DocumentSnapshot document) {

        String name = document.getString("name");

        if (name == null) {
            name = "האורח";
        }

        new AlertDialog.Builder(this)
                .setTitle("מחיקת אורח")
                .setMessage(
                        "למחוק את " + name + " מרשימת האורחים?"
                )
                .setPositiveButton("מחיקה", (dialog, which) -> {

                    document.getReference()
                            .delete()
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(
                                            this,
                                            "האורח נמחק בהצלחה",
                                            Toast.LENGTH_SHORT
                                    ).show()
                            )
                            .addOnFailureListener(e ->
                                    Toast.makeText(
                                            this,
                                            "מחיקת האורח נכשלה",
                                            Toast.LENGTH_SHORT
                                    ).show()
                            );
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    // טעינת המשימות וסיכום ההתקדמות
    private void loadTasks() {

        tasksListener = getPartyCollection("tasks")
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        Toast.makeText(this,
                                "לא ניתן לטעון משימות",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots == null) return;

                    tasksContainer.removeAllViews();

                    boolean isEmpty = snapshots.isEmpty();

                    tasksEmptyText.setVisibility(
                            isEmpty ? View.VISIBLE : View.GONE
                    );

                    int totalTasks = snapshots.size();
                    int completedTasks = 0;

                    for (DocumentSnapshot document :
                            snapshots.getDocuments()) {

                        Boolean completed =
                                document.getBoolean("completed");

                        if (Boolean.TRUE.equals(completed)) {
                            completedTasks++;
                        }
                    }

                    int remainingTasks =
                            totalTasks - completedTasks;

                    addTaskSummary(
                            totalTasks,
                            completedTasks,
                            remainingTasks
                    );

                    for (DocumentSnapshot document :
                            snapshots.getDocuments()) {

                        String name = document.getString("name");

                        if (name == null) continue;

                        CheckBox task = new CheckBox(this);

                        task.setText(name);
                        task.setTextSize(17);
                        task.setTextColor(DARK_PINK);

                        task.setButtonTintList(
                                ColorStateList.valueOf(PINK)
                        );

                        task.setLayoutDirection(
                                View.LAYOUT_DIRECTION_RTL
                        );

                        task.setTextDirection(
                                View.TEXT_DIRECTION_RTL
                        );

                        task.setPadding(25, 20, 25, 20);

                        task.setBackgroundResource(
                                R.drawable.edit_text_background
                        );

                        Boolean completed =
                                document.getBoolean("completed");

                        task.setChecked(
                                Boolean.TRUE.equals(completed)
                        );

                        task.setOnCheckedChangeListener(
                                (buttonView, isChecked) -> {

                                    document.getReference()
                                            .update("completed", isChecked)
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(
                                                            this,
                                                            "לא ניתן לעדכן משימה",
                                                            Toast.LENGTH_SHORT
                                                    ).show()
                                            );
                                }
                        );

                        LinearLayout.LayoutParams params =
                                new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                );

                        params.bottomMargin = 15;
                        task.setLayoutParams(params);

                        tasksContainer.addView(task);
                    }
                });
    }

    // כרטיס סיכום התקדמות המשימות
    private void addTaskSummary(
            int total,
            int completed,
            int remaining) {

        LinearLayout summaryCard = new LinearLayout(this);

        summaryCard.setOrientation(LinearLayout.VERTICAL);
        summaryCard.setGravity(Gravity.CENTER);
        summaryCard.setPadding(25, 25, 25, 25);

        summaryCard.setBackgroundResource(
                R.drawable.edit_text_background
        );

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.bottomMargin = 20;
        summaryCard.setLayoutParams(cardParams);

        TextView title = new TextView(this);

        title.setText("✨ התקדמות המשימות");
        title.setTextSize(21);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(DARK_PINK);
        title.setGravity(Gravity.CENTER);

        TextView completedText = new TextView(this);

        completedText.setText(
                "משימות שהושלמו: " + completed + " ✓"
        );

        completedText.setTextSize(17);
        completedText.setTextColor(PINK);
        completedText.setGravity(Gravity.CENTER);

        TextView remainingText = new TextView(this);

        remainingText.setText(
                "משימות שנותרו: " + remaining
        );

        remainingText.setTextSize(17);
        remainingText.setTextColor(DARK_PINK);
        remainingText.setGravity(Gravity.CENTER);

        int percentage = total == 0
                ? 0
                : (completed * 100 / total);

        TextView percentageText = new TextView(this);

        percentageText.setText(
                percentage + "% הושלמו"
        );

        percentageText.setTextSize(19);
        percentageText.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        percentageText.setTextColor(PINK);
        percentageText.setGravity(Gravity.CENTER);

        ProgressBar progressBar = new ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
        );

        progressBar.setMax(100);
        progressBar.setProgress(percentage);

        progressBar.setProgressTintList(
                ColorStateList.valueOf(PINK)
        );

        progressBar.setProgressBackgroundTintList(
                ColorStateList.valueOf(
                        Color.parseColor("#F8C9DC")
                )
        );

        LinearLayout.LayoutParams progressParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        22
                );

        progressParams.setMargins(0, 20, 0, 20);
        progressBar.setLayoutParams(progressParams);

        summaryCard.addView(title);
        summaryCard.addView(completedText);
        summaryCard.addView(remainingText);
        summaryCard.addView(progressBar);
        summaryCard.addView(percentageText);

        tasksContainer.addView(summaryCard);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (guestsListener != null) {
            guestsListener.remove();
        }

        if (tasksListener != null) {
            tasksListener.remove();
        }
    }
}
