package com.example.licentfront.models;

import java.util.List;

public class Book {
    private String id;
    private String title;
    private String language;
    private String content;
    private List<String> pages;
    private String author;

    // Progress tracking fields
    private int currentPage = 0;
    private int totalPages = 0;
    private boolean isCompleted = false;
    private long lastReadTime = 0;
    private String originalBookId; // For user library references

    public Book() {}

    public Book(String id, String title, String language, String content) {
        this.id = id;
        this.title = title;
        this.language = language;
        this.content = content;
        if (pages != null) {
            this.totalPages = pages.size();
        }
    }

    // Existing getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public int getCurrentPage() {
        return currentPage;
    }
    public int getTotalPages() {
        return totalPages;
    }


}