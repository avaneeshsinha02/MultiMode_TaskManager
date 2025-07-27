package com.example.multimodetaskmanager;

import java.util.UUID;

public class Task {
    private String id;
    private String title;
    private long dueDate;
    private int priority;

    public Task(String title, long dueDate, int priority) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.dueDate = dueDate;
        this.priority = priority;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public long getDueDate() { return dueDate; }
    public void setDueDate(long dueDate) { this.dueDate = dueDate; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
}