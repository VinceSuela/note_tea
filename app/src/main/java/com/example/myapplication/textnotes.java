package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class textnotes extends AppCompatActivity {

    TextInputEditText note;
    FirebaseFirestore db;
    ImageView exit, check;
    EditText note_title;
    String text, time, title, id, uid;
    FirebaseAuth auth;
    FirebaseUser user;
    DocumentReference userRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_textnotes);

        note_title = findViewById(R.id.title);
        note = findViewById(R.id.note);
        exit = findViewById(R.id.exit);
        check = findViewById(R.id.check);
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        id = db.collection("notes").document().getId().toString();

        if(user != null) {
            uid = user.getUid();
        }

        userRef = db.collection("users").document(uid);

        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                text = note.getText().toString().trim();
                title = note_title.getText().toString();
                time = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date()).toString();


                Map<String, Object> user = new HashMap<>();
                user.put("note_content", text);
                user.put("note_date", time);
                user.put("note_title", title);
                user.put("note_id", id);
                user.put("isPinned", false);
                user.put("isLocked", false);
                user.put("hashedPin", null);
                user.put("folder_id", null);

                userRef.collection("notes").document(id).set(user)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(textnotes.this,"Fail: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                text = note.getText().toString().trim();
                title = note_title.getText().toString();
                time = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date()).toString();

                Map<String, Object> user = new HashMap<>();
                user.put("note_content", text);
                user.put("note_date", time);
                user.put("note_title", title);
                user.put("note_id", id);
                user.put("isPinned", false);
                user.put("isLocked", false);
                user.put("hashedPin", null);
                user.put("folder_id", null);


                userRef.collection("notes").document(id).set(user)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(textnotes.this,"Fail: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });

                Intent intent = new Intent(getApplicationContext(), mainpage.class);
                startActivity(intent);
                finish();
            }
        });


    }

}