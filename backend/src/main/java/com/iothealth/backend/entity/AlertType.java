package com.iothealth.backend.entity;

public enum AlertType {
    HIGH_HEART_RATE("Heart rate is above the safe threshold"),
    LOW_HEART_RATE("Heart rate is below the safe threshold"),
    HIGH_TEMPERATURE("Body temperature is above the safe threshold"),
    LOW_TEMPERATURE("Body temperature is below the safe threshold"),
    LOW_SPO2("Oxygen saturation is below the safe threshold");

    private final String description;

    AlertType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}