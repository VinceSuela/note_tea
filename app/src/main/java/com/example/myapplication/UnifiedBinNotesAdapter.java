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
import java.util.Arrays;

public class UnifiedBinNotesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "UnifiedBinNotesAdapter";
    private Context context;
    private ArrayList<note> noteArrayList;
    private rv_onClick listener;

    private static final int VIEW_TYPE_TEXT_NOTE = 0;
    private static final int VIEW_TYPE_MISC_NOTE = 1;

    public UnifiedBinNotesAdapter(Context context, ArrayList<note> noteArrayList, rv_onClick listener, boolean isGridLayout) {
        this.context = context;
        this.noteArrayList = noteArrayList;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (noteArrayList == null || position < 0 || position >= noteArrayList.size()) {
            Log.e(TAG, "getItemViewType: Invalid position or null noteArrayList. Position: " + position + ", Size: " + (noteArrayList != null ? noteArrayList.size() : "null"));
            return VIEW_TYPE_TEXT_NOTE;
        }

        note currentNote = noteArrayList.get(position);
        if (currentNote == null || currentNote.getType() == null) {
            Log.w(TAG, "getItemViewType: Note object or its type is null at position " + position);
            return VIEW_TYPE_TEXT_NOTE;
        }

        if ("text".equals(currentNote.getType())) {
            return VIEW_TYPE_TEXT_NOTE;
        } else if (Arrays.asList("drawing", "audio", "image", "list").contains(currentNote.getType())) {
            return VIEW_TYPE_MISC_NOTE;
        }
        Log.w(TAG, "Unknown note type encountered: " + currentNote.getType() + " at position " + position);
        return VIEW_TYPE_TEXT_NOTE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case VIEW_TYPE_TEXT_NOTE:
                view = LayoutInflater.from(context).inflate(R.layout.notelist, parent, false);
                return new TextNoteViewHolder(view, listener, noteArrayList);
            case VIEW_TYPE_MISC_NOTE:
                view = LayoutInflater.from(context).inflate(R.layout.card_drawing_list_mode, parent, false);
                return new MiscellaneousNoteViewHolder(view, listener, noteArrayList);
            default:
                Log.e(TAG, "onCreateViewHolder: Unexpected viewType: " + viewType + ". Defaulting to TextNoteViewHolder.");
                view = LayoutInflater.from(context).inflate(R.layout.notelist, parent, false);
                return new TextNoteViewHolder(view, listener, noteArrayList);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (noteArrayList == null || position < 0 || position >= noteArrayList.size()) {
            Log.e(TAG, "onBindViewHolder: Invalid position or null noteArrayList. Position: " + position + ", Size: " + (noteArrayList != null ? noteArrayList.size() : "null"));
            return;
        }

        note currentNote = noteArrayList.get(position);
        if (currentNote == null) {
            Log.e(TAG, "onBindViewHolder: currentNote is null at position " + position);
            return;
        }

        if (holder.getItemViewType() == VIEW_TYPE_TEXT_NOTE) {
            TextNoteViewHolder textHolder = (TextNoteViewHolder) holder;

            if (textHolder.note_title != null) {
                textHolder.note_title.setText(currentNote.getNote_title() != null ? currentNote.getNote_title() : "");
            }
            if (textHolder.note_date != null) {
                textHolder.note_date.setText(currentNote.getNote_date() != null ? currentNote.getNote_date() : "");
            }
            if (textHolder.note_content != null) {
                textHolder.note_content.setText(currentNote.getNote_content() != null ? currentNote.getNote_content() : "");
            }

            if (textHolder.delete != null) {
                textHolder.delete.setVisibility(View.GONE);
            }
            if (textHolder.pin != null) {
                textHolder.pin.setVisibility(View.GONE);
            }
            if (textHolder.locked != null) {
                textHolder.locked.setVisibility(View.GONE);
            }

        } else if (holder.getItemViewType() == VIEW_TYPE_MISC_NOTE) {
            MiscellaneousNoteViewHolder miscHolder = (MiscellaneousNoteViewHolder) holder;

            if (miscHolder.noteTitle != null) {
                miscHolder.noteTitle.setText(currentNote.getNote_title() != null ? currentNote.getNote_title() : "");
            }
            if (miscHolder.noteDate != null) {
                miscHolder.noteDate.setText(currentNote.getNote_date() != null ? currentNote.getNote_date() : "");
            }
            if (miscHolder.noteContent != null) {
                miscHolder.noteContent.setText(currentNote.getType() != null ?
                        currentNote.getType().substring(0, 1).toUpperCase() + currentNote.getType().substring(1) + " Note" :
                        "Miscellaneous Note");
            }


            if ("drawing".equals(currentNote.getType()) && currentNote.getImageUrl() != null && !currentNote.getImageUrl().isEmpty()) {
                try {
                    if (miscHolder.drawingThumbnail != null) {
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
                    }
                } catch (IllegalArgumentException e) {
                    if (miscHolder.drawingThumbnail != null) {
                        miscHolder.drawingThumbnail.setImageResource(R.drawable.placeholder_drawing);
                        miscHolder.drawingThumbnail.setVisibility(View.VISIBLE);
                    }
                    Log.e(TAG, "Invalid Base64 string for drawing: " + currentNote.getNote_id(), e);
                } catch (Exception e) {
                    if (miscHolder.drawingThumbnail != null) {
                        miscHolder.drawingThumbnail.setImageResource(R.drawable.placeholder_drawing);
                        miscHolder.drawingThumbnail.setVisibility(View.VISIBLE);
                    }
                    Log.e(TAG, "Error processing Base64 image for drawing: " + currentNote.getNote_id(), e);
                }
            } else if ("audio".equals(currentNote.getType())) {
                if (miscHolder.drawingThumbnail != null) {
                    miscHolder.drawingThumbnail.setImageResource(R.drawable.mic);
                    miscHolder.drawingThumbnail.setVisibility(View.VISIBLE);
                }
            } else if ("image".equals(currentNote.getType())) {
                if (miscHolder.drawingThumbnail != null) {
                    miscHolder.drawingThumbnail.setImageResource(R.drawable.img2);
                    miscHolder.drawingThumbnail.setVisibility(View.VISIBLE);
                }
            } else if ("list".equals(currentNote.getType())) {
                if (miscHolder.drawingThumbnail != null) {
                    miscHolder.drawingThumbnail.setImageResource(R.drawable.todo);
                    miscHolder.drawingThumbnail.setVisibility(View.VISIBLE);
                }
            } else {
                if (miscHolder.drawingThumbnail != null) {
                    miscHolder.drawingThumbnail.setVisibility(View.GONE);
                }
            }

            if (miscHolder.deleteIcon != null) {
                miscHolder.deleteIcon.setVisibility(View.GONE);
            }
            if (miscHolder.pinIcon != null) {
                miscHolder.pinIcon.setVisibility(View.GONE);
            }
            if (miscHolder.lockedIcon != null) {
                miscHolder.lockedIcon.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return noteArrayList != null ? noteArrayList.size() : 0;
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
                    if (rv_onClick != null && getAdapterPosition() != RecyclerView.NO_POSITION && notesArrayList != null && getAdapterPosition() < notesArrayList.size()) {
                        rv_onClick.onItemClicked(notesArrayList.get(getAdapterPosition()));
                    } else {
                        Log.e(TAG, "TextNoteViewHolder: onItemClicked - Listener or data invalid. Pos: " + getAdapterPosition());
                    }
                });

                noteCardView.setOnLongClickListener(v -> {
                    if (rv_onClick != null && getAdapterPosition() != RecyclerView.NO_POSITION && notesArrayList != null && getAdapterPosition() < notesArrayList.size()) {
                        rv_onClick.onNoteLongClick(getAdapterPosition());
                        return true;
                    } else {
                        Log.e(TAG, "TextNoteViewHolder: onNoteLongClick - Listener or data invalid. Pos: " + getAdapterPosition());
                    }
                    return false;
                });
            }

            if (delete != null) {
                delete.setOnClickListener(v -> {
                    if (rv_onClick != null && getAdapterPosition() != RecyclerView.NO_POSITION && notesArrayList != null && getAdapterPosition() < notesArrayList.size()) {
                        rv_onClick.onDeleteClick(getAdapterPosition());
                    } else {
                        Log.e(TAG, "TextNoteViewHolder: onDeleteClick - Listener or data invalid. Pos: " + getAdapterPosition());
                    }
                });
            }
            if (pin != null) {
                pin.setOnClickListener(v -> {
                    if (rv_onClick != null && getAdapterPosition() != RecyclerView.NO_POSITION && notesArrayList != null && getAdapterPosition() < notesArrayList.size()) {
                        boolean currentPinnedState = notesArrayList.get(getAdapterPosition()).getIsPinned();
                        rv_onClick.onPinClick(getAdapterPosition(), currentPinnedState);
                    } else {
                        Log.e(TAG, "TextNoteViewHolder: onPinClick - Listener or data invalid. Pos: " + getAdapterPosition());
                    }
                });
            }
        }
    }

    public static class MiscellaneousNoteViewHolder extends RecyclerView.ViewHolder {
        ImageView drawingThumbnail;
        TextView noteTitle;
        TextView noteDate;
        TextView noteContent;
        ImageView pinIcon;
        ImageView lockedIcon;
        ImageView deleteIcon;
        CardView noteCardView;

        public MiscellaneousNoteViewHolder(@NonNull View itemView, rv_onClick rv_onClick, ArrayList<note> notesArrayList) {
            super(itemView);
            drawingThumbnail = itemView.findViewById(R.id.drawing_thumbnail);
            noteTitle = itemView.findViewById(R.id.drawing_title);
            noteDate = itemView.findViewById(R.id.drawing_date);
            noteContent = itemView.findViewById(R.id.note_content);
            pinIcon = itemView.findViewById(R.id.pin);
            lockedIcon = itemView.findViewById(R.id.locked);
            deleteIcon = itemView.findViewById(R.id.delete);

            noteCardView = itemView.findViewById(R.id.card);

            if (noteCardView != null) {
                noteCardView.setOnClickListener(v -> {
                    if (rv_onClick != null && getAdapterPosition() != RecyclerView.NO_POSITION && notesArrayList != null && getAdapterPosition() < notesArrayList.size()) {
                        rv_onClick.onItemClicked(notesArrayList.get(getAdapterPosition()));
                    } else {
                        Log.e(TAG, "MiscNoteViewHolder: onItemClicked - Listener or data invalid. Pos: " + getAdapterPosition());
                    }
                });

                noteCardView.setOnLongClickListener(v -> {
                    if (rv_onClick != null && getAdapterPosition() != RecyclerView.NO_POSITION && notesArrayList != null && getAdapterPosition() < notesArrayList.size()) {
                        rv_onClick.onNoteLongClick(getAdapterPosition());
                        return true;
                    } else {
                        Log.e(TAG, "MiscNoteViewHolder: onNoteLongClick - Listener or data invalid. Pos: " + getAdapterPosition());
                    }
                    return false;
                });
            }

            if (deleteIcon != null) {
                deleteIcon.setOnClickListener(v -> {
                    if (rv_onClick != null && getAdapterPosition() != RecyclerView.NO_POSITION && notesArrayList != null && getAdapterPosition() < notesArrayList.size()) {
                        rv_onClick.onDeleteClick(getAdapterPosition());
                    } else {
                        Log.e(TAG, "MiscNoteViewHolder: onDeleteClick - Listener or data invalid. Pos: " + getAdapterPosition());
                    }
                });
            }
            if (pinIcon != null) {
                pinIcon.setOnClickListener(v -> {
                    if (rv_onClick != null && getAdapterPosition() != RecyclerView.NO_POSITION && notesArrayList != null && getAdapterPosition() < notesArrayList.size()) {
                        boolean currentPinnedState = notesArrayList.get(getAdapterPosition()).getIsPinned();
                        rv_onClick.onPinClick(getAdapterPosition(), currentPinnedState);
                    } else {
                        Log.e(TAG, "MiscNoteViewHolder: onPinClick - Listener or data invalid. Pos: " + getAdapterPosition());
                    }
                });
            }
        }
    }
}