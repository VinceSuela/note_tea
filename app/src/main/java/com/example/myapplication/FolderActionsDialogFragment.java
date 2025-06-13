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

public class FolderActionsDialogFragment extends DialogFragment {

    private static final String ARG_FOLDER_ID = "folderId";
    private static final String ARG_FOLDER_NAME = "folderName";

    private FolderActionListener listener;
    private String folderId;
    private String folderName;

    public interface FolderActionListener {
        void onRenameFolder(String folderId, String currentFolderName);
        void onDeleteFolder(String folderId);
    }

    public static FolderActionsDialogFragment newInstance(String folderId, String folderName) {
        FolderActionsDialogFragment fragment = new FolderActionsDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FOLDER_ID, folderId);
        args.putString(ARG_FOLDER_NAME, folderName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof FolderActionListener) {
            listener = (FolderActionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement FolderActionListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            folderId = getArguments().getString(ARG_FOLDER_ID);
            folderName = getArguments().getString(ARG_FOLDER_NAME);
        }
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.FolderActionDialogStyle);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.folderfragment, container, false);

        TextView renameOption = view.findViewById(R.id.renamefolder);
        TextView deleteOption = view.findViewById(R.id.deletefolder);
        TextView dialogTitle = view.findViewById(R.id.option);

        renameOption.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRenameFolder(folderId, folderName);
            }
            dismiss();
        });

        deleteOption.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteFolder(folderId);
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