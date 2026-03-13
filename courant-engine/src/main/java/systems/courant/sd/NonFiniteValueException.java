package systems.courant.sd;

/**
 * Thrown in strict mode when a simulation produces a non-finite value (NaN or Infinity)
 * in a stock, flow, or auxiliary calculation.
 */
public class NonFiniteValueException extends RuntimeException {

    public NonFiniteValueException(String message) {
        super(message);
    }
}
