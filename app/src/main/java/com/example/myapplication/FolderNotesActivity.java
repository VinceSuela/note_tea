package com.example.myapplication;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FolderNotesActivity extends AppCompatActivity
        implements rv_onClick, NoteActionsDialogFragment.NoteActionListener {

    private static final String TAG = "FolderNotesActivity";
    private RecyclerView notesRecyclerView;
    private myadapter noteAdapter; // Changed to myadapter to match mainpage
    private ArrayList<note> notesModels;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private String uid;
    private DocumentReference userRef;
    private ListenerRegistration textNoteListenerRegistration;
    private ListenerRegistration miscNoteListenerRegistration;
    private ProgressDialog progressDialog;

    private String folderId;
    private String folderName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_notes);

        Intent intent = getIntent();
        if (intent != null) {
            folderId = intent.getStringExtra("folder_id");
            folderName = intent.getStringExtra("folder_name");
        }

        if (folderId == null || folderName == null) {
            Toast.makeText(this, "Folder not specified.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar_folder_notes);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(folderName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        if (user == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            startActivity(new Intent(this, loginpage.class));
            return;
        }
        uid = user.getUid();
        userRef = db.collection("users").document(uid);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Loading notes...");

        notesRecyclerView = findViewById(R.id.folder_notes_recycler_view);
        notesRecyclerView.setHasFixedSize(true);
        notesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notesModels = new ArrayList<>();

        noteAdapter = new myadapter(FolderNotesActivity.this, notesModels, this, false); // Assuming list layout
        notesRecyclerView.setAdapter(noteAdapter);
        notesRecyclerView.setItemAnimator(null);

        // Instead of calling fetchAllFolderNotes from two places, we call it once from a consolidated listener
        listenForFolderNotes();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    // Combines listeners to prevent duplicate fetch calls
    private void listenForFolderNotes() {
        if (userRef == null || folderId == null) {
            Log.e(TAG, "UserRef or folderId is null, cannot set up listeners.");
            return;
        }
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }

        // Listener for text notes
        textNoteListenerRegistration = userRef.collection("notes")
                .whereEqualTo("folder_id", folderId)
                .whereEqualTo("isDeleted", false)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Text notes listener failed.", e);
                        return;
                    }
                    fetchAllFolderNotes(); // Refetch all notes on any change
                });

        // Listener for miscellaneous notes
        miscNoteListenerRegistration = userRef.collection("miscellaneous_notes")
                .whereEqualTo("folder_id", folderId)
                .whereEqualTo("isDeleted", false)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Misc notes listener failed.", e);
                        return;
                    }
                    fetchAllFolderNotes(); // Refetch all notes on any change
                });
    }


    private void fetchAllFolderNotes() {
        if (userRef == null || folderId == null) {
            Log.e(TAG, "userRef or folderId is null in fetchAllFolderNotes.");
            if (progressDialog.isShowing()) progressDialog.dismiss();
            return;
        }

        Task<QuerySnapshot> textNotesTask = userRef.collection("notes")
                .whereEqualTo("folder_id", folderId)
                .whereEqualTo("isDeleted", false)
                .get();

        Task<QuerySnapshot> miscNotesTask = userRef.collection("miscellaneous_notes")
                .whereEqualTo("folder_id", folderId)
                .whereEqualTo("isDeleted", false)
                .get();

        Tasks.whenAllSuccess(textNotesTask, miscNotesTask)
                .addOnSuccessListener(results -> {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }

                    notesModels.clear();

                    QuerySnapshot textNotesSnapshot = (QuerySnapshot) results.get(0);
                    for (DocumentSnapshot doc : textNotesSnapshot.getDocuments()) {
                        try {
                            note noteItem = doc.toObject(note.class);
                            if (noteItem != null) {
                                noteItem.setNote_id(doc.getId());
                                noteItem.setType("text"); // Explicitly set type
                                notesModels.add(noteItem);
                            }
                        } catch (Exception e) {
                             Log.e(TAG, "Error parsing text note from folder", e);
                        }
                    }

                    QuerySnapshot miscNotesSnapshot = (QuerySnapshot) results.get(1);
                    for (DocumentSnapshot doc : miscNotesSnapshot.getDocuments()) {
                         try {
                            note noteItem = doc.toObject(note.class);
                            if (noteItem != null) {
                                noteItem.setNote_id(doc.getId());
                                // Type should already be set correctly for misc notes (drawing, list, etc.)
                                if (noteItem.getType() != null) {
                                    notesModels.add(noteItem);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing misc note from folder", e);
                        }
                    }

                    Collections.sort(notesModels, (n1, n2) -> {
                        int pinnedCompare = Boolean.compare(n2.getIsPinned(), n1.getIsPinned());
                        if (pinnedCompare != 0) return pinnedCompare;
                        if (n1.getTimestamp() == null || n2.getTimestamp() == null) return 0;
                        return n2.getTimestamp().compareTo(n1.getTimestamp());
                    });

                    noteAdapter.notifyDataSetChanged();
                    Log.d(TAG, "All folder notes fetched and sorted. Total: " + notesModels.size());
                })
                .addOnFailureListener(e -> {
                    if (progressDialog.isShowing()) progressDialog.dismiss();
                    Toast.makeText(FolderNotesActivity.this, "Failed to load notes: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error fetching all notes for folder: ", e);
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Listeners are now persistent, but a manual fetch on resume can help sync faster if the page was paused for a long time
        fetchAllFolderNotes(); 
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textNoteListenerRegistration != null) textNoteListenerRegistration.remove();
        if (miscNoteListenerRegistration != null) miscNoteListenerRegistration.remove();
        if (progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
    }

    //************************************************************************
    // CORE FIX: Method to open editors, now passing the folder_id
    //************************************************************************
    private void openNoteForEditing(note note) {
        String noteType = note.getType();
        if (noteType == null) {
            Toast.makeText(this, "Cannot open note: Unknown type.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent;
        switch (noteType) {
            case "text":
                intent = new Intent(this, textnoteedit.class);
                intent.putExtra("key", note.getNote_id());
                intent.putExtra("key2", note.getNote_content());
                break;
            case "drawing":
                intent = new Intent(this, drawingpageedit.class);
                intent.putExtra("note_id", note.getNote_id());
                intent.putExtra("base64Image", note.getImageUrl());
                break;
            case "list":
                intent = new Intent(this, todolistpage.class);
                intent.putExtra("note_id", note.getNote_id());
                break;
//            case "audio":
//                intent = new Intent(this, AudioNoteActivity.class);
//                intent.putExtra("note_id", note.getNote_id());
//                break;
            default:
                Toast.makeText(this, "Cannot open this note type.", Toast.LENGTH_SHORT).show();
                return;
        }

        intent.putExtra("note_title", note.getNote_title());
        intent.putExtra("isPinned", note.getIsPinned());
        intent.putExtra("isLocked", note.getIsLocked());
        intent.putExtra("hashedPin", note.getHashedPin());
        intent.putExtra("folder_id", note.getFolder_id());

        startActivity(intent);
    }

    @Override
    public void onItemClicked(note clickedNote) {
        if (clickedNote.getIsLocked()) {
            showVerifyPinDialog(clickedNote, true);
        } else {
            openNoteForEditing(clickedNote);
        }
    }

    @Override
    public void onNoteLongClick(int position) {
        if (position >= 0 && position < notesModels.size()) {
            note selectedNote = notesModels.get(position);
            NoteActionsDialogFragment.newInstance(
                    selectedNote.getIsLocked(),
                    selectedNote.getIsPinned(),
                    selectedNote.getIsDeleted(),
                    selectedNote.getNote_id(),
                    position,
                    selectedNote.getFolder_id(),
                    selectedNote.getType()
            ).show(getSupportFragmentManager(), "NoteActionsDialogFragment");
        }
    }

    @Override
    public void onRestoreNote(String noteId, int position) {

    }

    @Override
    public void onPermanentlyDeleteNote(String noteId, int position) {

    }

    @Override
    public void onPinClick(int position, boolean currentPinnedStatus) {
        if (position >= 0 && position < notesModels.size()) {
            note noteToUpdate = notesModels.get(position);
            Map<String, Object> updates = new HashMap<>();
            updates.put("isPinned", !currentPinnedStatus);
            updates.put("timestamp", new Date());
            getNoteCollectionRef(noteToUpdate.getType()).document(noteToUpdate.getNote_id()).update(updates)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Note pin status updated.", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Error updating pin status.", Toast.LENGTH_SHORT).show());
        }
    }
    
    @Override
    public void onMoveToBin(String noteId, int position) {
        if (position >= 0 && position < notesModels.size()) {
            note noteToBin = notesModels.get(position);
            new AlertDialog.Builder(this)
                .setTitle("Move to Bin")
                .setMessage("Move this note to bin?")
                .setPositiveButton("Move", (dialog, which) -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("isDeleted", true);
                    updates.put("deleted_date", new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date()));
                    updates.put("timestamp", new Date());
                    getNoteCollectionRef(noteToBin.getType()).document(noteId).update(updates)
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "Note moved to bin.", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .setNegativeButton(android.R.string.cancel, null).show();
        }
    }

    @Override
    public void onAddToFolder(String noteId, String noteType, int position) {

    }

    @Override
    public void onLockNote(String noteId, int position) {
        if (position >= 0 && position < notesModels.size()) {
            note noteToUpdate = notesModels.get(position);
            showSetPinDialog(noteId, noteToUpdate.getType());
        }
    }

    @Override
    public void onUnlockNote(String noteId, int position) {
        if (position >= 0 && position < notesModels.size()) {
            note noteToUnlock = notesModels.get(position);
            showVerifyPinDialog(noteToUnlock, false);
        }
    }

    // Helper methods for PIN functionality
    private void showSetPinDialog(String noteId, String noteType) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint("Enter new 4-digit PIN");
        new AlertDialog.Builder(this)
                .setTitle("Set PIN")
                .setView(input)
                .setPositiveButton("Set", (dialog, which) -> {
                    String pinText = input.getText().toString().trim();
                    if (pinText.length() == 4) {
                        String hashedPin = hashPin(pinText);
                        if (hashedPin != null) {
                            updateNoteLockStatus(noteId, true, hashedPin, noteType);
                        }
                    } else {
                        Toast.makeText(this, "PIN must be 4 digits.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void showVerifyPinDialog(note noteToVerify, boolean isOpeningNote) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint("Enter PIN");
        new AlertDialog.Builder(this)
                .setTitle("Enter PIN to " + (isOpeningNote ? "Open" : "Unlock"))
                .setView(input)
                .setPositiveButton("Verify", (dialog, which) -> {
                    String hashedEnteredPin = hashPin(input.getText().toString().trim());
                    if (hashedEnteredPin != null && hashedEnteredPin.equals(noteToVerify.getHashedPin())) {
                        if (isOpeningNote) {
                            openNoteForEditing(noteToVerify);
                        } else {
                            updateNoteLockStatus(noteToVerify.getNote_id(), false, null, noteToVerify.getType());
                        }
                    } else {
                        Toast.makeText(this, "Incorrect PIN.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void updateNoteLockStatus(String noteId, boolean lockedStatus, @Nullable String hashedPin, String noteType) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isLocked", lockedStatus);
        updates.put("hashedPin", lockedStatus ? hashedPin : null);
        updates.put("timestamp", new Date());
        getNoteCollectionRef(noteType).document(noteId).update(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Note " + (lockedStatus ? "locked" : "unlocked") + ".", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update lock status.", Toast.LENGTH_LONG).show());
    }

    private String hashPin(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] messageDigest = md.digest(pin.getBytes());
            String hashtext = new BigInteger(1, messageDigest).toString(16);
            while (hashtext.length() < 32) hashtext = "0" + hashtext;
            return hashtext;
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
    
    // Unused but required by interface
    @Override
    public void onAddToFolder(String noteId, int position) {}
    @Override
    public void onDeleteClick(int position) {}
    @Override
    public void onRestoreNote(String noteId, String noteType) {}
    @Override
    public void onPermanentlyDeleteNote(String noteId, String noteType) {}
    @Override
    public void onRemoveFromFolder(String noteId, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Remove from Folder")
                .setMessage("Are you sure you want to remove this note from '" + folderName + "' and show it on the main page?")
                .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        note noteToRemove = notesModels.get(position);
                        String noteType = noteToRemove.getType();

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("folder_id", null);
                        updates.put("timestamp", new Date());

                        getNoteCollectionRef(noteType).document(noteId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(FolderNotesActivity.this, "Note removed from folder!", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(FolderNotesActivity.this, "Error removing note from folder: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    Log.e(TAG, "Error removing note from folder", e);
                                });
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }


    private CollectionReference getNoteCollectionRef(String noteType) {
        if (userRef == null) return null;
        if ("text".equals(noteType)) {
            return userRef.collection("notes");
        } else if (Arrays.asList("drawing", "list", "audio").contains(noteType)) {
            return userRef.collection("miscellaneous_notes");
        }
        return null;
    }
}
