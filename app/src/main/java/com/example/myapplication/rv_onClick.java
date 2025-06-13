package com.example.myapplication;

public interface rv_onClick {

    void onItemClicked(note note);
    void onDeleteClick(int position);
    void onPinClick(int position, boolean currentPinnedStatus);
    void onNoteLongClick(int position);

    void onRestoreNote(String noteId, int position);

    void onPermanentlyDeleteNote(String noteId, int position);

    void onLockNote(String noteId, int position);

    void onUnlockNote(String noteId, int position);

    void onMoveToBin(String noteId, int position);

    void onAddToFolder(String noteId, String noteType, int position);

    void onAddToFolder(String noteId, int position);

    void onRemoveFromFolder(String noteId, int position);

    void onRestoreNote(String noteId, String noteType);

    void onPermanentlyDeleteNote(String noteId, String noteType);
}
