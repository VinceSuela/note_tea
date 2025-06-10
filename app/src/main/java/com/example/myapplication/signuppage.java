package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
    TextView tv_email, tv_password, haveacc;
    FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signuppage);

        et_email = findViewById(R.id.email);
        et_password = findViewById(R.id.password);
        haveacc = findViewById(R.id.tv6);
        signup = findViewById(R.id.signup);
        tv_email = findViewById(R.id.tv3);
        tv_password = findViewById(R.id.tv5);
        mAuth = FirebaseAuth.getInstance();


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
                email = String.valueOf(et_email.getText());
                password = String.valueOf(et_password.getText());

                if (TextUtils.isEmpty(email)) {
                    tv_email.setText("Enter email");
                    tv_email.setTextColor(getColor(R.color.red));
                }
                if (TextUtils.isEmpty(password)) {
                    tv_password.setText("Enter password");
                    tv_password.setTextColor(getColor(R.color.red));
                }

                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Intent intent = new Intent(getApplicationContext(), loginpage.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(signuppage.this, "Authentication failed.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

            }
        });
    }
}