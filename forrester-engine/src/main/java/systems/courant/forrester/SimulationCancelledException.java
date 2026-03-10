package systems.courant.forrester;

/**
 * Thrown when a simulation is cancelled via thread interruption.
 */
public class SimulationCancelledException extends RuntimeException {

    public SimulationCancelledException(String message) {
        super(message);
    }
}
