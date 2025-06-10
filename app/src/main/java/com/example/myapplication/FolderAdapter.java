
package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.FolderViewHolder> {

    private Context context;
    private ArrayList<Folder> folderArrayList;
    private OnFolderClickListener folderClickListener; // Interface for clicks

    public interface OnFolderClickListener {
        void onFolderClick(Folder folder);
        void onFolderLongClick(Folder folder); // For renaming/deleting folders
    }

    public FolderAdapter(Context context, ArrayList<Folder> folderArrayList, OnFolderClickListener folderClickListener) {
        this.context = context;
        this.folderArrayList = folderArrayList;
        this.folderClickListener = folderClickListener;
    }

    @NonNull
    @Override
    public FolderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.folder, parent, false);
        return new FolderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FolderViewHolder holder, int position) {
        Folder currentFolder = folderArrayList.get(position);
        holder.folderNameTextView.setText(currentFolder.getFolder_name());

        holder.folderNoteCountTextView.setText(currentFolder.getNotesCount() + " notes");


        holder.folderCardView.setOnClickListener(v -> {
            if (folderClickListener != null) {
                folderClickListener.onFolderClick(currentFolder);
            }
        });

        // Set OnLongClickListener for folder options (rename/delete)
        holder.folderCardView.setOnLongClickListener(v -> {
            if (folderClickListener != null) {
                folderClickListener.onFolderLongClick(currentFolder); // Pass the Folder object
                return true; // Consume the long click event
            }
            return false; // Do not consume
        });
    }

    @Override
    public int getItemCount() {
        return folderArrayList.size();
    }

    public static class FolderViewHolder extends RecyclerView.ViewHolder {
        CardView folderCardView;
        ImageView folderIcon;
        TextView folderNameTextView;
        TextView folderNoteCountTextView;

        public FolderViewHolder(@NonNull View itemView) {
            super(itemView);
            folderCardView = itemView.findViewById(R.id.folder_card_view);
            folderIcon = itemView.findViewById(R.id.folder_icon);
            folderNameTextView = itemView.findViewById(R.id.folder_name_text_view);
            folderNoteCountTextView = itemView.findViewById(R.id.folder_note_count_text_view);

            // Removed the itemView.setOnLongClickListener from here.
            // It's handled in onBindViewHolder now, which is more appropriate
            // for setting listeners based on specific data for each item.
        }
    }
}