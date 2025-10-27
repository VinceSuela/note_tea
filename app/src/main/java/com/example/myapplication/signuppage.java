package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class signuppage extends AppCompatActivity {

    EditText et_email, et_password;
    Button signup;
    TextView tv_email_error, tv_password_error, haveacc; // Renamed for clarity
    FirebaseAuth mAuth;
    RelativeLayout about, contact;
    ImageView viewstate;
    private boolean isPasswordVisible = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signuppage);

        about = findViewById(R.id.about);
        contact = findViewById(R.id.contact);
        et_email = findViewById(R.id.email);
        et_password = findViewById(R.id.password);
        haveacc = findViewById(R.id.tv6);
        signup = findViewById(R.id.signup);
        tv_email_error = findViewById(R.id.tv3); // Assuming tv3 is for email error/info
        tv_password_error = findViewById(R.id.tv5); // Assuming tv5 is for password error/info
        viewstate = findViewById(R.id.viewstate);
        mAuth = FirebaseAuth.getInstance();
        String url = "https://paffle.my.canva.site/notetea";

        about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            }
        });

        contact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            }
        });

        viewstate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPasswordVisible) {
                    // Hide password
                    et_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    viewstate.setImageResource(R.drawable.view); // Change to your 'view' icon
                } else {
                    // Show password
                    et_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    viewstate.setImageResource(R.drawable.hide); // Change to your 'hide' icon
                }
                et_password.setSelection(et_password.getText().length()); // Move cursor to end
                isPasswordVisible = !isPasswordVisible;
            }
        });


        haveacc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), loginpage.class);
                startActivity(intent);
                finish();
            }
        });


        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email, password;
                email = String.valueOf(et_email.getText()).trim();
                password = String.valueOf(et_password.getText()).trim();

                // Reset previous errors
                tv_email_error.setText("We'll never share your email.");
                tv_email_error.setTextColor(getColor(R.color.def));
                tv_password_error.setText("Minimum 8 characters.");
                tv_password_error.setTextColor(getColor(R.color.def));

                boolean hasError = false;
                if (TextUtils.isEmpty(email)) {
                    tv_email_error.setText("Enter email");
                    tv_email_error.setTextColor(getColor(R.color.red));
                    hasError = true;
                }
                // You might want to add email format validation here too

                if (TextUtils.isEmpty(password)) {
                    tv_password_error.setText("Enter password");
                    tv_password_error.setTextColor(getColor(R.color.red));
                    hasError = true;
                } else if (password.length() < 8) {
                    tv_password_error.setText("Password must be at least 8 characters");
                    tv_password_error.setTextColor(getColor(R.color.red));
                    hasError = true;
                }

                if (hasError) {
                    return; // Stop processing if there are input errors
                }

                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(signuppage.this, "Account created.", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(getApplicationContext(), loginpage.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    // Check for specific Firebase Auth errors if needed
                                    String errorMessage = task.getException() != null ? task.getException().getMessage() : "Authentication failed.";
                                    Toast.makeText(signuppage.this, "Authentication failed: " + errorMessage,
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        });

            }
        });
    }
}
