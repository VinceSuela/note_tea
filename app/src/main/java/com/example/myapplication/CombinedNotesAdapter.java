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

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CombinedNotesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "CombinedNotesAdapter";

    private Context context;
    private ArrayList<note> noteArrayList;
    private rv_onClick listener;

    private static final int VIEW_TYPE_TEXT_NOTE = 0;
    private static final int VIEW_TYPE_MISCELLANEOUS_NOTE = 1;

    public CombinedNotesAdapter(Context context, ArrayList<note> noteArrayList, rv_onClick listener) {
        this.context = context;
        this.noteArrayList = noteArrayList;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        note currentNote = noteArrayList.get(position);
        if ("text".equals(currentNote.getType())) {
            return VIEW_TYPE_TEXT_NOTE;
        } else if ("drawing".equals(currentNote.getType()) || "audio".equals(currentNote.getType()) || "image".equals(currentNote.getType()) || "list".equals(currentNote.getType())) {
            return VIEW_TYPE_MISCELLANEOUS_NOTE;
        }
        // Fallback for unknown types - ensures a view type is always returned
        Log.w(TAG, "Unknown note type encountered: " + currentNote.getType() + " at position " + position + ". Defaulting to text note layout.");
        return VIEW_TYPE_TEXT_NOTE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_TEXT_NOTE) {
            view = LayoutInflater.from(context).inflate(R.layout.notelist, parent, false);
            return new TextNoteViewHolder(view, listener, noteArrayList);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.card_drawing_list_mode, parent, false);
            return new MiscellaneousNoteViewHolder(view, listener, noteArrayList);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        note currentNote = noteArrayList.get(position);

        if (holder.getItemViewType() == VIEW_TYPE_TEXT_NOTE) {
            TextNoteViewHolder textHolder = (TextNoteViewHolder) holder;

            // Defensive null check for note_title TextView
            if (textHolder.note_title != null) {
                textHolder.note_title.setText(currentNote.getNote_title());
            } else {
                Log.e(TAG, "TextNoteViewHolder's note_title TextView is null for note: " + currentNote.getNote_id());
            }

            if (textHolder.note_date != null) {
                textHolder.note_date.setText(currentNote.getNote_date());
            }
            if (textHolder.note_content != null) {
                textHolder.note_content.setText(currentNote.getNote_content());
            }

            if (textHolder.pin != null) {
                if (currentNote.getIsPinned()) {
                    textHolder.pin.setImageResource(R.drawable.pinned);
                } else {
                    textHolder.pin.setImageResource(R.drawable.pin2);
                }
                textHolder.pin.setVisibility(View.VISIBLE);
            }

            if (textHolder.locked != null) {
                if (currentNote.getIsLocked()) {
                    textHolder.locked.setVisibility(View.VISIBLE);
                    textHolder.locked.setImageResource(R.drawable.locked);
                } else {
                    textHolder.locked.setVisibility(View.GONE);
                }
            }
            if (textHolder.delete != null) {
                textHolder.delete.setVisibility(View.VISIBLE);
            }

        } else if (holder.getItemViewType() == VIEW_TYPE_MISCELLANEOUS_NOTE) {
            MiscellaneousNoteViewHolder miscHolder = (MiscellaneousNoteViewHolder) holder;

            if (miscHolder.noteTitle != null) {
                miscHolder.noteTitle.setText(currentNote.getNote_title());
            } else {
                Log.e(TAG, "MiscellaneousNoteViewHolder's noteTitle TextView is null for note: " + currentNote.getNote_id());
            }


            if ("drawing".equals(currentNote.getType()) && currentNote.getImageUrl() != null && !currentNote.getImageUrl().isEmpty()) {
                if (miscHolder.drawingThumbnail != null) {
                    try {
                        byte[] decodedString = Base64.decode(currentNote.getImageUrl(), Base64.DEFAULT);
                        Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        if (decodedBitmap != null) {
                            miscHolder.drawingThumbnail.setImageBitmap(decodedBitmap);
                            miscHolder.drawingThumbnail.setVisibility(View.VISIBLE);
                        } else {
                            miscHolder.drawingThumbnail.setImageResource(R.drawable.placeholder_drawing);
                            miscHolder.drawingThumbnail.setVisibility(View.VISIBLE);
                            Log.e(TAG, "Failed to decode Base64 to bitmap for drawing: " + currentNote.getNote_id());
                        }
                    } catch (IllegalArgumentException e) {
                        miscHolder.drawingThumbnail.setImageResource(R.drawable.placeholder_drawing);
                        miscHolder.drawingThumbnail.setVisibility(View.VISIBLE);
                        Log.e(TAG, "Invalid Base64 string for drawing: " + currentNote.getNote_id(), e);
                    } catch (Exception e) {
                        miscHolder.drawingThumbnail.setImageResource(R.drawable.placeholder_drawing);
                        miscHolder.drawingThumbnail.setVisibility(View.VISIBLE);
                        Log.e(TAG, "Error processing Base64 image for drawing: " + currentNote.getNote_id(), e);
                    }
                }
            } else {
                if (miscHolder.drawingThumbnail != null) {
                    miscHolder.drawingThumbnail.setImageResource(R.drawable.placeholder_drawing);
                    miscHolder.drawingThumbnail.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Showing placeholder for miscellaneous note type: " + currentNote.getType() + ", ID: " + currentNote.getNote_id());
                }
            }

            if (miscHolder.noteDate != null) {
                miscHolder.noteDate.setText(currentNote.getNote_date());
                miscHolder.noteDate.setVisibility(View.VISIBLE);
            }
            if (miscHolder.deleteIcon != null) {
                miscHolder.deleteIcon.setVisibility(View.VISIBLE);
            }
            if (miscHolder.noteContent != null) {
                miscHolder.noteContent.setText(currentNote.getType() != null ?
                        currentNote.getType().substring(0, 1).toUpperCase() + currentNote.getType().substring(1) + " Note" :
                        "Miscellaneous Note");
                miscHolder.noteContent.setVisibility(View.VISIBLE);
            } else {
                Log.e(TAG, "MiscellaneousNoteViewHolder's noteContent TextView is null for note: " + currentNote.getNote_id());
            }


            if (miscHolder.pinIcon != null) {
                if (currentNote.getIsPinned()) {
                    miscHolder.pinIcon.setImageResource(R.drawable.pinned);
                } else {
                    miscHolder.pinIcon.setImageResource(R.drawable.pin2);
                }
                miscHolder.pinIcon.setVisibility(View.VISIBLE);
            }

            if (miscHolder.lockedIcon != null) {
                if (currentNote.getIsLocked()) {
                    miscHolder.lockedIcon.setVisibility(View.VISIBLE);
                    miscHolder.lockedIcon.setImageResource(R.drawable.locked);
                } else {
                    miscHolder.lockedIcon.setVisibility(View.GONE);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return noteArrayList.size();
    }

    public static class TextNoteViewHolder extends RecyclerView.ViewHolder {
        TextView note_title, note_date, note_content;
        CardView noteCardView;
        ImageView delete, pin, locked;

        public TextNoteViewHolder(@NonNull View itemView, rv_onClick rv_onClick, ArrayList<note> notesArrayList) {
            super(itemView);
            note_title = itemView.findViewById(R.id.note_title);
            note_date = itemView.findViewById(R.id.note_date);
            note_content = itemView.findViewById(R.id.note_content);
            delete = itemView.findViewById(R.id.delete);
            pin = itemView.findViewById(R.id.pin);
            locked = itemView.findViewById(R.id.locked);
            noteCardView = itemView.findViewById(R.id.card);

            if (noteCardView != null) {
                noteCardView.setOnClickListener(v -> {
                    if (rv_onClick != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                        rv_onClick.onItemClicked(notesArrayList.get(getAdapterPosition()));
                    }
                });
                noteCardView.setOnLongClickListener(v -> {
                    if (rv_onClick != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                        rv_onClick.onNoteLongClick(getAdapterPosition());
                    }
                    return true;
                });
            } else {
                Log.e(TAG, "TextNoteViewHolder: noteCardView is null.");
            }
            if (delete != null) {
                delete.setOnClickListener(v -> {
                    if (rv_onClick != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                        rv_onClick.onDeleteClick(getAdapterPosition());
                    }
                });
            } else {
                Log.w(TAG, "TextNoteViewHolder: delete ImageView is null.");
            }
            if (pin != null) {
                pin.setOnClickListener(v -> {
                    if (rv_onClick != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                        boolean currentPinnedState = notesArrayList.get(getAdapterPosition()).getIsPinned();
                        rv_onClick.onPinClick(getAdapterPosition(), currentPinnedState);
                    }
                });
            } else {
                Log.w(TAG, "TextNoteViewHolder: pin ImageView is null.");
            }
        }
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

            if (listCardView != null) {
                listCardView.setOnClickListener(v -> {
                    if (rv_onClick != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                        rv_onClick.onItemClicked(notesArrayList.get(getAdapterPosition()));
                    }
                });
                listCardView.setOnLongClickListener(v -> {
                    if (rv_onClick != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                        rv_onClick.onNoteLongClick(getAdapterPosition());
                    }
                    return true;
                });
            } else {
                Log.e(TAG, "MiscellaneousNoteViewHolder: listCardView is null.");
            }
            if (deleteIcon != null) {
                deleteIcon.setOnClickListener(v -> {
                    if (rv_onClick != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                        rv_onClick.onDeleteClick(getAdapterPosition());
                    }
                });
            } else {
                Log.w(TAG, "MiscellaneousNoteViewHolder: deleteIcon ImageView is null.");
            }
            if (pinIcon != null) {
                pinIcon.setOnClickListener(v -> {
                    if (rv_onClick != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                        boolean currentPinnedState = notesArrayList.get(getAdapterPosition()).getIsPinned();
                        rv_onClick.onPinClick(getAdapterPosition(), currentPinnedState);
                    }
                });
            } else {
                Log.w(TAG, "MiscellaneousNoteViewHolder: pinIcon ImageView is null.");
            }
        }
    }
}
