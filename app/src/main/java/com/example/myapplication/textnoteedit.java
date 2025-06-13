package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jp.wasabeef.richeditor.RichEditor;


public class textnoteedit extends AppCompatActivity {

    EditText titleEditText;
    boolean isPinned, isLocked;

    RichEditor editor;
    String documentID, note_title, uid;
    String intl_title, hashedPin, folder_id;
    String originalContent;
    FirebaseFirestore db;
    ImageView exitButton, checkButton;
    DocumentReference noteRef;
    FirebaseAuth auth;
    FirebaseUser user;
    String currentTime, deleted_date;
    boolean originalIsDeleted;
    private ImageView boldButton, italicButton, underlineButton, imageButton, undoButton, redoButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_textnoteedit);

        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        editor = findViewById(R.id.editor);
        editor.setPlaceholder("Write here...");
        editor.setPadding(20, 20, 20, 20);

        titleEditText = findViewById(R.id.title);
        exitButton = findViewById(R.id.exit);
        checkButton = findViewById(R.id.check);

        boldButton = findViewById(R.id.action_bold);
        italicButton = findViewById(R.id.action_italic);
        underlineButton = findViewById(R.id.action_underline);
        imageButton = findViewById(R.id.action_image);
        undoButton = findViewById(R.id.action_undo);
        redoButton = findViewById(R.id.action_redo);


        boldButton.setOnClickListener(v -> {
            editor.setBold();
            Toast.makeText(this, "Bold format toggled", Toast.LENGTH_SHORT).show();
        });

        italicButton.setOnClickListener(v -> {
            editor.setItalic();
            Toast.makeText(this, "Italic format toggled", Toast.LENGTH_SHORT).show();
        });

        underlineButton.setOnClickListener(v -> {
            editor.setUnderline();
            Toast.makeText(this, "Underline format toggled", Toast.LENGTH_SHORT).show();
        });

        undoButton.setOnClickListener(v -> {
            editor.undo();
            Toast.makeText(this, "Undo action performed", Toast.LENGTH_SHORT).show();
        });

        redoButton.setOnClickListener(v -> {
            editor.redo();
            Toast.makeText(this, "Redo action performed", Toast.LENGTH_SHORT).show();
        });


        imageButton.setOnClickListener(v -> {
            editor.removeFormat();
            Toast.makeText(this, "Formatting cleared", Toast.LENGTH_SHORT).show();
        });


        if(user != null) {
            uid = user.getUid();
        } else {
            Toast.makeText(this, "User not authenticated. Please log in.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        documentID = getIntent().getStringExtra("key");
        intl_title = getIntent().getStringExtra("key1");
        originalContent = getIntent().getStringExtra("key2");
        isPinned = getIntent().getExtras().getBoolean("key3");
        isLocked = getIntent().getExtras().getBoolean("key4");
        hashedPin = getIntent().getStringExtra("key5");
        folder_id = getIntent().getStringExtra("key6");
        originalIsDeleted = getIntent().getExtras().getBoolean("key7", false);
        deleted_date = getIntent().getStringExtra("key8");

        noteRef = db.collection("users").document(uid).collection("notes").document(documentID);

        titleEditText.setText(intl_title);
        editor.setHtml(originalContent);


        checkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateNote();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        });

        exitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateNote();
                Intent intent = new Intent(getApplicationContext(), mainpage.class);
                startActivity(intent);
                finish();
            }
        });

    }

    private void updateNote() {
        String newNoteText = editor.getHtml();
        String newNoteTitle = titleEditText.getText().toString().trim();
        currentTime = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date()).toString();

        if (newNoteTitle.isEmpty() && newNoteText.isEmpty()) {
            Toast.makeText(this, "Note cannot be empty. Discarding changes.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        Map<String, Object> noteUpdates = new HashMap<>();
        noteUpdates.put("note_content", newNoteText);
        noteUpdates.put("note_date", currentTime);
        noteUpdates.put("note_title", newNoteTitle);
        noteUpdates.put("note_id", documentID);
        noteUpdates.put("isPinned", isPinned);
        noteUpdates.put("isLocked", isLocked);
        noteUpdates.put("hashedPin", hashedPin);
        noteUpdates.put("folder_id", folder_id);
        noteUpdates.put("isDeleted", originalIsDeleted);
        noteUpdates.put("deleted_date", deleted_date);
        noteUpdates.put("type", "note");


        noteRef.set(noteUpdates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(textnoteedit.this, "Note updated!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(textnoteedit.this,"Fail to update note: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}