package com.example.myapplication;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import android.util.Log;

import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class binpage extends AppCompatActivity
        implements rv_onClick, NoteActionsDialogFragment.NoteActionListener {

    private static final String TAG = "BinPage";

    private RecyclerView binRecyclerView;
    private UnifiedBinNotesAdapter binAdapter;
    private ArrayList<note> binNotesModels;
    private ArrayList<note> originalBinNotesList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private String uid;
    private ListenerRegistration textNoteListenerRegistration;
    private ListenerRegistration miscNoteListenerRegistration;
    private ProgressDialog progressDialog;
    private TextView usernameTextView;
    private RelativeLayout notesFooterButton;
    private RelativeLayout foldersFooterButton;
    private ImageView signoutButton;

    private ArrayList<note> tempTextNotes;
    private ArrayList<note> tempMiscNotes;

    private int notesFetchCount = 0;
    private final int TOTAL_LISTENERS = 2;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_binpage);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        usernameTextView = findViewById(R.id.tv1);
        TextView userDescriptionTextView = findViewById(R.id.tv2);
        signoutButton = findViewById(R.id.signout);
        notesFooterButton = findViewById(R.id.note);
        foldersFooterButton = findViewById(R.id.folders);


        if (user == null) {
            Toast.makeText(this, "User not logged in. Redirecting to login.", Toast.LENGTH_SHORT).show();
            finish();
            startActivity(new Intent(this, loginpage.class));
            return;
        }
        uid = user.getUid();
        usernameTextView.setText(user.getEmail());
        userDescriptionTextView.setText("Your deleted notes");

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Loading deleted notes...");

        binRecyclerView = findViewById(R.id.notelist);
        binRecyclerView.setHasFixedSize(true);
        binRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        binNotesModels = new ArrayList<>();
        originalBinNotesList = new ArrayList<>();
        tempTextNotes = new ArrayList<>();
        tempMiscNotes = new ArrayList<>();

        binAdapter = new UnifiedBinNotesAdapter(this, binNotesModels, this, false);
        binRecyclerView.setAdapter(binAdapter);

        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }

        listenForBinNotes();

        notesFooterButton.setOnClickListener(v -> {
            Intent intent = new Intent(binpage.this, mainpage.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        foldersFooterButton.setOnClickListener(v -> {
            Intent intent = new Intent(binpage.this, folderpage.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        signoutButton.setOnClickListener(v -> {
            if (textNoteListenerRegistration != null) {
                textNoteListenerRegistration.remove();
            }
            if (miscNoteListenerRegistration != null) {
                miscNoteListenerRegistration.remove();
            }
            auth.signOut();
            Intent intent = new Intent(getApplicationContext(), loginpage.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void listenForBinNotes() {
        if (uid == null) {
            Log.e(TAG, "UID is null, cannot fetch bin notes.");
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            return;
        }

        notesFetchCount = 0;
        tempTextNotes.clear();
        tempMiscNotes.clear();
        originalBinNotesList.clear();

        textNoteListenerRegistration = db.collection("users").document(uid)
                .collection("notes")
                .whereEqualTo("isDeleted", true)
                .orderBy("deleted_date", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        handleNotesSnapshot(value, error, "text", tempTextNotes);
                    }
                });

        miscNoteListenerRegistration = db.collection("users").document(uid)
                .collection("miscellaneous_notes")
                .whereEqualTo("isDeleted", true)
                .orderBy("deleted_date", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        handleNotesSnapshot(value, error, "miscellaneous", tempMiscNotes);
                    }
                });
    }

    private synchronized void handleNotesSnapshot(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error, String type, ArrayList<note> targetList) {
        if (error != null) {
            Log.e(TAG, "Firestore error for " + type + " notes: " + error.getMessage(), error);
            Toast.makeText(this, "Failed to load " + type + " bin notes: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            notesFetchCount++;
            checkAllNotesFetched();
            return;
        }

        targetList.clear();

        if (value != null) {
            for (DocumentSnapshot doc : value.getDocuments()) {
                note noteItem = doc.toObject(note.class);
                if (noteItem != null && noteItem.getType() != null && doc.getId() != null) {
                    noteItem.setNote_id(doc.getId());
                    targetList.add(noteItem);
                    Log.d(TAG, "Fetched " + type + " note: ID=" + doc.getId() + ", Title='" + (noteItem.getNote_title() != null ? noteItem.getNote_title() : "N/A") + "', Type='" + (noteItem.getType() != null ? noteItem.getType() : "N/A") + "'");
                } else {
                    Log.w(TAG, "Skipping invalid " + type + " document: ID=" + doc.getId() + ". Note object null or missing type/ID. Data: " + doc.getData());
                }
            }
        } else {
            Log.d(TAG, "Received null QuerySnapshot for " + type + " bin notes. " + type + " temporary list remains empty.");
        }
        notesFetchCount++;
        checkAllNotesFetched();
    }

    private synchronized void checkAllNotesFetched() {
        if (notesFetchCount == TOTAL_LISTENERS) {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            Log.d(TAG, "All initial notes fetched. Merging data from temporary lists...");

            originalBinNotesList.clear();
            originalBinNotesList.addAll(tempTextNotes);
            originalBinNotesList.addAll(tempMiscNotes);

            Log.d(TAG, "Merged data. Total raw notes after merge: " + originalBinNotesList.size());

            filterBinNotes("");
        }
    }

    private void filterBinNotes(String query) {
        binNotesModels.clear();
        binNotesModels.addAll(originalBinNotesList);

        Collections.sort(binNotesModels, new Comparator<note>() {
            @Override
            public int compare(note n1, note n2) {
                if (n1 == null && n2 == null) return 0;
                if (n1 == null) return 1;
                if (n2 == null) return -1;

                if (n1.getDeleted_date() == null && n2.getDeleted_date() == null) return 0;
                if (n1.getDeleted_date() == null) return 1;
                if (n2.getDeleted_date() == null) return -1;
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());
                    Date date1 = (n1.getDeleted_date() != null && !n1.getDeleted_date().isEmpty()) ? sdf.parse(n1.getDeleted_date()) : new Date(0);
                    Date date2 = (n2.getDeleted_date() != null && !n2.getDeleted_date().isEmpty()) ? sdf.parse(n2.getDeleted_date()) : new Date(0);
                    return date2.compareTo(date1);
                } catch (java.text.ParseException e) {
                    Log.e(TAG, "Error parsing deleted_date for sorting: " + e.getMessage());
                    return 0;
                }
            }
        });

        binAdapter.notifyDataSetChanged();
        Log.d(TAG, "Filtered bin notes. Displaying: " + binNotesModels.size() + " notes.");
    }

    private CollectionReference getNoteCollectionRef(String noteType) {
        if (uid == null) {
            Log.e(TAG, "UID is null in getNoteCollectionRef.");
            return null;
        }
        if (noteType == null) {
            Log.e(TAG, "getNoteCollectionRef: noteType is null.");
            return null;
        }
        if ("text".equals(noteType) || "note".equals(noteType)) {
            return db.collection("users").document(uid).collection("notes");
        } else if (Arrays.asList("drawing", "audio", "image", "list").contains(noteType)) {
            return db.collection("users").document(uid).collection("miscellaneous_notes");
        }
        Log.e(TAG, "Unknown note type encountered in getNoteCollectionRef: " + noteType);
        return null;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textNoteListenerRegistration != null) {
            textNoteListenerRegistration.remove();
            Log.d(TAG, "Text note listener removed.");
        }
        if (miscNoteListenerRegistration != null) {
            miscNoteListenerRegistration.remove();
            Log.d(TAG, "Miscellaneous note listener removed.");
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onItemClicked(note note) {
        Toast.makeText(this, "Cannot edit notes directly from bin. Please Restore them first.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDeleteClick(int position) {
        if (position >= 0 && position < binNotesModels.size()) {
            note noteToDelete = binNotesModels.get(position);
            if (noteToDelete != null && noteToDelete.getNote_id() != null && noteToDelete.getType() != null) {
                onPermanentlyDeleteNote(noteToDelete.getNote_id(), position);
            } else {
                Log.e(TAG, "onDeleteClick: Note or its ID/Type is null at position " + position);
                Toast.makeText(this, "Error: Cannot delete invalid note.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "onDeleteClick: Invalid position " + position);
        }
    }

    @Override
    public void onPinClick(int position, boolean currentPinnedStatus) {
        Toast.makeText(this, "Cannot pin/unpin notes in bin. Restore to main notes first.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNoteLongClick(int position) {
        if (position != RecyclerView.NO_POSITION && position < binNotesModels.size()) {
            note selectedNote = binNotesModels.get(position);
            if (selectedNote == null) {
                Log.e(TAG, "onNoteLongClick: selectedNote is null at position " + position);
                Toast.makeText(this, "Error: Cannot perform action, note is invalid.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedNote.getNote_id() == null || selectedNote.getType() == null) {
                Log.e(TAG, "onNoteLongClick: Note ID ('" + selectedNote.getNote_id() + "') or Type ('" + selectedNote.getType() + "') is null for note at position " + position);
                Toast.makeText(this, "Error: Invalid note for long-press actions.", Toast.LENGTH_SHORT).show();
                return;
            }

            NoteActionsDialogFragment fragment = NoteActionsDialogFragment.newInstance(
                    selectedNote.getIsLocked(),
                    selectedNote.getIsPinned(),
                    true,
                    selectedNote.getNote_id(),
                    position,
                    selectedNote.getFolder_id(),
                    selectedNote.getType()
            );
            Bundle args = fragment.getArguments();
            if (args == null) {
                args = new Bundle();
            }
            args.putString("note_type", selectedNote.getType());
            fragment.setArguments(args);

            fragment.show(getSupportFragmentManager(), "NoteActionsDialogFragment");
        } else {
            Log.e(TAG, "onNoteLongClick: Invalid position " + position + ". List size: " + binNotesModels.size());
        }
    }

    @Override
    public void onRestoreNote(String noteId, int position) {
        if (position < 0 || position >= binNotesModels.size()) {
            Log.e(TAG, "onRestoreNote(position): Invalid position: " + position);
            Toast.makeText(this, "Error: Invalid note position for restoration.", Toast.LENGTH_SHORT).show();
            return;
        }

        note selectedNote = binNotesModels.get(position);
        String noteType = selectedNote.getType();

        if (noteId == null || noteId.isEmpty() || noteType == null || noteType.isEmpty()) {
            Log.e(TAG, "onRestoreNote: Invalid noteId (" + noteId + ") or noteType (" + noteType + ").");
            Toast.makeText(this, "Error: Invalid note for restoration.", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "Attempting to restore note. ID: " + noteId + ", Type: " + noteType + ", Position: " + position);

        CollectionReference targetCollection = getNoteCollectionRef(noteType);

        if (targetCollection == null) {
            Toast.makeText(this, "Error: Invalid note type for restoration.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "onRestoreNote: Cannot find target collection for note type: " + noteType);
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Restore Note")
                .setMessage("Are you sure you want to restore this note from the bin?")
                .setPositiveButton("Restore", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("isDeleted", false);
                        updates.put("deleted_date", null);
                        updates.put("folder_id", null);
                        updates.put("timestamp", new Date());

                        Log.d(TAG, "Executing Firestore update for restore. Document path: " + targetCollection.document(noteId).getPath());
                        targetCollection.document(noteId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(binpage.this, "Note restored!", Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "Note " + noteId + " (" + noteType + ") restored successfully.");
                                    removeNoteFromLocalLists(noteId);
                                    binAdapter.notifyDataSetChanged();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(binpage.this, "Error restoring note: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    Log.e(TAG, "Error restoring note " + noteId + " (" + noteType + "): ", e);
                                    Log.e(TAG, "Firestore restore update failed. Document path: " + targetCollection.document(noteId).getPath() + ". Error: " + e.getMessage(), e);
                                });
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    @Override
    public void onPermanentlyDeleteNote(String noteId, int position) {
        if (position < 0 || position >= binNotesModels.size()) {
            Log.e(TAG, "onPermanentlyDeleteNote(position): Invalid position: " + position);
            Toast.makeText(this, "Error: Invalid note position for permanent deletion.", Toast.LENGTH_SHORT).show();
            return;
        }

        note selectedNote = binNotesModels.get(position);
        String noteType = selectedNote.getType();

        if (noteId == null || noteId.isEmpty() || noteType == null || noteType.isEmpty()) {
            Log.e(TAG, "onPermanentlyDeleteNote: Invalid noteId (" + noteId + ") or noteType (" + noteType + ").");
            Toast.makeText(this, "Error: Invalid note for permanent deletion.", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "Attempting to permanently delete note. ID: " + noteId + ", Type: " + noteType + ", Position: " + position);

        CollectionReference targetCollection = getNoteCollectionRef(noteType);

        if (targetCollection == null) {
            Toast.makeText(this, "Error: Invalid note type for permanent deletion.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "onPermanentlyDeleteNote: Cannot find target collection for note type: " + noteType);
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Permanently Delete Note")
                .setMessage("This action cannot be undone. Are you sure you want to permanently delete this note?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "Executing Firestore delete. Document path: " + targetCollection.document(noteId).getPath());
                        targetCollection.document(noteId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(binpage.this, "Note permanently deleted!", Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "Note " + noteId + " (" + noteType + ") permanently deleted successfully.");
                                    removeNoteFromLocalLists(noteId);
                                    binAdapter.notifyDataSetChanged();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(binpage.this, "Error permanently deleting note: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    Log.e(TAG, "Error permanently deleting note " + noteId + " (" + noteType + "): ", e);
                                    Log.e(TAG, "Firestore permanent delete failed. Document path: " + targetCollection.document(noteId).getPath() + ". Error: " + e.getMessage(), e);
                                });
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void removeNoteFromLocalLists(String noteId) {
        for (int i = 0; i < binNotesModels.size(); i++) {
            if (binNotesModels.get(i).getNote_id().equals(noteId)) {
                binNotesModels.remove(i);
                break;
            }
        }
        for (int i = 0; i < originalBinNotesList.size(); i++) {
            if (originalBinNotesList.get(i).getNote_id().equals(noteId)) {
                originalBinNotesList.remove(i);
                break;
            }
        }
    }


    @Override
    public void onLockNote(String noteId, int position) {
        Toast.makeText(this, "Cannot lock notes in bin. Restore to main notes first.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUnlockNote(String noteId, int position) {
        Toast.makeText(this, "Cannot unlock notes in bin. Restore to main notes first.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMoveToBin(String noteId, int position) {
        Toast.makeText(this, "Note is already in bin.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAddToFolder(String noteId, int position) {

    }

    @Override
    public void onAddToFolder(String noteId, String noteType, int position) {
        Toast.makeText(this, "Cannot add notes in bin to folder. Restore to main notes first.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRemoveFromFolder(String noteId, int position) {
        Toast.makeText(this, "Note is in bin, not in a folder.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRestoreNote(String noteId, String noteType) {

    }

    @Override
    public void onPermanentlyDeleteNote(String noteId, String noteType) {

    }
}