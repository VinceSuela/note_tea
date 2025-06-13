package com.example.myapplication;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;

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

public class secondarypage extends AppCompatActivity
        implements rv_onClick, NoteActionsDialogFragment.NoteActionListener {

    private static final String TAG = "SecondaryPage";

    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private RecyclerView notesRecyclerView;
    private MiscellaneousNotesAdapter noteAdapter;
    private ArrayList<note> noteList;
    private ArrayList<note> filteredNoteList;
    private ArrayList<Folder> folderArrayList;
    private ListenerRegistration folderListenerRegistration;
    private ListenerRegistration miscNotesListenerRegistration;

    private boolean isGridLayout = false;

    private TextView userNameTextView, userNotesTextView;
    private ImageView signoutButton;
    private ImageView circlePlusButton;
    private ImageView gridListIcon;
    private ImageView switchpage;
    private EditText searchEditText;
    private ImageView searchButton;

    private LinearLayout audioLayout, imageLayout, drawingLayout, listLayout, textLayout;
    private int num = 0;

    private String uid;
    private DocumentReference userRef;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secondarypage);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Loading notes...");

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        userNameTextView = findViewById(R.id.tv1);
        userNotesTextView = findViewById(R.id.tv2);
        signoutButton = findViewById(R.id.signout);
        notesRecyclerView = findViewById(R.id.notelist);

        audioLayout = findViewById(R.id.audio);
        imageLayout = findViewById(R.id.image);
        drawingLayout = findViewById(R.id.drawing);
        listLayout = findViewById(R.id.list);
        textLayout = findViewById(R.id.text);
        circlePlusButton = findViewById(R.id.circleplus);
        searchEditText = findViewById(R.id.search);
        searchButton = findViewById(R.id.searchbutton);
        switchpage = findViewById(R.id.switchpage);


        audioLayout.setVisibility(View.INVISIBLE);
        imageLayout.setVisibility(View.INVISIBLE);
        drawingLayout.setVisibility(View.INVISIBLE);
        listLayout.setVisibility(View.INVISIBLE);
        textLayout.setVisibility(View.INVISIBLE);


        gridListIcon = findViewById(R.id.grid);
        updateGridListIcon();

        noteList = new ArrayList<>();
        filteredNoteList = new ArrayList<>();
        noteAdapter = new MiscellaneousNotesAdapter(this, filteredNoteList, this, isGridLayout);
        notesRecyclerView.setAdapter(noteAdapter);

        if (isGridLayout) {
            notesRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        } else {
            notesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        }

        signoutButton.setOnClickListener(v -> signOut());


        drawingLayout.setOnClickListener(v -> {
            Intent intent = new Intent(secondarypage.this, drawingpage.class);
            startActivity(intent);
            toggleAddNoteMenu();
        });

//        audioLayout.setOnClickListener(v -> {
//            Intent intent = new Intent(secondarypage.this, audiopage.class);
//            startActivity(intent);
//            toggleAddNoteMenu();
//        });
//        imageLayout.setOnClickListener(v -> {
//            Intent intent = new Intent(secondarypage.this, imagepage.class);
//            startActivity(intent);
//            toggleAddNoteMenu();
//        });
        listLayout.setOnClickListener(v -> {
            Intent intent = new Intent(secondarypage.this, todolistpage.class);
            startActivity(intent);
            toggleAddNoteMenu();
        });
        textLayout.setOnClickListener(v -> {
            Intent intent = new Intent(secondarypage.this, textnotes.class);
            startActivity(intent);
            finish();
        });


        findViewById(R.id.GridList).setOnClickListener(v -> toggleLayoutMode());

        findViewById(R.id.trash).setOnClickListener(v -> {
            Intent binIntent = new Intent(secondarypage.this, binpage.class);
            startActivity(binIntent);
        });


        findViewById(R.id.folders).setOnClickListener(v -> {
            Intent foldersIntent = new Intent(secondarypage.this, folderpage.class);
            startActivity(foldersIntent);
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterNotes(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchButton.setOnClickListener(v -> {
            filterNotes(searchEditText.getText().toString());
             InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
             imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
        });

        switchpage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), mainpage.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });


        loadUserDetails();
        fetchNotesFromFirestore();
        listenForFolders();

        circlePlusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleAddNoteMenu();
            }
        });
        user = mAuth.getCurrentUser();
        userNameTextView.setText(user.getEmail());
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchNotesFromFirestore();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (folderListenerRegistration != null) {
            folderListenerRegistration.remove();
            Log.d(TAG, "Folder listener removed.");
        }
        if (miscNotesListenerRegistration != null) {
            miscNotesListenerRegistration.remove();
            Log.d(TAG, "Miscellaneous notes listener removed.");
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void loadUserDetails() {
        user = mAuth.getCurrentUser();
        if (user != null) {
            uid = user.getUid();
            userRef = db.collection("users").document(uid);
            userNameTextView.setText(user.getDisplayName() != null ? user.getDisplayName() : user.getEmail());
            userNotesTextView.setText("Your miscellaneous notes");
        } else {
            userNameTextView.setText("Guest");
            userNotesTextView.setText("Login to save your notes");
            Toast.makeText(this, "User not logged in. Redirecting to login.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, loginpage.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void fetchNotesFromFirestore() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "User not authenticated. Cannot fetch notes.");
            return;
        }

        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }

        Log.d(TAG, "Initiating fetch from 'miscellaneous_notes' for user: " + currentUser.getUid());


        miscNotesListenerRegistration = db.collection("users").document(currentUser.getUid())
                .collection("miscellaneous_notes")
                .whereEqualTo("isDeleted", false)
                .whereEqualTo("folder_id", null)
                .whereIn("type", Arrays.asList("drawing", "audio", "image", "list"))

                .addSnapshotListener((snapshots, e) -> {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }

                    if (e != null) {
                        Log.e(TAG, "Firestore Listen failed in fetchNotesFromFirestore: " + e.getMessage(), e);
                        Toast.makeText(secondarypage.this, "Error loading notes: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (snapshots != null) {
                        noteList.clear();
                        Log.d(TAG, "Received " + snapshots.size() + " documents from Firestore.");
                        for (QueryDocumentSnapshot doc : snapshots) {
                            try {
                                note note = doc.toObject(note.class);
                                if (note != null) {
                                    note.setNote_id(doc.getId());

                                    Log.d(TAG, "Fetched Document: ID=" + doc.getId() +
                                            ", Title='" + (note.getNote_title() != null ? note.getNote_title() : "No Title") +
                                            "', Type='" + (note.getType() != null ? note.getType() : "Unknown Type") +
                                            ", Pinned=" + note.getIsPinned() +
                                            ", Locked=" + note.getIsLocked() +
                                            ", Deleted=" + note.getIsDeleted() +
                                            ", FolderId=" + (note.getFolder_id() != null ? note.getFolder_id() : "NULL") +
                                            ", Timestamp=" + (note.getTimestamp() != null ? note.getTimestamp().toString() : "NULL_TIMESTAMP") +
                                            ", ImageUrl Empty: " + (note.getImageUrl() == null || note.getImageUrl().isEmpty()));

                                    if (note.getType() != null && Arrays.asList("drawing", "audio", "image", "list").contains(note.getType())) {
                                        noteList.add(note);
                                    } else {
                                        Log.w(TAG, "Note type mismatch or null type after fetch (expected misc, got " + note.getType() + "): " + doc.getId());
                                    }
                                } else {
                                    Log.w(TAG, "Failed to deserialize document to note object: " + doc.getId() + ". Document data: " + doc.getData());
                                }
                            } catch (Exception deserializeException) {
                                Log.e(TAG, "Error deserializing document: " + doc.getId() + ". Exception: " + deserializeException.getMessage(), deserializeException);
                                Log.e(TAG, "Document data: " + doc.getData());
                            }
                        }
                        filterNotes(searchEditText.getText().toString());
                    } else {
                        Log.d(TAG, "Received null snapshots for miscellaneous notes collection. No notes to display.");
                        noteList.clear();
                        filterNotes("");
                    }
                });
    }

    private void filterNotes(String query) {
        filteredNoteList.clear();
        if (query.isEmpty()) {
            filteredNoteList.addAll(noteList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (note n : noteList) {
                if (n != null &&
                        ((n.getNote_title() != null && n.getNote_title().toLowerCase().contains(lowerCaseQuery)) ||
                                (n.getNote_content() != null && n.getNote_content().toLowerCase().contains(lowerCaseQuery)))) {
                    filteredNoteList.add(n);
                }
            }
        }


        Collections.sort(filteredNoteList, new Comparator<note>() {
            @Override
            public int compare(note n1, note n2) {

                if (n1 == null && n2 == null) return 0;
                if (n1 == null) return 1;
                if (n2 == null) return -1;


                int pinnedCompare = Boolean.compare(n2.getIsPinned(), n1.getIsPinned());
                if (pinnedCompare != 0) {
                    return pinnedCompare;
                }

                if (n1.getTimestamp() == null && n2.getTimestamp() == null) return 0;
                if (n1.getTimestamp() == null) return 1;
                if (n2.getTimestamp() == null) return -1;
                return n2.getTimestamp().compareTo(n1.getTimestamp());
            }
        });

        noteAdapter.notifyDataSetChanged();
        Log.d(TAG, "Filtered notes. Query: '" + query + "', Displaying: " + filteredNoteList.size() + " notes.");
    }

    private void listenForFolders() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || currentUser.getUid() == null) {
            Log.e(TAG, "User not authenticated or UID is null, cannot listen for folders.");
            return;
        }
        String currentUid = currentUser.getUid();
        folderArrayList = new ArrayList<>();

        folderListenerRegistration = db.collection("users").document(currentUid)
                .collection("folders")
                .orderBy("folder_name", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e(TAG, "Folders listen failed.", error);
                            Toast.makeText(secondarypage.this, "Failed to load folders: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (value == null) {
                            Log.d(TAG, "Received null QuerySnapshot for folders.");
                            return;
                        }

                        folderArrayList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Folder folder = doc.toObject(Folder.class);
                            if (folder != null) {
                                folder.setFolder_id(doc.getId());
                                folderArrayList.add(folder);
                            }
                        }
                        Log.d(TAG, "Folders updated. Total: " + folderArrayList.size());
                    }
                });
    }

    private void signOut() {
        if (miscNotesListenerRegistration != null) {
            miscNotesListenerRegistration.remove();
        }
        if (folderListenerRegistration != null) {
            folderListenerRegistration.remove();
        }

        mAuth.signOut();
        Intent intent = new Intent(secondarypage.this, loginpage.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void toggleAddNoteMenu() {
        Animation cp_animation = AnimationUtils.loadAnimation(this, R.anim.circleplus);
        Animation cp_animation2 = AnimationUtils.loadAnimation(this, R.anim.circleplus2);
//        Animation a_audio = AnimationUtils.loadAnimation(this, R.anim.aidlt_alpha);
//        Animation a_audio2 = AnimationUtils.loadAnimation(this, R.anim.aidlt);
//        Animation a_image = AnimationUtils.loadAnimation(this, R.anim.aidlt_alpha);
//        Animation a_image2 = AnimationUtils.loadAnimation(this, R.anim.aidlt);
        Animation a_drawing = AnimationUtils.loadAnimation(this, R.anim.aidlt_alpha);
        Animation a_drawing2 = AnimationUtils.loadAnimation(this, R.anim.aidlt);
        Animation a_list = AnimationUtils.loadAnimation(this, R.anim.aidlt_alpha);
        Animation a_list2 = AnimationUtils.loadAnimation(this, R.anim.aidlt);
        Animation a_text = AnimationUtils.loadAnimation(this, R.anim.aidlt_alpha); // Not used for visibility but for animation
        Animation a_text2 = AnimationUtils.loadAnimation(this, R.anim.aidlt);

        circlePlusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (num == 0){
                    circlePlusButton.startAnimation(cp_animation);

                    Runnable runnable =  () -> textLayout.startAnimation(a_text);
                    new Handler(Looper.getMainLooper()).postDelayed(runnable, 25);

                    Runnable runnable1 =  () -> listLayout.startAnimation(a_list);
                    new Handler(Looper.getMainLooper()).postDelayed(runnable1, 50);

                    Runnable runnable2 =  () -> drawingLayout.startAnimation(a_drawing);
                    new Handler(Looper.getMainLooper()).postDelayed(runnable2, 75);

//                    Runnable runnable3 =  () -> imageLayout.startAnimation(a_image);
//                    new Handler(Looper.getMainLooper()).postDelayed(runnable3, 100);
//
//                    Runnable runnable4 =  () -> audioLayout.startAnimation(a_audio);
//                    new Handler(Looper.getMainLooper()).postDelayed(runnable4, 125);

                    num = 1;
                }

                else if(num == 1){
                    circlePlusButton.startAnimation(cp_animation2);
                    num = 0;

                    Runnable runnable =  () -> textLayout.startAnimation(a_text2);
                    new Handler(Looper.getMainLooper()).postDelayed(runnable, 125);

                    Runnable runnable1 =  () -> listLayout.startAnimation(a_list2);
                    new Handler(Looper.getMainLooper()).postDelayed(runnable1, 100);

                    Runnable runnable2 =  () -> drawingLayout.startAnimation(a_drawing2);
                    new Handler(Looper.getMainLooper()).postDelayed(runnable2, 75);

//                    Runnable runnable3 =  () -> imageLayout.startAnimation(a_image2);
//                    new Handler(Looper.getMainLooper()).postDelayed(runnable3, 50);
//
//                    Runnable runnable4 =  () -> audioLayout.startAnimation(a_audio2);
//                    new Handler(Looper.getMainLooper()).postDelayed(runnable4, 25);
                }
            }
        });
    }

    private void toggleLayoutMode() {
        if (isGridLayout) {
            isGridLayout = false;
            notesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        } else {
            isGridLayout = true;
            notesRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        }
        noteAdapter.setLayoutMode(isGridLayout);
        updateGridListIcon();
    }

    private void updateGridListIcon() {
        if (gridListIcon != null) {
            if (isGridLayout) {
                gridListIcon.setImageResource(R.drawable.list2);
            } else {
                gridListIcon.setImageResource(R.drawable.grid);
            }
        }
    }


    @Override
    public void onItemClicked(note clickedNote) {
        // Add null check for clickedNote itself
        if (clickedNote == null) {
            Log.e(TAG, "onItemClicked: clickedNote is null.");
            Toast.makeText(this, "Error: Selected note is invalid.", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "onItemClicked: Note clicked - Title: " + (clickedNote.getNote_title() != null ? clickedNote.getNote_title() : "N/A") +
                ", Type: " + (clickedNote.getType() != null ? clickedNote.getType() : "N/A") +
                ", ID: " + (clickedNote.getNote_id() != null ? clickedNote.getNote_id() : "N/A"));

        if (clickedNote.getIsLocked()) {
            Log.d(TAG, "Note is locked. Showing PIN verification dialog.");
            if (clickedNote.getNote_id() != null) {
                showVerifyPinDialog(clickedNote.getNote_id(), filteredNoteList.indexOf(clickedNote), true);
            } else {
                Log.e(TAG, "onItemClicked: Cannot verify PIN, note ID is null.");
                Toast.makeText(this, "Error: Locked note has no ID.", Toast.LENGTH_SHORT).show();
            }
            return;
        }


        openNoteForEditing(clickedNote);
    }

    @Override
    public void onNoteLongClick(int position) {
        Log.d(TAG, "onNoteLongClick: Position " + position);
        if (position >= 0 && position < filteredNoteList.size()) {
            note selectedNote = filteredNoteList.get(position);
            // Add null check for selectedNote
            if (selectedNote == null) {
                Log.e(TAG, "onNoteLongClick: selectedNote is null at position " + position);
                Toast.makeText(this, "Error: Cannot perform action, note is invalid.", Toast.LENGTH_SHORT).show();
                return;
            }

            NoteActionsDialogFragment fragment = NoteActionsDialogFragment.newInstance(
                    selectedNote.getIsLocked(),
                    selectedNote.getIsPinned(),
                    false,
                    selectedNote.getNote_id(),
                    position,
                    selectedNote.getFolder_id(),
                    selectedNote.getType()
            );

            fragment.show(getSupportFragmentManager(), "NoteActionsDialogFragment");
        } else {
            Log.e(TAG, "onNoteLongClick: Invalid position " + position + ". List size: " + filteredNoteList.size());
        }
    }

    @Override
    public void onRestoreNote(String noteId, int position) {
        Log.w(TAG, "onRestoreNote called on SecondaryPage. This should not happen.");
        Toast.makeText(this, "Restore function is only for the bin.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPermanentlyDeleteNote(String noteId, int position) {
        Log.w(TAG, "onPermanentlyDeleteNote called on SecondaryPage. This should not happen.");
        Toast.makeText(this, "Permanent delete function is only for the bin.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLockNote(String noteId, int position) {
        showSetPinDialog(noteId, position);
    }

    @Override
    public void onUnlockNote(String noteId, int position) {
        showVerifyPinDialog(noteId, position, false);
    }

    @Override
    public void onMoveToBin(String noteId, int position) {
        onDeleteClick(position);
    }

    @Override
    public void onAddToFolder(String noteId, String noteType, int position) {
        if (folderArrayList.isEmpty()) {
            Toast.makeText(this, "No folders available. Please create one first.", Toast.LENGTH_LONG).show();
            return;
        }

        String[] folderNames = new String[folderArrayList.size()];
        for (int i = 0; i < folderArrayList.size(); i++) {
            folderNames[i] = folderArrayList.get(i).getFolder_name();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Folder");
        builder.setItems(folderNames, (dialog, which) -> {
            Folder selectedFolder = folderArrayList.get(which);
            assignNoteToFolder(noteId, selectedFolder.getFolder_id(), noteType);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    public void onAddToFolder(String noteId, int position) {

    }

    @Override
    public void onRemoveFromFolder(String noteId, int position) {

        new AlertDialog.Builder(this)
                .setTitle("Remove from Folder")
                .setMessage("Are you sure you want to remove this note from its folder?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    if (position >= 0 && position < filteredNoteList.size()) {
                        note noteToRemove = filteredNoteList.get(position); // Get from filtered list
                        String noteType = noteToRemove.getType();
                        removeNoteFromFolder(noteId, noteType);
                    } else {
                        Toast.makeText(this, "Error: Invalid note position to remove from folder.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "onRemoveFromFolder: Invalid position " + position);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }

    @Override
    public void onRestoreNote(String noteId, String noteType) {

    }

    @Override
    public void onPermanentlyDeleteNote(String noteId, String noteType) {

    }

    @Override
    public void onDeleteClick(int position) {
        Log.d(TAG, "onDeleteClick: Position " + position);
        if (position >= 0 && position < filteredNoteList.size()) {
            note noteToDelete = filteredNoteList.get(position);
            if (noteToDelete != null && noteToDelete.getNote_id() != null && noteToDelete.getType() != null) {
                updateNoteStatusInFirestore(noteToDelete.getNote_id(), noteToDelete.getType(), "isDeleted", true, "Note moved to bin.");
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
        Log.d(TAG, "onPinClick: Position " + position + ", currentPinnedStatus: " + currentPinnedStatus);
        if (position >= 0 && position < filteredNoteList.size()) {
            note noteToPin = filteredNoteList.get(position);
            if (noteToPin != null && noteToPin.getNote_id() != null && noteToPin.getType() != null) {
                updateNoteStatusInFirestore(noteToPin.getNote_id(), noteToPin.getType(), "isPinned", !currentPinnedStatus, "Pin status updated.");
            } else {
                Toast.makeText(this, "Error: Cannot pin/unpin invalid note.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "onPinClick: Invalid position " + position);
        }
    }

    private void updateNoteStatusInFirestore(String noteId, String noteType, String fieldName, boolean newValue, String toastMessage) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (uid == null || userRef == null) {
            Log.e(TAG, "UID or userRef is null, cannot update note status.");
            Toast.makeText(this, "Error: User data not loaded.", Toast.LENGTH_SHORT).show();
            return;
        }

        CollectionReference targetCollection = getNoteCollectionRef(noteType);
        if (targetCollection == null) {
            Toast.makeText(this, "Error: Invalid note type for update.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Invalid note type received for updateNoteStatusInFirestore: " + noteType);
            return;
        }
        if (noteId == null || noteId.isEmpty()) {
            Log.e(TAG, "updateNoteStatusInFirestore: noteId is null or empty.");
            Toast.makeText(this, "Error: Note ID is missing for update.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put(fieldName, newValue);
        updates.put("timestamp", new Date());

        if ("isDeleted".equals(fieldName)) {
            updates.put("deleted_date", new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date()));
        }

        Log.d(TAG, "Updating note: ID=" + noteId + ", Type=" + noteType + ", Field=" + fieldName + ", Value=" + newValue);
        targetCollection.document(noteId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(secondarypage.this, toastMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Note " + noteId + " updated successfully for field " + fieldName + ".");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(secondarypage.this, "Error updating note status: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error updating note status for ID: " + noteId + " type: " + noteType + ", field: " + fieldName + ". Error: " + e.getMessage());
                });
    }

    private CollectionReference getNoteCollectionRef(String noteType) {
        if (uid == null) {
            Log.e(TAG, "UID is null in getNoteCollectionRef.");
            return null;
        }
        if (Arrays.asList("drawing", "audio", "image", "list").contains(noteType)) {
            return db.collection("users").document(uid).collection("miscellaneous_notes");
        }
        else if ("text".equals(noteType)) {
            Log.w(TAG, "getNoteCollectionRef in secondarypage received 'text' type. This is unexpected but handled.");
            return db.collection("users").document(uid).collection("notes");
        }
        Log.e(TAG, "Unknown note type encountered in getNoteCollectionRef: " + noteType);
        return null;
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
                note noteToUpdate = filteredNoteList.get(position);
                updateNoteLockStatus(noteId, position, true, hashedPin, noteToUpdate.getType());
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

            note noteToVerify = filteredNoteList.get(position);
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

    private void openNoteForEditing(note note) {
        String noteType = note.getType();
        if (noteType == null) {
            Log.w(TAG, "openNoteForEditing: Note type is null for note ID: " + note.getNote_id());
            Toast.makeText(this, "Cannot open: Note type is undefined.", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("drawing".equals(noteType)) {
            Log.d(TAG, "Opening Drawing for editing: " + note.getNote_id());
            Intent intent = new Intent(secondarypage.this, drawingpageedit.class);
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
        }
//        else if ("audio".equals(clickedNote.getType())) {
//            Log.d(TAG, "Opening Audio for editing: " + clickedNote.getNote_id());
//            Intent intent = new Intent(secondarypage.this, audiopage.class);
//            intent.putExtra("note_id", clickedNote.getNote_id());
//            // Pass other audio note data
//            startActivity(intent);
//        } else if ("image".equals(clickedNote.getType())) {
//            Log.d(TAG, "Opening Image for editing: " + clickedNote.getNote_id());
//            Intent intent = new Intent(secondarypage.this, imagepage.class);
//            intent.putExtra("note_id", clickedNote.getNote_id());
//            // Pass other image note data
//            startActivity(intent);
//        }
        else if ("list".equals(noteType)) {
            Log.d(TAG, "Opening List for editing: " + note.getNote_id());
            Intent intent = new Intent(secondarypage.this, todolistpage.class);
            intent.putExtra("note_id", note.getNote_id());
            // Pass other list note data
            intent.putExtra("note_title", note.getNote_title());
            intent.putExtra("isPinned", note.getIsPinned());
            intent.putExtra("isLocked", note.getIsLocked());
            intent.putExtra("hashedPin", note.getHashedPin());
            intent.putExtra("folder_id", note.getFolder_id());
            intent.putExtra("isDeleted", note.getIsDeleted());
            intent.putExtra("deleted_date", note.getDeleted_date());
            startActivity(intent);
        } else {
            Toast.makeText(this, "Cannot open unsupported note type: " + noteType, Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Attempted to open unsupported note type in secondarypage: " + noteType + " for note ID: " + note.getNote_id());
        }
    }

    private void updateNoteLockStatus(String noteId, int position, boolean lockedStatus, String hashedPin, String noteType) {
        if (uid == null) {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
            return;
        }

        CollectionReference targetCollection = getNoteCollectionRef(noteType);
        if (targetCollection == null) {
            Toast.makeText(this, "Error: Invalid note type for lock update.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Invalid note type received for updateNoteLockStatus: " + noteType);
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("isLocked", lockedStatus);
        updates.put("hashedPin", hashedPin);
        updates.put("timestamp", new Date());

        targetCollection.document(noteId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(secondarypage.this, "Note " + (lockedStatus ? "locked" : "unlocked") + "!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(secondarypage.this, "Failed to update lock status: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error updating note lock status for " + noteId + " (" + noteType + ")", e);
                });
    }

    private void assignNoteToFolder(String noteId, String folderId, String noteType) {
        if (noteId == null || uid == null || noteType == null) {
            Toast.makeText(this, "Error: Note, user, or type not identified.", Toast.LENGTH_SHORT).show();
            return;
        }

        CollectionReference targetCollection = getNoteCollectionRef(noteType);
        if (targetCollection == null) {
            Toast.makeText(this, "Error: Invalid note type for folder assignment.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Invalid note type received for assignNoteToFolder: " + noteType);
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("folder_id", folderId);
        updates.put("timestamp", new Date());

        targetCollection.document(noteId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(secondarypage.this, "Note added to folder!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(secondarypage.this, "Error adding note to folder: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void removeNoteFromFolder(String noteId, String noteType) {
        if (noteId == null || uid == null || noteType == null) {
            Toast.makeText(this, "Error: Note, user, or type not identified.", Toast.LENGTH_SHORT).show();
            return;
        }

        CollectionReference targetCollection = getNoteCollectionRef(noteType);
        if (targetCollection == null) {
            Toast.makeText(this, "Error: Invalid note type for folder removal.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Invalid note type received for removeNoteFromFolder: " + noteType);
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("folder_id", null);
        updates.put("timestamp", new Date());

        targetCollection.document(noteId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(secondarypage.this, "Note removed from folder!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(secondarypage.this, "Error removing note from folder: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
