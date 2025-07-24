package com.example.licentfront.models;

public class Flashcard {
    private String front;
    private String back;
    private boolean isFlipped=false;
    private String id;
    private int interval; //days until next review
    private double easeFactor;
    private long nextReviewTime;
    private int repetition;
    private String exampleSentence;


    public Flashcard(String front, String back) {
        this.front = front;
        this.back = back;
        this.isFlipped=false;
        this.interval = 1;
        this.easeFactor = 2.5;
        this.nextReviewTime = System.currentTimeMillis(); //review now
        this.repetition = 0;
    }
    public Flashcard(String front, String back, String example) {
        this.front = front;
        this.back = back;
        this.isFlipped=false;
        this.interval = 1;
        this.easeFactor = 2.5;
        this.nextReviewTime = System.currentTimeMillis();
        this.repetition = 0;
        this.exampleSentence=example;
    }

    public String getFront() {
        return front;
    }

    public void setFront(String front) {
        this.front = front;
    }

    public String getBack() {
        return back;
    }

    public void setBack(String back) {
        this.back = back;
    }

    public int getRepetition() {
        return repetition;
    }
    public void setRepetition(int repetition) {
        this.repetition = repetition;
    }
    public boolean isFlipped() {
        return isFlipped;
    }

    public void setFlipped(boolean flipped) {
        isFlipped = flipped;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public double getEaseFactor() {
        return easeFactor;
    }

    public void setEaseFactor(double easeFactor) {
        this.easeFactor = easeFactor;
    }

    public long getNextReviewTime() {
        return nextReviewTime;
    }

    public void setNextReviewTime(long nextReviewTime) {
        this.nextReviewTime = nextReviewTime;
    }
    public Flashcard() {
    }

}
