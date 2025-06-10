package com.example.myapplication;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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
import java.util.List; // For List<Folder>
import java.util.Map;


public class mainpage extends AppCompatActivity
        implements rv_onClick, NoteActionsDialogFragment.NoteActionListener { // Implements both

    TextView username;
    ImageView circleplus, signout;
    RelativeLayout folders;
    LinearLayout audio, image, drawing, list, text;

    ArrayList<Folder> folderArrayList; // To load available folders
    ListenerRegistration folderListenerRegistration; // For folders
    String selectedNoteIdForFolder = null; // To store the note ID temporarily
    int num = 0;
    FirebaseFirestore db;
    FirebaseUser user;
    FirebaseAuth auth;
    RecyclerView recyclerView;
    myadapter myadapter; // Your note adapter
    ArrayList<note> notesModels; // Your note model list
    ProgressDialog progressDialog;
    DocumentReference userRef;
    String uid;

    private ListenerRegistration noteListenerRegistration; // Declare ListenerRegistration

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainpage);

        progressDialog = new ProgressDialog(this);
        recyclerView = findViewById(R.id.notelist);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        username = findViewById(R.id.tv1);
        circleplus = findViewById(R.id.circleplus);
        signout = findViewById(R.id.signout);
        audio = findViewById(R.id.audio);
        image = findViewById(R.id.image);
        drawing = findViewById(R.id.drawing);
        folders = findViewById(R.id.folders);
        list = findViewById(R.id.list);
        text = findViewById(R.id.text);

        if(user != null) {
            uid = user.getUid();
            userRef = db.collection("users").document(uid); // Initialize userRef here
        } else {
            // Handle not logged in, redirect to login or show error
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            finish(); // Close this activity
            startActivity(new Intent(this, loginpage.class)); // Redirect to login
            return; // Exit onCreate if user is null
        }


        progressDialog.setCancelable(false);
        progressDialog.setMessage("Processing data...");
        progressDialog.show();

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        notesModels = new ArrayList<note>();
        myadapter = new myadapter(mainpage.this, notesModels, this); // Pass 'this' for rv_onClick
        recyclerView.setAdapter(myadapter);
        recyclerView.setItemAnimator(null);

        // Initialize folder list for "Add to Folder" functionality
        folderArrayList = new ArrayList<>();


        noteChangeListener();
        listenForFoldersForAddNote(); // <--- NEW: Start listening for folders here


        // The user check below is redundant if handled at the beginning of onCreate
        // if (user == null) {
        //     Intent intent = new Intent(getApplicationContext(), loginpage.class);
        //     startActivity(intent);
        //     finish();
        // }
        // else {
        //     username.setText(user.getEmail());
        // }
        username.setText(user.getEmail()); // Set username directly since user is checked above

        folders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mainpage.this, folderpage.class);
                startActivity(intent);
            }
        });

        signout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (noteListenerRegistration != null) {
                    noteListenerRegistration.remove();
                }
                if (folderListenerRegistration != null) { // Detach folder listener on signout
                    folderListenerRegistration.remove();
                }
                auth.signOut();
                Intent intent = new Intent(getApplicationContext(), loginpage.class);
                startActivity(intent);
                finish();
            }
        });

        text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), textnotes.class);
                startActivity(intent);
                // Removed finish() here, as it might be disruptive if user navigates back
                // Consider if you really want to finish mainpage here or just move to textnotes
            }
        });

        Animation cp_animation = AnimationUtils.loadAnimation(this, R.anim.circleplus);
        Animation cp_animation2 = AnimationUtils.loadAnimation(this, R.anim.circleplus2);
        Animation a_audio = AnimationUtils.loadAnimation(this, R.anim.aidlt_alpha);
        Animation a_audio2 = AnimationUtils.loadAnimation(this, R.anim.aidlt);
        Animation a_image = AnimationUtils.loadAnimation(this, R.anim.aidlt_alpha);
        Animation a_image2 = AnimationUtils.loadAnimation(this, R.anim.aidlt);
        Animation a_drawing = AnimationUtils.loadAnimation(this, R.anim.aidlt_alpha);
        Animation a_drawing2 = AnimationUtils.loadAnimation(this, R.anim.aidlt);
        Animation a_list = AnimationUtils.loadAnimation(this, R.anim.aidlt_alpha);
        Animation a_list2 = AnimationUtils.loadAnimation(this, R.anim.aidlt); // Typo fixed
        Animation a_text = AnimationUtils.loadAnimation(this, R.anim.aidlt_alpha);
        Animation a_text2 = AnimationUtils.loadAnimation(this, R.anim.aidlt);


        circleplus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (num == 0){
                    circleplus.startAnimation(cp_animation);

                    Runnable runnable =  () -> text.startAnimation(a_text);
                    new Handler(Looper.getMainLooper()).postDelayed(runnable, 25);

                    Runnable runnable1 =  () -> list.startAnimation(a_list);
                    new Handler(Looper.getMainLooper()).postDelayed(runnable1, 50);

                    Runnable runnable2 =  () -> drawing.startAnimation(a_drawing);
                    new Handler(Looper.getMainLooper()).postDelayed(runnable2, 75);

                    Runnable runnable3 =  () -> image.startAnimation(a_image);
                    new Handler(Looper.getMainLooper()).postDelayed(runnable3, 100);

                    Runnable runnable4 =  () -> audio.startAnimation(a_audio);
                    new Handler(Looper.getMainLooper()).postDelayed(runnable4, 125);

                    num = 1;
                }

                else if(num == 1){
                    circleplus.startAnimation(cp_animation2);
                    num = 0;

                    Runnable runnable =  () -> text.startAnimation(a_text2);
                    new Handler(Looper.getMainLooper()).postDelayed(runnable, 125);

                    Runnable runnable1 =  () -> list.startAnimation(a_list2);
                    new Handler(Looper.getMainLooper()).postDelayed(runnable1, 100);

                    Runnable runnable2 =  () -> drawing.startAnimation(a_drawing2);
                    new Handler(Looper.getMainLooper()).postDelayed(runnable2, 75);

                    Runnable runnable3 =  () -> image.startAnimation(a_image2);
                    new Handler(Looper.getMainLooper()).postDelayed(runnable3, 50);

                    Runnable runnable4 =  () -> audio.startAnimation(a_audio2);
                    new Handler(Looper.getMainLooper()).postDelayed(runnable4, 25);
                }
            }
        });
    }

    private void noteChangeListener() {
        noteListenerRegistration = userRef.collection("notes")
                .whereEqualTo("folder_id", null) // Filter for notes not in a folder
                .orderBy("isPinned", Query.Direction.DESCENDING)
                .orderBy("note_date", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {

                        if (progressDialog.isShowing())
                            progressDialog.dismiss();

                        if (error != null) {
                            Toast.makeText(mainpage.this, "Fail: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e("mainpage", "Firestore error for notes", error); // Specific tag
                            return;
                        }

                        if (value == null) {
                            return;
                        }

                        // Clear and re-add or handle changes incrementally
                        // For simplicity, clearing and re-adding for now.
                        // For a more robust solution, you can process DocumentChange types.
                        notesModels.clear();
                        for (DocumentChange dc : value.getDocumentChanges()) {
                            note changedNote = dc.getDocument().toObject(note.class);
                            if (changedNote != null) {
                                changedNote.setNote_id(dc.getDocument().getId()); // Set the document ID
                                notesModels.add(changedNote);
                            }
                        }
                        // Sort again if needed after clear/add, as Firestore might not guarantee order across changes for non-order fields.
                        // Or, use the granular updates as you had, ensuring IDs are set in `toObject` and `setNote_id`.
                        notesModels.clear(); // Clear before processing document changes to avoid duplicates
                        for (DocumentChange dc : value.getDocumentChanges()){
                            note changedNote = dc.getDocument().toObject(note.class);
                            changedNote.setNote_id(dc.getDocument().getId()); // Ensure note_id is set

                            int oldIndex = dc.getOldIndex();
                            int newIndex = dc.getNewIndex();

                            switch (dc.getType()) {
                                case ADDED:
                                    if (!notesModels.contains(changedNote)) { // Prevent adding duplicates on initial load
                                        notesModels.add(newIndex, changedNote);
                                    }
                                    break;
                                case MODIFIED:
                                    if (oldIndex != -1 && oldIndex < notesModels.size()) {
                                        notesModels.set(oldIndex, changedNote);
                                    } else {
                                        // If modified document is new to this list (e.g. was in folder, now not)
                                        // find by ID or re-add
                                        boolean found = false;
                                        for (int i = 0; i < notesModels.size(); i++) {
                                            if (notesModels.get(i).getNote_id().equals(changedNote.getNote_id())) {
                                                notesModels.set(i, changedNote);
                                                found = true;
                                                break;
                                            }
                                        }
                                        if (!found) {
                                            notesModels.add(newIndex, changedNote);
                                        }
                                    }
                                    break;
                                case REMOVED:
                                    if (oldIndex != -1 && oldIndex < notesModels.size() && notesModels.get(oldIndex).getNote_id().equals(changedNote.getNote_id())) {
                                        notesModels.remove(oldIndex);
                                    } else {
                                        // If index is unreliable, remove by ID
                                        for (int i = 0; i < notesModels.size(); i++) {
                                            if (notesModels.get(i).getNote_id().equals(changedNote.getNote_id())) {
                                                notesModels.remove(i);
                                                break;
                                            }
                                        }
                                    }
                                    break;
                            }
                        }
                        myadapter.notifyDataSetChanged(); // Call notifyDataSetChanged after all updates
                    }
                });
    }

    // <--- NEW: Listener for Folders for the "Add to Folder" feature --->
    private void listenForFoldersForAddNote() {
        if (uid == null) return;

        folderListenerRegistration = db.collection("users").document(uid)
                .collection("folders")
                .orderBy("folder_name", Query.Direction.ASCENDING) // Order by name for display
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e("mainpage", "Folders listen failed.", error);
                            return;
                        }
                        if (value == null) return;

                        folderArrayList.clear();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            Folder folder = doc.toObject(Folder.class);
                            if (folder != null) {
                                folder.setFolder_id(doc.getId()); // Set the Firestore document ID
                                folderArrayList.add(folder);
                            }
                        }
                        // No need to notify adapter here as this list is internal for the dialog
                    }
                });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (noteListenerRegistration != null) {
            noteListenerRegistration.remove();
        }
        if (folderListenerRegistration != null) { // Detach folder listener too
            folderListenerRegistration.remove();
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onItemClicked(note note) {
        Intent intent = new Intent(getApplicationContext(), textnoteedit.class);
        if (note.getIsLocked()) {
            showVerifyPinDialog(note.getNote_id(), notesModels.indexOf(note), true); // true = opening note
        } else {
            openNoteForEditing(note);
        }
    }

    @Override
    public void onPinClick(int position, boolean currentPinnedStatus) { // currentPinnedStatus is the state *before* the click
        if (position != RecyclerView.NO_POSITION) {
            note noteToUpdate = notesModels.get(position);
            String documentId = noteToUpdate.getNote_id();

            if (documentId != null && !documentId.isEmpty()) {
                boolean newPinnedStatus = !currentPinnedStatus;

                Map<String, Object> updates = new HashMap<>();
                updates.put("isPinned", newPinnedStatus);

                userRef.collection("notes").document(documentId).update(updates)
                        .addOnSuccessListener(aVoid -> {
                            // No need for Toast, noteChangeListener will update UI
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(mainpage.this, "Error toggling pin status: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            } else {
                Toast.makeText(mainpage.this, "Error: Note ID missing for pin toggle.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // --- rv_onClick (Long Click) Implementation ---
    @Override
    public void onNoteLongClick(int position) {
        if (position != RecyclerView.NO_POSITION) {
            note selectedNote = notesModels.get(position);
            // Pass the correct parameters to newInstance as defined in NoteActionsDialogFragment
            NoteActionsDialogFragment fragment = NoteActionsDialogFragment.newInstance(
                    selectedNote.getIsLocked(),
                    selectedNote.getIsPinned(),
                    selectedNote.getNote_id(),
                    position,
                    selectedNote.getFolder_id()
            );
            fragment.show(getSupportFragmentManager(), "NoteActionsDialogFragment");
        }
    }

    // --- NoteActionsDialogFragment.NoteActionListener (from Fragment) Implementations ---
    @Override
    public void onLockNote(String noteId, int position) {
        // This is called when user wants to lock the note
        showSetPinDialog(noteId, position);
    }

    @Override
    public void onUnlockNote(String noteId, int position) {
        // This is called when user wants to unlock the note
        showVerifyPinDialog(noteId, position, false); // false = unlocking operation
    }

    @Override
    public void onDeleteNoteFromFragment(String noteId, int position) {
        // Re-use your existing onDeleteClick logic
        onDeleteClick(position); // This will handle the confirmation dialog and deletion
    }

    @Override
    public void onAddToFolder(String noteId, int position) { // <--- NEW: Implementation for "Add to Folder"
        selectedNoteIdForFolder = noteId; // Store the note ID temporarily

        if (folderArrayList.isEmpty()) {
            Toast.makeText(this, "No folders available. Please create one first.", Toast.LENGTH_LONG).show();
            return;
        }

        // Prepare folder names for the AlertDialog
        String[] folderNames = new String[folderArrayList.size()];
        for (int i = 0; i < folderArrayList.size(); i++) {
            folderNames[i] = folderArrayList.get(i).getFolder_name();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Folder");
        builder.setItems(folderNames, (dialog, which) -> {
            Folder selectedFolder = folderArrayList.get(which);
            assignNoteToFolder(selectedNoteIdForFolder, selectedFolder.getFolder_id());
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    public void onRemoveFromFolder(String noteId, int position) {

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
            Log.e("Hashing", "SHA-256 algorithm not found", e);
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
                // Optionally re-show dialog, or just let user dismiss and try again
                // showSetPinDialog(noteId, position);
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

    private void updateNoteLockStatus(String noteId, int position, boolean lockedStatus, String hashedPin) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isLocked", lockedStatus);
        updates.put("hashedPin", hashedPin);

        userRef.collection("notes").document(noteId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(mainpage.this, "Note " + (lockedStatus ? "locked" : "unlocked") + "!", Toast.LENGTH_SHORT).show();
                    // noteChangeListener will automatically handle UI update
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(mainpage.this, "Failed to update lock status: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("Mainpage", "Error updating note lock status", e);
                });
    }

    private void openNoteForEditing(note note) {
        Intent intent = new Intent(mainpage.this, textnoteedit.class);
        intent.putExtra("key", note.getNote_id());
        intent.putExtra("key1", note.getNote_title());
        intent.putExtra("key2", note.getNote_content());
        intent.putExtra("key3", note.getIsPinned());
        intent.putExtra("key4", note.getIsLocked());
        intent.putExtra("key5", note.getHashedPin());
        intent.putExtra("key6", note.getFolder_id()); // Pass folder_id

        startActivity(intent);
    }

    @Override
    public void onDeleteClick(int position) { // This is triggered by rv_onClick's onDeleteClick (if you still use it directly)
        // or by onDeleteNoteFromFragment calling this.
        if (position != RecyclerView.NO_POSITION && position < notesModels.size()) { // Add bounds check
            new AlertDialog.Builder(this)
                    .setTitle("Delete Note")
                    .setMessage("Are you sure you want to delete this note permanently?")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            note noteToDelete = notesModels.get(position);
                            String documentId = noteToDelete.getNote_id();

                            if (documentId != null && !documentId.isEmpty()) {
                                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                if (currentUser != null) {
                                    String uid = currentUser.getUid();
                                    FirebaseFirestore.getInstance().collection("users")
                                            .document(uid)
                                            .collection("notes")
                                            .document(documentId)
                                            .delete()
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(mainpage.this, "Note deleted successfully!", Toast.LENGTH_SHORT).show();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(mainpage.this, "Error deleting note: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                Log.e("Mainpage", "Error deleting note", e);
                                            });
                                } else {
                                    Toast.makeText(mainpage.this, "User not authenticated for deletion.", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(mainpage.this, "Note ID is missing, cannot delete.", Toast.LENGTH_SHORT).show();
                                Log.e("Mainpage", "Note ID was null or empty for deletion at position: " + position);
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        } else {
            Toast.makeText(this, "Error: Invalid note position for deletion.", Toast.LENGTH_SHORT).show();
            Log.e("mainpage", "Attempted to delete note at invalid position: " + position);
        }
    }

    // <--- NEW: Method to assign note to a folder --->
    private void assignNoteToFolder(String noteId, String folderId) {
        if (noteId == null || uid == null) {
            Toast.makeText(this, "Error: Note or user not identified.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Set folder_id on the note. If folderId is null, it means removing from folder.
        Map<String, Object> updates = new HashMap<>();
        updates.put("folder_id", folderId); // Use "folder_id" as per your existing listener query

        userRef.collection("notes").document(noteId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(mainpage.this, "Note added to folder!", Toast.LENGTH_SHORT).show();
                    // The noteChangeListener will automatically remove this note from the main list
                    // because its "folder_id" will no longer be null.
                })
                .addOnFailureListener(e -> Toast.makeText(mainpage.this, "Error adding note to folder: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

}