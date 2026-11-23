
package com.example.liel;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class BudgetActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String userId;
    private String partyId;

    private TextView budgetText;
    private TextView spentText;
    private TextView remainingText;
    private LinearLayout expensesContainer;

    private double budget = 0;
    private double spent = 0;

    private ListenerRegistration budgetListener;
    private ListenerRegistration expensesListener;

    private final int PINK = Color.parseColor("#E83E82");
    private final int DARK_PINK = Color.parseColor("#B92D68");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget);

        db = FirebaseFirestore.getInstance();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        userId = FirebaseAuth.getInstance()
                .getCurrentUser().getUid();

        partyId = getIntent().getStringExtra("partyId");

        if (partyId == null || partyId.isEmpty()) {
            Toast.makeText(this, "לא נמצאה מסיבה",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        budgetText = findViewById(R.id.budgetText);
        spentText = findViewById(R.id.spentText);
        remainingText = findViewById(R.id.remainingText);
        expensesContainer = findViewById(R.id.expensesContainer);

        findViewById(R.id.setBudgetButton)
                .setOnClickListener(v -> showBudgetDialog());

        findViewById(R.id.addExpenseButton)
                .setOnClickListener(v -> showExpenseDialog());

        findViewById(R.id.backButton)
                .setOnClickListener(v -> finish());

        loadBudget();
        loadExpenses();
    }

    private com.google.firebase.firestore.DocumentReference
    getPartyDocument() {

        return db.collection("users")
                .document(userId)
                .collection("parties")
                .document(partyId);
    }

    private void loadBudget() {

        budgetListener = getPartyDocument()
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null || snapshot == null) return;

                    Double savedBudget = snapshot.getDouble("budget");

                    budget = savedBudget == null ? 0 : savedBudget;

                    updateSummary();
                });
    }

    private void loadExpenses() {

        expensesListener = getPartyDocument()
                .collection("expenses")
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        Toast.makeText(this,
                                "לא ניתן לטעון הוצאות",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots == null) return;

                    expensesContainer.removeAllViews();
                    spent = 0;

                    for (DocumentSnapshot document :
                            snapshots.getDocuments()) {

                        Double amount = document.getDouble("amount");

                        if (amount != null) {
                            spent += amount;
                        }

                        String name = document.getString("name");

                        if (name == null) continue;

                        addExpenseCard(document, name,
                                amount == null ? 0 : amount);
                    }

                    updateSummary();
                });
    }

    private void updateSummary() {

        double remaining = budget - spent;

        budgetText.setText("תקציב כולל: " +
                String.format("%.2f", budget) + " ₪");

        spentText.setText("סך ההוצאות: " +
                String.format("%.2f", spent) + " ₪");

        remainingText.setText("יתרה: " +
                String.format("%.2f", remaining) + " ₪");

        remainingText.setTextColor(
                remaining < 0 ? Color.RED : DARK_PINK
        );
    }

    private void showBudgetDialog() {

        EditText input = new EditText(this);

        input.setHint("הכניסי תקציב בשקלים");
        input.setInputType(InputType.TYPE_CLASS_NUMBER |
                InputType.TYPE_NUMBER_FLAG_DECIMAL);

        input.setTextDirection(android.view.View.TEXT_DIRECTION_LTR);

        new AlertDialog.Builder(this)
                .setTitle("הגדרת תקציב 💗")
                .setView(input)
                .setPositiveButton("שמירה", (dialog, which) -> {

                    String value = input.getText().toString().trim();

                    if (value.isEmpty()) {
                        Toast.makeText(this,
                                "נא להזין תקציב",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        double newBudget = Double.parseDouble(value);

                        if (newBudget <= 0) {
                            Toast.makeText(this,
                                    "התקציב חייב להיות גדול מאפס",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        getPartyDocument()
                                .update("budget", newBudget)
                                .addOnSuccessListener(unused ->
                                        Toast.makeText(this,
                                                "התקציב נשמר בהצלחה 💗",
                                                Toast.LENGTH_SHORT).show()
                                )
                                .addOnFailureListener(e ->
                                        Toast.makeText(this,
                                                "שמירת התקציב נכשלה",
                                                Toast.LENGTH_SHORT).show()
                                );

                    } catch (NumberFormatException e) {
                        Toast.makeText(this,
                                "סכום לא תקין",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void showExpenseDialog() {

        LinearLayout layout = new LinearLayout(this);

        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(45, 15, 45, 5);

        EditText nameInput = new EditText(this);
        nameInput.setHint("על מה ההוצאה?");

        EditText amountInput = new EditText(this);
        amountInput.setHint("סכום בשקלים");
        amountInput.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                        InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        layout.addView(nameInput);
        layout.addView(amountInput);

        new AlertDialog.Builder(this)
                .setTitle("הוספת הוצאה 💸")
                .setView(layout)
                .setPositiveButton("הוספה", (dialog, which) -> {

                    String name = nameInput.getText()
                            .toString().trim();

                    String amountString = amountInput.getText()
                            .toString().trim();

                    if (name.isEmpty() || amountString.isEmpty()) {
                        Toast.makeText(this,
                                "נא למלא את כל השדות",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        double amount =
                                Double.parseDouble(amountString);

                        if (amount <= 0) {
                            Toast.makeText(this,
                                    "הסכום חייב להיות גדול מאפס",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Map<String, Object> expense =
                                new HashMap<>();

                        expense.put("name", name);
                        expense.put("amount", amount);
                        expense.put("createdAt",
                                FieldValue.serverTimestamp());

                        getPartyDocument()
                                .collection("expenses")
                                .add(expense)
                                .addOnSuccessListener(reference ->
                                        Toast.makeText(this,
                                                "ההוצאה נוספה 💗",
                                                Toast.LENGTH_SHORT).show()
                                )
                                .addOnFailureListener(e ->
                                        Toast.makeText(this,
                                                "הוספת ההוצאה נכשלה",
                                                Toast.LENGTH_SHORT).show()
                                );

                    } catch (NumberFormatException e) {
                        Toast.makeText(this,
                                "סכום לא תקין",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void addExpenseCard(
            DocumentSnapshot document,
            String name,
            double amount) {

        LinearLayout row = new LinearLayout(this);

        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(20, 18, 20, 18);
        row.setLayoutDirection(
                android.view.View.LAYOUT_DIRECTION_RTL
        );

        row.setBackgroundResource(
                R.drawable.edit_text_background
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.bottomMargin = 12;
        row.setLayoutParams(params);

        TextView expenseName = new TextView(this);

        expenseName.setText(name);
        expenseName.setTextColor(DARK_PINK);
        expenseName.setTextSize(17);

        LinearLayout.LayoutParams nameParams =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        expenseName.setLayoutParams(nameParams);

        TextView expenseAmount = new TextView(this);

        expenseAmount.setText(
                String.format("%.2f ₪", amount)
        );

        expenseAmount.setTextColor(PINK);
        expenseAmount.setTextSize(16);

        TextView deleteButton = new TextView(this);

        deleteButton.setText(" ✕ ");
        deleteButton.setTextColor(PINK);
        deleteButton.setTextSize(20);

        deleteButton.setOnClickListener(v ->

                new AlertDialog.Builder(this)
                        .setTitle("מחיקת הוצאה")
                        .setMessage("למחוק את ההוצאה " + name + "?")
                        .setPositiveButton("מחיקה",
                                (dialog, which) ->
                                        document.getReference()
                                                .delete()
                                                .addOnFailureListener(e ->
                                                        Toast.makeText(this,
                                                                "המחיקה נכשלה",
                                                                Toast.LENGTH_SHORT).show()
                                                )
                        )
                        .setNegativeButton("ביטול", null)
                        .show()
        );

        row.addView(expenseName);
        row.addView(expenseAmount);
        row.addView(deleteButton);

        expensesContainer.addView(row);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (budgetListener != null) {
            budgetListener.remove();
        }

        if (expensesListener != null) {
            expensesListener.remove();
        }
    }
}
