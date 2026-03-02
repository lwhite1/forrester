package com.deathrayresearch.forrester.model.expr;

/**
 * Thrown when an expression string cannot be parsed.
 */
public class ParseException extends RuntimeException {

    private final int position;

    public ParseException(String message, int position) {
        super(message + " (at position " + position + ")");
        this.position = position;
    }

    public ParseException(String message, int position, Throwable cause) {
        super(message + " (at position " + position + ")", cause);
        this.position = position;
    }

    public int getPosition() {
        return position;
    }
}
