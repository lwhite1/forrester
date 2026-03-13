package systems.courant.sd;

/**
 * Thrown when a simulation is cancelled via thread interruption.
 */
public class SimulationCancelledException extends RuntimeException {

    public SimulationCancelledException(String message) {
        super(message);
    }
}
