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
    private CombinedNotesAdapter noteAdapter;
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

        noteAdapter = new CombinedNotesAdapter(FolderNotesActivity.this, notesModels, this);
        notesRecyclerView.setAdapter(noteAdapter);
        notesRecyclerView.setItemAnimator(null);

        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }

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

    private void listenForFolderNotes() {
        if (userRef == null || folderId == null) {
            Log.e(TAG, "UserRef or folderId is null, cannot set up Firestore listener.");
            if (progressDialog.isShowing()) progressDialog.dismiss();
            return;
        }

        Query textNotesQuery = userRef.collection("notes")
                .whereEqualTo("folder_id", folderId)
                .whereEqualTo("isDeleted", false);

        textNoteListenerRegistration = textNotesQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.e(TAG, "Firestore error for text notes in folder: ", error);
                    return;
                }
                if (value == null) {
                    Log.d(TAG, "Received null QuerySnapshot for text notes in folder.");
                    return;
                }
                Log.d(TAG, "Text notes in folder updated. Re-fetching all notes.");
                fetchAllFolderNotes();
            }
        });

        Query miscNotesQuery = userRef.collection("miscellaneous_notes")
                .whereEqualTo("folder_id", folderId)
                .whereEqualTo("isDeleted", false);

        miscNoteListenerRegistration = miscNotesQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.e(TAG, "Firestore error for miscellaneous notes in folder: ", error);
                    return;
                }
                if (value == null) {
                    Log.d(TAG, "Received null QuerySnapshot for miscellaneous notes in folder.");
                    return;
                }
                Log.d(TAG, "Miscellaneous notes in folder updated. Re-fetching all notes.");
                fetchAllFolderNotes();
            }
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

                    QuerySnapshot textNotesSnapshot = (QuerySnapshot) results.get(0);
                    QuerySnapshot miscNotesSnapshot = (QuerySnapshot) results.get(1);

                    notesModels.clear();

                    for (DocumentSnapshot doc : textNotesSnapshot.getDocuments()) {
                        note noteItem = doc.toObject(note.class);
                        if (noteItem != null) {
                            noteItem.setNote_id(doc.getId());
                            if (noteItem.getType() == null) noteItem.setType("text");
                            notesModels.add(noteItem);
                        }
                    }


                    for (DocumentSnapshot doc : miscNotesSnapshot.getDocuments()) {
                        note noteItem = doc.toObject(note.class);
                        if (noteItem != null) {
                            noteItem.setNote_id(doc.getId());
                            notesModels.add(noteItem);
                        }
                    }

                    Collections.sort(notesModels, new Comparator<note>() {
                        @Override
                        public int compare(note n1, note n2) {

                            int pinnedCompare = Boolean.compare(n2.getIsPinned(), n1.getIsPinned());
                            if (pinnedCompare != 0) {
                                return pinnedCompare;
                            }
                            if (n1.getTimestamp() == null && n2.getTimestamp() == null) return 0;
                            if (n1.getTimestamp() == null) return 1; // Null timestamps go last
                            if (n2.getTimestamp() == null) return -1;
                            return n2.getTimestamp().compareTo(n1.getTimestamp());
                        }
                    });

                    noteAdapter.notifyDataSetChanged();
                    Log.d(TAG, "All folder notes fetched and sorted. Total: " + notesModels.size());
                })
                .addOnFailureListener(e -> {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    Toast.makeText(FolderNotesActivity.this, "Failed to load notes for folder: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error fetching notes from both collections for folder: ", e);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (textNoteListenerRegistration != null) {
            textNoteListenerRegistration.remove();
        }
        if (miscNoteListenerRegistration != null) {
            miscNoteListenerRegistration.remove();
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onItemClicked(note note) {

        if (note.getIsLocked()) {
            showVerifyPinDialog(note.getNote_id(), notesModels.indexOf(note), true, note.getType());
        } else {
            openNoteForEditing(note);
        }
    }

    @Override
    public void onPinClick(int position, boolean currentPinnedStatus) {
        if (position != RecyclerView.NO_POSITION) {
            note noteToUpdate = notesModels.get(position);
            String documentId = noteToUpdate.getNote_id();
            String noteType = noteToUpdate.getType();

            if (documentId != null && !documentId.isEmpty() && noteType != null) {
                boolean newPinnedStatus = !currentPinnedStatus;
                Map<String, Object> updates = new HashMap<>();
                updates.put("isPinned", newPinnedStatus);
                updates.put("timestamp", new Date());
                getNoteCollectionRef(noteType).document(documentId).update(updates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(FolderNotesActivity.this, "Note pinned status updated!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(FolderNotesActivity.this, "Error toggling pin status: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Error toggling pin status for note ID: " + documentId, e);
                        });
            } else {
                Toast.makeText(FolderNotesActivity.this, "Error: Note ID or type missing for pin toggle.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Note ID or type was null/empty for pin toggle at position: " + position);
            }
        }
    }

    @Override
    public void onNoteLongClick(int position) {
        Log.d(TAG, "onNoteLongClick called for position: " + position);

        if (position == RecyclerView.NO_POSITION) {
            Log.w(TAG, "onNoteLongClick: Invalid position (RecyclerView.NO_POSITION)");
            return;
        }
        if (notesModels == null) {
            Log.e(TAG, "onNoteLongClick: notesModels is NULL! RecyclerView setup probably failed.");
            return;
        }
        if (position >= notesModels.size()) {
            Log.e(TAG, "onNoteLongClick: Position " + position + " is out of bounds for notesModels (size: " + notesModels.size() + ")");
            return;
        }

        note selectedNote = notesModels.get(position);
        if (selectedNote == null) {
            Log.e(TAG, "onNoteLongClick: selectedNote object at position " + position + " is NULL!");
            return;
        }

        Log.d(TAG, "onNoteLongClick: Selected Note ID: " + selectedNote.getNote_id() +
                ", Locked: " + selectedNote.getIsLocked() +
                ", Pinned: " + selectedNote.getIsPinned() +
                ", Deleted: " + selectedNote.getIsDeleted() +
                ", Folder ID: " + selectedNote.getFolder_id() +
                ", Type: " + selectedNote.getType());

        NoteActionsDialogFragment fragment = NoteActionsDialogFragment.newInstance(
                selectedNote.getIsLocked(),
                selectedNote.getIsPinned(),
                selectedNote.getIsDeleted(),
                selectedNote.getNote_id(),
                position,
                selectedNote.getFolder_id(),
                selectedNote.getType()
        );
        fragment.show(getSupportFragmentManager(), "NoteActionsDialogFragment");
    }

    @Override
    public void onDeleteClick(int position) {
        String currentTime = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date()).toString();
        if (position != RecyclerView.NO_POSITION && position < notesModels.size()) {
            new AlertDialog.Builder(this)
                    .setTitle("Move Note to Bin")
                    .setMessage("Are you sure you want to move this note to the bin?")
                    .setPositiveButton("Move to Bin", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            note noteToDelete = notesModels.get(position);
                            String documentId = noteToDelete.getNote_id();
                            String noteType = noteToDelete.getType();

                            if (documentId != null && !documentId.isEmpty() && noteType != null) {
                                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                if (currentUser != null) {
                                    Map<String, Object> updates = new HashMap<>();
                                    updates.put("isDeleted", true);
                                    updates.put("deleted_date", currentTime);
                                    updates.put("folder_id", null);
                                    updates.put("timestamp", new Date());

                                    getNoteCollectionRef(noteType)
                                            .document(documentId)
                                            .update(updates)
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(FolderNotesActivity.this, "Note moved to bin!", Toast.LENGTH_SHORT).show();

                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(FolderNotesActivity.this, "Error moving note to bin: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                Log.e(TAG, "Error moving note to bin", e);
                                            });
                                } else {
                                    Toast.makeText(FolderNotesActivity.this, "User not authenticated for deletion.", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(FolderNotesActivity.this, "Note ID or type is missing, cannot move to bin.", Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Note ID or type was null or empty for moving to bin at position: " + position);
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .setIcon(android.R.drawable.ic_menu_delete)
                    .show();
        } else {
            Toast.makeText(this, "Error: Invalid note position for action.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Attempted action on note at invalid position: " + position);
        }
    }

    @Override
    public void onLockNote(String noteId, int position) {
        showSetPinDialog(noteId, position);
    }

    @Override
    public void onUnlockNote(String noteId, int position) {
        showVerifyPinDialog(noteId, position, false, notesModels.get(position).getType());
    }

    @Override
    public void onMoveToBin(String noteId, int position) {
        onDeleteClick(position);
    }

    @Override
    public void onAddToFolder(String noteId, String noteType, int position) {
        Toast.makeText(this, "Note is already in a folder. Use 'Remove from Folder' first if moving.", Toast.LENGTH_LONG).show();

    }

    @Override
    public void onAddToFolder(String noteId, int position) {
        if (position != RecyclerView.NO_POSITION && position < notesModels.size()) {
            note selectedNote = notesModels.get(position);
            onAddToFolder(noteId, selectedNote.getType(), position);
        } else {
            Toast.makeText(this, "Error: Invalid note position for action.", Toast.LENGTH_SHORT).show();
        }
    }

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

    @Override
    public void onRestoreNote(String noteId, String noteType) {
        Toast.makeText(this, "This note is not in the bin.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPermanentlyDeleteNote(String noteId, String noteType) {
        new AlertDialog.Builder(this)
                .setTitle("Permanently Delete Note")
                .setMessage("This note will be permanently deleted and cannot be recovered. Are you sure?")
                .setPositiveButton("Delete Permanently", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        getNoteCollectionRef(noteType).document(noteId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(FolderNotesActivity.this, "Note permanently deleted!", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(FolderNotesActivity.this, "Error deleting note permanently: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    Log.e(TAG, "Error permanently deleting note", e);
                                });
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }


    @Override
    public void onRestoreNote(String noteId, int position) {
        Toast.makeText(this, "This note is not in the bin.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPermanentlyDeleteNote(String noteId, int position) {
        if (position != RecyclerView.NO_POSITION && position < notesModels.size()) {
            note noteToDelete = notesModels.get(position);
            String noteType = noteToDelete.getType();
            onPermanentlyDeleteNote(noteId, noteType);
        } else {
            Toast.makeText(this, "Error: Invalid note position for action.", Toast.LENGTH_SHORT).show();
        }
    }

    private String hashPin(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] messageDigest = md.digest(pin.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            String hashtext = no.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "SHA-256 algorithm not found", e);
            Toast.makeText(this, "Hashing error, cannot secure note.", Toast.LENGTH_SHORT).show();
            return null;
        }
    }


    private void showSetPinDialog(String noteId, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set PIN for Note");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint("Enter new 4-digit PIN");
        builder.setView(input);

        builder.setPositiveButton("Set", (dialog, which) -> {
            String pin = input.getText().toString().trim();
            if (pin.length() != 4 || !pin.matches("\\d{4}")) {
                Toast.makeText(this, "PIN must be exactly 4 digits.", Toast.LENGTH_SHORT).show();
                return;
            }
            String hashedPin = hashPin(pin);
            if (hashedPin != null) {
                note noteToUpdate = notesModels.get(position); // Get note to retrieve its type
                updateNoteLockStatus(noteId, position, true, hashedPin, noteToUpdate.getType());
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showVerifyPinDialog(String noteId, int position, boolean openingNote, @Nullable String noteType) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter PIN to " + (openingNote ? "Open" : "Unlock") + " Note");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint("Enter PIN");
        builder.setView(input);

        builder.setPositiveButton("Verify", (dialog, which) -> {
            String enteredPin = input.getText().toString().trim();
            String hashedEnteredPin = hashPin(enteredPin);

            note noteToVerify = notesModels.get(position);
            if (hashedEnteredPin != null && hashedEnteredPin.equals(noteToVerify.getHashedPin())) {
                Toast.makeText(this, "PIN correct!", Toast.LENGTH_SHORT).show();

                if (openingNote) {

                    note tempNoteForOpening = new note(
                            noteToVerify.getImageUrl(),
                            noteToVerify.getType(),
                            noteToVerify.getFolder_id(),
                            noteToVerify.getDeleted_date(),
                            null,
                            noteToVerify.getIsDeleted(),
                            false,
                            noteToVerify.getIsPinned(),
                            noteToVerify.getNote_content(),
                            noteToVerify.getNote_date(),
                            noteToVerify.getNote_id(),
                            noteToVerify.getNote_title(),
                            noteToVerify.getTimestamp()
                    );
                    openNoteForEditing(tempNoteForOpening);
                } else {
                    updateNoteLockStatus(noteId, position, false, null, noteToVerify.getType());
                }
            } else {
                Toast.makeText(this, "Incorrect PIN.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateNoteLockStatus(String noteId, int position, boolean lockedStatus, String hashedPin, String noteType) {
        if (noteType == null) {
            Log.e(TAG, "noteType is null in updateNoteLockStatus. Cannot update note.");
            Toast.makeText(this, "Error: Note type missing for lock update.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("isLocked", lockedStatus);
        updates.put("hashedPin", hashedPin);
        updates.put("timestamp", new Date());

        getNoteCollectionRef(noteType).document(noteId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(FolderNotesActivity.this, "Note " + (lockedStatus ? "locked" : "unlocked") + "!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(FolderNotesActivity.this, "Failed to update lock status: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error updating note lock status", e);
                });
    }

    private void openNoteForEditing(note note) {
        String noteType = note.getType();
        if (noteType == null) {
            Log.w(TAG, "openNoteForEditing: Note type is null for note ID: " + note.getNote_id());
            Toast.makeText(this, "Cannot open: Note type is undefined.", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("text".equals(noteType)) {
            Intent intent = new Intent(FolderNotesActivity.this, textnoteedit.class);
            intent.putExtra("key", note.getNote_id());
            intent.putExtra("key1", note.getNote_title());
            intent.putExtra("key2", note.getNote_content());
            intent.putExtra("key3", note.getIsPinned());
            intent.putExtra("key4", note.getIsLocked());
            intent.putExtra("key5", note.getHashedPin());
            intent.putExtra("key6", note.getFolder_id());
            intent.putExtra("key7", note.getIsDeleted());
            intent.putExtra("key8", note.getDeleted_date());
            startActivity(intent);
        } else if ("drawing".equals(noteType)) {
            Intent intent = new Intent(FolderNotesActivity.this, drawingpageedit.class);
            intent.putExtra("note_id", note.getNote_id());
            intent.putExtra("note_title", note.getNote_title());
            intent.putExtra("base64Image", note.getImageUrl());
            intent.putExtra("isPinned", note.getIsPinned());
            intent.putExtra("isLocked", note.getIsLocked());
            intent.putExtra("hashedPin", note.getHashedPin());
            intent.putExtra("folder_id", note.getFolder_id());
            intent.putExtra("isDeleted", note.getIsDeleted());
            intent.putExtra("deleted_date", note.getDeleted_date());
            startActivity(intent);
        } else if ("list".equals(noteType)) {
            Intent intent = new Intent(FolderNotesActivity.this, todolistpage.class);
            intent.putExtra("note_id", note.getNote_id());
            intent.putExtra("note_title", note.getNote_title());
            intent.putExtra("isPinned", note.getIsPinned());
            intent.putExtra("isLocked", note.getIsLocked());
            intent.putExtra("hashedPin", note.getHashedPin());
            intent.putExtra("folder_id", note.getFolder_id());
            intent.putExtra("isDeleted", note.getIsDeleted());
            intent.putExtra("deleted_date", note.getDeleted_date());
            startActivity(intent);
        }

        else {
            Toast.makeText(this, "Cannot open unsupported note type: " + noteType, Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Attempted to open unsupported note type: " + noteType + " for note ID: " + note.getNote_id());
        }
    }

    private CollectionReference getNoteCollectionRef(String noteType) {
        if (uid == null) {
            Log.e(TAG, "UID is null in getNoteCollectionRef.");
            return null;
        }
        if ("text".equals(noteType)) {
            return userRef.collection("notes");
        } else if ("drawing".equals(noteType) || "audio".equals(noteType) || "image".equals(noteType) || "list".equals(noteType)) {
            return userRef.collection("miscellaneous_notes");
        }
        Log.e(TAG, "Unknown note type encountered in getNoteCollectionRef: " + noteType + ". Defaulting to 'notes'.");
        return userRef.collection("notes");
    }
}
