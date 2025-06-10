// com.example.myapplication/Folder.java
package com.example.myapplication;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Folder {
    private String folder_id;
    private String folder_name;
    private int notesCount = 0;
    private @ServerTimestamp Date creation_date; // Optional: to track creation time

    public Folder() {
        // Public no-argument constructor needed for Firestore
    }

    public Folder(String folder_name) {
        this.folder_name = folder_name;
        // folder_id will be set by Firestore document ID
    }

    @Exclude // Exclude from direct Firestore mapping, as it's the doc ID
    public String getFolder_id() {
        return folder_id;
    }

    public void setFolder_id(String folder_id) {
        this.folder_id = folder_id;
    }

    public String getFolder_name() {
        return folder_name;
    }

    public void setFolder_name(String folder_name) {
        this.folder_name = folder_name;
    }

    public Date getCreation_date() {
        return creation_date;
    }

    public void setCreation_date(Date creation_date) {
        this.creation_date = creation_date;
    }

    public int getNotesCount() {
        return notesCount;
    }

    public void setNotesCount(int notesCount) {
        this.notesCount = notesCount;
    }
}