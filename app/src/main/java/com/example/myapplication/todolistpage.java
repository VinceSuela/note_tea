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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class todolistpage extends AppCompatActivity implements TodoAdapter.TodoItemClickListener {

    private static final String TAG = "Todolistpage";

    private ImageView exitButton;
    private RelativeLayout addTodoButton;
    private RecyclerView notCompleteRecyclerView;
    private RecyclerView completeRecyclerView;
    private TextView notCompletedTitle;
    private TextView completedTitle;


    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser user;
    private String uid;
    private DocumentReference userRef;

    private TodoAdapter notCompleteAdapter;
    private TodoAdapter completeAdapter;
    private ArrayList<TodoTask> notCompleteTasks;
    private ArrayList<TodoTask> completeTasks;

    private ListenerRegistration notCompleteListenerRegistration;
    private ListenerRegistration completeListenerRegistration;
    private ProgressDialog progressDialog;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todolistpage);

        exitButton = findViewById(R.id.exit);
        addTodoButton = findViewById(R.id.add_to_do);
        notCompleteRecyclerView = findViewById(R.id.notcomplete_recycler_view);
        completeRecyclerView = findViewById(R.id.complete_recycler_view);
        notCompletedTitle = findViewById(R.id.not_completed_title);
        completedTitle = findViewById(R.id.completed_title);


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
        progressDialog.setMessage("Loading To-Do list...");

        notCompleteTasks = new ArrayList<>();
        completeTasks = new ArrayList<>();

        notCompleteRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notCompleteAdapter = new TodoAdapter(this, notCompleteTasks, this);
        notCompleteRecyclerView.setAdapter(notCompleteAdapter);

        completeRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        completeAdapter = new TodoAdapter(this, completeTasks, this);
        completeRecyclerView.setAdapter(completeAdapter);

        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }

        listenForNotCompleteTasks();
        listenForCompleteTasks();

        exitButton.setOnClickListener(v -> {
            Intent intent = new Intent(todolistpage.this, mainpage.class);
            startActivity(intent);
            finish();
        });

        addTodoButton.setOnClickListener(v -> showAddTaskDialog());
    }

    private void listenForNotCompleteTasks() {
        if (userRef == null) return;

        notCompleteListenerRegistration = userRef.collection("todos")
                .whereEqualTo("isCompleted", false)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }

                        if (e != null) {
                            Log.w(TAG, "Listen failed for not completed tasks.", e);
                            Toast.makeText(todolistpage.this, "Error loading incomplete tasks: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        notCompleteTasks.clear();
                        if (snapshots != null) {
                            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                TodoTask task = doc.toObject(TodoTask.class);
                                if (task != null) {
                                    task.setId(doc.getId());
                                    notCompleteTasks.add(task);
                                }
                            }
                        }
                        notCompleteAdapter.notifyDataSetChanged();
                        updateSectionVisibility();
                    }
                });
    }

    private void listenForCompleteTasks() {
        if (userRef == null) return;

        completeListenerRegistration = userRef.collection("todos")
                .whereEqualTo("isCompleted", true)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }

                        if (e != null) {
                            Log.w(TAG, "Listen failed for completed tasks.", e);
                            Toast.makeText(todolistpage.this, "Error loading completed tasks: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        completeTasks.clear();
                        if (snapshots != null) {
                            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                TodoTask task = doc.toObject(TodoTask.class);
                                if (task != null) {
                                    task.setId(doc.getId());
                                    completeTasks.add(task);
                                }
                            }
                        }
                        completeAdapter.notifyDataSetChanged();
                        updateSectionVisibility();
                    }
                });
    }

    private void updateSectionVisibility() {
        if (notCompleteTasks.isEmpty()) {
            notCompletedTitle.setVisibility(View.GONE);
        } else {
            notCompletedTitle.setVisibility(View.VISIBLE);
        }

        if (completeTasks.isEmpty()) {
            completedTitle.setVisibility(View.GONE);
        } else {
            completedTitle.setVisibility(View.VISIBLE);
        }
    }

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New To-Do");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Enter task name");
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String taskName = input.getText().toString().trim();
            if (!taskName.isEmpty()) {
                addTaskToFirestore(taskName);
            } else {
                Toast.makeText(todolistpage.this, "Task name cannot be empty.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void addTaskToFirestore(String taskName) {
        if (userRef == null) {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show();
            return;
        }

        TodoTask newTask = new TodoTask(taskName, false);
        userRef.collection("todos")
                .add(newTask)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(todolistpage.this, "Task added!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(todolistpage.this, "Error adding task: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error adding task", e);
                });
    }

    @Override
    public void onRadioClick(int position, TodoTask task) {
        boolean newStatus = !task.getIsCompleted();
        updateTaskCompletionStatus(task.getId(), newStatus);
    }

    @Override
    public void onEditClick(int position, TodoTask task) {
        showEditTaskDialog(task);
    }

    @Override
    public void onTodoLongClick(int position, TodoTask task) {
        showDeleteTaskConfirmation(task);
    }

    private void updateTaskCompletionStatus(String taskId, boolean isCompleted) {
        if (userRef == null || taskId == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("isCompleted", isCompleted);
        updates.put("timestamp", new Date());

        userRef.collection("todos").document(taskId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(todolistpage.this, "Task status updated!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(todolistpage.this, "Error updating task status: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error updating task completion status", e);
                });
    }

    private void showEditTaskDialog(final TodoTask task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit To-Do Task");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(task.getTaskName());
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newTaskName = input.getText().toString().trim();
            if (!newTaskName.isEmpty() && !newTaskName.equals(task.getTaskName())) {
                updateTaskName(task.getId(), newTaskName);
            } else if (newTaskName.isEmpty()) {
                Toast.makeText(todolistpage.this, "Task name cannot be empty.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(todolistpage.this, "No changes made.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateTaskName(String taskId, String newTaskName) {
        if (userRef == null || taskId == null) return;

        userRef.collection("todos").document(taskId)
                .update("taskName", newTaskName)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(todolistpage.this, "Task updated!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(todolistpage.this, "Error updating task: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error updating task name", e);
                });
    }

    private void showDeleteTaskConfirmation(final TodoTask task) {
        new AlertDialog.Builder(this)
                .setTitle("Delete To-Do Task")
                .setMessage("Are you sure you want to delete '" + task.getTaskName() + "' permanently?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        deleteTaskFromFirestore(task.getId());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteTaskFromFirestore(String taskId) {
        if (userRef == null || taskId == null) return;

        userRef.collection("todos").document(taskId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(todolistpage.this, "Task deleted!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(todolistpage.this, "Error deleting task: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error deleting task", e);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notCompleteListenerRegistration != null) {
            notCompleteListenerRegistration.remove();
        }
        if (completeListenerRegistration != null) {
            completeListenerRegistration.remove();
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}