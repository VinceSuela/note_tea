package com.example.myapplication;

import android.annotation.SuppressLint;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class mainpage extends AppCompatActivity
        implements rv_onClick, NoteActionsDialogFragment.NoteActionListener {

    TextView username;
    ImageView circleplus, signout, switchpage;
    RelativeLayout folders, bin;
    LinearLayout audio, image, drawing, list, text;

    ArrayList<Folder> folderArrayList;
    ArrayList<note> originalNotesList;
    ListenerRegistration folderListenerRegistration;
    String selectedNoteIdForFolder = null;
    int num = 0;
    FirebaseFirestore db;
    FirebaseUser user;
    FirebaseAuth auth;
    RecyclerView recyclerView;
    myadapter myadapter;
    ArrayList<note> notesModels;
    ProgressDialog progressDialog;
    DocumentReference userRef;
    String uid, currentTime;

    private ListenerRegistration noteListenerRegistration;
    private EditText searchEditText;
    private ImageView searchButton;
    private RelativeLayout gridListToggleButton;
    private ImageView gridListIcon;
    private boolean isGridLayout = false;
    private static final String PREFS_NAME = "MyNoteAppPrefs";
    private static final String PREF_LAYOUT_IS_GRID = "is_grid_layout";

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
        bin = findViewById(R.id.trash);
        drawing = findViewById(R.id.drawing);
        folders = findViewById(R.id.folders);
        list = findViewById(R.id.list);
        text = findViewById(R.id.text);
        searchEditText = findViewById(R.id.search);
        searchButton = findViewById(R.id.searchbutton);
        gridListToggleButton = findViewById(R.id.GridList);
        gridListIcon = findViewById(R.id.grid);
        switchpage = findViewById(R.id.switchpage);

        if(user != null) {
            uid = user.getUid();
            userRef = db.collection("users").document(uid);
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            startActivity(new Intent(this, loginpage.class));
            return;
        }

        originalNotesList = new ArrayList<>();
        folderArrayList = new ArrayList<>();

        progressDialog.setCancelable(false);
        progressDialog.setMessage("Loading data...");



        recyclerView.setHasFixedSize(true);
        notesModels = new ArrayList<note>();
        loadLayoutPreference();
        myadapter = new myadapter(mainpage.this, notesModels, this, isGridLayout);
        recyclerView.setAdapter(myadapter);
        recyclerView.setItemAnimator(null);





        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }


        noteChangeListener();
        listenForFoldersForAddNote();


        username.setText(user.getEmail());

        bin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mainpage.this, binpage.class);
                startActivity(intent);
            }
        });

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
                if (folderListenerRegistration != null) {
                    folderListenerRegistration.remove();
                }
                auth.signOut();
                Intent intent = new Intent(getApplicationContext(), loginpage.class);
                startActivity(intent);
                finish();
            }
        });

        switchpage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), secondarypage.class);
                startActivity(intent);
                finish();
            }
        });

        text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), textnotes.class);
                startActivity(intent);
            }
        });

        list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), todolistpage.class);
                startActivity(intent);
            }
        });

        drawing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), drawingpage.class);
                startActivity(intent);
            }
        });

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = searchEditText.getText().toString().trim();
                filterNotes(query);
            }
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

        gridListToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLayout();
            }
        });


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

//                    Runnable runnable3 =  () -> image.startAnimation(a_image);
//                    new Handler(Looper.getMainLooper()).postDelayed(runnable3, 100);
//
//                    Runnable runnable4 =  () -> audio.startAnimation(a_audio);
//                    new Handler(Looper.getMainLooper()).postDelayed(runnable4, 125);

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

//                    Runnable runnable3 =  () -> image.startAnimation(a_image2);
//                    new Handler(Looper.getMainLooper()).postDelayed(runnable3, 50);
//
//                    Runnable runnable4 =  () -> audio.startAnimation(a_audio2);
//                    new Handler(Looper.getMainLooper()).postDelayed(runnable4, 25);
                }
            }
        });
    }

    private void filterNotes(String query) {
        notesModels.clear();

        if (query.isEmpty()) {
            notesModels.addAll(originalNotesList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (note item : originalNotesList) {
                if (item.getNote_title() != null && item.getNote_title().toLowerCase().contains(lowerCaseQuery)) {
                    notesModels.add(item);
                }
//                else if (item.getNote_content() != null && item.getNote_content().toLowerCase().contains(lowerCaseQuery)) {
//                     notesModels.add(item);
//                }
            }
        }
        myadapter.notifyDataSetChanged();
    }




    private void noteChangeListener() {
        if (userRef == null) {
            Log.e("mainpage", "userRef is null, cannot set up Firestore listener.");

            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            return;
        }

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
                            Toast.makeText(mainpage.this, "Failed to load notes: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e("Mainpage", "Firestore error for main notes: ", error);
                            return;
                        }

                        if (value == null) {
                            Log.d("Mainpage", "Received null QuerySnapshot for main notes.");
                            return;
                        }

                        originalNotesList.clear();

                        for (DocumentSnapshot doc : value.getDocuments()) {
                            note noteItem = doc.toObject(note.class);
                            if (noteItem != null) {
                                noteItem.setNote_id(doc.getId());
                                originalNotesList.add(noteItem);
                            }
                        }

                        filterNotes(searchEditText.getText().toString());

                    }
                });
    }


    private void listenForFoldersForAddNote() {
        if (uid == null) return;

        folderListenerRegistration = db.collection("users").document(uid)
                .collection("folders")
                .orderBy("folder_name", Query.Direction.ASCENDING)
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
                                folder.setFolder_id(doc.getId());
                                folderArrayList.add(folder);
                            }
                        }

                    }
                });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (noteListenerRegistration != null) {
            noteListenerRegistration.remove();
        }
        if (folderListenerRegistration != null) {
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
            showVerifyPinDialog(note.getNote_id(), notesModels.indexOf(note), true);
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
                        .addOnSuccessListener(aVoid -> {
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(mainpage.this, "Error toggling pin status: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            } else {
                Toast.makeText(mainpage.this, "Error: Note ID missing for pin toggle.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onNoteLongClick(int position) {
        if (position != RecyclerView.NO_POSITION) {
            note selectedNote = notesModels.get(position);

            NoteActionsDialogFragment fragment = NoteActionsDialogFragment.newInstance(
                    selectedNote.getIsLocked(),
                    selectedNote.getIsPinned(),
                    selectedNote.getIsDeleted(),
                    selectedNote.getNote_id(),
                    position,
                    selectedNote.getFolder_id(),
                    selectedNote.getType() // Added the missing noteType argument here
            );
            fragment.show(getSupportFragmentManager(), "NoteActionsDialogFragment");
        }
    }

    @Override
    public void onRestoreNote(String noteId, int position) {

    }

    @Override
    public void onPermanentlyDeleteNote(String noteId, int position) {

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

    }

    @Override
    public void onAddToFolder(String noteId, int position) {
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
            assignNoteToFolder(selectedNoteIdForFolder, selectedFolder.getFolder_id());
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    public void onRemoveFromFolder(String noteId, int position) {

    }

    @Override
    public void onRestoreNote(String noteId, String noteType) {

    }

    @Override
    public void onPermanentlyDeleteNote(String noteId, String noteType) {

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
                return;
            }
            String hashedPin = hashPin(pin);
            if (hashedPin != null) {
                updateNoteLockStatus(noteId, position, true, hashedPin);
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

            note noteToVerify = notesModels.get(position);
            if (hashedEnteredPin != null && hashedEnteredPin.equals(noteToVerify.getHashedPin())) {
                Toast.makeText(this, "PIN correct!", Toast.LENGTH_SHORT).show();
                if (openingNote) {
                    openNoteForEditing(noteToVerify);
                } else {
                    updateNoteLockStatus(noteId, position, false, null);
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
        intent.putExtra("key6", note.getFolder_id());
        intent.putExtra("key7", note.getIsDeleted());
        intent.putExtra("key8", note.getDeleted_date());

        startActivity(intent);
    }

    @Override
    public void onDeleteClick(int position) {
        currentTime = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date()).toString();
        if (position != RecyclerView.NO_POSITION && position < notesModels.size()) {
            new AlertDialog.Builder(this)
                    .setTitle("Move to Bin")
                    .setMessage("Are you sure you want to move this note to the bin?")
                    .setPositiveButton("Move to Bin", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            note noteToDelete = notesModels.get(position);
                            String documentId = noteToDelete.getNote_id();

                            if (documentId != null && !documentId.isEmpty()) {
                                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                if (currentUser != null) {
                                    String uid = currentUser.getUid();
                                    Map<String, Object> updates = new HashMap<>();
                                    updates.put("isDeleted", true);
                                    updates.put("deleted_date", currentTime);

                                    FirebaseFirestore.getInstance().collection("users")
                                            .document(uid)
                                            .collection("notes")
                                            .document(documentId)
                                            .update(updates)
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(mainpage.this, "Note moved to bin!", Toast.LENGTH_SHORT).show();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(mainpage.this, "Error moving note to bin: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                Log.e("Mainpage", "Error moving note to bin", e);
                                            });
                                } else {
                                    Toast.makeText(mainpage.this, "User not authenticated for deletion.", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(mainpage.this, "Note ID is missing, cannot move to bin.", Toast.LENGTH_SHORT).show();
                                Log.e("Mainpage", "Note ID was null or empty for moving to bin at position: " + position);
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .setIcon(android.R.drawable.ic_menu_delete)
                    .show();
        } else {
            Toast.makeText(this, "Error: Invalid note position for action.", Toast.LENGTH_SHORT).show();
            Log.e("mainpage", "Attempted action on note at invalid position: " + position);
        }
    }

    private void assignNoteToFolder(String noteId, String folderId) {
        if (noteId == null || uid == null) {
            Toast.makeText(this, "Error: Note or user not identified.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("folder_id", folderId);

        userRef.collection("notes").document(noteId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(mainpage.this, "Note added to folder!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(mainpage.this, "Error adding note to folder: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveLayoutPreference(boolean isGrid) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREF_LAYOUT_IS_GRID, isGrid);
        editor.apply();
    }

    private void loadLayoutPreference() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        isGridLayout = prefs.getBoolean(PREF_LAYOUT_IS_GRID, false);
        applyLayout();
    }

    private void toggleLayout() {
        isGridLayout = !isGridLayout;
        applyLayout();
        saveLayoutPreference(isGridLayout);
    }

    private void applyLayout() {
        if (isGridLayout) {

            recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
            gridListIcon.setImageResource(R.drawable.list2);
        } else {

            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            gridListIcon.setImageResource(R.drawable.grid);
        }

        if (myadapter != null) {
            myadapter.setLayoutMode(isGridLayout);
        }
    }
}