package com.deathrayresearch.forrester.app.canvas;

import java.util.Objects;

/**
 * Identifies an info link connection by its source and target element names.
 * Used for hover and selection state tracking.
 */
public record ConnectionId(String from, String to) {

    public ConnectionId {
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(to, "to must not be null");
    }
}
