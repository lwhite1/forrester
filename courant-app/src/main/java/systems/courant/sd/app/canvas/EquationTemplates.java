package systems.courant.sd.app.canvas;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

/**
 * Provides equation template snippets for common system dynamics patterns.
 * Templates insert placeholder variable names that users can select and replace.
 */
final class EquationTemplates {

    private EquationTemplates() {
    }

    /**
     * Creates a context menu of equation templates. Selecting a template inserts
     * the snippet into the given equation field, replacing any existing content.
     */
    static ContextMenu createMenu(EquationField equationField) {
        ContextMenu menu = new ContextMenu();

        Menu flowPatterns = new Menu("Flow patterns");
        flowPatterns.getItems().addAll(
                templateItem(equationField, "Linear flow",
                        "constant_rate"),
                templateItem(equationField, "Exponential decay",
                        "Stock * fractional_rate"),
                templateItem(equationField, "Goal-seeking",
                        "(Goal - Stock) / Adjustment_Time"),
                templateItem(equationField, "Logistic growth",
                        "Stock * growth_rate * (1 - Stock / Capacity)"),
                templateItem(equationField, "First-order delay",
                        "Stock / Delay_Time"),
                templateItem(equationField, "Draining flow",
                        "Stock / Average_Lifetime")
        );

        Menu auxPatterns = new Menu("Variable patterns");
        auxPatterns.getItems().addAll(
                templateItem(equationField, "Ratio",
                        "Numerator / Denominator"),
                templateItem(equationField, "Effect multiplier",
                        "Normal_Value * Effect"),
                templateItem(equationField, "Weighted sum",
                        "Weight_A * A + Weight_B * B"),
                templateItem(equationField, "Clamped value",
                        "MAX(0, MIN(Value, Upper_Bound))"),
                templateItem(equationField, "Conditional",
                        "IF(Condition > Threshold, High_Value, Low_Value)")
        );

        Menu mathPatterns = new Menu("Math functions");
        mathPatterns.getItems().addAll(
                templateItem(equationField, "Smooth (exponential avg.)",
                        "SMOOTH(Input, Smoothing_Time)"),
                templateItem(equationField, "Delay",
                        "DELAY(Input, Delay_Time)"),
                templateItem(equationField, "Pulse",
                        "PULSE(Magnitude, Start_Time, Duration)"),
                templateItem(equationField, "Step function",
                        "STEP(Height, Step_Time)"),
                templateItem(equationField, "Ramp",
                        "RAMP(Slope, Start_Time, End_Time)")
        );

        menu.getItems().addAll(flowPatterns, auxPatterns, mathPatterns);
        return menu;
    }

    private static MenuItem templateItem(EquationField equationField, String label, String template) {
        MenuItem item = new MenuItem(label + "  \u2192  " + template);
        item.setOnAction(e -> {
            equationField.setText(template);
            equationField.requestFocus();
            equationField.selectAll();
        });
        return item;
    }
}
