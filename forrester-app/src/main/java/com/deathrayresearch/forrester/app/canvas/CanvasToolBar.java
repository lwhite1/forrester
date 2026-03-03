package com.deathrayresearch.forrester.app.canvas;

import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;

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
        PLACE_MODULE
    }

    private final ToggleGroup group = new ToggleGroup();
    private final ToggleButton selectButton;
    private final ToggleButton loopsButton;
    private Consumer<Tool> onToolChanged;
    private Consumer<Boolean> onLoopToggleChanged;

    public CanvasToolBar() {
        selectButton = makeButton("Select", Tool.SELECT);
        ToggleButton stockButton = makeButton("Stock", Tool.PLACE_STOCK);
        ToggleButton flowButton = makeButton("Flow", Tool.PLACE_FLOW);
        ToggleButton auxButton = makeButton("Auxiliary", Tool.PLACE_AUX);
        ToggleButton constantButton = makeButton("Constant", Tool.PLACE_CONSTANT);
        ToggleButton moduleButton = makeButton("Module", Tool.PLACE_MODULE);

        selectButton.setSelected(true);

        // Independent toggle for loop highlighting (not in the tool group)
        loopsButton = new ToggleButton("Loops");
        loopsButton.setOnAction(event -> {
            if (onLoopToggleChanged != null) {
                onLoopToggleChanged.accept(loopsButton.isSelected());
            }
        });

        getItems().addAll(selectButton, new Separator(),
                stockButton, flowButton, auxButton, constantButton, moduleButton,
                new Separator(), loopsButton);
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
}
