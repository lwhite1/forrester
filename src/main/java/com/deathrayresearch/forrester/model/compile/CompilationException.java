package com.deathrayresearch.forrester.model.compile;

/**
 * Thrown when a model definition cannot be compiled into a runnable model.
 */
public class CompilationException extends RuntimeException {

    private final String elementName;

    public CompilationException(String message, String elementName) {
        super(message);
        this.elementName = elementName;
    }

    public CompilationException(String message, String elementName, Throwable cause) {
        super(message, cause);
        this.elementName = elementName;
    }

    public String getElementName() {
        return elementName;
    }
}
