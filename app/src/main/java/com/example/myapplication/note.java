package com.example.myapplication;

public class note {

    String note_title, note_date, note_content, note_id, folder_id, hashedPin;
    boolean isPinned, isLocked;

    public note(){}

    public note(String hashedPin, boolean isLocked, boolean isPinned, String note_content, String note_date, String note_id, String note_title, String folder_id) {
        this.hashedPin = hashedPin;
        this.isLocked = isLocked;
        this.isPinned = isPinned;
        this.note_content = note_content;
        this.note_date = note_date;
        this.note_id = note_id;
        this.note_title = note_title;
        this.folder_id = folder_id;
    }

    public String getHashedPin() {
        return hashedPin;
    }

    public void setHashedPin(String hashedPin) {
        this.hashedPin = hashedPin;
    }

    public boolean getIsLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public boolean getIsPinned() {
        return isPinned;
    }

    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }

    public String getNote_content() {
        return note_content;
    }

    public void setNote_content(String note_content) {
        this.note_content = note_content;
    }

    public String getNote_id() {
        return note_id;
    }

    public void setNote_id(String note_id) {
        this.note_id = note_id;
    }

    public String getNote_date() {
        return note_date;
    }

    public void setNote_date(String note_date) {
        this.note_date = note_date;
    }

    public String getNote_title() {
        return note_title;
    }

    public void setNote_title(String note_title) {
        this.note_title = note_title;
    }

    public String getFolder_id() {
        return folder_id;
    }

    public void setFolder_id(String folder_id) {
        this.folder_id = folder_id;
    }
}
