package com.example.demo.model;

public enum BookCategory {
    FICTION("Fiction"),
    NON_FICTION("Non-Fiction"),
    SCIENCE("Science"),
    HISTORY("History"),
    TECHNOLOGY("Technology"),
    CHILDREN("Children"),
    BIOGRAPHY("Biography"),
    SELF_HELP("Self-Help"),
    OTHER("Other");

    private final String displayName;

    BookCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
