// com.example.myapplication/NoteActionsDialogFragment.java
package com.example.myapplication;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class NoteActionsDialogFragment extends DialogFragment {

    // These constants should match the keys used when creating the bundle
    private static final String ARG_NOTE_ID = "noteId";
    private static final String ARG_FOLDER_ID = "folderId";
    private static final String ARG_IS_LOCKED = "isLocked";
    private static final String ARG_IS_PINNED = "isPinned"; // Added for completeness, though not directly used in dialog
    private static final String ARG_POSITION = "position"; // Added for completeness

    private NoteActionListener listener;
    private String noteId, folderId;
    private boolean isLocked;
    private boolean isPinned; // Stored, but not directly used in this dialog's UI currently
    private int position;

    // Corrected interface to match what mainpage is trying to implement
    public interface NoteActionListener {
        void onLockNote(String noteId, int position); // For setting PIN
        void onUnlockNote(String noteId, int position); // For verifying PIN and unlocking
        void onDeleteNoteFromFragment(String noteId, int position); // Matches your existing method name
        void onAddToFolder(String noteId, int position);
        void onRemoveFromFolder(String noteId, int position);
    }

    // Corrected static factory method to match parameters sent from mainpage
    public static NoteActionsDialogFragment newInstance(boolean isLocked, boolean isPinned, String noteId, int position, @Nullable String folderId) {
        NoteActionsDialogFragment fragment = new NoteActionsDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NOTE_ID, noteId);
        args.putBoolean(ARG_IS_LOCKED, isLocked);
        args.putBoolean(ARG_IS_PINNED, isPinned);
        args.putInt(ARG_POSITION, position);
        args.putString(ARG_FOLDER_ID, folderId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof NoteActionListener) {
            listener = (NoteActionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement NoteActionListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            noteId = getArguments().getString(ARG_NOTE_ID);
            isLocked = getArguments().getBoolean(ARG_IS_LOCKED);
            isPinned = getArguments().getBoolean(ARG_IS_PINNED);
            position = getArguments().getInt(ARG_POSITION);
            folderId = getArguments().getString(ARG_FOLDER_ID); // Retrieve the folderId
        }
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.NoteActionDialogStyle);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment's UI
        View view = inflater.inflate(R.layout.fragment, container, false);

        // Find the TextViews for each action option
        TextView deleteOption = view.findViewById(R.id.action_delete);
        TextView lockUnlockOption = view.findViewById(R.id.action_lock_unlock);
        // Only one TextView for folder actions now
        TextView folderActionOption = view.findViewById(R.id.action_add_folder); // This will be reused

        // Set the text for the lock/unlock option based on the note's current lock status
        lockUnlockOption.setText(isLocked ? "Unlock" : "Lock");

        // Dynamic text and listener for the folder action option
        if (folderId != null && !folderId.isEmpty()) { // If the note HAS a folder_id, it's in a folder
            folderActionOption.setText("Remove from Folder"); // Change text
            folderActionOption.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemoveFromFolder(noteId, position); // Call remove function
                }
                dismiss(); // Dismiss the dialog after action
            });
        } else { // If folderId is null or empty, the note is on the main page (not in a folder)
            folderActionOption.setText("Add to Folder"); // Set text to default
            folderActionOption.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddToFolder(noteId, position); // Call add function
                }
                dismiss(); // Dismiss the dialog after action
            });
        }

        // Set OnClickListeners for other action options
        deleteOption.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteNoteFromFragment(noteId, position);
            }
            dismiss(); // Dismiss the dialog after action
        });

        lockUnlockOption.setOnClickListener(v -> {
            if (listener != null) {
                if (isLocked) {
                    listener.onUnlockNote(noteId, position);
                } else {
                    listener.onLockNote(noteId, position);
                }
            }
            dismiss(); // Dismiss the dialog after action
        });

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }
}