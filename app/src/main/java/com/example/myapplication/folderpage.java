package com.example.myapplication;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class folderpage extends AppCompatActivity implements FolderAdapter.OnFolderClickListener,
        FolderActionsDialogFragment.FolderActionListener {

    RelativeLayout notes, bin;
    TextView username;
    private RecyclerView foldersRecyclerView;
    private FolderAdapter folderAdapter;
    private ArrayList<Folder> folderArrayList;
    private ArrayList<Folder> originalFolderList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private String uid;
    private ListenerRegistration folderListenerRegistration;
    private ProgressDialog progressDialog;
    private EditText searchFolderEditText;
    private ImageView searchFolderButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folderpage);

        searchFolderEditText = findViewById(R.id.search);
        searchFolderButton = findViewById(R.id.searchbutton);
        notes = findViewById(R.id.note);
        bin = findViewById(R.id.trash);
        username = findViewById(R.id.tv1);

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

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Loading folders...");
        progressDialog.show();

        foldersRecyclerView = findViewById(R.id.folders_recycler_view);
        foldersRecyclerView.setHasFixedSize(true);
        foldersRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        originalFolderList = new ArrayList<>();
        folderArrayList = new ArrayList<>();
        folderAdapter = new FolderAdapter(this, folderArrayList, this);
        foldersRecyclerView.setAdapter(folderAdapter);

        FloatingActionButton fabAddFolder = findViewById(R.id.fab_add_folder);
        fabAddFolder.setOnClickListener(v -> showAddFolderDialog());

        listenForFolders();

        username.setText(user.getEmail());

        searchFolderButton.setOnClickListener(v -> {
            String query = searchFolderEditText.getText().toString().trim();
            filterFolders(query);
        });

        searchFolderEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* Not used */ }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterFolders(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { /* Not used */ }
        });

        notes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), mainpage.class);
                startActivity(intent);
                finish();
            }
        });

        bin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(folderpage.this, binpage.class);
                startActivity(intent);
            }
        });
    }

    private void showAddFolderDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Folder");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Folder Name");
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String folderName = input.getText().toString().trim();
            if (!folderName.isEmpty()) {
                addFolderToFirestore(folderName);
            } else {
                Toast.makeText(folderpage.this, "Folder name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void addFolderToFirestore(String folderName) {
        if (uid == null) {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
            return;
        }

        Folder newFolder = new Folder(folderName);

        db.collection("users").document(uid)
                .collection("folders")
                .add(newFolder)
                .addOnSuccessListener(documentReference -> {
                    String folderId = documentReference.getId();
                    documentReference.update("folder_id", folderId);
                    Toast.makeText(folderpage.this, "Folder '" + folderName + "' created!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(folderpage.this, "Error creating folder: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("folderpage", "Error creating folder", e);
                });
    }


    private void listenForFolders() {
        if (uid == null) {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();

            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            return;
        }


        if (progressDialog != null && !progressDialog.isShowing()) {
            progressDialog.show();
        }

        folderListenerRegistration = db.collection("users").document(uid)
                .collection("folders")
                .orderBy("creation_date", Query.Direction.ASCENDING)
                .addSnapshotListener(new com.google.firebase.firestore.EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {

                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }

                        if (error != null) {
                            Toast.makeText(folderpage.this, "Error loading folders: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e("folderpage", "Firestore error", error);
                            return;
                        }

                        if (value == null) {
                            Log.d("folderpage", "Received null QuerySnapshot for folders.");
                            return;
                        }

                        originalFolderList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Folder folder = doc.toObject(Folder.class);
                            if (folder != null) {
                                folder.setFolder_id(doc.getId());
                                originalFolderList.add(folder);
                            }
                        }

                        for (Folder folder : originalFolderList) {
                            fetchAndSetNoteCount(folder);
                        }

                        filterFolders(searchFolderEditText.getText().toString());
                    }
                });
    }

    private void filterFolders(String query) {
        folderArrayList.clear();

        if (query.isEmpty()) {

            folderArrayList.addAll(originalFolderList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Folder item : originalFolderList) {
                if (item.getFolder_name() != null && item.getFolder_name().toLowerCase().contains(lowerCaseQuery)) {
                    folderArrayList.add(item);
                }
            }
        }
        folderAdapter.notifyDataSetChanged();
    }

    private Task<List<Object>> fetchAndSetNoteCount(Folder folderToUpdateCountFor) {
        if (uid == null || folderToUpdateCountFor.getFolder_id() == null) {
            return Tasks.forResult(null);
        }

        Task<QuerySnapshot> textNotesCountTask = db.collection("users").document(uid)
                .collection("notes")
                .whereEqualTo("folder_id", folderToUpdateCountFor.getFolder_id())
                .whereEqualTo("isDeleted", false)
                .get();

        Task<QuerySnapshot> miscNotesCountTask = db.collection("users").document(uid)
                .collection("miscellaneous_notes")
                .whereEqualTo("folder_id", folderToUpdateCountFor.getFolder_id())
                .whereEqualTo("isDeleted", false)
                .get();

        return Tasks.whenAllSuccess(textNotesCountTask, miscNotesCountTask)
                .addOnSuccessListener(results -> {
                    int textNotesCount = ((QuerySnapshot) results.get(0)).size();
                    int miscNotesCount = ((QuerySnapshot) results.get(1)).size();
                    int totalCount = textNotesCount + miscNotesCount;

                    folderToUpdateCountFor.setNotesCount(totalCount);

                    int index = folderArrayList.indexOf(folderToUpdateCountFor);
                    if (index != -1) {
                        if (searchFolderEditText.getText().toString().isEmpty()) {
                            folderAdapter.notifyItemChanged(index);
                        }
                    }
                    Log.d("folderpage", "Updated count for folder " + folderToUpdateCountFor.getFolder_name() + ": " + totalCount);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching note count for folder " + folderToUpdateCountFor.getFolder_name() + ": " + e.getMessage());
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (folderListenerRegistration != null) {
            folderListenerRegistration.remove();
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onFolderClick(Folder folder) {
        Intent intent = new Intent(this, FolderNotesActivity.class);
        intent.putExtra("folder_id", folder.getFolder_id());
        intent.putExtra("folder_name", folder.getFolder_name());
        startActivity(intent);
    }

    @Override
    public void onFolderLongClick(Folder folder) {
        FolderActionsDialogFragment fragment = FolderActionsDialogFragment.newInstance(
                folder.getFolder_id(),
                folder.getFolder_name()
        );
        fragment.show(getSupportFragmentManager(), "FolderActionsDialogFragment");
    }


    @Override
    public void onRenameFolder(String folderId, String currentFolderName) {
        Folder folderToRename = new Folder(currentFolderName);
        folderToRename.setFolder_id(folderId);
        showRenameFolderDialog(folderToRename);
    }

    @Override
    public void onDeleteFolder(String folderId) {
        String folderName = "Unknown Folder";
        for (Folder f : originalFolderList) {
            if (f.getFolder_id().equals(folderId)) {
                folderName = f.getFolder_name();
                break;
            }
        }
        Folder folderToDelete = new Folder(folderName);
        folderToDelete.setFolder_id(folderId);
        showDeleteFolderConfirmation(folderToDelete);
    }

    private void showRenameFolderDialog(Folder folder) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename Folder");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("New Folder Name");
        input.setText(folder.getFolder_name());
        builder.setView(input);

        builder.setPositiveButton("Rename", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty() && !newName.equals(folder.getFolder_name())) {
                db.collection("users").document(uid)
                        .collection("folders").document(folder.getFolder_id())
                        .update("folder_name", newName)
                        .addOnSuccessListener(aVoid -> Toast.makeText(folderpage.this, "Folder renamed!", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(folderpage.this, "Error renaming folder: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(this, "New name cannot be empty or same as old name.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showDeleteFolderConfirmation(Folder folder) {
        String time = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date()).toString();

        new AlertDialog.Builder(this)
                .setTitle("Delete Folder")
                .setMessage("Are you sure you want to delete folder '" + folder.getFolder_name() + "'? All notes inside this folder will be moved to the bin.")
                .setPositiveButton("Delete Folder", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ProgressDialog deletionProgressDialog = new ProgressDialog(folderpage.this);
                        deletionProgressDialog.setMessage("Deleting folder and moving notes to bin...");
                        deletionProgressDialog.setCancelable(false);
                        deletionProgressDialog.show();

                        db.collection("users").document(uid)
                                .collection("notes")
                                .whereEqualTo("folder_id", folder.getFolder_id())
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    List<Task<Void>> noteUpdateTasks = new ArrayList<>();
                                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                        Map<String, Object> updates = new HashMap<>();
                                        updates.put("isDeleted", true);
                                        updates.put("deleted_date", time);
                                        updates.put("folder_id", null);

                                        noteUpdateTasks.add(doc.getReference().update(updates));
                                    }


                                    Tasks.whenAllSuccess(noteUpdateTasks)
                                            .addOnSuccessListener(aVoid -> {
                                                db.collection("users").document(uid)
                                                        .collection("folders").document(folder.getFolder_id())
                                                        .delete()
                                                        .addOnSuccessListener(v -> {
                                                            deletionProgressDialog.dismiss();
                                                            Toast.makeText(folderpage.this, "Folder deleted and notes moved to bin!", Toast.LENGTH_SHORT).show();
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            deletionProgressDialog.dismiss();
                                                            Toast.makeText(folderpage.this, "Error deleting folder: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                            Log.e("folderpage", "Error deleting folder itself", e);
                                                        });
                                            })
                                            .addOnFailureListener(e -> {
                                                deletionProgressDialog.dismiss();
                                                Toast.makeText(folderpage.this, "Error moving notes to bin: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                Log.e("folderpage", "Error updating notes to bin status", e);
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    deletionProgressDialog.dismiss();
                                    Toast.makeText(folderpage.this, "Error finding notes in folder: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    Log.e("folderpage", "Error querying notes in folder", e);
                                });
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}