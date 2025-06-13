package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class NoteActionsDialogFragment extends DialogFragment {

    private static final String TAG = "NoteActionsDialog";

    private static final String ARG_IS_LOCKED = "isLocked";
    private static final String ARG_IS_PINNED = "isPinned";
    private static final String ARG_IS_DELETED = "isDeleted";
    private static final String ARG_NOTE_ID = "noteId";
    private static final String ARG_POSITION = "position";
    private static final String ARG_FOLDER_ID = "folderId";
    private static final String ARG_NOTE_TYPE = "noteType";

    private boolean isLocked, isPinned, isDeleted;
    private String noteId;
    private int position;
    private String folderId;
    private String noteType;

    public interface NoteActionListener {
        void onRestoreNote(String noteId, int position);
        void onPermanentlyDeleteNote(String noteId, int position);
        void onLockNote(String noteId, int position);
        void onUnlockNote(String noteId, int position);
        void onMoveToBin(String noteId, int position);
        void onAddToFolder(String noteId, String noteType, int position);
        void onRemoveFromFolder(String noteId, int position);
    }

    private NoteActionListener listener;

    public static NoteActionsDialogFragment newInstance(boolean isLocked, boolean isPinned, boolean isDeleted,
                                                        String noteId, int position, String folderId, String noteType) {
        NoteActionsDialogFragment fragment = new NoteActionsDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_LOCKED, isLocked);
        args.putBoolean(ARG_IS_PINNED, isPinned);
        args.putBoolean(ARG_IS_DELETED, isDeleted);
        args.putString(ARG_NOTE_ID, noteId);
        args.putInt(ARG_POSITION, position);
        args.putString(ARG_FOLDER_ID, folderId);
        args.putString(ARG_NOTE_TYPE, noteType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof NoteActionListener) {
            listener = (NoteActionListener) context;
        } else if (getParentFragment() instanceof NoteActionListener) {
            listener = (NoteActionListener) getParentFragment();
        } else {
            Log.e(TAG, "Host (Activity or Parent Fragment) must implement NoteActionsDialogFragment.NoteActionListener");
            throw new RuntimeException(context.toString() + " or its parent Fragment must implement NoteActionsDialogFragment.NoteActionListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isLocked = getArguments().getBoolean(ARG_IS_LOCKED);
            isPinned = getArguments().getBoolean(ARG_IS_PINNED);
            isDeleted = getArguments().getBoolean(ARG_IS_DELETED);
            noteId = getArguments().getString(ARG_NOTE_ID);
            position = getArguments().getInt(ARG_POSITION);
            folderId = getArguments().getString(ARG_FOLDER_ID);
            noteType = getArguments().getString(ARG_NOTE_TYPE);
        }
        setStyle(DialogFragment.STYLE_NORMAL, R.style.NoteActionDialogStyle);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment, container, false);

        TextView actionDelete = view.findViewById(R.id.action_delete);
        TextView actionLockUnlock = view.findViewById(R.id.action_lock_unlock);
        TextView actionAddFolder = view.findViewById(R.id.action_add_folder);
        TextView actionRestore = view.findViewById(R.id.action_restore);
        TextView actionPermanentlyDelete = view.findViewById(R.id.action_permanently_delete);

        if (isDeleted) {
            actionDelete.setVisibility(View.GONE);
            actionLockUnlock.setVisibility(View.GONE);
            actionAddFolder.setVisibility(View.GONE);
            actionRestore.setVisibility(View.VISIBLE);
            actionPermanentlyDelete.setVisibility(View.VISIBLE);
        } else {
            actionRestore.setVisibility(View.GONE);
            actionPermanentlyDelete.setVisibility(View.GONE);

            actionDelete.setText("Move to Bin");
            actionDelete.setVisibility(View.VISIBLE);

            actionLockUnlock.setText(isLocked ? "Unlock Note" : "Lock Note");
            actionLockUnlock.setVisibility(View.VISIBLE);

            if (folderId != null && !folderId.isEmpty()) {
                actionAddFolder.setText("Remove from Folder");
            } else {
                actionAddFolder.setText("Add to Folder");
            }
            actionAddFolder.setVisibility(View.VISIBLE);
        }

        actionDelete.setOnClickListener(v -> {
            if (listener != null) {
                if (!isDeleted) {
                    listener.onMoveToBin(noteId, position);
                } else {
                    Log.w(TAG, "Unexpected state: actionDelete clicked when isDeleted is true. Handled as MoveToBin.");
                    listener.onMoveToBin(noteId, position);
                }
            }
            dismiss();
        });

        actionLockUnlock.setOnClickListener(v -> {
            if (listener != null) {
                if (isLocked) {
                    listener.onUnlockNote(noteId, position);
                } else {
                    listener.onLockNote(noteId, position);
                }
            }
            dismiss();
        });

        actionAddFolder.setOnClickListener(v -> {
            if (listener != null) {
                if (folderId != null && !folderId.isEmpty()) {
                    listener.onRemoveFromFolder(noteId, position);
                } else {
                    listener.onAddToFolder(noteId, noteType, position);
                }
            }
            dismiss();
        });

        actionRestore.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRestoreNote(noteId, position);
            }
            dismiss();
        });

        actionPermanentlyDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPermanentlyDeleteNote(noteId, position);
            }
            dismiss();
        });

        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }
}