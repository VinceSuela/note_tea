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

public class myadapter extends RecyclerView.Adapter<myadapter.TextNoteViewHolder> {

    private static final String TAG = "MyAdapter";
    private Context context;
    private ArrayList<note> noteArrayList;
    private rv_onClick listener;
    private boolean isGridLayout;
    private static final int VIEW_TYPE_LIST = 0;
    private static final int VIEW_TYPE_GRID = 1;

    public myadapter(Context context, ArrayList<note> noteArrayList, rv_onClick listener, boolean isGridLayout) {
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
    public TextNoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_LIST) {
            view = LayoutInflater.from(context).inflate(R.layout.notelist, parent, false);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.notegrid, parent, false);
        }
        return new TextNoteViewHolder(view, listener, noteArrayList);
    }

    @Override
    public void onBindViewHolder(@NonNull TextNoteViewHolder holder, int position) {
        note currentNote = noteArrayList.get(position);

        if (holder.noteTitle != null) {
            holder.noteTitle.setText(currentNote.getNote_title() != null ? currentNote.getNote_title() : "");
        }


        if (holder.pinIcon != null) {
            if (currentNote.getIsPinned()) {
                holder.pinIcon.setImageResource(R.drawable.pinned);
                holder.pinIcon.setVisibility(View.VISIBLE);
            } else {
                holder.pinIcon.setImageResource(R.drawable.pin2);
                holder.pinIcon.setVisibility(View.VISIBLE);
            }
        }


        if (holder.lockedIcon != null) {
            if (currentNote.getIsLocked()) {
                holder.lockedIcon.setVisibility(View.VISIBLE);
                holder.lockedIcon.setImageResource(R.drawable.locked);
            } else {
                holder.lockedIcon.setVisibility(View.GONE);
            }
        }


        if (!isGridLayout) {

            if (holder.listCardView != null) holder.listCardView.setVisibility(View.VISIBLE);
            if (holder.gridCardView != null) holder.gridCardView.setVisibility(View.GONE);

            if (holder.noteDate != null) {
                holder.noteDate.setText(currentNote.getNote_date() != null ? currentNote.getNote_date() : "");
                holder.noteDate.setVisibility(View.VISIBLE);
            }
            if (holder.deleteIcon != null) {
                holder.deleteIcon.setVisibility(View.VISIBLE);
            }
            if (holder.drawingThumbnail != null) {

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
                        }
                    } catch (IllegalArgumentException e) {
                        holder.drawingThumbnail.setImageResource(R.drawable.placeholder_drawing);
                        holder.drawingThumbnail.setVisibility(View.VISIBLE);
                        Log.e(TAG, "Invalid Base64 string for drawing: " + currentNote.getNote_id(), e);
                    }
                } else if ("text".equals(currentNote.getType()) || "note".equals(currentNote.getType())) {
                    holder.drawingThumbnail.setImageResource(R.drawable.placeholder_drawing);
                    holder.drawingThumbnail.setVisibility(View.VISIBLE);
                } else {
                    holder.drawingThumbnail.setVisibility(View.GONE);
                }
            }

            if (holder.noteContent != null) {
                if ("text".equals(currentNote.getType()) || "note".equals(currentNote.getType())) {
                    holder.noteContent.setText(currentNote.getNote_content() != null ? currentNote.getNote_content() : "");
                    holder.noteContent.setVisibility(View.VISIBLE);
                } else if ("drawing".equals(currentNote.getType())) {
                    holder.noteContent.setText("Drawing Note");
                    holder.noteContent.setVisibility(View.VISIBLE);
                } else {
                    holder.noteContent.setVisibility(View.GONE);
                }
            }

        } else {

            if (holder.listCardView != null) holder.listCardView.setVisibility(View.GONE);
            if (holder.gridCardView != null) holder.gridCardView.setVisibility(View.VISIBLE);

            if (holder.noteDate != null) holder.noteDate.setVisibility(View.GONE);
            if (holder.deleteIcon != null) holder.deleteIcon.setVisibility(View.GONE);
            if (holder.drawingThumbnail != null) holder.drawingThumbnail.setVisibility(View.GONE);

            if (holder.noteContent != null) {
                if ("text".equals(currentNote.getType()) || "note".equals(currentNote.getType())) {
                    holder.noteContent.setText(currentNote.getNote_content() != null ? currentNote.getNote_content() : "");
                    holder.noteContent.setVisibility(View.VISIBLE);
                } else if ("drawing".equals(currentNote.getType())) {
                    holder.noteContent.setText("Drawing Note");
                    holder.noteContent.setVisibility(View.VISIBLE);
                } else {
                    holder.noteContent.setVisibility(View.GONE);
                }
            } else {
                Log.w(TAG, "note_content TextView not found in grid layout for note: " + currentNote.getNote_id());
            }
        }
    }

    @Override
    public int getItemCount() {
        return noteArrayList.size();
    }

    public class TextNoteViewHolder extends RecyclerView.ViewHolder {

        ImageView drawingThumbnail;
        TextView noteTitle;
        ImageView pinIcon;
        ImageView lockedIcon;
        TextView noteDate;
        ImageView deleteIcon;
        TextView noteContent;
        CardView listCardView;
        CardView gridCardView;

        public TextNoteViewHolder(@NonNull View itemView, rv_onClick rv_onClick, ArrayList<note> notesArrayList) {
            super(itemView);

            noteTitle = itemView.findViewById(R.id.note_title);
            pinIcon = itemView.findViewById(R.id.pin);
            lockedIcon = itemView.findViewById(R.id.locked);
            noteContent = itemView.findViewById(R.id.note_content);
            drawingThumbnail = itemView.findViewById(R.id.drawing_thumbnail);
            noteDate = itemView.findViewById(R.id.note_date);
            deleteIcon = itemView.findViewById(R.id.delete);
            listCardView = itemView.findViewById(R.id.card);


            gridCardView = itemView.findViewById(R.id.gridcard);

            View clickableCard = null;
            if (listCardView != null) {
                clickableCard = listCardView;
            } else if (gridCardView != null) {
                clickableCard = gridCardView;
            }

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
