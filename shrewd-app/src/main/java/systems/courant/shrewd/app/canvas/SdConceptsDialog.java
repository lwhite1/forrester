package systems.courant.shrewd.app.canvas;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

/**
 * A help dialog that explains core System Dynamics concepts.
 */
public class SdConceptsDialog extends Stage {

    public SdConceptsDialog() {
        setTitle("SD Concepts");

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        tabs.getTabs().addAll(
                createTab("Overview", overviewText()),
                createTab("Stocks", stocksText()),
                createTab("Flows", flowsText()),
                createTab("Variables", auxiliariesText()),
                createTab("Feedback Loops", feedbackText()),
                createTab("Causal Loops", causalLoopsText()),
                createTab("Simulation", simulationText())
        );

        Scene scene = new Scene(tabs, 640, 520);
        setScene(scene);
    }

    private Tab createTab(String title, TextFlow content) {
        content.setPadding(new Insets(16));
        content.setLineSpacing(4);
        content.setMaxWidth(580);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);

        Tab tab = new Tab(title, scroll);
        return tab;
    }

    private TextFlow overviewText() {
        return new TextFlow(
                bold("System Dynamics (SD)"),
                plain(" is a methodology for understanding the behavior of complex systems over time. "
                        + "It was developed by Jay W. Forrester at MIT in the 1950s.\n\n"),
                plain("SD models capture how "),
                bold("stocks"),
                plain(" (accumulations) change through "),
                bold("flows"),
                plain(" (rates of change), influenced by "),
                bold("feedback loops"),
                plain(" that connect system variables.\n\n"),
                bold("Key principles:\n\n"),
                plain("  - System behavior arises from its structure, particularly its feedback loops\n"),
                plain("  - Stocks create delays and decouple rates of flow, producing disequilibrium dynamics\n"),
                plain("  - Nonlinear relationships are common and produce unexpected behavior\n"),
                plain("  - Policy resistance often results from unrecognized feedback structures\n\n"),
                bold("Stock and flow diagrams"),
                plain(" are the primary notation. They show the physical and information "
                        + "structure of a system as a network of stocks, flows, and variables "
                        + "connected by material pipes and information links.")
        );
    }

    private TextFlow stocksText() {
        return new TextFlow(
                bold("Stocks"),
                plain(" (also called levels or state variables) represent accumulations. "
                        + "They are quantities that persist over time and can only change through "
                        + "their inflows and outflows.\n\n"),
                bold("Examples:\n\n"),
                plain("  - Population (people accumulate through births, deplete through deaths)\n"),
                plain("  - Inventory (goods accumulate through production, deplete through shipments)\n"),
                plain("  - Bank balance (money accumulates through deposits, depletes through withdrawals)\n"),
                plain("  - Water in a bathtub (water accumulates through the faucet, drains through the plug)\n\n"),
                bold("Mathematical definition:\n\n"),
                plain("  Stock(t) = Stock(t0) + integral(Inflow - Outflow, t0, t)\n\n"),
                plain("Or equivalently:\n\n"),
                plain("  d(Stock)/dt = Inflow(t) - Outflow(t)\n\n"),
                bold("Key properties:\n\n"),
                plain("  - Stocks give systems memory and inertia\n"),
                plain("  - They decouple inflows from outflows, allowing them to differ\n"),
                plain("  - They create delays that are central to dynamic behavior\n"),
                plain("  - Stock values define the state of the system at any point in time\n"),
                plain("  - Stocks cannot change instantaneously; they require time to fill or drain")
        );
    }

    private TextFlow flowsText() {
        return new TextFlow(
                bold("Flows"),
                plain(" (also called rates) represent the rates of change that fill or drain stocks. "
                        + "A flow is the quantity per unit time entering or leaving a stock.\n\n"),
                bold("Types of flows:\n\n"),
                plain("  - "),
                bold("Inflow:"),
                plain(" Adds material to a stock (source -> stock)\n"),
                plain("  - "),
                bold("Outflow:"),
                plain(" Removes material from a stock (stock -> sink)\n"),
                plain("  - "),
                bold("Transfer flow:"),
                plain(" Moves material from one stock to another\n\n"),
                bold("Cloud symbols"),
                plain(" represent sources and sinks outside the model boundary. "
                        + "An inflow from a cloud means the material comes from outside the model; "
                        + "an outflow to a cloud means it leaves the model.\n\n"),
                bold("Flow equations"),
                plain(" define how fast material moves. They typically depend on stocks, "
                        + "variables, and constants. For example, a death rate flow might be:\n\n"),
                plain("  deaths = population * mortality_rate\n\n"),
                plain("The diamond symbol on the diagram represents the flow valve, "
                        + "which controls the rate. The thick pipe shows the path material travels.")
        );
    }

    private TextFlow auxiliariesText() {
        return new TextFlow(
                bold("Variables"),
                plain(" (called auxiliaries or converters in the SD literature) are intermediate "
                        + "values that compute results "
                        + "used by flows or other variables. They have no memory of their own; "
                        + "they are purely algebraic functions of other variables.\n\n"),
                bold("Parameters"),
                plain(" are variables whose equation is a single numeric value. They do not "
                        + "change during a simulation run and represent assumptions or policy levers. "
                        + "The sweep, Monte Carlo, and optimization tools automatically detect "
                        + "literal-valued variables as tunable parameters.\n\n"),
                bold("Lookup tables"),
                plain(" define nonlinear relationships as a set of (input, output) data points. "
                        + "The simulation engine interpolates between points. Lookup tables are "
                        + "essential for capturing real-world relationships that are not easily "
                        + "expressed as simple equations.\n\n"),
                bold("Information links"),
                plain(" (dashed arrows) show which variables influence which. If variable A "
                        + "appears in the equation for variable B, there is an information link "
                        + "from A to B. Information links carry information, not material; "
                        + "they do not add to or subtract from any stock.")
        );
    }

    private TextFlow feedbackText() {
        return new TextFlow(
                bold("Feedback loops"),
                plain(" are circular causal chains where a change in a variable propagates "
                        + "through the system and eventually comes back to affect that same variable. "
                        + "They are the source of dynamic behavior in SD models.\n\n"),
                bold("Positive (reinforcing) loops"),
                plain(" amplify change. A change in one direction feeds back to produce "
                        + "more change in the same direction. They generate exponential growth "
                        + "or collapse.\n\n"),
                plain("  Example: More people -> more births -> more people (population growth)\n\n"),
                bold("Negative (balancing) loops"),
                plain(" resist change. A change in one direction feeds back to produce "
                        + "change in the opposite direction. They seek equilibrium or goals.\n\n"),
                plain("  Example: More inventory -> fewer orders -> less inventory (inventory control)\n\n"),
                bold("Loop dominance:"),
                plain(" At any given time, one loop tends to dominate system behavior. "
                        + "Shifts in loop dominance explain why systems often change behavior mode "
                        + "over time (e.g., from growth to decline).\n\n"),
                bold("Delays"),
                plain(" in feedback loops cause oscillations. The longer the delay, "
                        + "the more severe the oscillation. Many real-world problems (boom-bust cycles, "
                        + "overcorrection) result from delayed feedback.")
        );
    }

    private TextFlow causalLoopsText() {
        return new TextFlow(
                bold("Causal Loop Diagrams (CLDs)"),
                plain(" are a qualitative modeling tool used to map the feedback structure "
                        + "of a system before building a full stock-and-flow model.\n\n"),
                bold("CLD Variables"),
                plain(" represent system concepts or quantities. Unlike stocks and variables, "
                        + "CLD variables have no equations or units — they capture the conceptual "
                        + "structure of a system at a high level.\n\n"),
                bold("Causal Links"),
                plain(" (solid arrows) connect variables to show cause-and-effect relationships. "
                        + "Each link has a "),
                bold("polarity"),
                plain(" that indicates the direction of influence:\n\n"),
                plain("  - "),
                bold("+"),
                plain(" (positive): A change in the cause produces a change in the effect "
                        + "in the same direction. More A leads to more B (all else equal).\n"),
                plain("  - "),
                bold("-"),
                plain(" (negative): A change in the cause produces a change in the effect "
                        + "in the opposite direction. More A leads to less B (all else equal).\n"),
                plain("  - "),
                bold("?"),
                plain(" (unknown): The direction of influence is not yet determined.\n\n"),
                bold("Loop Classification:\n\n"),
                plain("A closed chain of causal links forms a feedback loop. Loops are classified by "
                        + "counting the number of negative links in the cycle:\n\n"),
                plain("  - "),
                bold("R (Reinforcing):"),
                plain(" An even number of negative links (including zero). "
                        + "The loop amplifies change — growth or collapse.\n"),
                plain("  - "),
                bold("B (Balancing):"),
                plain(" An odd number of negative links. "
                        + "The loop counteracts change — goal-seeking behavior.\n"),
                plain("  - "),
                bold("? (Indeterminate):"),
                plain(" The loop contains links with unknown polarity, "
                        + "so classification is not possible.\n\n"),
                bold("From CLD to Stock-and-Flow:\n\n"),
                plain("CLD variables can be classified (right-click -> Classify as) into stock-and-flow "
                        + "elements (Stock, Flow, or Variable) as the model is refined "
                        + "from qualitative to quantitative. This preserves the variable's name and "
                        + "position while converting it into an element that can hold equations and units.")
        );
    }

    private TextFlow simulationText() {
        return new TextFlow(
                bold("Simulation"),
                plain(" solves the system of equations over time using numerical integration. "
                        + "The model steps forward in discrete time increments (DT), updating "
                        + "stocks at each step.\n\n"),
                bold("Key simulation parameters:\n\n"),
                plain("  - "),
                bold("Start time:"),
                plain(" When the simulation begins\n"),
                plain("  - "),
                bold("Stop time:"),
                plain(" When the simulation ends\n"),
                plain("  - "),
                bold("DT (time step):"),
                plain(" The size of each integration step. Smaller DT gives more accuracy "
                        + "but takes longer to compute\n\n"),
                bold("Euler integration"),
                plain(" is the simplest method:\n\n"),
                plain("  Stock(t + DT) = Stock(t) + (Inflow(t) - Outflow(t)) * DT\n\n"),
                bold("Analysis techniques:\n\n"),
                plain("  - "),
                bold("Parameter sweep:"),
                plain(" Run the model multiple times with different parameter values "
                        + "to see how behavior changes\n"),
                plain("  - "),
                bold("Monte Carlo:"),
                plain(" Run many simulations with randomly sampled parameters "
                        + "to understand the range of possible outcomes\n"),
                plain("  - "),
                bold("Optimization:"),
                plain(" Find parameter values that minimize or maximize an objective function")
        );
    }

    private Text bold(String content) {
        Text text = new Text(content);
        text.setStyle("-fx-font-weight: bold;");
        return text;
    }

    private Text plain(String content) {
        return new Text(content);
    }
}
