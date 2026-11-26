package com.cookie.csg.model;

import java.util.Locale;

public enum FireMode {
    SINGLE("SINGLE"),
    SEMI_AUTO("SEMI-AUTO"),
    AUTO("AUTO");

    private final String displayName;

    FireMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static FireMode fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "SINGLE" -> SINGLE;
            case "SEMI", "SEMI_AUTO", "SEMIAUTO", "BURST" -> SEMI_AUTO;
            case "AUTO", "FULL_AUTO" -> AUTO;
            default -> null;
        };
    }
}
