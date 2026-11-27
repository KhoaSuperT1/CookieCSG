package com.cookie.csg.model;

import java.util.Locale;

public enum WeaponMode {
    DEFAULT("default"),
    MODEL("model");

    private final String configValue;

    WeaponMode(String configValue) {
        this.configValue = configValue;
    }

    public String getConfigValue() {
        return configValue;
    }

    public static WeaponMode fromString(String raw, WeaponMode fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback != null ? fallback : MODEL;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "default", "vanilla", "simple" -> DEFAULT;
            case "model", "premium", "animated" -> MODEL;
            default -> fallback != null ? fallback : MODEL;
        };
    }
}
