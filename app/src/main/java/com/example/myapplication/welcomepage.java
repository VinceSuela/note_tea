package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.Timer;
import java.util.TimerTask;

public class welcomepage extends AppCompatActivity {

    FirebaseAuth mAuth;
    Timer timer;
    TimerTask timerTask;

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            timer = new Timer();
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    Intent intent = new Intent(welcomepage.this, mainpage.class);
                    startActivity(intent);
                    finish();
                }
            };

            timer.schedule(timerTask, 1500);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcomepage);

        mAuth = FirebaseAuth.getInstance();
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                Intent intent = new Intent(welcomepage.this, loginpage.class);
                startActivity(intent);
                finish();
            }
        };

        timer.schedule(timerTask, 1500);

    }
}