package systems.courant.sd.io;

/**
 * Thrown when a CSV output file cannot be created or written.
 */
public class CsvOutputException extends RuntimeException {

    /**
     * Creates a CSV output exception with a message.
     *
     * @param message the error description
     */
    public CsvOutputException(String message) {
        super(message);
    }

    /**
     * Creates a CSV output exception with a message and underlying cause.
     *
     * @param message the error description
     * @param cause   the underlying cause
     */
    public CsvOutputException(String message, Throwable cause) {
        super(message, cause);
    }
}
