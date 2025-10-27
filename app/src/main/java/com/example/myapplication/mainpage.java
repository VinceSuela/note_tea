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
import java.util.stream.Collectors;


public class mainpage extends AppCompatActivity
        implements rv_onClick, NoteActionsDialogFragment.NoteActionListener {

    private static final String TAG = "MainPage";

    private static final int DISPLAY_MODE_ALL = 0;
    private static final int DISPLAY_MODE_TEXT_ONLY = 1;
    private static final int DISPLAY_MODE_MISC_ONLY = 2; // Drawings and Lists
    private int currentDisplayMode = DISPLAY_MODE_ALL;

    FirebaseAuth mAuth;
    FirebaseUser user;
    private FirebaseFirestore db;
    private RecyclerView notesRecyclerView;
    private myadapter noteAdapter;

    private ArrayList<note> textNotesList;
    private ArrayList<note> miscellaneousNotesList;
    private ArrayList<note> notesModels; // This is the list for the adapter

    private ArrayList<Folder> folderArrayList;
    private ListenerRegistration textNoteListenerRegistration;
    private ListenerRegistration miscNotesListenerRegistration;
    private ListenerRegistration folderListenerRegistration;

    private boolean isGridLayout = false;
    private static final String PREFS_NAME = "MyNoteAppPrefs";
    private static final String PREF_LAYOUT_IS_GRID = "is_grid_layout";
    private static final String PREF_DISPLAY_MODE = "display_mode";

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
    private int num2 = 0;
    private Animation sp_anim;

    private String uid;
    private DocumentReference userRef;

    private ProgressDialog progressDialog;
    private String selectedNoteIdForFolder = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainpage);

        // Load animations that might be used by switchPageButton
        sp_anim = AnimationUtils.loadAnimation(this, R.anim.switchpage);

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

        audioLayout.setVisibility(View.GONE);
        imageLayout.setVisibility(View.GONE);
        drawingLayout.setVisibility(View.INVISIBLE);
        listLayout.setVisibility(View.INVISIBLE);
        textLayout.setVisibility(View.INVISIBLE);

        updateGridListIcon();

        textNotesList = new ArrayList<>();
        miscellaneousNotesList = new ArrayList<>();
        notesModels = new ArrayList<>();
        folderArrayList = new ArrayList<>();

        loadLayoutPreference();
        loadDisplayModePreference();

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

        if (user != null) {
            if (!progressDialog.isShowing()) {
                progressDialog.show();
            }
            textNoteChangeListener();
            miscNoteChangeListener();
            listenForFolders();
        }

        circlePlusButton.setOnClickListener(v -> toggleAddNoteMenu());
        setupSwitchPageButton();

        signoutButton.setOnClickListener(v -> signOut());

        textLayout.setOnClickListener(v -> {
            startActivity(new Intent(mainpage.this, textnotes.class));
            toggleAddNoteMenu();
        });
        drawingLayout.setOnClickListener(v -> {
            startActivity(new Intent(mainpage.this, drawingpage.class));
            toggleAddNoteMenu();
        });
        audioLayout.setOnClickListener(v -> {
            Toast.makeText(mainpage.this, "Audio note creation coming soon!", Toast.LENGTH_SHORT).show();
            toggleAddNoteMenu();
        });
        imageLayout.setOnClickListener(v -> {
            Toast.makeText(mainpage.this, "Image note creation coming soon!", Toast.LENGTH_SHORT).show();
            toggleAddNoteMenu();
        });
        listLayout.setOnClickListener(v -> {
            startActivity(new Intent(mainpage.this, todolistpage.class));
            toggleAddNoteMenu();
        });

        gridListToggleButton.setOnClickListener(v -> toggleLayoutMode());

        findViewById(R.id.trash).setOnClickListener(v -> startActivity(new Intent(mainpage.this, binpage.class)));
        findViewById(R.id.folders).setOnClickListener(v -> startActivity(new Intent(mainpage.this, folderpage.class)));

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                combineAndFilterNotes(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchButton.setOnClickListener(v -> {
            combineAndFilterNotes(searchEditText.getText().toString());
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
            }
        });
        
        updateUserNotesTextView();
    }
    
    private void setupSwitchPageButton() {
        if (switchPageButton != null) {
            switchPageButton.setVisibility(View.VISIBLE);
            switchPageButton.setOnClickListener(v -> {
                switchPageButton.startAnimation(sp_anim);
                currentDisplayMode = (currentDisplayMode + 1) % 3;
                saveDisplayModePreference(currentDisplayMode);
                updateUserNotesTextView();
                combineAndFilterNotes(searchEditText.getText().toString());

            });
        }
    }

    private void updateUserNotesTextView() {
        if (userNotesTextView != null) {
            switch (currentDisplayMode) {
                case DISPLAY_MODE_TEXT_ONLY:
                    userNotesTextView.setText("Text Notes");
                    break;
                case DISPLAY_MODE_MISC_ONLY:
                    userNotesTextView.setText("Drawings & Audio");
                    break;
                case DISPLAY_MODE_ALL:
                default:
                    userNotesTextView.setText("All Your Notes");
                    break;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserDetails(); 
        if (user != null) {
            if (textNoteListenerRegistration == null) textNoteChangeListener();
            if (miscNotesListenerRegistration == null) miscNoteChangeListener();
            if (folderListenerRegistration == null) listenForFolders();
            combineAndFilterNotes(searchEditText.getText().toString());
        }
        updateUserNotesTextView();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (textNoteListenerRegistration != null) textNoteListenerRegistration.remove();
        if (miscNotesListenerRegistration != null) miscNotesListenerRegistration.remove();
        if (folderListenerRegistration != null) folderListenerRegistration.remove();
        textNoteListenerRegistration = null;
        miscNotesListenerRegistration = null;
        folderListenerRegistration = null;
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
        } else {
            userNameTextView.setText("Guest");
            userNotesTextView.setText("Login to save your notes");
        }
    }

    private void textNoteChangeListener() {
        if (userRef == null) {
            Log.e(TAG, "userRef is null for textNoteChangeListener.");
            dismissProgressDialogIfReady();
            return;
        }
        if (textNoteListenerRegistration != null) textNoteListenerRegistration.remove();
        textNoteListenerRegistration = userRef.collection("notes")
                .whereEqualTo("folder_id", null)
                .whereEqualTo("isDeleted", false)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Text notes listener error", error);
                        dismissProgressDialogIfReady();
                        return;
                    }
                    if (value == null) {
                        Log.d(TAG, "Null QuerySnapshot for text notes.");
                         textNotesList.clear(); 
                         combineAndFilterNotes(searchEditText.getText().toString());
                        dismissProgressDialogIfReady();
                        return;
                    }
                    textNotesList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        try {
                            note noteItem = doc.toObject(note.class);
                            if (noteItem != null) {
                                noteItem.setNote_id(doc.getId());
                                if (noteItem.getType() == null || "note".equals(noteItem.getType())) {
                                    noteItem.setType("text");
                                }
                                textNotesList.add(noteItem);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error deserializing text note: " + doc.getId(), e);
                        }
                    }
                    combineAndFilterNotes(searchEditText.getText().toString());
                    dismissProgressDialogIfReady();
                });
    }

    private void miscNoteChangeListener() {
        if (userRef == null) {
            Log.e(TAG, "userRef is null for miscNoteChangeListener.");
            dismissProgressDialogIfReady();
            return;
        }
        if (miscNotesListenerRegistration != null) miscNotesListenerRegistration.remove();
        miscNotesListenerRegistration = userRef.collection("miscellaneous_notes")
                .whereEqualTo("folder_id", null)
                .whereEqualTo("isDeleted", false)
                .whereIn("type", Arrays.asList("drawing", "list"))
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Misc notes listener error", error);
                        dismissProgressDialogIfReady();
                        return;
                    }
                    if (value == null) {
                        Log.d(TAG, "Null QuerySnapshot for misc notes.");
                        miscellaneousNotesList.clear(); 
                        combineAndFilterNotes(searchEditText.getText().toString());
                        dismissProgressDialogIfReady();
                        return;
                    }
                    miscellaneousNotesList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        try {
                            note noteItem = doc.toObject(note.class);
                            if (noteItem != null) {
                                noteItem.setNote_id(doc.getId());
                                if ("drawing".equals(noteItem.getType()) || "list".equals(noteItem.getType())) {
                                    miscellaneousNotesList.add(noteItem);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error deserializing misc note: " + doc.getId(), e);
                        }
                    }
                    combineAndFilterNotes(searchEditText.getText().toString());
                    dismissProgressDialogIfReady();
                });
    }

private void combineAndFilterNotes(String query) {
    notesModels.clear();
    ArrayList<note> sourceList = new ArrayList<>();

    switch (currentDisplayMode) {
        case DISPLAY_MODE_TEXT_ONLY:
            sourceList.addAll(textNotesList);
            break;
        case DISPLAY_MODE_MISC_ONLY:
            for (note n : miscellaneousNotesList) {
                if ("drawing".equals(n.getType()) || "list".equals(n.getType())) {
                    sourceList.add(n);
                }
            }
            break;
        case DISPLAY_MODE_ALL:
        default:
            sourceList.addAll(textNotesList);
            for (note n : miscellaneousNotesList) {
                if ("drawing".equals(n.getType()) || "list".equals(n.getType())) {
                    sourceList.add(n);
                }
            }
            break;
    }

    if (query == null || query.isEmpty()) { 
        notesModels.addAll(sourceList);
    } else {
        String lowerCaseQuery = query.toLowerCase();
        for (note item : sourceList) {
            if (item.getNote_title() != null && item.getNote_title().toLowerCase().contains(lowerCaseQuery)) {
                 notesModels.add(item);
            }
        }
    }

    Collections.sort(notesModels, (n1, n2) -> {
        if (n1 == null && n2 == null) return 0;
        if (n1 == null) return 1;
        if (n2 == null) return -1;
        int pinnedCompare = Boolean.compare(n2.getIsPinned(), n1.getIsPinned());
        if (pinnedCompare != 0) return pinnedCompare;
        Date ts1 = n1.getTimestamp();
        Date ts2 = n2.getTimestamp();
        if (ts1 != null && ts2 != null) {
            int tsCompare = ts2.compareTo(ts1);
            if (tsCompare != 0) return tsCompare;
        } else if (ts1 != null) return -1;
        else if (ts2 != null) return 1;
        String title1 = n1.getNote_title() == null ? "" : n1.getNote_title();
        String title2 = n2.getNote_title() == null ? "" : n2.getNote_title();
        return title1.compareToIgnoreCase(title2);
    });

    if (noteAdapter != null) {
        noteAdapter.notifyDataSetChanged();
    }
}


    private void listenForFolders() {
        if (userRef == null) return;
        if (folderListenerRegistration != null) folderListenerRegistration.remove();
        folderListenerRegistration = userRef.collection("folders")
                .orderBy("folder_name", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Folders listener error", error);
                        return;
                    }
                    if (value == null) return;
                    folderArrayList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        Folder folder = doc.toObject(Folder.class);
                        if (folder != null) {
                            folder.setFolder_id(doc.getId());
                            folderArrayList.add(folder);
                        }
                    }
                });
    }

    private void signOut() {
        if (textNoteListenerRegistration != null) textNoteListenerRegistration.remove();
        if (miscNotesListenerRegistration != null) miscNotesListenerRegistration.remove();
        if (folderListenerRegistration != null) folderListenerRegistration.remove();
        textNoteListenerRegistration = null;
        miscNotesListenerRegistration = null;
        folderListenerRegistration = null;

        mAuth.signOut();
        Intent intent = new Intent(mainpage.this, loginpage.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void toggleAddNoteMenu() {
        // Load animations for circlePlusButton locally each time
        Animation cp_animation_open = AnimationUtils.loadAnimation(this, R.anim.circleplus);
        Animation cp_animation_close = AnimationUtils.loadAnimation(this, R.anim.circleplus2);
        
        Animation a_drawing_open = AnimationUtils.loadAnimation(this, R.anim.aidlt_alpha);
        Animation a_drawing_close = AnimationUtils.loadAnimation(this, R.anim.aidlt);
        Animation a_list_open = AnimationUtils.loadAnimation(this, R.anim.aidlt_alpha);
        Animation a_list_close = AnimationUtils.loadAnimation(this, R.anim.aidlt);
        Animation a_text_open = AnimationUtils.loadAnimation(this, R.anim.aidlt_alpha);
        Animation a_text_close = AnimationUtils.loadAnimation(this, R.anim.aidlt);

        if (num == 0) { // If menu is closed, open it
            circlePlusButton.startAnimation(cp_animation_open);
            
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                textLayout.startAnimation(a_text_open);
                textLayout.setVisibility(View.VISIBLE);
            }, 25);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                listLayout.startAnimation(a_list_open);
                listLayout.setVisibility(View.VISIBLE);
            }, 50);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                drawingLayout.startAnimation(a_drawing_open);
                drawingLayout.setVisibility(View.VISIBLE);
            }, 75);
            num = 1; // Menu is now open
        } else { // If menu is open, close it
            circlePlusButton.startAnimation(cp_animation_close);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                drawingLayout.startAnimation(a_drawing_close);
                drawingLayout.setVisibility(View.INVISIBLE);
            }, 25);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                listLayout.startAnimation(a_list_close);
                listLayout.setVisibility(View.INVISIBLE);
            }, 50);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                textLayout.startAnimation(a_text_close);
                textLayout.setVisibility(View.INVISIBLE);
            }, 75);
            num = 0; // Menu is now closed
        }
        // No animation of switchPageButton from here.
    }

    private void toggleLayoutMode() {
        isGridLayout = !isGridLayout;
        if (isGridLayout) {
            notesRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        } else {
            notesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        }
        if (noteAdapter != null) {
            noteAdapter.setLayoutMode(isGridLayout);
        }
        updateGridListIcon();
        saveLayoutPreference(isGridLayout);
    }

    private void updateGridListIcon() {
        if (gridListIcon != null) {
            gridListIcon.setImageResource(isGridLayout ? R.drawable.list2 : R.drawable.grid);
        }
    }

    private void loadLayoutPreference() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        isGridLayout = prefs.getBoolean(PREF_LAYOUT_IS_GRID, false);
        updateGridListIcon(); 
    }

    private void saveLayoutPreference(boolean isGrid) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREF_LAYOUT_IS_GRID, isGrid);
        editor.apply();
    }
    
    private void loadDisplayModePreference() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        currentDisplayMode = prefs.getInt(PREF_DISPLAY_MODE, DISPLAY_MODE_ALL);
    }

    private void saveDisplayModePreference(int mode) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_DISPLAY_MODE, mode);
        editor.apply();
    }


    @Override
    public void onItemClicked(note clickedNote) {
        if (clickedNote == null) return;
        if (clickedNote.getIsLocked()) {
            showVerifyPinDialog(clickedNote.getNote_id(), clickedNote, true);
            return;
        }
        openNoteForEditing(clickedNote);
    }

    @Override
    public void onNoteLongClick(int position) {
        if (position < 0 || position >= notesModels.size()) return;
        note selectedNote = notesModels.get(position);
        if (selectedNote == null) return;
        NoteActionsDialogFragment.newInstance(
                selectedNote.getIsLocked(), selectedNote.getIsPinned(), selectedNote.getIsDeleted(),
                selectedNote.getNote_id(), position, selectedNote.getFolder_id(), selectedNote.getType()
        ).show(getSupportFragmentManager(), "NoteActionsDialogFragment");
    }

    @Override
    public void onRestoreNote(String noteId, int position) { }
    @Override
    public void onPermanentlyDeleteNote(String noteId, int position) { }

    @Override
    public void onLockNote(String noteId, int position) {
        if (position < 0 || position >= notesModels.size()) return;
        note noteToUpdate = notesModels.get(position);
        if (noteToUpdate == null || noteToUpdate.getType() == null) return;
        showSetPinDialog(noteId, noteToUpdate.getType());
    }

    @Override
    public void onUnlockNote(String noteId, int position) {
        if (position < 0 || position >= notesModels.size()) return;
        note noteToUnlock = notesModels.get(position);
        if (noteToUnlock != null && noteToUnlock.getType() != null) {
            showVerifyPinDialog(noteId, noteToUnlock, false);
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
            Toast.makeText(this, "No folders available.", Toast.LENGTH_LONG).show();
            return;
        }
        String[] folderNames = folderArrayList.stream().map(Folder::getFolder_name).toArray(String[]::new);
        new AlertDialog.Builder(this)
                .setTitle("Select Folder")
                .setItems(folderNames, (dialog, which) -> {
                    Folder selectedFolder = folderArrayList.get(which);
                    assignNoteToFolder(selectedNoteIdForFolder, selectedFolder.getFolder_id(), noteType);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    @Override
    public void onAddToFolder(String noteId, int position){
         if (position < 0 || position >= notesModels.size()) return;
         note noteToMove = notesModels.get(position);
         if(noteToMove != null && noteToMove.getType() != null){
             onAddToFolder(noteId, noteToMove.getType(), position);
         }
    }

    @Override
    public void onRemoveFromFolder(String noteId, int position) { }

    @Override
    public void onDeleteClick(int position) {
        if (position < 0 || position >= notesModels.size()) return;
        note noteToDelete = notesModels.get(position);
        if (noteToDelete == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Move to Bin")
                .setMessage("Move this note to bin?")
                .setPositiveButton("Move", (dialog, which) -> {
                    String documentId = noteToDelete.getNote_id();
                    String noteType = noteToDelete.getType();
                    if (documentId == null || noteType == null) return;

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("isDeleted", true);
                    updates.put("deleted_date", new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date()));
                    updates.put("timestamp", new Date());

                    getNoteCollectionRef(noteType)
                            .document(documentId)
                            .update(updates)
                            .addOnSuccessListener(aVoid -> Toast.makeText(mainpage.this, "Note moved to bin.", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(mainpage.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setIcon(android.R.drawable.ic_menu_delete)
                .show();
    }

    @Override
    public void onPinClick(int position, boolean currentPinnedStatus) {
        if (position < 0 || position >= notesModels.size()) return;
        note noteToPin = notesModels.get(position);
        if (noteToPin == null) return;
        String documentId = noteToPin.getNote_id();
        String noteType = noteToPin.getType();
        if (documentId == null || noteType == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("isPinned", !currentPinnedStatus);
        updates.put("timestamp", new Date()); 

        getNoteCollectionRef(noteType).document(documentId).update(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(mainpage.this, "Note pin status updated.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(mainpage.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
    
    @Override
    public void onRestoreNote(String noteId, String noteType) { }

    @Override
    public void onPermanentlyDeleteNote(String noteId, String noteType) { }

    private void assignNoteToFolder(String noteId, String folderId, String noteType) {
        if (noteId == null || uid == null || noteType == null || userRef == null) return;
        CollectionReference targetCollection = getNoteCollectionRef(noteType);
        if (targetCollection == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("folder_id", folderId);
        updates.put("timestamp", new Date()); 

        targetCollection.document(noteId).update(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(mainpage.this, "Note added to folder.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(mainpage.this, "Error adding to folder: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void openNoteForEditing(note note) {
        String noteType = note.getType();
        if (noteType == null) return;
        Intent intent = null;
        switch (noteType) {
            case "text":
                intent = new Intent(mainpage.this, textnoteedit.class);
                intent.putExtra("key", note.getNote_id());
                intent.putExtra("key2", note.getNote_content());
                break;
            case "drawing":
                intent = new Intent(mainpage.this, drawingpageedit.class);
                intent.putExtra("note_id", note.getNote_id());
                intent.putExtra("base64Image", note.getImageUrl());
                break;
            case "list":
                intent = new Intent(mainpage.this, todolistpage.class);
                intent.putExtra("note_id", note.getNote_id());
                break;
            default:
                Toast.makeText(this, "Cannot open: " + noteType, Toast.LENGTH_SHORT).show();
                return;
        }
        if (intent != null) {
            intent.putExtra("note_title", note.getNote_title());
            intent.putExtra("isPinned", note.getIsPinned());
            intent.putExtra("isLocked", note.getIsLocked()); 
            intent.putExtra("hashedPin", note.getHashedPin()); 
            intent.putExtra("folder_id", note.getFolder_id());
            startActivity(intent);
        }
    }

    private String hashPin(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] messageDigest = md.digest(pin.getBytes());
            String hashtext = new BigInteger(1, messageDigest).toString(16);
            while (hashtext.length() < 32) hashtext = "0" + hashtext;
            return hashtext;
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "SHA-256 Error", e);
            return null;
        }
    }

    private void showSetPinDialog(String noteId, String noteType) {
         if (noteType == null) {
             Toast.makeText(this, "Cannot set PIN: Unknown note type.", Toast.LENGTH_SHORT).show();
             return;
         }
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint("Enter new 4-digit PIN");
        new AlertDialog.Builder(this)
                .setTitle("Set PIN")
                .setView(input)
                .setPositiveButton("Set", (dialog, which) -> {
                    String pinText = input.getText().toString().trim();
                    if (pinText.length() == 4 && pinText.matches("\\d{4}")) {
                        String hashedPin = hashPin(pinText);
                        if (hashedPin != null) {
                            updateNoteLockStatus(noteId, true, hashedPin, noteType);
                        }
                    } else {
                        Toast.makeText(this, "PIN must be 4 digits.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showVerifyPinDialog(String noteId, note noteToVerify, boolean openingNote) {
        if (noteToVerify == null || noteToVerify.getType() == null) {
            Toast.makeText(this, "Cannot verify PIN: Note data missing.", Toast.LENGTH_SHORT).show();
            return;
        }
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint("Enter PIN");
        new AlertDialog.Builder(this)
                .setTitle("Enter PIN to " + (openingNote ? "Open" : "Unlock"))
                .setView(input)
                .setPositiveButton("Verify", (dialog, which) -> {
                    String hashedEnteredPin = hashPin(input.getText().toString().trim());
                    if (hashedEnteredPin != null && hashedEnteredPin.equals(noteToVerify.getHashedPin())) {
                        if (openingNote) {
                            openNoteForEditing(noteToVerify);
                        } else { 
                            updateNoteLockStatus(noteId, false, null, noteToVerify.getType());
                        }
                    } else {
                        Toast.makeText(this, "Incorrect PIN.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateNoteLockStatus(String noteId, boolean lockedStatus, @Nullable String hashedPin, String noteType) {
        if (noteType == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("isLocked", lockedStatus);
        updates.put("hashedPin", lockedStatus ? hashedPin : null); 
        updates.put("timestamp", new Date()); 
        getNoteCollectionRef(noteType).document(noteId).update(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(mainpage.this, "Note " + (lockedStatus ? "locked" : "unlocked") + ".", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(mainpage.this, "Failed to update lock status.", Toast.LENGTH_LONG).show());
    }

    private CollectionReference getNoteCollectionRef(String noteType) {
        if (uid == null || userRef == null) return null;
        if ("text".equals(noteType)) {
            return userRef.collection("notes");
        } else if (Arrays.asList("drawing", "list").contains(noteType)) {
            return userRef.collection("miscellaneous_notes");
        }
        return null;
    }

    private void dismissProgressDialogIfReady() {
        boolean textProcessed = textNotesList != null;
        boolean miscProcessed = miscellaneousNotesList != null;
        boolean dismiss = false;
        switch (currentDisplayMode) {
            case DISPLAY_MODE_ALL: if (textProcessed && miscProcessed) dismiss = true; break;
            case DISPLAY_MODE_TEXT_ONLY: if (textProcessed) dismiss = true; break;
            case DISPLAY_MODE_MISC_ONLY: if (miscProcessed) dismiss = true; break;
        }
        if (dismiss && progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
