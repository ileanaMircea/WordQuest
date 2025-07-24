package com.example.licentfront.models;

import java.util.HashMap;
import java.util.Map;

public class Deck {
    private String id;
    private String title;
    private String language;
    private int day;

    // Progress tracking fields
    private long lastPracticedDate = 0;
    private int totalPracticeSessions = 0;
    private Map<String, Integer> dailyPracticeCount = new HashMap<>();
    private long createdDate;
    private int totalFlashcards = 0;
    private double averageDifficulty = 0.0; // Average difficulty of practice sessions
    private long totalPracticeTime = 0; // Total time spent practicing this deck

    public Deck() {
        this.createdDate = System.currentTimeMillis();
    }

    public Deck(String id, String title, String language, int day) {
        this.id = id;
        this.title = title;
        this.language = language;
        this.day = day;
        this.createdDate = System.currentTimeMillis();
    }

    // Existing getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public int getDay() { return day; }
    public void setDay(int day) { this.day = day; }

    // Progress tracking getters and setters
    public long getLastPracticedDate() { return lastPracticedDate; }
    public void setLastPracticedDate(long lastPracticedDate) { this.lastPracticedDate = lastPracticedDate; }

    public int getTotalPracticeSessions() { return totalPracticeSessions; }
    public void setTotalPracticeSessions(int totalPracticeSessions) { this.totalPracticeSessions = totalPracticeSessions; }

    public Map<String, Integer> getDailyPracticeCount() { return dailyPracticeCount; }
    public void setDailyPracticeCount(Map<String, Integer> dailyPracticeCount) { this.dailyPracticeCount = dailyPracticeCount; }

    public long getCreatedDate() { return createdDate; }
    public void setCreatedDate(long createdDate) { this.createdDate = createdDate; }

    public int getTotalFlashcards() { return totalFlashcards; }
    public void setTotalFlashcards(int totalFlashcards) { this.totalFlashcards = totalFlashcards; }

    public double getAverageDifficulty() { return averageDifficulty; }
    public void setAverageDifficulty(double averageDifficulty) { this.averageDifficulty = averageDifficulty; }

    public long getTotalPracticeTime() { return totalPracticeTime; }
    public void setTotalPracticeTime(long totalPracticeTime) { this.totalPracticeTime = totalPracticeTime; }

    @Override
    public String toString() {
        return "Deck{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", language='" + language + '\'' +
                ", day=" + day +
                ", totalPracticeSessions=" + totalPracticeSessions +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Deck deck = (Deck) obj;
        return id != null ? id.equals(deck.id) : deck.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}