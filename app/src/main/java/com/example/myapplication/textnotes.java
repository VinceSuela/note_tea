package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jp.wasabeef.richeditor.RichEditor;

public class textnotes extends AppCompatActivity {

    private EditText titleEditText;
    private jp.wasabeef.richeditor.RichEditor editor;
    private ImageView exitButton;
    private ImageView checkButton;
    private ImageView boldButton, italicButton, underlineButton, imageButton, undoButton, redoButton;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private String time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_textnotes);

        titleEditText = findViewById(R.id.title);
        exitButton = findViewById(R.id.exit);
        checkButton = findViewById(R.id.check);

        editor = findViewById(R.id.editor);
        editor.setPlaceholder("Write here...");
        editor.setPadding(20, 20, 20, 20);

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


        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        exitButton.setOnClickListener(v -> {
            saveNote();
            finish();
        });

        checkButton.setOnClickListener(v -> {
            saveNote();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        });
    }

    private void saveNote() {
        String title = titleEditText.getText().toString().trim();
        String content = editor.getHtml();
        time = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date()).toString();


        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(content)) {
            Toast.makeText(this, "Title and content cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (user == null) {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> noteData = new HashMap<>();
        noteData.put("note_title", title);
        noteData.put("note_content", content);
        noteData.put("note_date", time);
        noteData.put("isPinned", false);
        noteData.put("isLocked", false);
        noteData.put("hashedPin", null);
        noteData.put("folder_id", null);
        noteData.put("isDeleted", false);
        noteData.put("deleted_date", null);
        noteData.put("type", "note");


        db.collection("users").document(user.getUid())
                .collection("notes")
                .add(noteData)
                .addOnSuccessListener(documentReference -> {
                    documentReference.update("note_id", documentReference.getId());
                    Toast.makeText(textnotes.this, "Note saved!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(textnotes.this, "Error saving note: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}