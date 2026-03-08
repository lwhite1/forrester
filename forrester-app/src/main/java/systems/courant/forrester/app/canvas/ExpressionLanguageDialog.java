package systems.courant.forrester.app.canvas;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

/**
 * A help dialog explaining the Forrester expression language used in equations.
 */
public class ExpressionLanguageDialog extends Stage {

    public ExpressionLanguageDialog() {
        setTitle("Expression Language Reference");

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        tabs.getTabs().addAll(
                createTextTab("Basics", basicsContent()),
                createGridTab("Operators", operatorsContent()),
                createGridTab("Math Functions", mathFunctionsContent()),
                createTextTab("SD Functions", sdFunctionsContent()),
                createTextTab("Patterns", patternsContent()),
                createTextTab("Grammar", grammarContent())
        );

        Scene scene = new Scene(tabs, 680, 560);
        setScene(scene);
    }

    private Tab createTextTab(String title, TextFlow content) {
        content.setPadding(new Insets(16));
        content.setLineSpacing(4);
        content.setMaxWidth(640);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);

        return new Tab(title, scroll);
    }

    private Tab createGridTab(String title, VBox content) {
        content.setPadding(new Insets(16));

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);

        return new Tab(title, scroll);
    }

    private TextFlow basicsContent() {
        return new TextFlow(
                bold("Expressions"),
                plain(" are mathematical formulas used in flow equations, auxiliary equations, "
                        + "and lookup table inputs.\n\n"),
                bold("Variable references\n\n"),
                plain("Use the name of any stock, flow, constant, auxiliary, or lookup table:\n\n"),
                mono("  Birth_Rate * Population\n\n"),
                plain("Spaces in element names are converted to underscores. "
                        + "You can also use backtick-quoted names:\n\n"),
                mono("  `Birth Rate` * Population\n\n"),
                bold("Numbers\n\n"),
                plain("Integers, decimals, and scientific notation:\n\n"),
                mono("  42    3.14    0.5    .5    1.5e-3\n\n"),
                bold("Special variables\n\n"),
                mono("  TIME"),
                plain("  Current simulation timestep (starts at 0)\n"),
                mono("  DT"),
                plain("    Simulation time step size (default 1.0)\n\n"),
                bold("Conditional\n\n"),
                mono("  IF(condition, then_value, else_value)\n\n"),
                plain("Returns then_value if condition is non-zero, otherwise else_value.\n\n"),
                mono("  IF(Inventory > 0, Order_Rate, 0)\n"),
                mono("  IF(Population > Capacity, Capacity, Population)\n\n"),
                bold("Division by zero"),
                plain(" returns 0 (safe division).\n\n"),
                bold("Equality comparison"),
                plain(" uses epsilon tolerance (1e-10) for floating-point precision.")
        );
    }

    private VBox operatorsContent() {
        return new VBox(16,
                section("Arithmetic",
                        "+", "Addition",
                        "-", "Subtraction",
                        "*", "Multiplication",
                        "/", "Division",
                        "%", "Modulo (remainder)",
                        "**", "Exponentiation",
                        "- (unary)", "Negation"),
                section("Comparison (return 1 for true, 0 for false)",
                        "==", "Equal",
                        "!=", "Not equal",
                        "<", "Less than",
                        "<=", "Less than or equal",
                        ">", "Greater than",
                        ">=", "Greater than or equal"),
                section("Logical (0 = false, non-zero = true)",
                        "and", "Logical and",
                        "or", "Logical or",
                        "not", "Logical not"),
                section("Precedence (highest to lowest)",
                        "**", "Exponentiation (right-associative)",
                        "* / %", "Multiplicative",
                        "+ -", "Additive",
                        "== != < <= > >=", "Comparison",
                        "and", "Logical and",
                        "or", "Logical or")
        );
    }

    private VBox mathFunctionsContent() {
        return new VBox(16,
                section("Single-argument",
                        "ABS(x)", "Absolute value",
                        "SQRT(x)", "Square root",
                        "EXP(x)", "e raised to the power x",
                        "LN(x)", "Natural logarithm (base e)",
                        "LOG(x)", "Common logarithm (base 10)",
                        "SIN(x)", "Sine (radians)",
                        "COS(x)", "Cosine (radians)",
                        "TAN(x)", "Tangent (radians)",
                        "INT(x)", "Truncate to integer (toward zero)",
                        "ROUND(x)", "Round to nearest integer"),
                section("Two-argument",
                        "MIN(a, b)", "Smaller of two values",
                        "MAX(a, b)", "Larger of two values",
                        "MODULO(a, b)", "Remainder of a / b (0 if b is 0)",
                        "POWER(a, b)", "a raised to the power b"),
                section("Variable-argument",
                        "SUM(a, b, ...)", "Sum of all arguments",
                        "MEAN(a, b, ...)", "Arithmetic mean of all arguments")
        );
    }

    private TextFlow sdFunctionsContent() {
        return new TextFlow(
                bold("SMOOTH(input, time)\n"),
                bold("SMOOTH(input, time, initial)\n"),
                plain("First-order exponential smoothing. If no initial value given, "
                        + "uses first input value.\n\n"),
                bold("DELAY3(input, delay_time)\n"),
                bold("DELAY3(input, delay_time, initial)\n"),
                plain("Third-order material delay with smooth S-shaped response.\n\n"),
                bold("DELAY_FIXED(input, delay_time, initial)\n"),
                plain("Fixed pipeline delay. Returns the input from exactly delay_time "
                        + "steps ago.\n\n"),
                bold("STEP(height, step_time)\n"),
                plain("Returns 0 before step_time, then height.\n\n"),
                bold("RAMP(slope, start_time)\n"),
                bold("RAMP(slope, start_time, end_time)\n"),
                plain("Returns 0 before start_time, then increases linearly at slope per step. "
                        + "Holds constant after end_time if specified.\n\n"),
                bold("PULSE(magnitude, start_time)\n"),
                bold("PULSE(magnitude, start_time, interval)\n"),
                plain("Returns magnitude for one step at start_time, then 0. "
                        + "Repeats every interval if specified.\n\n"),
                bold("TREND(input, averaging_time, initial_trend)\n"),
                plain("Estimates fractional rate of change using exponential smoothing.\n\n"),
                bold("FORECAST(input, averaging_time, horizon, initial_trend)\n"),
                plain("Linear extrapolation: predicts where input will be after horizon steps.\n\n"),
                bold("NPV(stream, discount_rate)\n"),
                bold("NPV(stream, discount_rate, factor)\n"),
                plain("Accumulates discounted present value of a payment stream.\n\n"),
                bold("LOOKUP(table_name, input_value)\n"),
                plain("Interpolates the named lookup table at input_value.\n\n"),
                bold("RANDOM_NORMAL(min, max, mean, std_dev)\n"),
                plain("Random value from a normal distribution, clamped to [min, max].")
        );
    }

    private TextFlow patternsContent() {
        return new TextFlow(
                bold("Exponential growth\n"),
                mono("  Growth_Rate * Population\n\n"),
                bold("Exponential decay\n"),
                mono("  Decay_Rate * Remaining\n\n"),
                bold("Logistic growth\n"),
                mono("  Growth_Rate * Population * (1 - Population / Carrying_Capacity)\n\n"),
                bold("Goal-seeking (gap-closing)\n"),
                mono("  (Goal - Current) / Adjustment_Time\n\n"),
                bold("Conditional logic\n"),
                mono("  IF(Inventory > Reorder_Point, 0, Order_Quantity)\n\n"),
                bold("Seasonal input\n"),
                mono("  Base * (1 + Amplitude * SIN(2 * 3.14159 * TIME / Period))\n\n"),
                bold("Clamping to non-negative\n"),
                mono("  MAX(0, Calculated_Rate)\n")
        );
    }

    private TextFlow grammarContent() {
        return new TextFlow(
                bold("Formal grammar\n\n"),
                mono("expr       = or_expr\n"),
                mono("or_expr    = and_expr ( \"or\" and_expr )*\n"),
                mono("and_expr   = comparison ( \"and\" comparison )*\n"),
                mono("comparison = addition ( (\"==\" | \"!=\" | \"<\" | \"<=\" | \">\" | \">=\") addition )?\n"),
                mono("addition   = mult ( (\"+\" | \"-\") mult )*\n"),
                mono("mult       = power ( (\"*\" | \"/\" | \"%\") power )*\n"),
                mono("power      = unary ( \"**\" power )?\n"),
                mono("unary      = (\"-\" | \"not\") unary | call\n"),
                mono("call       = primary ( \"(\" arglist? \")\" )?\n"),
                mono("primary    = NUMBER | IDENTIFIER | QUOTED_ID | \"(\" expr \")\"\n"),
                mono("           | \"IF\" \"(\" expr \",\" expr \",\" expr \")\"\n"),
                mono("arglist    = expr ( \",\" expr )*\n\n"),
                bold("Tokens\n\n"),
                mono("NUMBER"),
                plain("      Integer, decimal, or scientific notation (42, 3.14, 1e-3)\n"),
                mono("IDENTIFIER"),
                plain("  Letters, digits, and underscores, starting with letter or _\n"),
                mono("QUOTED_ID"),
                plain("   Backtick-delimited name (`My Variable`)\n")
        );
    }

    private VBox section(String title, String... pairs) {
        Label header = new Label(title);
        header.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(4);
        grid.setPadding(new Insets(4, 0, 0, 0));

        ColumnConstraints keyCol = new ColumnConstraints();
        keyCol.setMinWidth(200);
        ColumnConstraints descCol = new ColumnConstraints();
        descCol.setMinWidth(280);
        grid.getColumnConstraints().addAll(keyCol, descCol);

        for (int i = 0; i < pairs.length; i += 2) {
            int row = i / 2;
            Label key = new Label(pairs[i]);
            key.setStyle("-fx-font-family: monospace; -fx-font-weight: bold;");
            Label desc = new Label(pairs[i + 1]);
            grid.add(key, 0, row);
            grid.add(desc, 1, row);
        }

        return new VBox(4, header, grid);
    }

    private Text bold(String content) {
        Text text = new Text(content);
        text.setStyle("-fx-font-weight: bold;");
        return text;
    }

    private Text plain(String content) {
        return new Text(content);
    }

    private Text mono(String content) {
        Text text = new Text(content);
        text.setStyle("-fx-font-family: monospace;");
        return text;
    }
}
