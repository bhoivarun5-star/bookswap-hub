package com.example.demo.model;

public enum BookCondition {
    NEW("New"),
    LIKE_NEW("Like New"),
    OLD("Old");

    private final String displayName;

    BookCondition(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
