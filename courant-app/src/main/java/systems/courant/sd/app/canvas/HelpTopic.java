package systems.courant.sd.app.canvas;

/**
 * Enumeration of all context-sensitive help topics.
 * Each topic has a display name and a category used for grouping in the help viewer sidebar.
 */
public enum HelpTopic {

    // Getting Started
    OVERVIEW("Overview", "Getting Started"),

    // Elements
    STOCK("Stocks", "Elements"),
    FLOW("Flows", "Elements"),
    VARIABLE("Variables", "Elements"),
    LOOKUP("Lookup Tables", "Elements"),
    MODULE("Modules", "Elements"),
    CLD_VARIABLE("CLD Variables", "Elements"),
    COMMENT("Comments", "Elements"),

    // Equations
    EXPRESSION_LANGUAGE("Expression Language", "Equations"),

    // Simulation
    SIMULATION_SETTINGS("Simulation Settings", "Simulation"),
    SIMULATION_RESULTS("Simulation Results", "Simulation"),

    // Analysis
    PARAMETER_SWEEP("Parameter Sweep", "Analysis"),
    MONTE_CARLO("Monte Carlo Analysis", "Analysis"),
    OPTIMIZATION("Optimization", "Analysis"),
    CALIBRATION("Calibration", "Analysis"),
    MULTI_SWEEP("Multi-Parameter Sweep", "Analysis"),

    // Structure
    FEEDBACK_LOOPS("Feedback Loops", "Structure"),
    CAUSAL_LOOPS("Causal Loop Diagrams", "Structure"),
    CAUSAL_TRACE("Causal Tracing", "Structure"),
    MODULE_PORTS("Module Ports & Bindings", "Structure");

    private final String displayName;
    private final String category;

    HelpTopic(String displayName, String category) {
        this.displayName = displayName;
        this.category = category;
    }

    public String displayName() {
        return displayName;
    }

    public String category() {
        return category;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
