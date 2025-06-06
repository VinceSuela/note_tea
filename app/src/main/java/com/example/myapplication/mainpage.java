package com.example.myapplication;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;


public class mainpage extends AppCompatActivity {

    TextView username;
    ImageView circleplus;
    LinearLayout audio, image, drawing, list, text;
    int num = 0;
    ArrayList<rv_adapter> notesModels = new ArrayList<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainpage);

        username = findViewById(R.id.tv1);
        circleplus = findViewById(R.id.circleplus);
        audio = findViewById(R.id.audio);
        image = findViewById(R.id.image);
        drawing = findViewById(R.id.drawing);
        list = findViewById(R.id.list);
        text = findViewById(R.id.text);

        Animation cp_animation = AnimationUtils.loadAnimation(this, R.anim.circleplus);
        Animation cp_animation2 = AnimationUtils.loadAnimation(this, R.anim.circleplus2);
        Animation a_audio = AnimationUtils.loadAnimation(this, R.anim.aidlt_alpha);
        Animation a_audio2 = AnimationUtils.loadAnimation(this, R.anim.aidlt);
        Animation a_image = AnimationUtils.loadAnimation(this, R.anim.aidlt_alpha);
        Animation a_image2 = AnimationUtils.loadAnimation(this, R.anim.aidlt);
        Animation a_drawing = AnimationUtils.loadAnimation(this, R.anim.aidlt_alpha);
        Animation a_drawing2 = AnimationUtils.loadAnimation(this, R.anim.aidlt);
        Animation a_list = AnimationUtils.loadAnimation(this, R.anim.aidlt_alpha);
        Animation a_list2 = AnimationUtils.loadAnimation(this, R.anim.aidlt);
        Animation a_text = AnimationUtils.loadAnimation(this, R.anim.aidlt_alpha);
        Animation a_text2 = AnimationUtils.loadAnimation(this, R.anim.aidlt);


        circleplus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (num == 0){
                    circleplus.startAnimation(cp_animation);

                    Runnable runnable =  new Runnable() {
                        @Override
                        public void run() {
                            text.startAnimation(a_text);
                        }
                    };

                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(runnable, 25);

                    Runnable runnable1 =  new Runnable() {
                        @Override
                        public void run() {
                            list.startAnimation(a_list);
                        }
                    };

                    Handler handler1 = new Handler(Looper.getMainLooper());
                    handler1.postDelayed(runnable1, 50);

                    Runnable runnable2 =  new Runnable() {
                        @Override
                        public void run() {
                            drawing.startAnimation(a_drawing);
                        }
                    };

                    Handler handler2 = new Handler(Looper.getMainLooper());
                    handler2.postDelayed(runnable2, 75);

                    Runnable runnable3 =  new Runnable() {
                        @Override
                        public void run() {
                            image.startAnimation(a_image);
                        }
                    };

                    Handler handler3 = new Handler(Looper.getMainLooper());
                    handler3.postDelayed(runnable3, 100);

                    Runnable runnable4 =  new Runnable() {
                        @Override
                        public void run() {
                            audio.startAnimation(a_audio);
                        }
                    };

                    Handler handler4 = new Handler(Looper.getMainLooper());
                    handler4.postDelayed(runnable4, 125);

                    num = 1;
                }

                else if(num == 1){
                    circleplus.startAnimation(cp_animation2);
                    num = 0;

                    Runnable runnable =  new Runnable() {
                        @Override
                        public void run() {
                            text.startAnimation(a_text2);
                        }
                    };

                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(runnable, 125);

                    Runnable runnable1 =  new Runnable() {
                        @Override
                        public void run() {
                            list.startAnimation(a_list2);
                        }
                    };

                    Handler handler1 = new Handler(Looper.getMainLooper());
                    handler1.postDelayed(runnable1, 100);

                    Runnable runnable2 =  new Runnable() {
                        @Override
                        public void run() {
                            drawing.startAnimation(a_drawing2);
                        }
                    };

                    Handler handler2 = new Handler(Looper.getMainLooper());
                    handler2.postDelayed(runnable2, 75);

                    Runnable runnable3 =  new Runnable() {
                        @Override
                        public void run() {
                            image.startAnimation(a_image2);
                        }
                    };

                    Handler handler3 = new Handler(Looper.getMainLooper());
                    handler3.postDelayed(runnable3, 50);

                    Runnable runnable4 =  new Runnable() {
                        @Override
                        public void run() {
                            audio.startAnimation(a_audio2);
                        }
                    };

                    Handler handler4 = new Handler(Looper.getMainLooper());
                    handler4.postDelayed(runnable4, 25);

                }
            }

        });
    }
}