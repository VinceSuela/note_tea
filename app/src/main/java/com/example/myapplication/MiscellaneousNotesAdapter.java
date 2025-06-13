package com.example.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MiscellaneousNotesAdapter extends RecyclerView.Adapter<MiscellaneousNotesAdapter.MiscellaneousNoteViewHolder> {

    private static final String TAG = "MiscNotesAdapter";
    private Context context;
    private ArrayList<note> noteArrayList;
    private rv_onClick listener;
    private boolean isGridLayout;

    private static final int VIEW_TYPE_LIST = 1;
    private static final int VIEW_TYPE_GRID = 2;

    public MiscellaneousNotesAdapter(Context context, ArrayList<note> noteArrayList, rv_onClick listener, boolean isGridLayout) {
        this.context = context;
        this.noteArrayList = noteArrayList;
        this.listener = listener;
        this.isGridLayout = isGridLayout;
    }

    public void setLayoutMode(boolean isGridLayout) {
        if (this.isGridLayout != isGridLayout) {
            this.isGridLayout = isGridLayout;
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemViewType(int position) {
        return isGridLayout ? VIEW_TYPE_GRID : VIEW_TYPE_LIST;
    }

    @NonNull
    @Override
    public MiscellaneousNoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_LIST) {
            view = LayoutInflater.from(context).inflate(R.layout.card_drawing_list_mode, parent, false);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.card_drawing_grid_mode, parent, false);
        }
        return new MiscellaneousNoteViewHolder(view, listener, noteArrayList);
    }

    @Override
    public void onBindViewHolder(@NonNull MiscellaneousNoteViewHolder holder, int position) {
        note currentNote = noteArrayList.get(position);

        holder.noteTitle.setText(currentNote.getNote_title());

        if ("drawing".equals(currentNote.getType()) && currentNote.getImageUrl() != null && !currentNote.getImageUrl().isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(currentNote.getImageUrl(), Base64.DEFAULT);
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                if (decodedBitmap != null) {
                    holder.drawingThumbnail.setImageBitmap(decodedBitmap);
                    holder.drawingThumbnail.setVisibility(View.VISIBLE);
                } else {
                    holder.drawingThumbnail.setImageResource(R.drawable.placeholder_drawing);
                    holder.drawingThumbnail.setVisibility(View.VISIBLE);
                    Log.e(TAG, "Failed to decode Base64 to bitmap for drawing: " + currentNote.getNote_id());
                }
            } catch (IllegalArgumentException e) {
                holder.drawingThumbnail.setImageResource(R.drawable.placeholder_drawing);
                holder.drawingThumbnail.setVisibility(View.VISIBLE);
                Log.e(TAG, "Invalid Base64 string for drawing: " + currentNote.getNote_id(), e);
            } catch (Exception e) {
                holder.drawingThumbnail.setImageResource(R.drawable.placeholder_drawing);
                holder.drawingThumbnail.setVisibility(View.VISIBLE);
                Log.e(TAG, "Error processing Base64 image for drawing: " + currentNote.getNote_id(), e);
            }
        } else if ("audio".equals(currentNote.getType())) {
            holder.drawingThumbnail.setImageResource(R.drawable.placeholder_drawing);
            holder.drawingThumbnail.setVisibility(View.VISIBLE);
        } else if ("image".equals(currentNote.getType())) {
            holder.drawingThumbnail.setImageResource(R.drawable.placeholder_drawing);
            holder.drawingThumbnail.setVisibility(View.VISIBLE);
        } else if ("list".equals(currentNote.getType())) {
            holder.drawingThumbnail.setImageResource(R.drawable.placeholder_drawing);
            holder.drawingThumbnail.setVisibility(View.VISIBLE);
        } else {
            holder.drawingThumbnail.setVisibility(View.GONE);
        }


        if (!isGridLayout) {
            if (holder.noteDate != null) {
                holder.noteDate.setText(currentNote.getNote_date());
                holder.noteDate.setVisibility(View.VISIBLE);
            }
            if (holder.deleteIcon != null) {
                holder.deleteIcon.setVisibility(View.VISIBLE);
            }
            if (holder.noteContent != null) {
                holder.noteContent.setText(currentNote.getType() != null ?
                        currentNote.getType().substring(0, 1).toUpperCase() + currentNote.getType().substring(1) + " Note" :
                        "Miscellaneous Note");
                holder.noteContent.setVisibility(View.VISIBLE);
            }
        } else {
            if (holder.noteDate != null) {
                holder.noteDate.setVisibility(View.GONE);
            }
            if (holder.deleteIcon != null) {
                holder.deleteIcon.setVisibility(View.GONE);
            }
            if (holder.noteContent != null) {
                holder.noteContent.setVisibility(View.GONE);
            }
        }

        if (holder.pinIcon != null) {
            if (currentNote.getIsPinned()) {
                holder.pinIcon.setImageResource(R.drawable.pinned);
            } else {
                holder.pinIcon.setImageResource(R.drawable.pin2);
            }
            holder.pinIcon.setVisibility(View.VISIBLE);
        }


        if (holder.lockedIcon != null) {
            if (currentNote.getIsLocked()) {
                holder.lockedIcon.setVisibility(View.VISIBLE);
                holder.lockedIcon.setImageResource(R.drawable.locked);
            } else {
                holder.lockedIcon.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return noteArrayList.size();
    }

    public static class MiscellaneousNoteViewHolder extends RecyclerView.ViewHolder {
        ImageView drawingThumbnail;
        TextView noteTitle;
        ImageView pinIcon;
        ImageView lockedIcon;

        TextView noteDate;
        ImageView deleteIcon;
        TextView noteContent;
        CardView listCardView;

        CardView gridCardView;

        public MiscellaneousNoteViewHolder(@NonNull View itemView, rv_onClick rv_onClick, ArrayList<note> notesArrayList) {
            super(itemView);
            drawingThumbnail = itemView.findViewById(R.id.drawing_thumbnail);
            noteTitle = itemView.findViewById(R.id.drawing_title);
            pinIcon = itemView.findViewById(R.id.pin);
            lockedIcon = itemView.findViewById(R.id.locked);

            noteDate = itemView.findViewById(R.id.drawing_date);
            deleteIcon = itemView.findViewById(R.id.delete);
            noteContent = itemView.findViewById(R.id.note_content);
            listCardView = itemView.findViewById(R.id.card);

            gridCardView = itemView.findViewById(R.id.gridcard);

            CardView clickableCard = (listCardView != null) ? listCardView : gridCardView;
            if (clickableCard != null) {
                clickableCard.setOnClickListener(v -> {
                    if (rv_onClick != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                        rv_onClick.onItemClicked(notesArrayList.get(getAdapterPosition()));
                    }
                });

                clickableCard.setOnLongClickListener(v -> {
                    if (rv_onClick != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                        rv_onClick.onNoteLongClick(getAdapterPosition());
                    }
                    return true;
                });
            }


            if (deleteIcon != null) {
                deleteIcon.setOnClickListener(v -> {
                    if (rv_onClick != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                        rv_onClick.onDeleteClick(getAdapterPosition());
                    }
                });
            }

            if (pinIcon != null) {
                pinIcon.setOnClickListener(v -> {
                    if (rv_onClick != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                        boolean currentPinnedState = notesArrayList.get(getAdapterPosition()).getIsPinned();
                        rv_onClick.onPinClick(getAdapterPosition(), currentPinnedState);
                    }
                });
            }
        }
    }
}