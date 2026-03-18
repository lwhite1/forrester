package systems.courant.sd.app.canvas;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * Provides help content for each {@link HelpTopic}.
 * Content is constructed lazily as JavaFX {@link Node} trees suitable for display
 * in a {@link ScrollPane}.
 */
public final class HelpContent {

    private HelpContent() {
    }

    /**
     * Returns the help content node for the given topic.
     *
     * @param topic the help topic
     * @return a scrollable node containing the help content
     */
    public static Node forTopic(HelpTopic topic) {
        TextFlow content = switch (topic) {
            case OVERVIEW -> overviewContent();
            case STOCK -> stockContent();
            case FLOW -> flowContent();
            case VARIABLE -> variableContent();
            case LOOKUP -> lookupContent();
            case MODULE -> moduleContent();
            case CLD_VARIABLE -> cldVariableContent();
            case COMMENT -> commentContent();
            case EXPRESSION_LANGUAGE -> expressionLanguageContent();
            case SIMULATION_SETTINGS -> simulationSettingsContent();
            case SIMULATION_RESULTS -> simulationResultsContent();
            case PARAMETER_SWEEP -> parameterSweepContent();
            case MONTE_CARLO -> monteCarloContent();
            case OPTIMIZATION -> optimizationContent();
            case CALIBRATION -> calibrationContent();
            case MULTI_SWEEP -> multiSweepContent();
            case VALIDATION -> validationContent();
            case EXTREME_CONDITION -> extremeConditionContent();
            case COLUMN_MAPPING -> columnMappingContent();
            case FEEDBACK_LOOPS -> feedbackLoopsContent();
            case CAUSAL_LOOPS -> causalLoopsContent();
            case CAUSAL_TRACE -> causalTraceContent();
            case MODULE_PORTS -> modulePortsContent();
        };
        content.setPadding(new Insets(16));
        content.setLineSpacing(4);
        content.setMaxWidth(520);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        return scroll;
    }

    // --- Content builders ---

    private static TextFlow overviewContent() {
        return new TextFlow(
                bold("System Dynamics (SD)"),
                plain(" is a methodology for understanding the behavior of complex systems over time. "
                        + "SD models capture how stocks (accumulations) change through flows (rates of "
                        + "change), influenced by feedback loops.\n\n"),
                bold("Key concepts:\n\n"),
                plain("  \u2022 "), bold("Stocks"), plain(" \u2014 accumulations that persist over time\n"),
                plain("  \u2022 "), bold("Flows"), plain(" \u2014 rates of change that fill or drain stocks\n"),
                plain("  \u2022 "), bold("Variables"), plain(" \u2014 intermediate calculations and parameters\n"),
                plain("  \u2022 "), bold("Feedback Loops"),
                plain(" \u2014 circular causal chains that drive behavior\n\n"),
                bold("Getting started:\n\n"),
                plain("  1. Place stocks to represent accumulations in your system\n"),
                plain("  2. Add flows to connect stocks and represent rates of change\n"),
                plain("  3. Define variables and parameters that influence the flows\n"),
                plain("  4. Write equations for each flow and variable\n"),
                plain("  5. Run the simulation to see how your system behaves over time\n\n"),
                plain("Press "), bold("F1"), plain(" at any time for context-sensitive help based on what "
                        + "you're currently working on.")
        );
    }

    private static TextFlow stockContent() {
        return new TextFlow(
                bold("Stocks"),
                plain(" (levels, state variables) represent accumulations. They persist over time and "
                        + "change only through their inflows and outflows.\n\n"),
                bold("Examples:\n"),
                plain("  \u2022 Population (births add, deaths remove)\n"),
                plain("  \u2022 Inventory (production adds, shipments remove)\n"),
                plain("  \u2022 Bank balance (deposits add, withdrawals remove)\n\n"),
                bold("Properties:\n"),
                plain("  \u2022 "), bold("Name"), plain(" \u2014 unique identifier for the stock\n"),
                plain("  \u2022 "), bold("Initial Value"),
                plain(" \u2014 the starting value (number or expression)\n"),
                plain("  \u2022 "), bold("Unit"), plain(" \u2014 the unit of measurement\n"),
                plain("  \u2022 "), bold("Non-negative"),
                plain(" \u2014 when enabled, the stock cannot go below zero\n\n"),
                bold("Mathematical definition:\n\n"),
                mono("  Stock(t) = Stock(t\u2080) + \u222b(Inflow \u2212 Outflow) dt\n\n"),
                bold("Key principles:\n"),
                plain("  \u2022 Stocks give systems memory and inertia\n"),
                plain("  \u2022 They decouple inflows from outflows\n"),
                plain("  \u2022 They create delays central to dynamic behavior\n"),
                plain("  \u2022 Stock values define the system state at any point in time")
        );
    }

    private static TextFlow flowContent() {
        return new TextFlow(
                bold("Flows"),
                plain(" represent rates of change that fill or drain stocks. A flow is the quantity "
                        + "per unit time entering or leaving a stock.\n\n"),
                bold("Types:\n"),
                plain("  \u2022 "), bold("Inflow"), plain(" \u2014 adds material to a stock (source \u2192 stock)\n"),
                plain("  \u2022 "), bold("Outflow"), plain(" \u2014 removes material (stock \u2192 sink)\n"),
                plain("  \u2022 "), bold("Transfer"), plain(" \u2014 moves material between two stocks\n\n"),
                bold("Cloud symbols"),
                plain(" represent sources and sinks outside the model boundary.\n\n"),
                bold("Properties:\n"),
                plain("  \u2022 "), bold("Name"), plain(" \u2014 unique identifier\n"),
                plain("  \u2022 "), bold("Equation"),
                plain(" \u2014 formula defining the flow rate (e.g., Population * Death_Rate)\n"),
                plain("  \u2022 "), bold("Unit"), plain(" \u2014 unit of measurement\n\n"),
                plain("The diamond symbol on the diagram represents the flow valve. "
                        + "The thick pipe shows the path material travels.")
        );
    }

    private static TextFlow variableContent() {
        return new TextFlow(
                bold("Variables"),
                plain(" (variables, converters) are intermediate calculations used by flows or other "
                        + "variables. They have no memory \u2014 they are purely algebraic functions.\n\n"),
                bold("Parameters"),
                plain(" are variables with a single numeric value. They represent assumptions or policy "
                        + "levers and are automatically detected as tunable parameters by the sweep, "
                        + "Monte Carlo, and optimization tools.\n\n"),
                bold("Properties:\n"),
                plain("  \u2022 "), bold("Name"), plain(" \u2014 unique identifier\n"),
                plain("  \u2022 "), bold("Equation"), plain(" \u2014 formula or constant value\n"),
                plain("  \u2022 "), bold("Unit"), plain(" \u2014 unit of measurement\n\n"),
                bold("Information links"),
                plain(" (dashed arrows) show which variables influence which. They carry information, "
                        + "not material.")
        );
    }

    private static TextFlow lookupContent() {
        return new TextFlow(
                bold("Lookup Tables"),
                plain(" define nonlinear relationships as a set of (input, output) data points. "
                        + "The simulation engine interpolates between points.\n\n"),
                bold("When to use a lookup:\n"),
                plain("  \u2022 The relationship between two quantities is nonlinear\n"),
                plain("  \u2022 The relationship is empirical (from data) rather than theoretical\n"),
                plain("  \u2022 The relationship is too complex for a simple equation\n\n"),
                bold("Defining points:\n"),
                plain("  \u2022 Click on the graph to add points\n"),
                plain("  \u2022 Drag points to adjust their position\n"),
                plain("  \u2022 Right-click a point to delete it\n"),
                plain("  \u2022 Set X and Y ranges to control the axis bounds\n\n"),
                bold("Using a lookup in an equation:\n\n"),
                mono("  Lookup_Name(input_expression)\n\n"),
                plain("The lookup returns the interpolated Y value for the given X input.")
        );
    }

    private static TextFlow moduleContent() {
        return new TextFlow(
                bold("Modules"),
                plain(" encapsulate a sub-model as a reusable component. They allow you to organize "
                        + "large models into manageable pieces and reuse common structures.\n\n"),
                bold("Working with modules:\n"),
                plain("  \u2022 "), bold("Double-click"),
                plain(" a module to drill into its internal structure\n"),
                plain("  \u2022 "), bold("Breadcrumb bar"),
                plain(" at the top shows your navigation path\n"),
                plain("  \u2022 Press "), bold("Escape"),
                plain(" or click the breadcrumb to navigate back\n\n"),
                bold("Ports"),
                plain(" define the module's interface \u2014 inputs the module receives from the parent "
                        + "and outputs it provides back. See "), bold("Module Ports & Bindings"),
                plain(" for details.\n\n"),
                bold("Instances:\n"),
                plain("Multiple instances of the same module type can be placed on the canvas. "
                        + "Each instance can have different input bindings.")
        );
    }

    private static TextFlow cldVariableContent() {
        return new TextFlow(
                bold("CLD Variables"),
                plain(" are used in Causal Loop Diagrams (CLDs) to represent system concepts. "
                        + "Unlike stocks and variables, CLD variables have no equations or units \u2014 "
                        + "they capture conceptual structure.\n\n"),
                bold("Causal Links"),
                plain(" connect CLD variables to show cause-and-effect. Each link has a polarity:\n"),
                plain("  \u2022 "), bold("+"), plain(" (positive) \u2014 same-direction influence\n"),
                plain("  \u2022 "), bold("\u2212"), plain(" (negative) \u2014 opposite-direction influence\n"),
                plain("  \u2022 "), bold("?"), plain(" (unknown) \u2014 direction not yet determined\n\n"),
                bold("Classification:\n"),
                plain("CLD variables can be classified (right-click \u2192 Classify as) into stock-and-flow "
                        + "elements as the model is refined from qualitative to quantitative.")
        );
    }

    private static TextFlow commentContent() {
        return new TextFlow(
                bold("Comments"),
                plain(" are text annotations placed on the canvas. They have no effect on the model "
                        + "or simulation \u2014 they exist solely for documentation.\n\n"),
                bold("Usage:\n"),
                plain("  \u2022 Place a comment to explain a section of the model\n"),
                plain("  \u2022 Document assumptions or data sources\n"),
                plain("  \u2022 Leave notes for collaborators\n\n"),
                plain("Comments can be resized by dragging their edges and repositioned by dragging.")
        );
    }

    private static TextFlow expressionLanguageContent() {
        return new TextFlow(
                bold("Expressions"),
                plain(" are mathematical formulas used in flow and variable equations.\n\n"),
                bold("Variable references:\n"),
                mono("  Birth_Rate * Population\n"),
                plain("Spaces are converted to underscores. Use backtick-quoted names for readability:\n"),
                mono("  `Birth Rate` * Population\n\n"),
                bold("Special variables:\n"),
                plain("  \u2022 "), mono("TIME"), plain(" \u2014 current simulation time\n"),
                plain("  \u2022 "), mono("DT"), plain(" \u2014 simulation time step size\n\n"),
                bold("Operators:\n"),
                plain("  \u2022 Arithmetic: "), mono("+ - * / % **\n"),
                plain("  \u2022 Comparison: "), mono("== != < <= > >=\n"),
                plain("  \u2022 Logical: "), mono("and  or  not\n\n"),
                bold("Conditional:\n"),
                mono("  IF(condition, then_value, else_value)\n\n"),
                bold("Common functions:\n"),
                plain("  \u2022 Math: "), mono("ABS, MIN, MAX, SQRT, EXP, LN, SIN, COS\n"),
                plain("  \u2022 SD: "), mono("SMOOTH, DELAY3, STEP, RAMP, PULSE, FORECAST, TREND\n\n"),
                plain("Open "), bold("Help \u2192 Expression Language"),
                plain(" for the complete reference.")
        );
    }

    private static TextFlow simulationSettingsContent() {
        return new TextFlow(
                bold("Simulation Settings"),
                plain(" control how the model is simulated.\n\n"),

                bold("Time Step\n"),
                plain("The base unit of time for the simulation model. This defines what "
                        + "\"one unit of time\" means in your equations and determines the "
                        + "units for flows and rates. For example, if Time Step is Year, "
                        + "a flow value of 10 means 10 per year.\n\n"),

                bold("DT (Delta Time)\n"),
                plain("The integration step size, expressed as a fraction of the Time Step. "
                        + "A DT of 1 means each integration step advances by one full Time Step "
                        + "unit. A DT of 0.25 means each step advances by one quarter of a "
                        + "Time Step. Smaller values give more accuracy but take longer to "
                        + "compute. For example, with Time Step = Day and DT = 0.5, each "
                        + "integration step advances 0.5 days.\n\n"),

                bold("Save Per\n"),
                plain("How often results are recorded, measured in integration steps (DT steps). "
                        + "A Save Per of 1 records every integration step. A Save Per of 10 "
                        + "records every 10th integration step. For example, with DT = 0.25 "
                        + "and Save Per = 4, results are recorded once per Time Step.\n\n"),

                bold("Duration\n"),
                plain("How long the simulation runs, measured in Duration Units. "
                        + "For example, a Duration of 100 with Duration Unit set to Year "
                        + "means the simulation runs for 100 simulated years.\n\n"),

                bold("Duration Unit\n"),
                plain("The unit for the Duration value. This determines the total simulated "
                        + "time span. Common choices include Day, Week, Month, and Year.\n\n"),

                bold("Strict Mode\n"),
                plain("When enabled, the simulation fails immediately if any variable "
                        + "produces NaN or Infinity. When disabled, the simulator reverts "
                        + "to the previous value and continues. Enable this during model "
                        + "development to catch errors early.\n\n"),

                bold("Euler integration:\n\n"),
                mono("  Stock(t + DT) = Stock(t) + (Inflow(t) \u2212 Outflow(t)) \u00d7 DT\n\n"),
                bold("Tips:\n"),
                plain("  \u2022 If results look unstable, try reducing DT\n"),
                plain("  \u2022 For oscillating systems, DT should be 1/4 to 1/8 of the shortest period\n"),
                plain("  \u2022 Use Ctrl+R to run the simulation quickly")
        );
    }

    private static TextFlow simulationResultsContent() {
        return new TextFlow(
                bold("Simulation Results"),
                plain(" are displayed in the Dashboard panel after running a simulation.\n\n"),
                bold("Time-series charts"),
                plain(" show how each variable changes over time. Click a variable name "
                        + "in the legend to select it on the canvas.\n\n"),
                bold("Ghost runs"),
                plain(" overlay previous simulation results for comparison. Up to 5 recent "
                        + "runs are retained, each shown in a different color.\n\n"),
                bold("Stale indicator:\n"),
                plain("An amber banner appears when the model has been modified since the last run. "
                        + "Click the re-run link to update results.\n\n"),
                bold("Reference data:\n"),
                plain("Import CSV reference data to overlay on simulation charts for calibration "
                        + "and validation.")
        );
    }

    private static TextFlow parameterSweepContent() {
        return new TextFlow(
                bold("Parameter Sweep"),
                plain(" runs the model multiple times, varying one parameter across a range "
                        + "of values.\n\n"),
                bold("Setup:\n"),
                plain("  1. Select the parameter to sweep\n"),
                plain("  2. Set the minimum and maximum values\n"),
                plain("  3. Set the number of steps\n"),
                plain("  4. Select the output variable(s) to observe\n\n"),
                bold("Results"),
                plain(" show a family of curves \u2014 one for each parameter value \u2014 allowing you "
                        + "to see how sensitive the system is to that parameter.\n\n"),
                plain("Only literal-valued variables (parameters) appear in the sweep dialog. "
                        + "Variables with formulas that depend on other elements are not sweepable.")
        );
    }

    private static TextFlow monteCarloContent() {
        return new TextFlow(
                bold("Monte Carlo Analysis"),
                plain(" runs many simulations with randomly sampled parameters to understand "
                        + "the range of possible outcomes.\n\n"),
                bold("Setup:\n"),
                plain("  1. Select parameters and their distributions (uniform, normal, triangular)\n"),
                plain("  2. Set the number of iterations (more = better statistics)\n"),
                plain("  3. Select the output variable(s) to observe\n\n"),
                bold("Results:\n"),
                plain("  \u2022 "), bold("Fan chart"),
                plain(" \u2014 shows percentile bands (e.g., 5th\u201395th) over time\n"),
                plain("  \u2022 "), bold("Histogram"),
                plain(" \u2014 distribution of final values across all runs\n"),
                plain("  \u2022 "), bold("Statistics"),
                plain(" \u2014 mean, standard deviation, percentiles\n\n"),
                plain("Monte Carlo is useful for risk analysis and understanding uncertainty.")
        );
    }

    private static TextFlow optimizationContent() {
        return new TextFlow(
                bold("Optimization"),
                plain(" finds parameter values that minimize or maximize an objective function.\n\n"),
                bold("Setup:\n"),
                plain("  1. Select parameters to optimize and their bounds\n"),
                plain("  2. Define the objective (minimize or maximize a variable's final value, "
                        + "or a custom expression)\n"),
                plain("  3. Set the algorithm and iteration count\n\n"),
                bold("Algorithms:\n"),
                plain("  \u2022 "), bold("Nelder-Mead"),
                plain(" \u2014 derivative-free simplex method, good for smooth objectives\n"),
                plain("  \u2022 "), bold("Random Search"),
                plain(" \u2014 samples random points, good for rough landscapes\n\n"),
                bold("Results"),
                plain(" show the best parameter values found, the objective value, "
                        + "and a convergence plot.")
        );
    }

    private static TextFlow calibrationContent() {
        return new TextFlow(
                bold("Calibration"),
                plain(" fits model parameters to observed data by minimizing the sum of squared "
                        + "errors (SSE) between simulated and observed time series.\n\n"),
                bold("Setup:\n"),
                plain("  1. "), bold("Import CSV"),
                plain(" \u2014 load a CSV file containing observed data. The first column must be "
                        + "the time axis; remaining columns are data series\n"),
                plain("  2. "), bold("Map columns"),
                plain(" \u2014 map each CSV column to a model stock. Columns that don't correspond "
                        + "to a stock can be skipped\n"),
                plain("  3. "), bold("Set parameter bounds"),
                plain(" \u2014 select which parameters to calibrate and specify lower/upper bounds "
                        + "and an optional initial guess\n"),
                plain("  4. "), bold("Pick algorithm"),
                plain(" \u2014 choose the optimization algorithm and maximum evaluations\n\n"),
                bold("How SSE works:\n\n"),
                mono("  SSE = \u03a3 (simulated[t] \u2212 observed[t])\u00b2\n\n"),
                plain("Lower SSE means a better fit. When multiple stocks are mapped, the total SSE "
                        + "is the sum across all fit targets.\n\n"),
                bold("Algorithms:\n"),
                plain("  \u2022 "), bold("Nelder-Mead"),
                plain(" \u2014 derivative-free simplex method, good for smooth objectives\n"),
                plain("  \u2022 "), bold("BOBYQA"),
                plain(" \u2014 bounded optimization by quadratic approximation, good for bounded parameters\n"),
                plain("  \u2022 "), bold("CMA-ES"),
                plain(" \u2014 evolutionary strategy, good for noisy or multimodal landscapes\n\n"),
                bold("Results"),
                plain(" show the recovered parameter values, total SSE, and a chart overlaying "
                        + "simulated and observed data for each fit target. Observed data is shown "
                        + "with a dashed line.")
        );
    }

    private static TextFlow multiSweepContent() {
        return new TextFlow(
                bold("Multi-Parameter Sweep"),
                plain(" varies two or more parameters simultaneously, running the model "
                        + "at every combination.\n\n"),
                bold("Setup:\n"),
                plain("  1. Select two or more parameters\n"),
                plain("  2. Set the range and steps for each\n"),
                plain("  3. Select the output variable(s) to observe\n\n"),
                bold("Results"),
                plain(" show a matrix or heatmap of outcomes across the parameter space. "
                        + "This helps identify interaction effects between parameters.\n\n"),
                bold("Caution:"),
                plain(" The number of runs grows exponentially with the number of parameters "
                        + "and steps. Keep step counts low (3\u20135) when sweeping multiple parameters.")
        );
    }

    private static TextFlow feedbackLoopsContent() {
        return new TextFlow(
                bold("Feedback Loops"),
                plain(" are circular causal chains where a change propagates through the system "
                        + "and eventually affects the original variable.\n\n"),
                bold("Types:\n"),
                plain("  \u2022 "), bold("Reinforcing (R)"),
                plain(" \u2014 amplifies change (growth or collapse)\n"),
                plain("  \u2022 "), bold("Balancing (B)"),
                plain(" \u2014 resists change (goal-seeking)\n\n"),
                bold("Loop highlighting:\n"),
                plain("  \u2022 Click the loop button in the toolbar to activate\n"),
                plain("  \u2022 Use "), bold("[ ]"), plain(" keys to step through loops\n"),
                plain("  \u2022 Filter by reinforcing or balancing using the navigator bar\n\n"),
                bold("Loop dominance:"),
                plain(" At any time, one loop tends to dominate. Shifts in dominance explain "
                        + "why systems change behavior mode over time.\n\n"),
                bold("Delays"),
                plain(" in feedback loops cause oscillations. The longer the delay, "
                        + "the more severe the oscillation.")
        );
    }

    private static TextFlow causalLoopsContent() {
        return new TextFlow(
                bold("Causal Loop Diagrams (CLDs)"),
                plain(" map the feedback structure of a system before building a full "
                        + "stock-and-flow model.\n\n"),
                bold("Building a CLD:\n"),
                plain("  1. Add CLD variables representing system concepts\n"),
                plain("  2. Draw causal links between variables\n"),
                plain("  3. Set link polarity (+, \u2212, or ?)\n"),
                plain("  4. Identify reinforcing and balancing loops\n\n"),
                bold("From CLD to stock-and-flow:\n"),
                plain("Right-click a CLD variable and choose "), bold("Classify as"),
                plain(" to convert it to a Stock, Flow, or Variable. This preserves the "
                        + "variable's name and position while adding equation capability.")
        );
    }

    private static TextFlow causalTraceContent() {
        return new TextFlow(
                bold("Causal Tracing"),
                plain(" highlights the dependency chain for a selected element.\n\n"),
                bold("Directions:\n"),
                plain("  \u2022 "), bold("Upstream"),
                plain(" \u2014 shows what influences the selected element\n"),
                plain("  \u2022 "), bold("Downstream"),
                plain(" \u2014 shows what the selected element influences\n\n"),
                bold("Usage:\n"),
                plain("  1. Right-click an element on the canvas\n"),
                plain("  2. Choose "), bold("Trace Upstream"), plain(" or "), bold("Trace Downstream\n"),
                plain("  3. Connected elements are highlighted; others are dimmed\n"),
                plain("  4. Press "), bold("Escape"), plain(" to clear the trace\n\n"),
                plain("Causal tracing is useful for understanding which parts of the model "
                        + "affect or are affected by a particular variable.")
        );
    }

    private static TextFlow modulePortsContent() {
        return new TextFlow(
                bold("Module Ports & Bindings"),
                plain(" define how a module communicates with its parent model.\n\n"),
                bold("Input ports"),
                plain(" receive values from the parent. The parent binds an expression "
                        + "to each input port.\n\n"),
                bold("Output ports"),
                plain(" expose internal values to the parent. The parent can reference "
                        + "module outputs in its equations.\n\n"),
                bold("Defining ports:\n"),
                plain("  1. Select the module on the canvas\n"),
                plain("  2. Click "), bold("Define Ports"), plain(" in the properties panel\n"),
                plain("  3. Add input and output ports with names and units\n\n"),
                bold("Binding inputs:\n"),
                plain("  1. Select the module instance on the parent canvas\n"),
                plain("  2. In the properties panel, enter expressions for each input port\n\n"),
                plain("All input ports must be bound before the model can be simulated.")
        );
    }

    private static TextFlow validationContent() {
        return new TextFlow(
                bold("Model Validation"),
                plain(" checks your model for structural problems before you run a simulation.\n\n"),
                bold("What is checked:\n"),
                plain("  \u2022 "), bold("Missing equations"),
                plain(" \u2014 flows and variables without a defining equation\n"),
                plain("  \u2022 "), bold("Undefined references"),
                plain(" \u2014 equations that refer to elements that do not exist\n"),
                plain("  \u2022 "), bold("Unit inconsistencies"),
                plain(" \u2014 mismatched units in equations\n"),
                plain("  \u2022 "), bold("Unconnected elements"),
                plain(" \u2014 stocks with no inflows or outflows\n"),
                plain("  \u2022 "), bold("Circular definitions"),
                plain(" \u2014 algebraic loops with no stock to break the cycle\n\n"),
                bold("Severity levels:\n"),
                plain("  \u2022 "), bold("Error"),
                plain(" \u2014 will prevent the model from simulating\n"),
                plain("  \u2022 "), bold("Warning"),
                plain(" \u2014 the model may simulate but results could be suspect\n\n"),
                plain("Click a row in the results table to select the affected element on the canvas.")
        );
    }

    private static TextFlow extremeConditionContent() {
        return new TextFlow(
                bold("Extreme Condition Testing"),
                plain(" drives each parameter to extreme values and checks whether the model "
                        + "produces unreasonable results (NaN, Infinity, or sign violations).\n\n"),
                bold("How it works:\n"),
                plain("  1. Each parameter is tested one at a time\n"),
                plain("  2. The parameter is set to extreme values (zero, very large, very small, "
                        + "negative)\n"),
                plain("  3. The simulation is run and all variables are checked for anomalies\n\n"),
                bold("Findings:\n"),
                plain("  \u2022 "), bold("NaN / Infinity"),
                plain(" \u2014 a variable produced a mathematically undefined result\n"),
                plain("  \u2022 "), bold("Sign change"),
                plain(" \u2014 a variable that should remain non-negative went negative\n\n"),
                bold("Why it matters:\n"),
                plain("Real-world inputs can be surprising. Extreme condition testing reveals "
                        + "hidden assumptions in your model equations and helps you add appropriate "
                        + "bounds or guards before deployment.")
        );
    }

    private static TextFlow columnMappingContent() {
        return new TextFlow(
                bold("Column Mapping"),
                plain(" lets you match columns from an imported CSV file to variables "
                        + "in your model.\n\n"),
                bold("How to use:\n"),
                plain("  \u2022 Each row shows a CSV column name with a dropdown of model variables\n"),
                plain("  \u2022 Columns whose names match a model variable are auto-selected\n"),
                plain("  \u2022 Select "), mono("(skip)"),
                plain(" to exclude a column from the import\n\n"),
                bold("Name matching:\n"),
                plain("Matching is case-insensitive and treats spaces and underscores as equivalent. "
                        + "For example, CSV column \"birth rate\" will auto-match model variable "
                        + "\"Birth_Rate\".\n\n"),
                bold("After mapping:"),
                plain(" The mapped data appears in calibration fit targets or as reference data "
                        + "on the dashboard chart.")
        );
    }

    private static Text bold(String content) {
        Text text = new Text(content);
        text.setStyle("-fx-font-weight: bold;");
        return text;
    }

    private static Text plain(String content) {
        return new Text(content);
    }

    private static Text mono(String content) {
        Text text = new Text(content);
        text.setStyle("-fx-font-family: monospace;");
        return text;
    }
}
