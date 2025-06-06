package com.example.myapplication;

public class rv_adapter {

    String title, date;

    public rv_adapter(String date, String title) {
        this.date = date;
        this.title = title;
    }

    public String getDate() {
        return date;
    }

    public String getTitle() {
        return title;
    }
}
