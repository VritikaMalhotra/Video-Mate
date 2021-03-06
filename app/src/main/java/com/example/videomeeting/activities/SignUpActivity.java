package com.example.videomeeting.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.videomeeting.R;
import com.example.videomeeting.utilities.Constants;
import com.example.videomeeting.utilities.PreferanceManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class SignUpActivity extends AppCompatActivity {

    private EditText inputFirstName, inputLastName, inputEmail, inputPassword, inputConfirmPassword;
    private MaterialButton buttonSignUp;
    private ProgressBar signUpProgressBar;
    private PreferanceManager preferanceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        signUpProgressBar = findViewById(R.id.signUpProgressBar);
        preferanceManager = new PreferanceManager(getApplicationContext());

        findViewById(R.id.imageBack).setOnClickListener(view -> onBackPressed());
        findViewById(R.id.textSignIn).setOnClickListener(view -> onBackPressed());

        inputFirstName = findViewById(R.id.inputFirstName);
        inputLastName = findViewById(R.id.inputLastName);
        inputEmail = findViewById(R.id.inputEmail);
        inputPassword = findViewById(R.id.inputPassword);
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword);
        buttonSignUp = findViewById(R.id.buttonsSignUp);

        buttonSignUp.setOnClickListener(view -> {

            if (inputFirstName.getText().toString().trim().isEmpty()) {

                inputFirstName.setError("First name required");
                inputFirstName.requestFocus();
                return;
            }
            if (inputLastName.getText().toString().trim().isEmpty()) {

                inputLastName.setError("Last name required");
                inputLastName.requestFocus();
                return;
            }
            if (inputEmail.getText().toString().trim().isEmpty()) {
                inputEmail.setError("Email is required");
                inputEmail.requestFocus();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(inputEmail.getText().toString().trim()).matches()) {
                inputEmail.setError("Please enter a correct email");
                inputEmail.requestFocus();
                return;
            }
            if (inputPassword.getText().toString().trim().isEmpty()) {
                inputPassword.setError("Password is required");
                inputPassword.requestFocus();
                return;
            }
            if (inputConfirmPassword.getText().toString().trim().isEmpty()) {
                inputConfirmPassword.setError("Confirm Password is required");
                inputConfirmPassword.requestFocus();
                return;
            }
            if (!((inputPassword.getText().toString().trim()).equals(inputConfirmPassword.getText().toString().trim()))) {

                inputConfirmPassword.setError("Confirm Password should match you previously entered password");
                inputConfirmPassword.requestFocus();
                return;
            }
            signup();


        });
    }

    private void signup() {

        buttonSignUp.setVisibility(View.INVISIBLE);
        signUpProgressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore database = FirebaseFirestore.getInstance();
        HashMap<String, Object> users = new HashMap<>();
        users.put(Constants.KEY_FIRST_NAME, inputFirstName.getText().toString());
        users.put(Constants.KEY_LAST_NAME, inputLastName.getText().toString());
        users.put(Constants.KEY_EMAIL, inputEmail.getText().toString());
        users.put(Constants.KEY_PASSWORD, inputPassword.getText().toString());

        database.collection(Constants.KEY_COLLECTION_USERS)
                .add(users)
                .addOnSuccessListener(documentReference -> {

                    preferanceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                    preferanceManager.putString(Constants.KEY_USER_ID,documentReference.getId());
                    preferanceManager.putString(Constants.KEY_FIRST_NAME, inputFirstName.getText().toString());
                    preferanceManager.putString(Constants.KEY_LAST_NAME, inputLastName.getText().toString());
                    preferanceManager.putString(Constants.KEY_EMAIL, inputEmail.getText().toString());
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);


                }).addOnFailureListener(e -> {

                    signUpProgressBar.setVisibility(View.INVISIBLE);
                    buttonSignUp.setVisibility(View.VISIBLE);
                    Toast.makeText(SignUpActivity.this, "Error : " + e.getMessage(), Toast.LENGTH_SHORT).show();

                });

    }
}