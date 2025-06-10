// com.example.myapplication/FolderNotesActivity.java
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
import androidx.appcompat.widget.Toolbar; // Import Toolbar
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FolderNotesActivity extends AppCompatActivity
        implements rv_onClick, NoteActionsDialogFragment.NoteActionListener { // Implement these interfaces

    private static final String TAG = "FolderNotesActivity";
    private RecyclerView notesRecyclerView;
    private myadapter noteAdapter; // Reuse your existing note adapter
    private ArrayList<note> notesModels;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private String uid;
    private DocumentReference userRef;
    private ListenerRegistration noteListenerRegistration;
    private ProgressDialog progressDialog;

    private String folderId; // To store the ID of the current folder
    private String folderName; // To store the name of the current folder

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_notes);

        // Get folder ID and name from the Intent
        Intent intent = getIntent();
        if (intent != null) {
            folderId = intent.getStringExtra("folder_id");
            folderName = intent.getStringExtra("folder_name");
        }

        if (folderId == null || folderName == null) {
            Toast.makeText(this, "Folder not specified.", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if no folder info
            return;
        }

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar_folder_notes);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(folderName); // Set toolbar title to folder name
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Show back button
        }

        // Firebase Initialization
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

        // Progress Dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Loading notes...");
        progressDialog.show();

        // RecyclerView Setup
        notesRecyclerView = findViewById(R.id.folder_notes_recycler_view);
        notesRecyclerView.setHasFixedSize(true);
        notesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notesModels = new ArrayList<>();
        noteAdapter = new myadapter(FolderNotesActivity.this, notesModels, this); // 'this' for rv_onClick
        notesRecyclerView.setAdapter(noteAdapter);
        notesRecyclerView.setItemAnimator(null);

        listenForFolderNotes(); // Start listening for notes in this folder
    }

    // Handle back button on toolbar
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // Go back to previous activity (FoldersActivity)
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void listenForFolderNotes() {
        if (uid == null || folderId == null) {
            return; // Should not happen if initial checks are done
        }

        noteListenerRegistration = userRef.collection("notes")
                .whereEqualTo("folder_id", folderId) // Crucial: Filter by folderId
                .orderBy("isPinned", Query.Direction.DESCENDING) // Still order them if desired
                .orderBy("note_date", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (progressDialog.isShowing())
                            progressDialog.dismiss();

                        if (error != null) {
                            Toast.makeText(FolderNotesActivity.this, "Error loading notes: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e("FolderNotesActivity", "Firestore error", error);
                            return;
                        }

                        if (value == null) {
                            return;
                        }

                        for (DocumentChange dc : value.getDocumentChanges()) {
                            note changedNote = dc.getDocument().toObject(note.class);
                            changedNote.setNote_id(dc.getDocument().getId()); // Ensure ID is set

                            int oldIndex = dc.getOldIndex();
                            int newIndex = dc.getNewIndex();

                            switch (dc.getType()) {
                                case ADDED:
                                    notesModels.add(newIndex, changedNote);
                                    noteAdapter.notifyItemInserted(newIndex);
                                    break;
                                case MODIFIED:
                                    if (oldIndex == newIndex) {
                                        notesModels.set(oldIndex, changedNote);
                                        noteAdapter.notifyItemChanged(oldIndex);
                                    } else {
                                        notesModels.remove(oldIndex);
                                        notesModels.add(newIndex, changedNote);
                                        noteAdapter.notifyItemMoved(oldIndex, newIndex);
                                        noteAdapter.notifyItemChanged(newIndex);
                                    }
                                    break;
                                case REMOVED:
                                    notesModels.remove(oldIndex);
                                    noteAdapter.notifyItemRemoved(oldIndex);
                                    break;
                            }
                        }
                        // It's generally better to use specific notify methods, but for initial setup,
                        // if you face issues, notifyDataSetChanged() can be a fallback.
                        // noteAdapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (noteListenerRegistration != null) {
            noteListenerRegistration.remove(); // Detach listener
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    // --- rv_onClick Implementations (reusing mainpage logic) ---
    @Override
    public void onItemClicked(note note) {
        if (note.getIsLocked()) { // Use isLocked() getter
            showVerifyPinDialog(note.getNote_id(), notesModels.indexOf(note), true); // true = opening note
        } else {
            openNoteForEditing(note);
        }
    }

    @Override
    public void onPinClick(int position, boolean currentPinnedStatus) {
        if (position != RecyclerView.NO_POSITION) {
            note noteToUpdate = notesModels.get(position);
            String documentId = noteToUpdate.getNote_id();

            if (documentId != null && !documentId.isEmpty()) {
                boolean newPinnedStatus = !currentPinnedStatus;
                Map<String, Object> updates = new HashMap<>();
                updates.put("isPinned", newPinnedStatus);

                userRef.collection("notes").document(documentId).update(updates)
                        .addOnSuccessListener(aVoid -> {}) // No toast needed, UI updates via listener
                        .addOnFailureListener(e -> {
                            Toast.makeText(FolderNotesActivity.this, "Error toggling pin status: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e("FolderNotesActivity", "Error toggling pin status", e);
                        });
            } else {
                Toast.makeText(FolderNotesActivity.this, "Error: Note ID missing for pin toggle.", Toast.LENGTH_SHORT).show();
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
            // This can happen if your Firestore conversion (toObject) failed or returned null
            return;
        }

        Log.d(TAG, "onNoteLongClick: Selected Note ID: " + selectedNote.getNote_id() +
                ", Locked: " + selectedNote.getIsLocked() +
                ", Pinned: " + selectedNote.getIsPinned());

        NoteActionsDialogFragment fragment = NoteActionsDialogFragment.newInstance(
                selectedNote.getIsLocked(),
                selectedNote.getIsPinned(),
                selectedNote.getNote_id(),
                position,
                selectedNote.getFolder_id()
        );
        fragment.show(getSupportFragmentManager(), "NoteActionsDialogFragment");
    }

    @Override
    public void onDeleteClick(int position) {
        if (position != RecyclerView.NO_POSITION) {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Note")
                    .setMessage("Are you sure you want to delete this note permanently?")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            note noteToDelete = notesModels.get(position);
                            String documentId = noteToDelete.getNote_id();

                            if (documentId != null && !documentId.isEmpty()) {
                                userRef.collection("notes").document(documentId)
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(FolderNotesActivity.this, "Note deleted successfully!", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(FolderNotesActivity.this, "Error deleting note: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            Log.e("FolderNotesActivity", "Error deleting note", e);
                                        });
                            } else {
                                Toast.makeText(FolderNotesActivity.this, "Note ID is missing, cannot delete.", Toast.LENGTH_SHORT).show();
                                Log.e("FolderNotesActivity", "Note ID was null or empty for deletion at position: " + position);
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    // --- NoteActionsDialogFragment.NoteActionListener Implementations (reusing mainpage logic) ---
    @Override
    public void onLockNote(String noteId, int position) {
        showSetPinDialog(noteId, position);
    }

    @Override
    public void onUnlockNote(String noteId, int position) {
        showVerifyPinDialog(noteId, position, false); // false = unlocking operation
    }

    @Override
    public void onDeleteNoteFromFragment(String noteId, int position) {
        onDeleteClick(position); // Reuse existing delete logic
    }

    @Override
    public void onAddToFolder(String noteId, int position) {

    }

    @Override
    public void onRemoveFromFolder(String noteId, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Remove from Folder")
                .setMessage("Are you sure you want to remove this note from '" + folderName + "' and show it on the main page?")
                .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("folder_id", null); // <--- This is the key: set folder_id to null

                        // Assuming userRef is correctly initialized in FolderNotesActivity
                        db.collection("users").document(uid).collection("notes").document(noteId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(FolderNotesActivity.this, "Note removed from folder and moved to main page!", Toast.LENGTH_SHORT).show();
                                    // The SnapshotListener in FolderNotesActivity (which filters by currentFolderId)
                                    // will automatically detect this change and remove the note from its RecyclerView.
                                    // The SnapshotListener in mainpage will detect it and add it to its RecyclerView.
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(FolderNotesActivity.this, "Error removing note from folder: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    Log.e("FolderNotesActivity", "Error removing note from folder", e);
                                });
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }


    // --- Helper Methods for PINs and Notes (Copy these directly from mainpage.java) ---
    // Ensure you copy hashPin, showSetPinDialog, showVerifyPinDialog, updateNoteLockStatus, openNoteForEditing
    // You might want to make these static utility methods in a separate class later to avoid duplication.

    /**
     * Hashes the given PIN using SHA-256.
     * @param pin The plain text PIN.
     * @return The SHA-256 hash as a hexadecimal string, or null if hashing fails.
     */
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
            Log.e("Hashing", "SHA-256 algorithm not found", e);
            Toast.makeText(this, "Hashing error, cannot secure note.", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    /**
     * Shows a dialog to the user to set a new PIN for a note.
     * @param noteId The ID of the note to lock.
     * @param position The position of the note in the RecyclerView.
     */
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
                // Optionally re-show dialog, or guide user better
                return;
            }
            String hashedPin = hashPin(pin);
            if (hashedPin != null) {
                updateNoteLockStatus(noteId, position, true, hashedPin); // Lock with the new hashed PIN
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Shows a dialog to the user to verify a PIN for a note.
     * @param noteId The ID of the note to verify PIN for.
     * @param position The position of the note in the RecyclerView.
     * @param openingNote True if the purpose is to open the note, false if to unlock it.
     */
    private void showVerifyPinDialog(String noteId, int position, boolean openingNote) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter PIN to " + (openingNote ? "Open" : "Unlock") + " Note");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint("Enter PIN");
        builder.setView(input);

        builder.setPositiveButton("Verify", (dialog, which) -> {
            String enteredPin = input.getText().toString().trim();
            String hashedEnteredPin = hashPin(enteredPin);

            note noteToVerify = notesModels.get(position); // Get the note object from the current list
            if (hashedEnteredPin != null && hashedEnteredPin.equals(noteToVerify.getHashedPin())) {
                Toast.makeText(this, "PIN correct!", Toast.LENGTH_SHORT).show();
                if (openingNote) {
                    openNoteForEditing(noteToVerify);
                } else {
                    updateNoteLockStatus(noteId, position, false, null); // Unlock and remove hashed PIN
                }
            } else {
                Toast.makeText(this, "Incorrect PIN.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    /**
     * Updates the lock status and hashed PIN of a note in Firestore.
     * @param noteId The ID of the note document.
     * @param position The position of the note in the RecyclerView (for local notesModels access).
     * @param lockedStatus The new locked status (true to lock, false to unlock).
     * @param hashedPin The hashed PIN (null if unlocking).
     */
    private void updateNoteLockStatus(String noteId, int position, boolean lockedStatus, String hashedPin) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isLocked", lockedStatus);
        updates.put("hashedPin", hashedPin);

        userRef.collection("notes").document(noteId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(FolderNotesActivity.this, "Note " + (lockedStatus ? "locked" : "unlocked") + "!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(FolderNotesActivity.this, "Failed to update lock status: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("FolderNotesActivity", "Error updating note lock status", e);
                });
    }

    /**
     * Launches textnoteedit activity to open a note for editing.
     * @param note The note object to open.
     */
    private void openNoteForEditing(note note) {
        Intent intent = new Intent(FolderNotesActivity.this, textnoteedit.class);
        intent.putExtra("key", note.getNote_id());
        intent.putExtra("key1", note.getNote_title());
        intent.putExtra("key2", note.getNote_content());
        intent.putExtra("key3", note.getIsPinned());
        intent.putExtra("key4", note.getIsLocked());
        intent.putExtra("key5", note.getHashedPin());
        intent.putExtra("key6", note.getFolder_id());

        startActivity(intent);
    }
}