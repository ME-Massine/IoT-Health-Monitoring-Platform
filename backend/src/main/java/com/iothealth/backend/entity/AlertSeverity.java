package com.iothealth.backend.entity;

public enum AlertSeverity {
    WARNING("Requires attention"),
    CRITICAL("Requires immediate action");

    private final String description;

    AlertSeverity(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}