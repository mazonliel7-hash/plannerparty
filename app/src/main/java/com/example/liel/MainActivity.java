
package com.example.liel;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);

        // התחברות
        findViewById(R.id.loginButton).setOnClickListener(v -> {

            String email = usernameEditText.getText()
                    .toString().trim();

            String password = passwordEditText.getText()
                    .toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(
                        MainActivity.this,
                        "נא למלא אימייל וסיסמה",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {

                        if (task.isSuccessful()) {

                            Toast.makeText(
                                    MainActivity.this,
                                    "ההתחברות הצליחה",
                                    Toast.LENGTH_SHORT
                            ).show();

                            // מעבר למסך הבית
                            android.content.Intent intent =
                                    new android.content.Intent(
                                            MainActivity.this,
                                            HomeActivity.class
                                    );

                            startActivity(intent);
                            finish();

                        } else {

                            String error = "ההתחברות נכשלה";

                            if (task.getException() != null) {
                                error = task.getException().getMessage();
                            }

                            Toast.makeText(
                                    MainActivity.this,
                                    error,
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });
        });

        // פתיחת חלון הרשמה
        findViewById(R.id.registerTextView).setOnClickListener(v -> {
            showRegisterDialog();
        });
    }

    // חלון הרשמה
    private void showRegisterDialog() {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 10, 50, 10);

        EditText name = new EditText(this);
        name.setHint("שם משתמש");

        EditText email = new EditText(this);
        email.setHint("אימייל");
        email.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        );

        EditText password = new EditText(this);
        password.setHint("סיסמה");
        password.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        EditText confirmPassword = new EditText(this);
        confirmPassword.setHint("אישור סיסמה");
        confirmPassword.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD
        );

        layout.addView(name);
        layout.addView(email);
        layout.addView(password);
        layout.addView(confirmPassword);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("הרשמה ל-Party Planner")
                .setView(layout)
                .setPositiveButton("הרשמה", null)
                .setNegativeButton("ביטול", null)
                .create();

        dialog.setOnShowListener(d -> {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(view -> {

                        String userName = name.getText()
                                .toString().trim();

                        String userEmail = email.getText()
                                .toString().trim();

                        String userPassword = password.getText()
                                .toString();

                        String confirm = confirmPassword.getText()
                                .toString();

                        if (userName.isEmpty()
                                || userEmail.isEmpty()
                                || userPassword.isEmpty()
                                || confirm.isEmpty()) {

                            Toast.makeText(
                                    MainActivity.this,
                                    "נא למלא את כל הפרטים",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        if (!userPassword.equals(confirm)) {
                            Toast.makeText(
                                    MainActivity.this,
                                    "הסיסמאות אינן זהות",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        if (userPassword.length() < 6) {
                            Toast.makeText(
                                    MainActivity.this,
                                    "הסיסמה חייבת להכיל לפחות 6 תווים",
                                    Toast.LENGTH_SHORT
                            ).show();
                            return;
                        }

                        mAuth.createUserWithEmailAndPassword(
                                userEmail,
                                userPassword
                        ).addOnCompleteListener(this, task -> {

                            if (task.isSuccessful()) {

                                // שמירת שם המשתמש בפרופיל Firebase
                                if (mAuth.getCurrentUser() != null) {

                                    com.google.firebase.auth.UserProfileChangeRequest profile =
                                            new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                                    .setDisplayName(userName)
                                                    .build();

                                    mAuth.getCurrentUser()
                                            .updateProfile(profile);
                                }

                                Toast.makeText(
                                        MainActivity.this,
                                        "נרשמת בהצלחה!",
                                        Toast.LENGTH_SHORT
                                ).show();

                                dialog.dismiss();

                            } else {

                                String error = "ההרשמה נכשלה";

                                if (task.getException() != null) {
                                    error = task.getException().getMessage();
                                }

                                Toast.makeText(
                                        MainActivity.this,
                                        error,
                                        Toast.LENGTH_LONG
                                ).show();
                            }
                        });
                    });
        });

        dialog.show();
    }
}
