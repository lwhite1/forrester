package systems.courant.sd.app.canvas.forms;

import systems.courant.sd.model.def.StockDef;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import systems.courant.sd.app.canvas.renderers.ElementRenderer;

/**
 * Property form for stock elements. Builds editable fields for name,
 * initial value, unit, and negative value policy.
 */
public class StockForm implements ElementForm {

    private final FormContext ctx;
    private final FormFieldBuilder fields;

    private TextField nameField;
    private TextField initialValueField;
    private ComboBox<String> unitBox;
    private ComboBox<String> policyBox;
    private TextArea commentArea;

    public StockForm(FormContext ctx, FormFieldBuilder fields) {
        this.ctx = ctx;
        this.fields = fields;
    }

    @Override
    public int build(int startRow) {
        Optional<StockDef> stockOpt = ctx.getEditor().getStockByName(ctx.getElementName());
        if (stockOpt.isEmpty()) {
            fields.addReadOnlyRow(startRow++, "Name", ctx.getElementName());
            return startRow;
        }
        StockDef stock = stockOpt.get();

        int row = startRow;
        nameField = fields.createNameField();
        fields.addFieldRow(row++, "Name", nameField,
                "The name used to reference this stock in equations");

        commentArea = fields.addCommentArea(row++, stock.comment(), this::commitComment);

        initialValueField = fields.createTextField(
                ElementRenderer.formatValue(stock.initialValue()));
        fields.addCommitHandlers(initialValueField, this::commitInitialValue);
        fields.addFieldRow(row++, "Initial Value", initialValueField,
                "The starting value at the beginning of the simulation");

        unitBox = fields.createUnitComboBox(stock.unit());
        fields.addComboCommitHandlers(unitBox, this::commitUnit);
        fields.addFieldRow(row++, "Unit", unitBox,
                "The unit of measurement (e.g., Person, Kilogram, USD)");

        policyBox = new ComboBox<>();
        policyBox.getItems().addAll("Clamp to Zero", "Allow");
        policyBox.setValue(policyDisplayValue(stock.negativeValuePolicy()));
        policyBox.setMaxWidth(Double.MAX_VALUE);
        policyBox.setOnAction(e -> {
            if (!ctx.isUpdatingFields()) {
                commitPolicy();
            }
        });
        fields.addFieldRow(row++, "Negative Values", policyBox,
                "What happens when the stock goes below zero.\n"
                + "'Clamp to Zero' prevents negative values (common for physical "
                + "quantities like population or inventory).\n"
                + "'Allow' permits negatives (e.g., bank balances, temperature deltas).");

        row = fields.addSubscriptRow(row, ctx.getEditor().getSubscripts(),
                stock.subscripts(), this::commitSubscripts);

        return row;
    }

    @Override
    public void updateValues() {
        Optional<StockDef> stockOpt = ctx.getEditor().getStockByName(ctx.getElementName());
        if (stockOpt.isEmpty() || nameField == null) {
            return;
        }
        StockDef stock = stockOpt.get();
        nameField.setText(ctx.getElementName());
        initialValueField.setText(
                ElementRenderer.formatValue(stock.initialValue()));
        unitBox.setValue(stock.unit() != null ? stock.unit() : "");
        policyBox.setValue(policyDisplayValue(stock.negativeValuePolicy()));
        commentArea.setText(stock.comment() != null ? stock.comment() : "");
    }

    private void commitInitialValue(TextField field) {
        try {
            double value = Double.parseDouble(field.getText().trim());
            Optional<StockDef> stockOpt = ctx.getEditor().getStockByName(ctx.getElementName());
            if (stockOpt.isEmpty() || stockOpt.get().initialValue() == value) {
                return;
            }
            ctx.getCanvas().applyMutation(() -> ctx.getEditor().setStockInitialValue(ctx.getElementName(), value));
        } catch (NumberFormatException ignored) {
            ctx.getEditor().getStockByName(ctx.getElementName())
                    .ifPresent(stock -> field.setText(ElementRenderer.formatValue(stock.initialValue())));
            fields.flashInvalidInput(field);
        }
    }

    private void commitUnit(ComboBox<String> box) {
        String unit = FormFieldBuilder.normalizeToNull(box.getValue());
        Optional<StockDef> stockOpt = ctx.getEditor().getStockByName(ctx.getElementName());
        if (stockOpt.isPresent() && Objects.equals(unit, stockOpt.get().unit())) {
            return;
        }
        ctx.getCanvas().applyMutation(() -> ctx.getEditor().setStockUnit(ctx.getElementName(), unit));
    }

    private void commitPolicy() {
        String policyValue = "Allow".equals(policyBox.getValue())
                ? "ALLOW" : "CLAMP_TO_ZERO";
        Optional<StockDef> stockOpt = ctx.getEditor().getStockByName(ctx.getElementName());
        String currentPolicy = stockOpt.map(StockDef::negativeValuePolicy).orElse("CLAMP_TO_ZERO");
        if (stockOpt.isPresent() && Objects.equals(policyValue, currentPolicy)) {
            return;
        }
        ctx.getCanvas().applyMutation(() -> ctx.getEditor().setStockNegativeValuePolicy(ctx.getElementName(), policyValue));
    }

    private void commitComment(TextArea area) {
        String comment = FormFieldBuilder.normalizeToNull(area.getText());
        Optional<StockDef> stockOpt = ctx.getEditor().getStockByName(ctx.getElementName());
        if (stockOpt.isEmpty() || Objects.equals(comment, stockOpt.get().comment())) {
            return;
        }
        ctx.getCanvas().applyMutation(() -> ctx.getEditor().setStockComment(ctx.getElementName(), comment));
    }

    /**
     * Maps the stored policy string to a display value.
     * Null and "CLAMP_TO_ZERO" both display as "Clamp to Zero" (the engine default).
     */
    private void commitSubscripts(List<String> subscripts) {
        Optional<StockDef> stockOpt = ctx.getEditor().getStockByName(ctx.getElementName());
        if (stockOpt.isEmpty() || stockOpt.get().subscripts().equals(subscripts)) {
            return;
        }
        ctx.getCanvas().applyMutation(
                () -> ctx.getEditor().setStockSubscripts(ctx.getElementName(), subscripts));
    }

    private static String policyDisplayValue(String policy) {
        if ("ALLOW".equals(policy)) {
            return "Allow";
        }
        return "Clamp to Zero";
    }
}
