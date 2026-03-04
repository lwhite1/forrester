package com.deathrayresearch.forrester.app.canvas;

import java.time.LocalDateTime;

/**
 * A single entry in the activity log, recording a user action with a timestamp.
 *
 * @param timestamp when the action occurred
 * @param type      category of the action (e.g. "edit", "simulation", "file")
 * @param message   human-readable description
 */
public record ActivityLogEntry(LocalDateTime timestamp, String type, String message) {

    public ActivityLogEntry {
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp must not be null");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type must not be blank");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}
