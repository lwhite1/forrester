package com.deathrayresearch.forrester.app.canvas;

import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;

import java.util.function.Consumer;

/**
 * Toolbar component with tool-selection buttons for the canvas editor.
 * Buttons are mutually exclusive — only one tool is active at a time.
 * The default active tool is {@link Tool#SELECT}.
 */
public class CanvasToolBar extends ToolBar {

    /**
     * The available canvas tools.
     */
    public enum Tool {
        SELECT,
        PLACE_STOCK,
        PLACE_FLOW,
        PLACE_AUX,
        PLACE_CONSTANT,
        PLACE_MODULE,
        PLACE_LOOKUP
    }

    private final ToggleGroup group = new ToggleGroup();
    private final ToggleButton selectButton;
    private final ToggleButton loopsButton;
    private final Button validateButton;
    private Consumer<Tool> onToolChanged;
    private Consumer<Boolean> onLoopToggleChanged;
    private Runnable onValidateClicked;

    public CanvasToolBar() {
        setId("canvasToolBar");

        selectButton = makeButton("Select", Tool.SELECT);
        selectButton.setId("toolSelect");
        selectButton.setTooltip(new Tooltip("Select and move elements (S)"));
        ToggleButton stockButton = makeButton("Stock", Tool.PLACE_STOCK);
        stockButton.setId("toolStock");
        stockButton.setTooltip(new Tooltip("Add a stock \u2014 an accumulator that holds a quantity"));
        ToggleButton flowButton = makeButton("Flow", Tool.PLACE_FLOW);
        flowButton.setId("toolFlow");
        flowButton.setTooltip(new Tooltip("Add a flow \u2014 a rate that moves material between stocks"));
        ToggleButton auxButton = makeButton("Auxiliary", Tool.PLACE_AUX);
        auxButton.setId("toolAux");
        auxButton.setTooltip(new Tooltip("Add an auxiliary \u2014 a computed intermediate variable"));
        ToggleButton constantButton = makeButton("Constant", Tool.PLACE_CONSTANT);
        constantButton.setId("toolConstant");
        constantButton.setTooltip(new Tooltip("Add a constant \u2014 a fixed parameter value"));
        ToggleButton moduleButton = makeButton("Module", Tool.PLACE_MODULE);
        moduleButton.setId("toolModule");
        moduleButton.setTooltip(new Tooltip("Add a module \u2014 a reusable sub-model"));
        ToggleButton lookupButton = makeButton("Lookup", Tool.PLACE_LOOKUP);
        lookupButton.setId("toolLookup");
        lookupButton.setTooltip(new Tooltip("Add a lookup table \u2014 a graphical function"));

        selectButton.setSelected(true);

        // Independent toggle for loop highlighting (not in the tool group)
        loopsButton = new ToggleButton("Loops");
        loopsButton.setId("toolLoops");
        loopsButton.setTooltip(new Tooltip("Highlight feedback loops in the model"));
        loopsButton.setOnAction(event -> {
            if (onLoopToggleChanged != null) {
                onLoopToggleChanged.accept(loopsButton.isSelected());
            }
        });

        validateButton = new Button("Validate");
        validateButton.setId("toolValidate");
        validateButton.setTooltip(new Tooltip("Check the model for structural issues"));
        validateButton.setOnAction(event -> {
            if (onValidateClicked != null) {
                onValidateClicked.run();
            }
        });

        getItems().addAll(selectButton, new Separator(),
                stockButton, flowButton, auxButton, constantButton, moduleButton, lookupButton,
                new Separator(), loopsButton, new Separator(), validateButton);
    }

    private ToggleButton makeButton(String label, Tool tool) {
        ToggleButton button = new ToggleButton(label);
        button.setToggleGroup(group);
        button.setUserData(tool);
        button.setOnAction(event -> {
            // Prevent deselecting all buttons — force at least one active
            if (group.getSelectedToggle() == null) {
                selectButton.setSelected(true);
            }
            if (onToolChanged != null) {
                onToolChanged.accept(getActiveTool());
            }
        });
        return button;
    }

    /**
     * Returns the currently active tool.
     */
    public Tool getActiveTool() {
        if (group.getSelectedToggle() == null) {
            return Tool.SELECT;
        }
        return (Tool) group.getSelectedToggle().getUserData();
    }

    /**
     * Resets the active tool to SELECT.
     */
    public void resetToSelect() {
        selectButton.setSelected(true);
        if (onToolChanged != null) {
            onToolChanged.accept(Tool.SELECT);
        }
    }

    /**
     * Programmatically selects the given tool. Iterates toggle buttons to find the
     * matching one, selects it, and fires the tool-changed callback.
     */
    public void selectTool(Tool tool) {
        for (javafx.scene.control.Toggle toggle : group.getToggles()) {
            if (toggle.getUserData() == tool) {
                toggle.setSelected(true);
                if (onToolChanged != null) {
                    onToolChanged.accept(tool);
                }
                return;
            }
        }
    }

    /**
     * Sets a callback invoked whenever the active tool changes.
     */
    public void setOnToolChanged(Consumer<Tool> callback) {
        this.onToolChanged = callback;
    }

    /**
     * Sets a callback invoked when the Loops toggle button is toggled on or off.
     */
    public void setOnLoopToggleChanged(Consumer<Boolean> callback) {
        this.onLoopToggleChanged = callback;
    }

    /**
     * Sets a callback invoked when the Validate button is clicked.
     */
    public void setOnValidateClicked(Runnable callback) {
        this.onValidateClicked = callback;
    }
}
