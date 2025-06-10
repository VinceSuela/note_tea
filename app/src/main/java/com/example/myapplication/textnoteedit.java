package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class textnoteedit extends AppCompatActivity {

    EditText title;
    boolean isPinned, isLocked;
    TextInputEditText text;
    String documentID, note_text, note_title, time, uid;
    String intl_text, intl_title, hashedPin, folder_id;
    FirebaseFirestore db;
    ImageView exit, check;
    DocumentReference userRef;
    FirebaseAuth auth;
    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_textnoteedit);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        text = findViewById(R.id.note);
        title = findViewById(R.id.title);
        exit = findViewById(R.id.exit);
        check = findViewById(R.id.check);

        if(user != null) {
            uid = user.getUid();
        }

        userRef = db.collection("users").document(uid);


        documentID = getIntent().getStringExtra("key");
        intl_title = getIntent().getStringExtra("key1");
        intl_text = getIntent().getStringExtra("key2");
        isPinned = getIntent().getExtras().getBoolean("key3");
        isLocked = getIntent().getExtras().getBoolean("key4");
        hashedPin = getIntent().getStringExtra("key5");
        folder_id = getIntent().getStringExtra("key6");

        title.setText(intl_title);
        text.setText(intl_text);


        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                note_text = text.getText().toString().trim();
                note_title = title.getText().toString();
                time = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date()).toString();


                Map<String, Object> user = new HashMap<>();
                user.put("note_content", note_text);
                user.put("note_date", time);
                user.put("note_title", note_title);
                user.put("note_id", documentID);
                user.put("isPinned", isPinned);
                user.put("isLocked", isLocked);
                user.put("hashedPin", hashedPin);
                user.put("folder_id", folder_id);

                userRef.collection("notes").document(documentID).set(user)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(textnoteedit.this,"Fail: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                note_text = text.getText().toString().trim();
                note_title = title.getText().toString();
                time = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date()).toString();

                Map<String, Object> user = new HashMap<>();
                user.put("note_content", note_text);
                user.put("note_date", time);
                user.put("note_title", note_title);
                user.put("note_id", documentID);
                user.put("isPinned", isPinned);
                user.put("isLocked", isLocked);
                user.put("hashedPin", hashedPin);
                user.put("folder_id", folder_id);

                userRef.collection("notes").document(documentID).set(user)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(textnoteedit.this,"Fail: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });

                Intent intent = new Intent(getApplicationContext(), mainpage.class);
                startActivity(intent);
                finish();
            }
        });


    }
}