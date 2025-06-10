// com.example.myapplication/folderpage.java
package com.example.myapplication;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Query; // Import Query

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class folderpage extends AppCompatActivity implements FolderAdapter.OnFolderClickListener,
        FolderActionsDialogFragment.FolderActionListener { // Ensure this is still implemented

    RelativeLayout notes;
    private RecyclerView foldersRecyclerView;
    private FolderAdapter folderAdapter;
    private ArrayList<Folder> folderArrayList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private String uid;
    private ListenerRegistration folderListenerRegistration;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folderpage);

        notes = findViewById(R.id.note);

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

        folderArrayList = new ArrayList<>();
        folderAdapter = new FolderAdapter(this, folderArrayList, this);
        foldersRecyclerView.setAdapter(folderAdapter);

        FloatingActionButton fabAddFolder = findViewById(R.id.fab_add_folder);
        fabAddFolder.setOnClickListener(v -> showAddFolderDialog());

        listenForFolders();

        notes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), mainpage.class);
                startActivity(intent);
                finish();
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
                    documentReference.update("folder_id", folderId); // Update the document with its own ID
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
            return;
        }

        folderListenerRegistration = db.collection("users").document(uid)
                .collection("folders")
                .orderBy("creation_date", Query.Direction.ASCENDING)
                .addSnapshotListener(new com.google.firebase.firestore.EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }

                        if (error != null) {
                            Toast.makeText(folderpage.this, "Error loading folders: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e("folderpage", "Firestore error", error);
                            return;
                        }

                        if (value == null) {
                            return;
                        }

                        // Use a temporary list to build the new state, then update the main list
                        ArrayList<Folder> updatedFolderList = new ArrayList<>();
                        // Track changes based on document IDs to properly handle modifications and additions
                        HashMap<String, Folder> currentFoldersMap = new HashMap<>();
                        for(Folder f : folderArrayList) {
                            currentFoldersMap.put(f.getFolder_id(), f);
                        }

                        for (DocumentChange dc : value.getDocumentChanges()) {
                            Folder folder = dc.getDocument().toObject(Folder.class);
                            folder.setFolder_id(dc.getDocument().getId()); // Manually set the ID

                            switch (dc.getType()) {
                                case ADDED:
                                    // If added, start fetching its count
                                    updatedFolderList.add(folder); // Add to temp list for display
                                    fetchAndSetNoteCount(folder); // Fetch count for the new folder
                                    break;
                                case MODIFIED:
                                    // If modified, update its properties and re-fetch its count
                                    // Find it in the existing list and update, or add if new (shouldn't be new if modified)
                                    boolean found = false;
                                    for (int i = 0; i < folderArrayList.size(); i++) {
                                        if (folderArrayList.get(i).getFolder_id().equals(folder.getFolder_id())) {
                                            folderArrayList.set(i, folder); // Update the object in place
                                            found = true;
                                            break;
                                        }
                                    }
                                    if (!found) { // Should not happen often if IDs are consistent
                                        folderArrayList.add(folder);
                                    }
                                    fetchAndSetNoteCount(folder); // Fetch count for the modified folder
                                    break;
                                case REMOVED:
                                    // Removing handled by Adapter
                                    // The `folderArrayList.clear()` and re-add approach handles this simpler if you switch to it
                                    // If using granular, you'd remove from `folderArrayList` based on oldIndex or ID.
                                    break;
                            }
                        }

                        // For simplicity and to ensure order matches query,
                        // it's often easier to clear and re-add all current documents
                        // after handling individual changes. This also ensures counts for *all*
                        // existing folders are eventually updated if notes change.
                        folderArrayList.clear();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            Folder folder = doc.toObject(Folder.class);
                            if (folder != null) {
                                folder.setFolder_id(doc.getId());

                                // If the folder was already in our list, copy its existing notesCount
                                // to avoid flickering "0 notes" if count fetch is still pending.
                                if (currentFoldersMap.containsKey(folder.getFolder_id())) {
                                    folder.setNotesCount(currentFoldersMap.get(folder.getFolder_id()).getNotesCount());
                                }
                                folderArrayList.add(folder);
                                // Always fetch count for all folders to ensure counts are fresh
                                // This will trigger 'notifyDataSetChanged' on its own
                                fetchAndSetNoteCount(folder);
                            }
                        }
                        folderAdapter.notifyDataSetChanged(); // Notify adapter of structural changes
                    }
                });
    }

    // <--- NEW: Method to fetch and set the notes count for a single folder --->
    private void fetchAndSetNoteCount(Folder folder) {
        if (uid == null || folder.getFolder_id() == null) {
            return;
        }

        db.collection("users").document(uid)
                .collection("notes")
                .whereEqualTo("folder_id", folder.getFolder_id()) // Query notes belonging to this folder
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    folder.setNotesCount(count); // Update the notesCount in the Folder object

                    // Find the position of this folder in the ArrayList and notify just that item
                    int index = folderArrayList.indexOf(folder);
                    if (index != -1) {
                        folderAdapter.notifyItemChanged(index);
                    } else {
                        // If folder not found (e.g., list structure changed), notify all.
                        folderAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("folderpage", "Error fetching note count for folder " + folder.getFolder_name() + ": " + e.getMessage());
                    // Optionally set count to -1 or show error for this specific folder
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

    // --- FolderAdapter.OnFolderClickListener implementations ---

    @Override
    public void onFolderClick(Folder folder) {
        Toast.makeText(this, "Opening folder: " + folder.getFolder_name(), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, FolderNotesActivity.class);
        intent.putExtra("folder_id", folder.getFolder_id());
        intent.putExtra("folder_name", folder.getFolder_name());
        startActivity(intent);
    }

    @Override
    public void onFolderLongClick(Folder folder) {
        Toast.makeText(this, "Long clicked folder: " + folder.getFolder_name(), Toast.LENGTH_SHORT).show();
        FolderActionsDialogFragment fragment = FolderActionsDialogFragment.newInstance(
                folder.getFolder_id(),
                folder.getFolder_name()
        );
        // Assuming FolderActionsDialogFragment has a setTargetFragment if you want to pass results back
        // Or ensure its listener interface is correctly implemented by folderpage
        fragment.show(getSupportFragmentManager(), "FolderActionsDialogFragment");
    }

    // --- FolderActionsDialogFragment.FolderActionListener implementations ---
    @Override
    public void onRenameFolder(String folderId, String currentFolderName) {
        Folder folderToRename = new Folder(currentFolderName);
        folderToRename.setFolder_id(folderId);
        showRenameFolderDialog(folderToRename);
    }

    @Override
    public void onDeleteFolder(String folderId) {
        String folderName = "Unknown Folder";
        for (Folder f : folderArrayList) {
            if (f.getFolder_id().equals(folderId)) {
                folderName = f.getFolder_name();
                break;
            }
        }
        Folder folderToDelete = new Folder(folderName);
        folderToDelete.setFolder_id(folderId);
        showDeleteFolderConfirmation(folderToDelete);
    }

    // Existing methods for handling rename and delete logic
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
        new AlertDialog.Builder(this)
                .setTitle("Delete Folder")
                .setMessage("Are you sure you want to delete folder '" + folder.getFolder_name() + "'? All notes inside this folder will also be deleted.")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // First, delete notes within the folder
                        db.collection("users").document(uid)
                                .collection("notes")
                                .whereEqualTo("folder_id", folder.getFolder_id()) // Ensure "folder_id" matches note's field
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                                        doc.getReference().delete(); // Delete each note
                                    }
                                    // Then delete the folder itself
                                    db.collection("users").document(uid)
                                            .collection("folders").document(folder.getFolder_id())
                                            .delete()
                                            .addOnSuccessListener(aVoid -> Toast.makeText(folderpage.this, "Folder and its notes deleted!", Toast.LENGTH_SHORT).show())
                                            .addOnFailureListener(e -> Toast.makeText(folderpage.this, "Error deleting folder: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                })
                                .addOnFailureListener(e -> Toast.makeText(folderpage.this, "Error finding notes in folder: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}