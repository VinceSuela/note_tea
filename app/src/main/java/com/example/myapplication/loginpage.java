package com.example.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class loginpage extends AppCompatActivity {

    private static final String TAG = "LoginPage";
    private static final int RC_SIGN_IN = 9001;

    EditText et_email, et_password;
    Button loginButton, forgotPasswordButton, loginWithGoogleButton;
    ImageView newuser, viewstate;
    TextView tv_email_helper, tv_password_helper; // Renamed for clarity (helper/error text)
    RelativeLayout about, contact;

    FirebaseAuth mAuth;
    GoogleSignInClient mGoogleSignInClient;
    private boolean isPasswordVisible = false;

    // Define default text color (e.g., from your XML #9F8F8F)
    // You might need to define this color in your colors.xml if not already present
    // For example, <color name="text_helper_default_color">#9F8F8F</color>
    // Then use ContextCompat.getColor(this, R.color.text_helper_default_color)
    // For simplicity, I'm hardcoding it here based on your XML, but using R.color is better.
    // Let's assume you have a color resource R.color.text_helper_default_color that corresponds to #9F8F8F
    // If not, you should add it to res/values/colors.xml like:
    // <color name="text_helper_default_color">#9F8F8F</color>
    // <color name="text_error_color">#FF0000</color> <!-- Example for red -->

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        et_email = findViewById(R.id.email);
        et_password = findViewById(R.id.password);
        newuser = findViewById(R.id.newuser);
        loginButton = findViewById(R.id.login);
        loginWithGoogleButton = findViewById(R.id.loginWithGoogleButton);
        tv_email_helper = findViewById(R.id.tv3);
        tv_password_helper = findViewById(R.id.tv5);
        viewstate = findViewById(R.id.viewstate);
        forgotPasswordButton = findViewById(R.id.forgotPassword);
        about = findViewById(R.id.about);
        contact = findViewById(R.id.contact);

        mAuth = FirebaseAuth.getInstance();
        String url = "https://paffle.my.canva.site/notetea";

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Crucial: from google-services.json
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        loginWithGoogleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithGoogle();
            }
        });

        about.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        });

        contact.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        });

        viewstate.setOnClickListener(v -> {
            if (isPasswordVisible) {
                et_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                viewstate.setImageResource(R.drawable.view);
            } else {
                et_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                viewstate.setImageResource(R.drawable.hide);
            }
            et_password.setSelection(et_password.getText().length());
            isPasswordVisible = !isPasswordVisible;
        });

        forgotPasswordButton.setOnClickListener(v -> showForgotPasswordDialog());
        newuser.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), signuppage.class);
            startActivity(intent);
            finish();
        });

        loginButton.setOnClickListener(v -> loginUserWithEmailPassword());
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account);
                }
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Google Sign-In failed: " + e.getStatusCode(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential;success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(loginpage.this, "Google Sign-In Successful.", Toast.LENGTH_SHORT).show();
                            updateUI(user);
                        } else {
                            Log.w(TAG, "signInWithCredential;failure", task.getException());
                            Toast.makeText(loginpage.this, "Google Authentication Failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                            updateUI(null);
                        }
                    }
                });
    }

    private void loginUserWithEmailPassword() {
        String email = et_email.getText().toString().trim();
        String password = et_password.getText().toString().trim();

        // Reset helper/error messages to default state
        // Make sure you have R.color.text_helper_default_color (e.g. #9F8F8F) and R.color.text_error_color (e.g. #FF0000) in your colors.xml
        int defaultHelperTextColor = getColor(R.color.def); // Replace with your actual default color resource
        int errorTextColor = getColor(R.color.red   ); // Replace with your actual error color resource

        tv_email_helper.setText("We\'ll never share your email.");
        tv_email_helper.setTextColor(defaultHelperTextColor);
        tv_password_helper.setText("Minimum 8 characters."); // This text seems more for signup page
        tv_password_helper.setTextColor(defaultHelperTextColor);

        boolean hasError = false;
        if (TextUtils.isEmpty(email)) {
            tv_email_helper.setText("Enter email");
            tv_email_helper.setTextColor(errorTextColor);
            hasError = true;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tv_email_helper.setText("Enter a valid email");
            tv_email_helper.setTextColor(errorTextColor);
            hasError = true;
        }

        if (TextUtils.isEmpty(password)) {
            tv_password_helper.setText("Enter your password"); // Corrected helper text for login
            tv_password_helper.setTextColor(errorTextColor);
            hasError = true;
        }

        if (hasError) {
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(loginpage.this, "Login Successful.", Toast.LENGTH_SHORT).show();
                            updateUI(user);
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Authentication failed.";
                            Toast.makeText(loginpage.this, "Authentication failed: " + errorMessage,
                                    Toast.LENGTH_LONG).show();
                            updateUI(null);
                        }
                    }
                });
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reset Password");
        final EditText inputEmail = new EditText(this);
        inputEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        inputEmail.setHint("Enter your registered email");
        LinearLayout container = new LinearLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        int paddingDp = 20;
        float density = getResources().getDisplayMetrics().density;
        int paddingPixel = (int)(paddingDp * density);
        container.setPadding(paddingPixel, paddingPixel / 2, paddingPixel, paddingPixel / 2);
        container.addView(inputEmail, lp);
        builder.setView(container);

        builder.setPositiveButton("Send Reset Email", (dialog, which) -> {
            String email = inputEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(loginpage.this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show();
                return;
            }
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(loginpage.this, "Password reset email sent.", Toast.LENGTH_LONG).show();
                        } else {
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Failed to send.";
                            Toast.makeText(loginpage.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            Intent intent = new Intent(getApplicationContext(), mainpage.class);
            startActivity(intent);
            finish();
        }
    }

    // This newuser method might be redundant if the newuser ImageView has its click listener set in onCreate
    public void newuser (View view) {
        Intent intent = new Intent(getApplicationContext(), signuppage.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        // FirebaseUser currentUser = mAuth.getCurrentUser();
        // if(currentUser != null){
        //    updateUI(currentUser); // Optional: Auto-login if already signed in
        // }
    }
}
