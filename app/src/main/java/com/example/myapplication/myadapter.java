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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import jp.wasabeef.richeditor.RichEditor;

public class myadapter extends RecyclerView.Adapter<myadapter.NoteViewHolder> {

    private static final String TAG = "MyAdapter";
    Context context;
    ArrayList<note> noteArrayList;
    rv_onClick listener;
    private boolean isGridLayout;

    // Define view types
    private static final int VIEW_TYPE_TEXT_LIST_ITEM = 1;
    private static final int VIEW_TYPE_TEXT_GRID_ITEM = 2;
    private static final int VIEW_TYPE_DRAWING_LIST_ITEM = 3;
    private static final int VIEW_TYPE_DRAWING_GRID_ITEM = 4;


    public myadapter(Context context, ArrayList<note> noteArrayList, rv_onClick listener, boolean isGridLayout) {
        this.context = context;
        this.noteArrayList = noteArrayList;
        this.listener = listener;
        this.isGridLayout = isGridLayout;
    }

    public void setLayoutMode(boolean isGridLayout) {
        this.isGridLayout = isGridLayout;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        String type = noteArrayList.get(position).getType();
        if (type == null) type = "text"; // Default if type is missing

        if ("drawing".equals(type)) {
            return isGridLayout ? VIEW_TYPE_DRAWING_GRID_ITEM : VIEW_TYPE_DRAWING_LIST_ITEM;
        } else { // Covers "text", "list" (and future "audio", "image" if not given specific layouts)
            return isGridLayout ? VIEW_TYPE_TEXT_GRID_ITEM : VIEW_TYPE_TEXT_LIST_ITEM;
        }
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case VIEW_TYPE_DRAWING_LIST_ITEM:
                view = LayoutInflater.from(context).inflate(R.layout.card_drawing_list_mode, parent, false);
                break;
            case VIEW_TYPE_DRAWING_GRID_ITEM:
                view = LayoutInflater.from(context).inflate(R.layout.card_drawing_grid_mode, parent, false);
                break;
            case VIEW_TYPE_TEXT_GRID_ITEM:
                view = LayoutInflater.from(context).inflate(R.layout.notegrid, parent, false);
                break;
            case VIEW_TYPE_TEXT_LIST_ITEM:
            default:
                view = LayoutInflater.from(context).inflate(R.layout.notelist, parent, false);
                break;
        }
        return new NoteViewHolder(view, listener, noteArrayList, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {

        note currentNote = noteArrayList.get(position);
        if (currentNote == null) {
            Log.e(TAG, "currentNote is null at position: " + position);
            return;
        }

        String noteType = currentNote.getType();
        if (noteType == null) noteType = "text";

        // Common Pin and Lock icons
        if (holder.pinIcon != null) {
            holder.pinIcon.setImageResource(currentNote.getIsPinned() ? R.drawable.pinned : R.drawable.pin2);
        }

        if (holder.lockedIcon != null) {
            holder.lockedIcon.setVisibility(currentNote.getIsLocked() ? View.VISIBLE : View.GONE);
            if(currentNote.getIsLocked()) holder.lockedIcon.setImageResource(R.drawable.locked);
        }
        
        int viewType = holder.getItemViewType();

        switch (viewType) {
            case VIEW_TYPE_DRAWING_LIST_ITEM:
                if (holder.drawingTitle != null) {
                    holder.drawingTitle.setText(currentNote.getNote_title());
                }
                if (holder.drawingDate != null) {
                    holder.drawingDate.setText(currentNote.getNote_date()); 
                }
                if (holder.drawingThumbnail != null) {
                    setDrawingThumbnail(holder.drawingThumbnail, currentNote.getImageUrl());
                }
                break;

            case VIEW_TYPE_DRAWING_GRID_ITEM:
                if (holder.drawingTitle != null) {
                    holder.drawingTitle.setText(currentNote.getNote_title());
                }
                if (holder.drawingThumbnail != null) {
                    setDrawingThumbnail(holder.drawingThumbnail, currentNote.getImageUrl());
                }
                break;

            case VIEW_TYPE_TEXT_GRID_ITEM:
                if (holder.textListNoteTitle != null) {
                    holder.textListNoteTitle.setText(currentNote.getNote_title());
                }
                if (holder.textListNoteContentGrid != null) {
                    if ("text".equals(noteType)) {
                        holder.textListNoteContentGrid.setHtml(currentNote.getNote_content() != null ? currentNote.getNote_content() : "");
                    } else if ("list".equals(noteType)){
                        // **THE FIX**: Use a helper to format the list content for the RichEditor.
                        String listPreview = formatListContentForPreview(currentNote.getNote_content());
                        holder.textListNoteContentGrid.setHtml(listPreview);
                    } else {
                        holder.textListNoteContentGrid.setHtml(getNoteTypeDescription(noteType));
                    }
                }
                break;

            case VIEW_TYPE_TEXT_LIST_ITEM:
            default:
                if (holder.textListNoteTitle != null) {
                    holder.textListNoteTitle.setText(currentNote.getNote_title());
                }
                // **THE FIX**: Always show the date, regardless of whether it's a text or list note.
                if (holder.textListNoteDate != null) {
                    holder.textListNoteDate.setText(currentNote.getNote_date());
                }
                break;
        }
    }
    
    // Refactored method to handle drawing thumbnails
    private void setDrawingThumbnail(ImageView imageView, String base64Data) {
        if (base64Data != null && !base64Data.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(base64Data, Base64.DEFAULT);
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                imageView.setImageBitmap(decodedBitmap);
            } catch (Exception e) {
                Log.e(TAG, "Error decoding drawing thumbnail: " + e.getMessage());
                imageView.setImageResource(R.drawable.placeholder_drawing); // Fallback placeholder
            }
        } else {
            imageView.setImageResource(R.drawable.placeholder_drawing); // Fallback placeholder
        }
    }

    // Helper method to create a preview for list notes
    private String formatListContentForPreview(String jsonContent) {
        if (jsonContent == null || jsonContent.isEmpty()) {
            return "<i>Empty list</i>";
        }
        StringBuilder htmlBuilder = new StringBuilder();
        try {
            JSONArray items = new JSONArray(jsonContent);
            int limit = Math.min(items.length(), 5); // Show max 5 items in preview
            for (int i = 0; i < limit; i++) {
                JSONObject item = items.getJSONObject(i);
                boolean isChecked = item.optBoolean("isChecked", false);
                String text = item.optString("text", "");

                if (isChecked) {
                    htmlBuilder.append("☑ ").append("<s>").append(text).append("</s>");
                } else {
                    htmlBuilder.append("☐ ").append(text);
                }
                htmlBuilder.append("<br>");
            }
            if (items.length() > limit) {
                htmlBuilder.append("...");
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing list content for preview", e);
            return "<i>List note</i>"; // Fallback text on parse error
        }
        return htmlBuilder.toString();
    }

    private String getNoteTypeDescription(String noteType) {
        if (noteType == null) return "Note";
        switch (noteType) {
            case "list": return "List Note";
            case "audio": return "Audio Note";
            case "drawing": return "Drawing";
            case "text": return "Text Note";
            default: return "Note";
        }
    }

    @Override
    public int getItemCount() {
        return noteArrayList != null ? noteArrayList.size() : 0;
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        CardView cardViewRoot;
        ImageView pinIcon;
        ImageView lockedIcon;

        // Views for Text/List notes (from notelist.xml & notegrid.xml)
        TextView textListNoteTitle;
        TextView textListNoteDate;
        RichEditor textListNoteContentGrid;
        ImageView textListDeleteIcon;

        // Views for Drawing notes
        ImageView drawingThumbnail;
        TextView drawingTitle;
        TextView drawingDate;      
        ImageView drawingDeleteIcon;

        public NoteViewHolder(@NonNull View itemView, rv_onClick rv_onClick_listener, ArrayList<note> notesList, int viewType) {
            super(itemView);

            pinIcon = itemView.findViewById(R.id.pin);
            lockedIcon = itemView.findViewById(R.id.locked);

            switch (viewType) {
                case VIEW_TYPE_DRAWING_LIST_ITEM:
                    drawingThumbnail = itemView.findViewById(R.id.drawing_thumbnail);
                    drawingTitle = itemView.findViewById(R.id.drawing_title);
                    drawingDate = itemView.findViewById(R.id.drawing_date);
                    drawingDeleteIcon = itemView.findViewById(R.id.delete); 
                    cardViewRoot = itemView.findViewById(R.id.card); 
                    break;
                case VIEW_TYPE_DRAWING_GRID_ITEM:
                    drawingThumbnail = itemView.findViewById(R.id.drawing_thumbnail);
                    drawingTitle = itemView.findViewById(R.id.drawing_title);
                    cardViewRoot = itemView.findViewById(R.id.gridcard); 
                    break;
                case VIEW_TYPE_TEXT_GRID_ITEM:
                    textListNoteTitle = itemView.findViewById(R.id.note_title);
                    textListNoteContentGrid = itemView.findViewById(R.id.note_content);
                    cardViewRoot = itemView.findViewById(R.id.gridcard); 
                    break;
                case VIEW_TYPE_TEXT_LIST_ITEM:
                default:
                    textListNoteTitle = itemView.findViewById(R.id.note_title);
                    textListNoteDate = itemView.findViewById(R.id.note_date);
                    textListDeleteIcon = itemView.findViewById(R.id.delete);
                    cardViewRoot = itemView.findViewById(R.id.card); 
                    break;
            }
            
            View clickableTarget = (cardViewRoot != null) ? cardViewRoot : itemView;

            clickableTarget.setOnClickListener(v -> {
                if (rv_onClick_listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        rv_onClick_listener.onItemClicked(notesList.get(position));
                    }
                }
            });

            clickableTarget.setOnLongClickListener(v -> {
                if (rv_onClick_listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        rv_onClick_listener.onNoteLongClick(position);
                    }
                }
                return true;
            });

            ImageView deleteIcon = (drawingDeleteIcon != null) ? drawingDeleteIcon : textListDeleteIcon;
            if (deleteIcon != null) {
                deleteIcon.setOnClickListener(v -> {
                    if (rv_onClick_listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            rv_onClick_listener.onDeleteClick(position);
                        }
                    }
                });
            }

            if (pinIcon != null) {
                pinIcon.setOnClickListener(v -> {
                    if (rv_onClick_listener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            rv_onClick_listener.onPinClick(position, notesList.get(position).getIsPinned());
                        }
                    }
                });
            }
        }
    }
}
