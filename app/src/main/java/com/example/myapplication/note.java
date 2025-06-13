package com.example.myapplication;

import java.util.Date;
import java.util.Objects;

public class note {

    String note_title, note_date, note_content, note_id, folder_id, hashedPin, deleted_date, imageUrl, type;
    boolean isPinned, isLocked, isDeleted;
    Date timestamp;

    public note(){}

    public note(String imageUrl, String type, String folder_id, String deleted_date, String hashedPin, boolean isDeleted, boolean isLocked, boolean isPinned, String note_content, String note_date, String note_id, String note_title, Date timestamp) {
        this.folder_id = folder_id;
        this.deleted_date = deleted_date;
        this.hashedPin = hashedPin;
        this.isDeleted = isDeleted;
        this.isLocked = isLocked;
        this.isPinned = isPinned;
        this.note_content = note_content;
        this.note_date = note_date;
        this.note_id = note_id;
        this.note_title = note_title;
        this.imageUrl = imageUrl;
        this.type = type;
        this.timestamp = timestamp;
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

    public void setIsLocked(boolean locked) {
        isLocked = locked;
    }

    public boolean getIsPinned() {
        return isPinned;
    }

    public void setIsPinned(boolean pinned) {
        isPinned = pinned;
    }

    public boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(boolean deleted) {
        isDeleted = deleted;
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

    public String getDeleted_date() {
        return deleted_date;
    }

    public void setDeleted_date(String deleted_date) {
        this.deleted_date = deleted_date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        note note = (note) o;
        return Objects.equals(note_id, note.note_id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(note_id);
    }

}
