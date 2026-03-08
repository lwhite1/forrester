package systems.courant.forrester.app.canvas;

import systems.courant.forrester.model.def.StockDef;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.Objects;
import java.util.Optional;

/**
 * Property form for stock elements. Builds editable fields for name,
 * initial value, unit, and negative value policy.
 */
class StockForm implements ElementForm {

    private final FormContext ctx;

    private TextField nameField;
    private TextField initialValueField;
    private ComboBox<String> unitBox;
    private ComboBox<String> policyBox;
    private TextArea commentArea;

    StockForm(FormContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public int build(int startRow) {
        Optional<StockDef> stockOpt = ctx.editor.getStockByName(ctx.elementName);
        if (stockOpt.isEmpty()) {
            ctx.addReadOnlyRow(startRow++, "Name", ctx.elementName);
            return startRow;
        }
        StockDef stock = stockOpt.get();

        int row = startRow;
        nameField = ctx.createNameField();
        ctx.addFieldRow(row++, "Name", nameField,
                "The name used to reference this stock in equations");

        initialValueField = ctx.createTextField(
                ElementRenderer.formatValue(stock.initialValue()));
        ctx.addCommitHandlers(initialValueField, this::commitInitialValue);
        ctx.addFieldRow(row++, "Initial Value", initialValueField,
                "The starting value at the beginning of the simulation");

        unitBox = ctx.createUnitComboBox(stock.unit());
        ctx.addComboCommitHandlers(unitBox, this::commitUnit);
        ctx.addFieldRow(row++, "Unit", unitBox,
                "The unit of measurement (e.g., Person, Kilogram, USD)");

        policyBox = new ComboBox<>();
        policyBox.getItems().addAll("Clamp to Zero", "Allow");
        policyBox.setValue(policyDisplayValue(stock.negativeValuePolicy()));
        policyBox.setMaxWidth(Double.MAX_VALUE);
        policyBox.setOnAction(e -> {
            if (!ctx.updatingFields) {
                commitPolicy();
            }
        });
        ctx.addFieldRow(row++, "Negative Values", policyBox,
                "What happens when the stock goes below zero.\n"
                + "'Clamp to Zero' prevents negative values (common for physical "
                + "quantities like population or inventory).\n"
                + "'Allow' permits negatives (e.g., bank balances, temperature deltas).");

        commentArea = new TextArea(stock.comment() != null ? stock.comment() : "");
        commentArea.setId("propComment");
        commentArea.setPrefRowCount(2);
        commentArea.setWrapText(true);
        commentArea.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(commentArea, Priority.ALWAYS);
        ctx.addTextAreaCommitHandlers(commentArea, this::commitComment);
        ctx.addFieldRow(row++, "Comment", commentArea,
                "Optional documentation for this element");

        return row;
    }

    @Override
    public void updateValues() {
        Optional<StockDef> stockOpt = ctx.editor.getStockByName(ctx.elementName);
        if (stockOpt.isEmpty() || nameField == null) {
            return;
        }
        StockDef stock = stockOpt.get();
        nameField.setText(ctx.elementName);
        initialValueField.setText(
                ElementRenderer.formatValue(stock.initialValue()));
        unitBox.setValue(stock.unit() != null ? stock.unit() : "");
        policyBox.setValue(policyDisplayValue(stock.negativeValuePolicy()));
        commentArea.setText(stock.comment() != null ? stock.comment() : "");
    }

    private void commitInitialValue(TextField field) {
        try {
            double value = Double.parseDouble(field.getText().trim());
            Optional<StockDef> stockOpt = ctx.editor.getStockByName(ctx.elementName);
            if (stockOpt.isEmpty() || stockOpt.get().initialValue() == value) {
                return;
            }
            ctx.canvas.applyMutation(() -> ctx.editor.setStockInitialValue(ctx.elementName, value));
        } catch (NumberFormatException ignored) {
            ctx.editor.getStockByName(ctx.elementName)
                    .ifPresent(stock -> field.setText(ElementRenderer.formatValue(stock.initialValue())));
        }
    }

    private void commitUnit(ComboBox<String> box) {
        String unit = box.getValue() != null ? box.getValue().trim() : "";
        Optional<StockDef> stockOpt = ctx.editor.getStockByName(ctx.elementName);
        if (stockOpt.isPresent() && unit.equals(stockOpt.get().unit())) {
            return;
        }
        ctx.canvas.applyMutation(() -> ctx.editor.setStockUnit(ctx.elementName, unit));
    }

    private void commitPolicy() {
        String policyValue = "Allow".equals(policyBox.getValue())
                ? "ALLOW" : "CLAMP_TO_ZERO";
        Optional<StockDef> stockOpt = ctx.editor.getStockByName(ctx.elementName);
        if (stockOpt.isPresent() && Objects.equals(policyValue,
                stockOpt.get().negativeValuePolicy() != null ? stockOpt.get().negativeValuePolicy() : "CLAMP_TO_ZERO")) {
            return;
        }
        ctx.canvas.applyMutation(() -> ctx.editor.setStockNegativeValuePolicy(ctx.elementName, policyValue));
    }

    private void commitComment(TextArea area) {
        String text = area.getText().trim();
        String comment = text.isEmpty() ? null : text;
        Optional<StockDef> stockOpt = ctx.editor.getStockByName(ctx.elementName);
        if (stockOpt.isEmpty() || Objects.equals(comment, stockOpt.get().comment())) {
            return;
        }
        ctx.canvas.applyMutation(() -> ctx.editor.setStockComment(ctx.elementName, comment));
    }

    /**
     * Maps the stored policy string to a display value.
     * Null and "CLAMP_TO_ZERO" both display as "Clamp to Zero" (the engine default).
     */
    private static String policyDisplayValue(String policy) {
        if ("ALLOW".equals(policy)) {
            return "Allow";
        }
        return "Clamp to Zero";
    }
}
