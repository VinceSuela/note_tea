package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class loginpage extends AppCompatActivity {

    EditText et_email, et_password;
    Button login;
    ImageView newuser;
    TextView tv_email, tv_password;
    RelativeLayout about, contact;
    FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        about = findViewById(R.id.about);
        contact = findViewById(R.id.contact);
        et_email = findViewById(R.id.email);
        et_password = findViewById(R.id.password);
        newuser = findViewById(R.id.newuser);
        login = findViewById(R.id.login);
        tv_email = findViewById(R.id.tv3);
        tv_password = findViewById(R.id.tv5);
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


        newuser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), signuppage.class);
                startActivity(intent);
                finish();
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email, password;
                email = String.valueOf(et_email.getText());
                password = String.valueOf(et_password.getText());

                if (TextUtils.isEmpty(email)) {
                    tv_email.setText("Enter valid email");
                    tv_email.setTextColor(getColor(R.color.red));
                }
                if (TextUtils.isEmpty(password)) {
                    tv_password.setText("Enter your password");
                    tv_password.setTextColor(getColor(R.color.red));
                }

                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Intent intent = new Intent(getApplicationContext(), mainpage.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(loginpage.this, "Authentication failed.",
                                            Toast.LENGTH_SHORT).show();

                                }
                            }
                        });
            }
        });
    }

    public void newuser (View view) {
        Intent intent = new Intent(getApplicationContext(), signuppage.class);
        startActivity(intent);
        finish();
    }

}