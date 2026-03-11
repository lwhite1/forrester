package systems.courant.forrester.model.expr;

/**
 * Thrown when an expression string cannot be parsed.
 */
public class ParseException extends RuntimeException {

    private final int position;

    /**
     * Creates a parse exception with the error message and character position.
     *
     * @param message  the error description
     * @param position the zero-based character position in the input where the error occurred
     */
    public ParseException(String message, int position) {
        super(message + " (at position " + position + ")");
        this.position = position;
    }

    /**
     * Creates a parse exception with the error message, character position, and underlying cause.
     *
     * @param message  the error description
     * @param position the zero-based character position in the input where the error occurred
     * @param cause    the underlying cause
     */
    public ParseException(String message, int position, Throwable cause) {
        super(message + " (at position " + position + ")", cause);
        this.position = position;
    }

    /**
     * Returns the zero-based character position in the input where the error occurred.
     */
    public int getPosition() {
        return position;
    }
}
