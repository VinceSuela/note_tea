package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
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

public class myadapter extends RecyclerView.Adapter<myadapter.myviewholder> {

    // Renamed for clarity: this is the listener instance
    rv_onClick itemClickListener;
    Context context;
    // note note; // No need to declare this here, you get it in onBindViewHolder
    ArrayList<note> noteArrayList;

    public myadapter(Context context, ArrayList<note> noteArrayList, rv_onClick itemClickListener) {
        this.context = context;
        this.noteArrayList = noteArrayList;
        this.itemClickListener = itemClickListener; // Assign the listener instance
    }

    @NonNull
    @Override
    public myadapter.myviewholder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.notelist,parent,false);
        return new myviewholder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final myadapter.myviewholder holder, @SuppressLint("RecyclerView") final int position) {

        note currentNote = noteArrayList.get(position);
        holder.note_title.setText(currentNote.note_title);
        holder.note_date.setText(currentNote.note_date);

        if (currentNote.getIsPinned()) {
            holder.pin.setImageResource(R.drawable.pinned);
        } else {
            holder.pin.setImageResource(R.drawable.pin2);
        }

        if (currentNote.getIsLocked()) {
            holder.locked.setVisibility(View.VISIBLE);
            holder.locked.setImageResource(R.drawable.locked);
        } else {
            holder.locked.setVisibility(View.GONE);
        }


        holder.card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemClickListener != null) {
                    itemClickListener.onItemClicked(currentNote);
                }
            }
        });

        holder.card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (itemClickListener != null) {
                    itemClickListener.onNoteLongClick(holder.getAdapterPosition()); // Pass position
                    return true; // Consume the long click event
                }
                return false;
            }
        });

        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemClickListener != null) {
                    itemClickListener.onDeleteClick(holder.getAdapterPosition()); // Corrected call
                }
            }
        });

        holder.pin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemClickListener != null) {
                    itemClickListener.onPinClick(holder.getAdapterPosition(), currentNote.getIsPinned());
                }
            }
        });

    }

    @Override
    public int getItemCount() {
        return noteArrayList.size();
    }



    public static class myviewholder extends RecyclerView.ViewHolder {

        TextView note_title, note_date;
        CardView card;
        ImageView delete, pin, locked;

        public myviewholder(@NonNull View itemView) {
            super(itemView);
            note_title = itemView.findViewById(R.id.note_title);
            note_date = itemView.findViewById(R.id.note_date);
            card = itemView.findViewById(R.id.card);
            delete = itemView.findViewById(R.id.delete);
            pin = itemView.findViewById(R.id.pin);
            locked = itemView.findViewById(R.id.locked);

        }
    }
}
