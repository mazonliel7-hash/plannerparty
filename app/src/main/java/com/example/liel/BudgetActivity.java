package com.example.liel;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class BudgetActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String userId;
    private String partyId;

    private TextView budgetText, spentText, remainingText;
    private TextView budgetStatusText, emptyExpensesText;
    private TextView categorySummaryText;

    private ProgressBar budgetProgress;
    private LinearLayout expensesContainer;

    private double budget = 0;
    private double spent = 0;

    private final Map<String, Double> categoryTotals = new TreeMap<>();

    private ListenerRegistration budgetListener;
    private ListenerRegistration expensesListener;

    private final int PINK = Color.parseColor("#E83E82");
    private final int DARK_PINK = Color.parseColor("#B92D68");

    private final String[] categories = {
            "אוכל",
            "שתייה",
            "מוזיקה",
            "עיצוב וקישוטים",
            "מקום",
            "בידור ואטרקציות",
            "אחר"
    };

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
                .getCurrentUser()
                .getUid();

        partyId = getIntent().getStringExtra("partyId");

        if (partyId == null || partyId.isEmpty()) {
            Toast.makeText(
                    this,
                    "לא נמצאה מסיבה",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        budgetText = findViewById(R.id.budgetText);
        spentText = findViewById(R.id.spentText);
        remainingText = findViewById(R.id.remainingText);

        budgetProgress = findViewById(R.id.budgetProgress);
        budgetStatusText = findViewById(R.id.budgetStatusText);

        emptyExpensesText = findViewById(R.id.emptyExpensesText);
        expensesContainer = findViewById(R.id.expensesContainer);
        categorySummaryText = findViewById(R.id.categorySummaryText);

        findViewById(R.id.setBudgetButton)
                .setOnClickListener(v -> showBudgetDialog());

        findViewById(R.id.addExpenseButton)
                .setOnClickListener(v -> showExpenseDialog());

        findViewById(R.id.backButton)
                .setOnClickListener(v -> finish());

        loadBudget();
        loadExpenses();
    }

    private DocumentReference getPartyDocument() {
        return db.collection("users")
                .document(userId)
                .collection("parties")
                .document(partyId);
    }

    private void loadBudget() {
        budgetListener = getPartyDocument()
                .addSnapshotListener((snapshot, error) -> {

                    if (error != null) {
                        Toast.makeText(
                                this,
                                "לא ניתן לטעון את התקציב",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Object value = snapshot.get("budget");

                        if (value instanceof Number) {
                            budget = ((Number) value).doubleValue();
                        } else {
                            budget = 0;
                        }

                        updateBudgetDisplay();
                    }
                });
    }

    private void loadExpenses() {
        expensesListener = getPartyDocument()
                .collection("expenses")
                .addSnapshotListener((snapshots, error) -> {

                    if (error != null) {
                        Toast.makeText(
                                this,
                                "לא ניתן לטעון את ההוצאות",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    expensesContainer.removeAllViews();
                    categoryTotals.clear();
                    spent = 0;

                    if (snapshots == null || snapshots.isEmpty()) {
                        emptyExpensesText.setVisibility(View.VISIBLE);
                        categorySummaryText.setText("עדיין אין הוצאות");

                        updateBudgetDisplay();
                        return;
                    }

                    emptyExpensesText.setVisibility(View.GONE);

                    for (DocumentSnapshot document :
                            snapshots.getDocuments()) {

                        String name = document.getString("name");

                        if (name == null || name.trim().isEmpty()) {
                            name = "הוצאה";
                        }

                        Object amountValue = document.get("amount");

                        double amount = amountValue instanceof Number
                                ? ((Number) amountValue).doubleValue()
                                : 0;

                        String category =
                                document.getString("category");

                        if (category == null ||
                                category.trim().isEmpty()) {
                            category = "אחר";
                        }

                        spent += amount;

                        categoryTotals.put(
                                category,
                                categoryTotals.getOrDefault(
                                        category,
                                        0.0
                                ) + amount
                        );

                        addExpenseCard(
                                document,
                                name,
                                amount,
                                category
                        );
                    }

                    updateCategorySummary();
                    updateBudgetDisplay();
                });
    }

    private void updateBudgetDisplay() {
        double remaining = budget - spent;

        budgetText.setText(
                String.format("תקציב כולל: %.2f ₪", budget)
        );

        spentText.setText(
                String.format("סך ההוצאות: %.2f ₪", spent)
        );

        remainingText.setText(
                String.format("יתרה: %.2f ₪", remaining)
        );

        int progress = 0;

        if (budget > 0) {
            progress = (int) Math.min(
                    100,
                    Math.round((spent / budget) * 100)
            );
        }

        budgetProgress.setProgress(progress);

        if (budget <= 0) {

            budgetStatusText.setText(
                    "הגדירי תקציב כדי לעקוב אחרי ההוצאות"
            );

            budgetStatusText.setTextColor(DARK_PINK);

        } else if (spent >= budget) {

            budgetStatusText.setText(
                    "שימי לב! הגעת לתקציב או חרגת ממנו"
            );

            budgetStatusText.setTextColor(Color.RED);

        } else if (spent >= budget * 0.8) {

            budgetStatusText.setText(
                    "ניצלת כבר 80% מהתקציב"
            );

            budgetStatusText.setTextColor(DARK_PINK);

        } else {

            budgetStatusText.setText(
                    "התקציב שלך בשליטה 💗"
            );

            budgetStatusText.setTextColor(PINK);
        }
    }

    private void updateCategorySummary() {
        if (categoryTotals.isEmpty()) {
            categorySummaryText.setText("עדיין אין הוצאות");
            return;
        }

        StringBuilder builder = new StringBuilder();

        for (Map.Entry<String, Double> entry :
                categoryTotals.entrySet()) {

            builder.append(entry.getKey())
                    .append(": ")
                    .append(String.format(
                            "%.2f ₪",
                            entry.getValue()
                    ))
                    .append("\n");
        }

        categorySummaryText.setText(
                builder.toString().trim()
        );
    }

    private void showBudgetDialog() {
        EditText input = new EditText(this);

        input.setHint("הכניסי סכום תקציב");

        input.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                        InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        input.setText(
                budget > 0 ? String.valueOf(budget) : ""
        );

        input.setGravity(Gravity.CENTER);
        input.setTextColor(DARK_PINK);

        new AlertDialog.Builder(this)
                .setTitle("הגדרת תקציב")
                .setView(input)
                .setPositiveButton("שמירה", (dialog, which) -> {

                    String text = input.getText()
                            .toString()
                            .trim();

                    if (text.isEmpty()) {
                        Toast.makeText(
                                this,
                                "הכניסי סכום תקין",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    try {
                        double newBudget =
                                Double.parseDouble(text);

                        if (newBudget < 0) {
                            Toast.makeText(
                                    this,
                                    "התקציב לא יכול להיות שלילי",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        getPartyDocument()
                                .update("budget", newBudget)
                                .addOnSuccessListener(unused ->
                                        Toast.makeText(
                                                this,
                                                "התקציב נשמר",
                                                Toast.LENGTH_SHORT
                                        ).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(
                                                this,
                                                "שמירת התקציב נכשלה",
                                                Toast.LENGTH_SHORT
                                        ).show());

                    } catch (NumberFormatException e) {
                        Toast.makeText(
                                this,
                                "הכניסי סכום תקין",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void showExpenseDialog() {
        showExpenseForm(
                null,
                "",
                "",
                "אחר",
                false
        );
    }

    private void showEditExpenseDialog(
            DocumentSnapshot document,
            String name,
            double amount,
            String category) {

        showExpenseForm(
                document,
                name,
                String.valueOf(amount),
                category,
                true
        );
    }

    private void showExpenseForm(
            DocumentSnapshot document,
            String oldName,
            String oldAmount,
            String oldCategory,
            boolean editing) {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 10, 30, 0);

        EditText nameInput = new EditText(this);
        nameInput.setHint("שם ההוצאה");
        nameInput.setText(oldName);
        nameInput.setTextColor(DARK_PINK);

        layout.addView(nameInput);

        EditText amountInput = new EditText(this);
        amountInput.setHint("סכום בשקלים");

        amountInput.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                        InputType.TYPE_NUMBER_FLAG_DECIMAL
        );

        amountInput.setText(oldAmount);
        amountInput.setTextColor(DARK_PINK);

        layout.addView(amountInput);

        TextView categoryLabel = new TextView(this);
        categoryLabel.setText("קטגוריה");
        categoryLabel.setTextColor(DARK_PINK);
        categoryLabel.setTextSize(16);
        categoryLabel.setPadding(0, 12, 0, 6);

        layout.addView(categoryLabel);

        Spinner categorySpinner = new Spinner(this);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        categories
                );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        categorySpinner.setAdapter(adapter);

        int selected = 0;

        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(oldCategory)) {
                selected = i;
                break;
            }
        }

        categorySpinner.setSelection(selected);
        layout.addView(categorySpinner);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(
                        editing
                                ? "עריכת הוצאה"
                                : "הוספת הוצאה"
                )
                .setView(layout)
                .setPositiveButton("שמירה", null)
                .setNegativeButton("ביטול", null)
                .create();

        dialog.setOnShowListener(d ->
                dialog.getButton(
                        AlertDialog.BUTTON_POSITIVE
                ).setOnClickListener(v -> {

                    String name = nameInput.getText()
                            .toString()
                            .trim();

                    String amountText = amountInput.getText()
                            .toString()
                            .trim();

                    if (name.isEmpty() ||
                            amountText.isEmpty()) {

                        Toast.makeText(
                                this,
                                "מלאי שם וסכום",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    double amount;

                    try {
                        amount = Double.parseDouble(
                                amountText
                        );

                    } catch (NumberFormatException e) {

                        Toast.makeText(
                                this,
                                "הכניסי סכום תקין",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    if (amount <= 0) {
                        Toast.makeText(
                                this,
                                "הסכום חייב להיות גדול מאפס",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    String category =
                            categorySpinner.getSelectedItem()
                                    .toString();

                    Map<String, Object> values =
                            new HashMap<>();

                    values.put("name", name);
                    values.put("amount", amount);
                    values.put("category", category);

                    if (editing && document != null) {

                        document.getReference()
                                .update(values)
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(
                                            this,
                                            "ההוצאה עודכנה",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    dialog.dismiss();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(
                                                this,
                                                "עדכון ההוצאה נכשל",
                                                Toast.LENGTH_SHORT
                                        ).show());

                    } else {

                        values.put(
                                "createdAt",
                                FieldValue.serverTimestamp()
                        );

                        getPartyDocument()
                                .collection("expenses")
                                .add(values)
                                .addOnSuccessListener(reference -> {
                                    Toast.makeText(
                                            this,
                                            "ההוצאה נוספה",
                                            Toast.LENGTH_SHORT
                                    ).show();

                                    dialog.dismiss();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(
                                                this,
                                                "הוספת ההוצאה נכשלה",
                                                Toast.LENGTH_SHORT
                                        ).show());
                    }
                }));

        dialog.show();
    }

    private void addExpenseCard(
            DocumentSnapshot document,
            String name,
            double amount,
            String category) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(24, 18, 24, 18);

        card.setBackgroundResource(
                R.drawable.edit_text_background
        );

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.setMargins(0, 0, 0, 14);
        card.setLayoutParams(cardParams);

        TextView details = new TextView(this);

        details.setText(
                name + "\n" +
                        category + "\n" +
                        String.format("%.2f ₪", amount)
        );

        details.setTextColor(DARK_PINK);
        details.setTextSize(17);
        details.setGravity(Gravity.CENTER);

        card.addView(details);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);
        buttons.setPadding(0, 10, 0, 0);

        TextView editButton =
                makeActionText("✎ עריכה", PINK);

        editButton.setOnClickListener(v ->
                showEditExpenseDialog(
                        document,
                        name,
                        amount,
                        category
                ));

        TextView deleteButton =
                makeActionText("✕ מחיקה", Color.RED);

        deleteButton.setOnClickListener(v -> {

            new AlertDialog.Builder(this)
                    .setTitle("מחיקת הוצאה")
                    .setMessage(
                            "למחוק את ההוצאה \"" +
                                    name + "\"?"
                    )
                    .setPositiveButton(
                            "מחיקה",
                            (dialog, which) ->
                                    document.getReference()
                                            .delete()
                                            .addOnSuccessListener(unused ->
                                                    Toast.makeText(
                                                            this,
                                                            "ההוצאה נמחקה",
                                                            Toast.LENGTH_SHORT
                                                    ).show())
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(
                                                            this,
                                                            "מחיקת ההוצאה נכשלה",
                                                            Toast.LENGTH_SHORT
                                                    ).show())
                    )
                    .setNegativeButton("ביטול", null)
                    .show();
        });

        buttons.addView(editButton);
        buttons.addView(deleteButton);

        card.addView(buttons);

        expensesContainer.addView(card);
    }

    private TextView makeActionText(
            String text,
            int color) {

        TextView view = new TextView(this);

        view.setText(text);
        view.setTextColor(color);
        view.setTextSize(16);
        view.setGravity(Gravity.CENTER);
        view.setPadding(24, 8, 24, 8);

        return view;
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