package systems.courant.shrewd.model.compile;

/**
 * Thrown when a model definition cannot be compiled into a runnable model.
 */
public class CompilationException extends RuntimeException {

    private final String elementName;

    /**
     * Creates a compilation exception with a message and the name of the problematic element.
     *
     * @param message     the error description
     * @param elementName the model element that caused the error
     */
    public CompilationException(String message, String elementName) {
        super(message);
        this.elementName = elementName;
    }

    /**
     * Creates a compilation exception with a message, element name, and underlying cause.
     *
     * @param message     the error description
     * @param elementName the model element that caused the error
     * @param cause       the underlying cause
     */
    public CompilationException(String message, String elementName, Throwable cause) {
        super(message, cause);
        this.elementName = elementName;
    }

    /**
     * Returns the name of the model element that caused the compilation error.
     */
    public String getElementName() {
        return elementName;
    }
}
