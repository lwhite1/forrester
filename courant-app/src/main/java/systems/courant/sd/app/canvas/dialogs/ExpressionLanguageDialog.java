package systems.courant.sd.app.canvas.dialogs;

import static systems.courant.sd.app.canvas.dialogs.StyledText.bold;
import static systems.courant.sd.app.canvas.dialogs.StyledText.plain;

import systems.courant.sd.model.compile.FunctionDoc;
import systems.courant.sd.model.compile.FunctionDocRegistry;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.util.List;
import systems.courant.sd.app.canvas.Styles;

/**
 * A help dialog explaining the Courant expression language used in equations.
 */
public class ExpressionLanguageDialog extends Stage {

    private static final String SD_TAB_TITLE = "SD Functions";
    private static final String MATH_TAB_TITLE = "Math Functions";

    private final TabPane tabs;

    public ExpressionLanguageDialog() {
        setTitle("Expression Language Reference");

        tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        tabs.getTabs().addAll(
                createTextTab("Basics", basicsContent()),
                createGridTab("Operators", operatorsContent()),
                createFunctionTab(MATH_TAB_TITLE, FunctionDocRegistry.byCategory("Math")),
                createFunctionTab(SD_TAB_TITLE, FunctionDocRegistry.byCategory("SD")),
                createTextTab("Patterns", patternsContent()),
                createTextTab("Grammar", grammarContent())
        );

        Scene scene = new Scene(tabs,
                Styles.screenAwareWidth(680), Styles.screenAwareHeight(560));
        setScene(scene);
    }

    /**
     * Selects the SD Functions tab and brings the window to front.
     */
    public void focusSdFunctions() {
        for (Tab tab : tabs.getTabs()) {
            if (SD_TAB_TITLE.equals(tab.getText())) {
                tabs.getSelectionModel().select(tab);
                break;
            }
        }
        toFront();
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

    private Tab createFunctionTab(String title, List<FunctionDoc> functions) {
        VBox container = new VBox(2);
        container.setPadding(new Insets(8));

        for (FunctionDoc doc : functions) {
            TitledPane pane = new TitledPane(doc.signature() + "  —  " + doc.oneLiner(),
                    buildFunctionDetail(doc));
            pane.setExpanded(false);
            pane.setAnimated(false);
            container.getChildren().add(pane);
        }

        ScrollPane scroll = new ScrollPane(container);
        scroll.setFitToWidth(true);

        return new Tab(title, scroll);
    }

    private Node buildFunctionDetail(FunctionDoc doc) {
        VBox detail = new VBox(8);
        detail.setPadding(new Insets(8, 12, 12, 12));

        // Parameters section
        if (!doc.parameters().isEmpty()) {
            Label paramHeader = new Label("Parameters");
            paramHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");
            GridPane paramGrid = new GridPane();
            paramGrid.setHgap(12);
            paramGrid.setVgap(4);
            paramGrid.setPadding(new Insets(2, 0, 0, 8));

            ColumnConstraints nameCol = new ColumnConstraints();
            nameCol.setMinWidth(120);
            ColumnConstraints descCol = new ColumnConstraints();
            descCol.setMinWidth(300);
            paramGrid.getColumnConstraints().addAll(nameCol, descCol);

            for (int i = 0; i < doc.parameters().size(); i++) {
                FunctionDoc.ParamDoc p = doc.parameters().get(i);
                Label nameLabel = new Label(p.name());
                nameLabel.setStyle("-fx-font-family: monospace; -fx-font-weight: bold;");
                Label descLabel = new Label(p.description());
                descLabel.setWrapText(true);
                paramGrid.add(nameLabel, 0, i);
                paramGrid.add(descLabel, 1, i);
            }
            detail.getChildren().addAll(paramHeader, paramGrid);
        }

        // Behavior section
        Label behaviorHeader = new Label("Behavior");
        behaviorHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");
        Label behaviorText = new Label(doc.behavior());
        behaviorText.setWrapText(true);
        behaviorText.setPadding(new Insets(0, 0, 0, 8));
        detail.getChildren().addAll(behaviorHeader, behaviorText);

        // Example section
        Label exampleHeader = new Label("Example");
        exampleHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");
        Label exampleText = new Label(doc.example());
        exampleText.setStyle("-fx-font-family: monospace;");
        exampleText.setWrapText(true);
        exampleText.setPadding(new Insets(0, 0, 0, 8));
        detail.getChildren().addAll(exampleHeader, exampleText);

        // Related functions section
        if (!doc.related().isEmpty()) {
            Label relatedHeader = new Label("Related");
            relatedHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");
            Label relatedText = new Label(String.join(", ", doc.related()));
            relatedText.setStyle("-fx-font-family: monospace;");
            relatedText.setPadding(new Insets(0, 0, 0, 8));
            detail.getChildren().addAll(relatedHeader, relatedText);
        }

        return detail;
    }

    private TextFlow basicsContent() {
        return new TextFlow(
                bold("Expressions"),
                plain(" are mathematical formulas used in flow equations, variable equations, "
                        + "and lookup table inputs.\n\n"),
                bold("Variable references\n\n"),
                plain("Use the name of any stock, flow, constant, variable, or lookup table:\n\n"),
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
                mono("  Base * (1 + Amplitude * SIN(2 * PI * TIME / Period))\n\n"),
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

    private Text mono(String content) {
        Text text = new Text(content);
        text.setStyle("-fx-font-family: monospace;");
        return text;
    }
}
