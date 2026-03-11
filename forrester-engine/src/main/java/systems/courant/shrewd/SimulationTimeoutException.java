package systems.courant.shrewd;

/**
 * Thrown when a simulation exceeds its configured wall-clock timeout.
 */
public class SimulationTimeoutException extends RuntimeException {

    public SimulationTimeoutException(String message) {
        super(message);
    }
}
