package com.example.myapplication;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.RelativeLayout;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
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


public class mainpage extends AppCompatActivity
        implements rv_onClick, NoteActionsDialogFragment.NoteActionListener {

    private static final String TAG = "MainPage";

    FirebaseAuth mAuth;
    FirebaseUser user;
    private FirebaseFirestore db;
    private RecyclerView notesRecyclerView;
    private myadapter noteAdapter;

    private ArrayList<note> originalNotesList;
    private ArrayList<note> notesModels;

    private ArrayList<Folder> folderArrayList;
    private ListenerRegistration noteListenerRegistration;
    private ListenerRegistration folderListenerRegistration;

    private boolean isGridLayout = false;
    private static final String PREFS_NAME = "MyNoteAppPrefs";
    private static final String PREF_LAYOUT_IS_GRID = "is_grid_layout";

    private TextView userNameTextView, userNotesTextView;
    private ImageView signoutButton;
    private ImageView circlePlusButton;
    private ImageView gridListIcon;
    private ImageView switchPageButton;
    private EditText searchEditText;
    private ImageView searchButton;
    private RelativeLayout gridListToggleButton;

    private LinearLayout audioLayout, imageLayout, drawingLayout, listLayout, textLayout;
    private int num = 0;

    private String uid;
    private DocumentReference userRef;

    private ProgressDialog progressDialog;

    private String selectedNoteIdForFolder = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainpage);

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
        switchPageButton = findViewById(R.id.switchpage);
        gridListToggleButton = findViewById(R.id.GridList);
        gridListIcon = findViewById(R.id.grid);

        audioLayout.setVisibility(View.INVISIBLE);
        imageLayout.setVisibility(View.INVISIBLE);
        drawingLayout.setVisibility(View.INVISIBLE);
        listLayout.setVisibility(View.INVISIBLE);
        textLayout.setVisibility(View.INVISIBLE);

        updateGridListIcon();

        originalNotesList = new ArrayList<>();
        notesModels = new ArrayList<>();
        folderArrayList = new ArrayList<>();

        loadLayoutPreference();

        notesRecyclerView.setHasFixedSize(true);
        notesRecyclerView.setItemAnimator(null);
        noteAdapter = new myadapter(this, notesModels, this, isGridLayout);
        notesRecyclerView.setAdapter(noteAdapter);

        if (isGridLayout) {
            notesRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        } else {
            notesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        }


        loadUserDetails();

        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }

        noteChangeListener();
        listenForFolders();
        setupCirclePlusAnimation();

        signoutButton.setOnClickListener(v -> signOut());

        textLayout.setOnClickListener(v -> {
            Intent intent = new Intent(mainpage.this, textnotes.class);
            startActivity(intent);
            toggleAddNoteMenu();
        });
        drawingLayout.setOnClickListener(v -> {
            Intent intent = new Intent(mainpage.this, drawingpage.class);
            startActivity(intent);
            toggleAddNoteMenu();
        });
        audioLayout.setOnClickListener(v -> {
            Toast.makeText(mainpage.this, "Audio notes are managed on the miscellaneous page.", Toast.LENGTH_SHORT).show();
            toggleAddNoteMenu();
        });
        imageLayout.setOnClickListener(v -> {
            Toast.makeText(mainpage.this, "Image notes are managed on the miscellaneous page.", Toast.LENGTH_SHORT).show();
            toggleAddNoteMenu();
        });
        listLayout.setOnClickListener(v -> {
            Intent intent = new Intent(mainpage.this, todolistpage.class);
            startActivity(intent);
            toggleAddNoteMenu();
        });

        gridListToggleButton.setOnClickListener(v -> toggleLayoutMode());

        findViewById(R.id.trash).setOnClickListener(v -> {
            Intent binIntent = new Intent(mainpage.this, binpage.class);
            startActivity(binIntent);
        });

        findViewById(R.id.folders).setOnClickListener(v -> {
            Intent foldersIntent = new Intent(mainpage.this, folderpage.class);
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
            if (imm != null) {
                imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
            }
        });

        switchPageButton.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), secondarypage.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserDetails();
        noteChangeListener();
        listenForFolders();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (noteListenerRegistration != null) {
            noteListenerRegistration.remove();
            Log.d(TAG, "Removed noteListenerRegistration on stop.");
        }
        if (folderListenerRegistration != null) {
            folderListenerRegistration.remove();
            Log.d(TAG, "Removed folderListenerRegistration on stop.");
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
            userNameTextView.setText(user.getEmail());
            userNotesTextView.setText("Your notes");
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

    private void noteChangeListener() {
        if (userRef == null) {
            Log.e(TAG, "userRef is null. Cannot set up noteChangeListener.");
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            return;
        }

        if (noteListenerRegistration != null) {
            noteListenerRegistration.remove();
            Log.d(TAG, "Removed previous noteListenerRegistration.");
        }

        Log.d(TAG, "Initiating real-time listener for 'notes' collection for user: " + uid);

        noteListenerRegistration = userRef.collection("notes")
                .whereEqualTo("folder_id", null)
                .whereEqualTo("isDeleted", false)
                .orderBy("isPinned", Query.Direction.DESCENDING)
                .orderBy("note_date", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {

                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }

                        if (error != null) {
                            Toast.makeText(mainpage.this, "Error loading notes: " + error.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Firestore real-time error for notes: " + error.getMessage(), error);
                            return;
                        }

                        if (value == null) {
                            Log.d(TAG, "Received null QuerySnapshot for notes in real-time listener. No notes to display.");
                            originalNotesList.clear();
                            filterNotes(searchEditText.getText().toString());
                            return;
                        }

                        originalNotesList.clear();
                        Log.d(TAG, "Received " + value.size() + " documents from Firestore for notes.");
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            try {
                                note noteItem = doc.toObject(note.class);
                                if (noteItem != null) {
                                    noteItem.setNote_id(doc.getId());
                                    if (noteItem.getType() == null || "note".equals(noteItem.getType())) {
                                        noteItem.setType("text");
                                    }

                                    Log.d(TAG, "Fetched Note: ID=" + doc.getId() +
                                            ", Title='" + (noteItem.getNote_title() != null ? noteItem.getNote_title() : "No Title") +
                                            "', Type='" + (noteItem.getType() != null ? noteItem.getType() : "Unknown Type") +
                                            ", Pinned=" + noteItem.getIsPinned() +
                                            ", Locked=" + noteItem.getIsLocked() +
                                            ", Deleted=" + noteItem.getIsDeleted() +
                                            ", FolderId=" + (noteItem.getFolder_id() != null ? noteItem.getFolder_id() : "NULL") +
                                            ", Date=" + (noteItem.getNote_date() != null ? noteItem.getNote_date() : "NULL_DATE"));

                                    originalNotesList.add(noteItem);
                                } else {
                                    Log.w(TAG, "Failed to deserialize document to note object: " + doc.getId() + ". Document data: " + doc.getData());
                                }
                            } catch (Exception deserializeException) {
                                Log.e(TAG, "Error deserializing real-time note document: " + doc.getId() + ". Exception: " + deserializeException.getMessage(), deserializeException);
                                Log.e(TAG, "Document data: " + doc.getData());
                            }
                        }
                        filterNotes(searchEditText.getText().toString());
                        Log.d(TAG, "Notes list updated in real-time. Total notes: " + originalNotesList.size());
                    }
                });
        Log.d(TAG, "Real-time listener for main notes set up.");
    }

    private void filterNotes(String query) {
        notesModels.clear();

        if (query.isEmpty()) {
            notesModels.addAll(originalNotesList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (note item : originalNotesList) {
                if (item != null &&
                        ((item.getNote_title() != null && item.getNote_title().toLowerCase().contains(lowerCaseQuery)) ||
                                (item.getNote_content() != null && item.getNote_content().toLowerCase().contains(lowerCaseQuery)))) {
                    notesModels.add(item);
                }
            }
        }
        noteAdapter.notifyDataSetChanged();
        Log.d(TAG, "Filtered notes list updated. Displaying: " + notesModels.size() + " notes.");
    }

    private void listenForFolders() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || currentUser.getUid() == null) {
            Log.e(TAG, "User not authenticated. Cannot set up folder listener.");
            return;
        }
        String currentUid = currentUser.getUid();
        folderArrayList.clear();

        if (folderListenerRegistration != null) {
            folderListenerRegistration.remove();
            Log.d(TAG, "Removed previous folderListenerRegistration.");
        }

        Log.d(TAG, "Initiating real-time listener for 'folders' collection for user: " + currentUid);

        folderListenerRegistration = db.collection("users").document(currentUid)
                .collection("folders")
                .orderBy("folder_name", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            Toast.makeText(mainpage.this, "Failed to load folders: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Firestore real-time error for folders: " + error.getMessage(), error);
                            return;
                        }
                        if (value == null) {
                            Log.d(TAG, "Received null QuerySnapshot for folders.");
                            folderArrayList.clear();
                            return;
                        }

                        folderArrayList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Folder folder = doc.toObject(Folder.class);
                            if (folder != null) {
                                folder.setFolder_id(doc.getId());
                                folderArrayList.add(folder);
                            } else {
                                Log.w(TAG, "Failed to deserialize folder document: " + doc.getId());
                            }
                        }
                        Log.d(TAG, "Folders list updated. Total folders: " + folderArrayList.size());
                    }
                });
        Log.d(TAG, "Real-time listener for folders set up.");
    }

    private void signOut() {
        if (noteListenerRegistration != null) {
            noteListenerRegistration.remove();
            Log.d(TAG, "Removed noteListenerRegistration before sign out.");
        }
        if (folderListenerRegistration != null) {
            folderListenerRegistration.remove();
            Log.d(TAG, "Removed folderListenerRegistration before sign out.");
        }

        mAuth.signOut();
        Intent intent = new Intent(mainpage.this, loginpage.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
        Toast.makeText(this, "Signed out successfully.", Toast.LENGTH_SHORT).show();
    }

    private void setupCirclePlusAnimation() {
        circlePlusButton.setOnClickListener(v -> toggleAddNoteMenu());
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
        Animation a_text = AnimationUtils.loadAnimation(this, R.anim.aidlt_alpha);
        Animation a_text2 = AnimationUtils.loadAnimation(this, R.anim.aidlt);

        if (num == 0) {
            circlePlusButton.startAnimation(cp_animation);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                textLayout.startAnimation(a_text);
                textLayout.setVisibility(View.VISIBLE);
            }, 25);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                listLayout.startAnimation(a_list);
                listLayout.setVisibility(View.VISIBLE);
            }, 50);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                drawingLayout.startAnimation(a_drawing);
                drawingLayout.setVisibility(View.VISIBLE);
            }, 75);

            audioLayout.setVisibility(View.INVISIBLE);
            imageLayout.setVisibility(View.INVISIBLE);


            num = 1;
        } else if (num == 1) {
            circlePlusButton.startAnimation(cp_animation2);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                drawingLayout.startAnimation(a_drawing2);
                drawingLayout.setVisibility(View.INVISIBLE);
            }, 25);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                listLayout.startAnimation(a_list2);
                listLayout.setVisibility(View.INVISIBLE);
            }, 50);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                textLayout.startAnimation(a_text2);
                textLayout.setVisibility(View.INVISIBLE);
            }, 75);

            audioLayout.setVisibility(View.INVISIBLE);
            imageLayout.setVisibility(View.INVISIBLE);


            num = 0;
        }
    }
    private void toggleLayoutMode() {
        isGridLayout = !isGridLayout;
        if (isGridLayout) {
            notesRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        } else {
            notesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        }
        noteAdapter.setLayoutMode(isGridLayout);
        updateGridListIcon();
        saveLayoutPreference(isGridLayout);
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

    private void loadLayoutPreference() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        isGridLayout = prefs.getBoolean(PREF_LAYOUT_IS_GRID, false);
        if (notesRecyclerView != null) {
            if (isGridLayout) {
                notesRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
            } else {
                notesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            }
            if (noteAdapter != null) {
                noteAdapter.setLayoutMode(isGridLayout);
            }
        }
        updateGridListIcon();
    }

    private void saveLayoutPreference(boolean isGrid) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREF_LAYOUT_IS_GRID, isGrid);
        editor.apply();
        Log.d(TAG, "Layout preference saved: isGridLayout = " + isGrid);
    }

    @Override
    public void onItemClicked(note clickedNote) {
        if (clickedNote == null) {
            Toast.makeText(this, "Error: Selected note is invalid.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (clickedNote.getIsLocked()) {
            showVerifyPinDialog(clickedNote.getNote_id(), notesModels.indexOf(clickedNote), true, clickedNote.getType());
            return;
        }
        openNoteForEditing(clickedNote);
    }

    @Override
    public void onNoteLongClick(int position) {
        if (position >= 0 && position < notesModels.size()) {
            note selectedNote = notesModels.get(position);
            if (selectedNote == null) {
                Toast.makeText(this, "Error: Cannot perform action, note is invalid.", Toast.LENGTH_SHORT).show();
                return;
            }

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
        } else {
            Log.e(TAG, "Invalid position for onNoteLongClick: " + position + ", notesModels size: " + notesModels.size());
            Toast.makeText(this, "Error: Invalid note position for long click.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRestoreNote(String noteId, int position) {
        Toast.makeText(this, "Restore function is only for the bin.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPermanentlyDeleteNote(String noteId, int position) {
        Toast.makeText(this, "Permanent delete function is only for the bin.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLockNote(String noteId, int position) {
        showSetPinDialog(noteId, position);
    }

    @Override
    public void onUnlockNote(String noteId, int position) {
        if (position >= 0 && position < notesModels.size()) {
            note noteToUnlock = notesModels.get(position);
            showVerifyPinDialog(noteId, position, false, noteToUnlock.getType());
        } else {
            Log.e(TAG, "Invalid position for onUnlockNote: " + position + ", notesModels size: " + notesModels.size());
            Toast.makeText(this, "Error: Invalid note position for unlock.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMoveToBin(String noteId, int position) {
        onDeleteClick(position);
    }

    @Override
    public void onAddToFolder(String noteId, String noteType, int position) {
        selectedNoteIdForFolder = noteId;

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
            assignNoteToFolder(selectedNoteIdForFolder, selectedFolder.getFolder_id(), noteType);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    public void onAddToFolder(String noteId, int position) {
        if (position >= 0 && position < notesModels.size()) {
            note noteToMove = notesModels.get(position);
            onAddToFolder(noteId, noteToMove.getType(), position);
        } else {
            Toast.makeText(this, "Error: Invalid note position for adding to folder.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Invalid position for onAddToFolder (int position): " + position + ", notesModels size: " + notesModels.size());
        }
    }

    @Override
    public void onRemoveFromFolder(String noteId, int position) {
        Toast.makeText(this, "Note is not currently in a folder on this view.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeleteClick(int position) {
        String currentTime = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date());

        if (position != RecyclerView.NO_POSITION && position < notesModels.size()) {
            new AlertDialog.Builder(this)
                    .setTitle("Move to Bin")
                    .setMessage("Are you sure you want to move this note to the bin? It can be restored later.")
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
                                    updates.put("timestamp", new Date());

                                    getNoteCollectionRef(noteType)
                                            .document(documentId)
                                            .update(updates)
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(mainpage.this, "Note moved to bin!", Toast.LENGTH_SHORT).show();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(mainpage.this, "Error moving note to bin: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                Log.e(TAG, "Failed to move note to bin: " + e.getMessage(), e);
                                            });
                                } else {
                                    Toast.makeText(mainpage.this, "User not authenticated for deletion.", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(mainpage.this, "Note ID or type is missing, cannot move to bin.", Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Attempted to move to bin with null noteId or noteType. ID: " + documentId + ", Type: " + noteType);
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .setIcon(android.R.drawable.ic_menu_delete)
                    .show();
        } else {
            Toast.makeText(this, "Error: Invalid note position for action.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Invalid position for onDeleteClick: " + position + ", notesModels size: " + notesModels.size());
        }
    }

    @Override
    public void onPinClick(int position, boolean currentPinnedStatus) {
        if (position >= 0 && position < notesModels.size()) {
            note noteToPin = notesModels.get(position);
            String documentId = noteToPin.getNote_id();
            String noteType = noteToPin.getType();

            if (documentId != null && !documentId.isEmpty() && noteType != null) {
                boolean newPinnedStatus = !currentPinnedStatus;

                Map<String, Object> updates = new HashMap<>();
                updates.put("isPinned", newPinnedStatus);
                updates.put("timestamp", new Date());

                getNoteCollectionRef(noteType).document(documentId).update(updates)
                        .addOnSuccessListener(aVoid -> Toast.makeText(mainpage.this, "Note pin status updated!", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> {
                            Toast.makeText(mainpage.this, "Error toggling pin status: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Failed to toggle pin status: " + e.getMessage(), e);
                        });
            } else {
                Toast.makeText(this, "Error: Note ID or type missing for pin toggle.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Attempted to toggle pin with null noteId or noteType. ID: " + documentId + ", Type: " + noteType);
            }
        } else {
            Log.e(TAG, "Invalid position for onPinClick: " + position + ", notesModels size: " + notesModels.size());
            Toast.makeText(this, "Error: Invalid note position for pin toggle.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRestoreNote(String noteId, String noteType) {
        Toast.makeText(this, "This note is not in the bin.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPermanentlyDeleteNote(String noteId, String noteType) {
        Toast.makeText(this, "Permanent delete function is only for the bin.", Toast.LENGTH_SHORT).show();
    }

    private void assignNoteToFolder(String noteId, String folderId, String noteType) {
        if (noteId == null || uid == null || noteType == null) {
            Toast.makeText(this, "Error: Note, user, or type not identified.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "assignNoteToFolder: Missing noteId, uid, or noteType. Note ID: " + noteId + ", User ID: " + uid + ", Note Type: " + noteType);
            return;
        }

        CollectionReference targetCollection = getNoteCollectionRef(noteType);
        if (targetCollection == null) {
            Toast.makeText(this, "Error: Invalid note type for folder assignment.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "assignNoteToFolder: Could not get collection reference for type: " + noteType);
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("folder_id", folderId);
        updates.put("timestamp", new Date());

        targetCollection.document(noteId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(mainpage.this, "Note added to folder!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(mainpage.this, "Error adding note to folder: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to assign note to folder: " + e.getMessage(), e);
                });
    }

    private void openNoteForEditing(note note) {
        String noteType = note.getType();
        if (noteType == null) {
            Toast.makeText(this, "Cannot open: Note type is undefined.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "openNoteForEditing: Note type is null for note ID: " + note.getNote_id());
            return;
        }

        Intent intent;
        switch (noteType) {
            case "text":
                intent = new Intent(mainpage.this, textnoteedit.class);
                intent.putExtra("key", note.getNote_id());
                intent.putExtra("key1", note.getNote_title());
                intent.putExtra("key2", note.getNote_content());
                intent.putExtra("key3", note.getIsPinned());
                intent.putExtra("key4", note.getIsLocked());
                intent.putExtra("key5", note.getHashedPin());
                intent.putExtra("key6", note.getFolder_id());
                intent.putExtra("key7", note.getIsDeleted());
                intent.putExtra("key8", note.getDeleted_date());
                break;
            case "drawing":
                intent = new Intent(mainpage.this, drawingpageedit.class);
                intent.putExtra("note_id", note.getNote_id());
                intent.putExtra("note_title", note.getNote_title());
                intent.putExtra("base64Image", note.getImageUrl());
                intent.putExtra("isPinned", note.getIsPinned());
                intent.putExtra("isLocked", note.getIsLocked());
                intent.putExtra("hashedPin", note.getHashedPin());
                intent.putExtra("folder_id", note.getFolder_id());
                intent.putExtra("isDeleted", note.getIsDeleted());
                intent.putExtra("deleted_date", note.getDeleted_date());
                break;
            case "list":
                intent = new Intent(mainpage.this, todolistpage.class);
                intent.putExtra("note_id", note.getNote_id());
                intent.putExtra("note_title", note.getNote_title());
                intent.putExtra("isPinned", note.getIsPinned());
                intent.putExtra("isLocked", note.getIsLocked());
                intent.putExtra("hashedPin", note.getHashedPin());
                intent.putExtra("folder_id", note.getFolder_id());
                intent.putExtra("isDeleted", note.getIsDeleted());
                intent.putExtra("deleted_date", note.getDeleted_date());
                break;
            default:
                Toast.makeText(this, "Cannot open unsupported note type: " + noteType, Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Attempted to open unsupported note type: " + noteType + " for note ID: " + note.getNote_id());
                return;
        }
        startActivity(intent);
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
            Toast.makeText(this, "Hashing error, cannot secure note.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "SHA-256 algorithm not found: " + e.getMessage(), e);
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
                if (position >= 0 && position < notesModels.size()) {
                    note noteToUpdate = notesModels.get(position);
                    updateNoteLockStatus(noteId, position, true, hashedPin, noteToUpdate.getType());
                } else {
                    Log.e(TAG, "Invalid position for showSetPinDialog: " + position + ", notesModels size: " + notesModels.size());
                    Toast.makeText(mainpage.this, "Error: Invalid note position for setting PIN.", Toast.LENGTH_SHORT).show();
                }
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

            if (position < 0 || position >= notesModels.size()) {
                Toast.makeText(this, "Error: Note not found for PIN verification.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "showVerifyPinDialog: Invalid position: " + position + ", notesModels size: " + notesModels.size());
                return;
            }

            note noteToVerify = notesModels.get(position);
            if (hashedEnteredPin != null && hashedEnteredPin.equals(noteToVerify.getHashedPin())) {
                Toast.makeText(this, "PIN correct!", Toast.LENGTH_SHORT).show();
                if (openingNote) {
                    note tempNoteForOpening = new note(
                            noteToVerify.getImageUrl(),
                            noteToVerify.getType(),
                            noteToVerify.getFolder_id(),
                            noteToVerify.getDeleted_date(),
                            noteToVerify.getHashedPin(),
                            noteToVerify.getIsDeleted(),
                            noteToVerify.getIsLocked(),
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
                Log.d(TAG, "Incorrect PIN entered for note: " + noteId);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateNoteLockStatus(String noteId, int position, boolean lockedStatus, String hashedPin, String noteType) {
        if (noteType == null) {
            Toast.makeText(this, "Error: Note type missing for lock update.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "updateNoteLockStatus: noteType is null for noteId: " + noteId);
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("isLocked", lockedStatus);
        updates.put("hashedPin", hashedPin);
        updates.put("timestamp", new Date());

        getNoteCollectionRef(noteType).document(noteId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(mainpage.this, "Note " + (lockedStatus ? "locked" : "unlocked") + "!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Lock status updated for note " + noteId + ". Locked: " + lockedStatus);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(mainpage.this, "Failed to update lock status: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to update lock status for note " + noteId + ": " + e.getMessage(), e);
                });
    }

    private CollectionReference getNoteCollectionRef(String noteType) {
        if (uid == null) {
            Log.e(TAG, "UID is null in getNoteCollectionRef. Cannot get collection.");
            return null;
        }
        if ("text".equals(noteType)) {
            return userRef.collection("notes");
        } else if (Arrays.asList("drawing", "audio", "image", "list").contains(noteType)) {
            return userRef.collection("miscellaneous_notes");
        }
        Log.w(TAG, "Unknown note type '" + noteType + "' in getNoteCollectionRef. Defaulting to 'notes' collection.");
        return userRef.collection("notes");
    }

    private void dismissProgressDialogIfReady() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            Log.d(TAG, "Progress dialog dismissed.");
        }
    }
}
