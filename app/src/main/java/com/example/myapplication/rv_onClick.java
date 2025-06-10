package com.example.myapplication;

public interface rv_onClick {

    void onItemClicked(note note);
    void onDeleteClick(int position);
    void onPinClick(int position, boolean currentPinnedStatus);
    void onNoteLongClick(int position);
}
