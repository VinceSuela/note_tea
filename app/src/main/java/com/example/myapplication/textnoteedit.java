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
// import com.google.firebase.firestore.FieldValue; // For serverTimestamp, if chosen

import java.text.SimpleDateFormat;
import java.util.Date; // Make sure this is java.util.Date
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jp.wasabeef.richeditor.RichEditor;


public class textnoteedit extends AppCompatActivity {

    EditText titleEditText;
    boolean isPinned, isLocked;

    RichEditor editor;
    String documentID, receivedTitle, uid;
    String receivedHashedPin, receivedFolderId;
    String originalContent;
    FirebaseFirestore db;
    ImageView exitButton, checkButton;
    DocumentReference noteRef;
    FirebaseAuth auth;
    FirebaseUser user;
    String currentTime;
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
        originalContent = getIntent().getStringExtra("key2");

        receivedTitle = getIntent().getStringExtra("note_title");
        isPinned = getIntent().getBooleanExtra("isPinned", false);
        isLocked = getIntent().getBooleanExtra("isLocked", false);
        receivedHashedPin = getIntent().getStringExtra("hashedPin");
        receivedFolderId = getIntent().getStringExtra("folder_id");

        if (documentID == null || documentID.isEmpty()) {
            Toast.makeText(this, "Error: Note ID missing.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        noteRef = db.collection("users").document(uid).collection("notes").document(documentID);

        titleEditText.setText(receivedTitle);
        if (originalContent != null) {
            editor.setHtml(originalContent);
        }


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
                finish();
            }
        });

    }

    private void updateNote() {
        String newNoteText = editor.getHtml();
        String newNoteTitle = titleEditText.getText().toString().trim();
        currentTime = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date()); // String date

        if (newNoteTitle.isEmpty() && (newNoteText == null || newNoteText.isEmpty())) {
            Toast.makeText(this, "Note cannot be empty. Discarding changes.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Map<String, Object> noteUpdates = new HashMap<>();
        noteUpdates.put("note_title", newNoteTitle);
        noteUpdates.put("note_content", newNoteText);
        noteUpdates.put("note_date", currentTime); // Your string representation of the date
        noteUpdates.put("note_id", documentID);

        noteUpdates.put("isPinned", isPinned);
        noteUpdates.put("isLocked", isLocked);
        noteUpdates.put("hashedPin", receivedHashedPin);
        noteUpdates.put("folder_id", receivedFolderId);

        noteUpdates.put("type", "text");

        // ***** ADD/UPDATE TIMESTAMP *****
        // Use java.util.Date; Firestore will convert it to its own Timestamp type.
        // This is crucial for sorting in mainpage.java
        noteUpdates.put("timestamp", new Date());

        // ***** ENSURE isDELETED is FALSE for active notes *****
        // This is crucial for filtering in mainpage.java
        noteUpdates.put("isDeleted", false);

        noteRef.set(noteUpdates) // .set() replaces the entire document.
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(textnoteedit.this, "Note updated!", Toast.LENGTH_SHORT).show();
                        // You might want to finish() here too after checkmark save
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
