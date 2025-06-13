package com.example.myapplication;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class TodoAdapter extends RecyclerView.Adapter<TodoAdapter.TodoViewHolder> {

    private Context context;
    private ArrayList<TodoTask> todoList;
    private TodoItemClickListener itemClickListener;

    public interface TodoItemClickListener {
        void onRadioClick(int position, TodoTask task);
        void onEditClick(int position, TodoTask task);
        void onTodoLongClick(int position, TodoTask task);
    }

    public TodoAdapter(Context context, ArrayList<TodoTask> todoList, TodoItemClickListener itemClickListener) {
        this.context = context;
        this.todoList = todoList;
        this.itemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public TodoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.todolist, parent, false);
        return new TodoViewHolder(view, itemClickListener, todoList);
    }

    @Override
    public void onBindViewHolder(@NonNull TodoViewHolder holder, int position) {
        TodoTask currentTask = todoList.get(position);

        holder.taskTextView.setText(currentTask.getTaskName());

        if (currentTask.getIsCompleted()) {
            holder.radioImageView.setImageResource(R.drawable.completedtodo);
            holder.taskTextView.setPaintFlags(holder.taskTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.radioImageView.setImageResource(R.drawable.radio);
            holder.taskTextView.setPaintFlags(holder.taskTextView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }
    }

    @Override
    public int getItemCount() {
        return todoList.size();
    }

    public static class TodoViewHolder extends RecyclerView.ViewHolder {
        TextView taskTextView;
        ImageView radioImageView;
        ImageView editImageView;

        public TodoViewHolder(@NonNull View itemView, final TodoItemClickListener listener, final ArrayList<TodoTask> todoList) {
            super(itemView);
            taskTextView = itemView.findViewById(R.id.to_do);
            radioImageView = itemView.findViewById(R.id.radio);
            editImageView = itemView.findViewById(R.id.edit);

            radioImageView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onRadioClick(getAdapterPosition(), todoList.get(getAdapterPosition()));
                }
            });

            editImageView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onEditClick(getAdapterPosition(), todoList.get(getAdapterPosition()));
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onTodoLongClick(getAdapterPosition(), todoList.get(getAdapterPosition()));
                    return true;
                }
                return false;
            });
        }
    }
}