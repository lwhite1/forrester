package systems.courant.sd.app.canvas;

import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

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
        SELECT("select"),
        PLACE_STOCK("stock"),
        PLACE_FLOW("flow"),
        PLACE_VARIABLE("variable"),
        PLACE_MODULE("module"),
        PLACE_LOOKUP("lookup"),
        PLACE_CLD_VARIABLE("variable"),
        PLACE_CAUSAL_LINK("causal link"),
        PLACE_INFO_LINK("info link"),
        PLACE_COMMENT("comment");

        private final String label;

        Tool(String label) {
            this.label = label;
        }

        /** Human-readable label for undo history and UI display. */
        public String label() {
            return label;
        }
    }

    private final ToggleGroup group = new ToggleGroup();
    private final ToggleButton selectButton;
    private final ToggleButton loopsButton;
    private final Button validateButton;
    private Consumer<Tool> onToolChanged;
    private Consumer<Boolean> onLoopToggleChanged;
    private Runnable onValidateClicked;
    private Runnable onSearchClicked;

    public CanvasToolBar() {
        setId("canvasToolBar");

        selectButton = makeButton("Select", Tool.SELECT);
        selectButton.setId("toolSelect");
        selectButton.setTooltip(new Tooltip("Select and move elements (1)"));
        ToggleButton stockButton = makeButton("Stock", Tool.PLACE_STOCK);
        stockButton.setId("toolStock");
        stockButton.setTooltip(new Tooltip("Add a stock \u2014 an accumulator that holds a quantity (4)"));
        ToggleButton flowButton = makeButton("Flow", Tool.PLACE_FLOW);
        flowButton.setId("toolFlow");
        flowButton.setTooltip(new Tooltip("Add a flow \u2014 a rate that moves material between stocks (5)"));
        ToggleButton auxButton = makeButton("Variable", Tool.PLACE_VARIABLE);
        auxButton.setId("toolAux");
        auxButton.setTooltip(new Tooltip("Add a variable \u2014 a computed intermediate value (6)"));
        ToggleButton moduleButton = makeButton("Module", Tool.PLACE_MODULE);
        moduleButton.setId("toolModule");
        moduleButton.setTooltip(new Tooltip("Add a module \u2014 a reusable sub-model (8)"));
        ToggleButton lookupButton = makeButton("Lookup", Tool.PLACE_LOOKUP);
        lookupButton.setId("toolLookup");
        lookupButton.setTooltip(new Tooltip("Add a lookup table \u2014 a graphical function (7)"));
        ToggleButton cldVarButton = makeButton("Causal Variable", Tool.PLACE_CLD_VARIABLE);
        cldVarButton.setId("toolCldVar");
        cldVarButton.setTooltip(new Tooltip("Add a CLD variable \u2014 a qualitative causal factor (2)"));
        ToggleButton causalLinkButton = makeButton("Causal Link", Tool.PLACE_CAUSAL_LINK);
        causalLinkButton.setId("toolCausalLink");
        causalLinkButton.setTooltip(new Tooltip("Draw a causal link between variables (3)"));
        ToggleButton infoLinkButton = makeButton("Info Link", Tool.PLACE_INFO_LINK);
        infoLinkButton.setId("toolInfoLink");
        infoLinkButton.setTooltip(new Tooltip("Draw info link to/from module ports"));
        ToggleButton commentButton = makeButton("Comment", Tool.PLACE_COMMENT);
        commentButton.setId("toolComment");
        commentButton.setTooltip(new Tooltip("Add a comment \u2014 a free-text annotation on the canvas (9)"));

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

        // Spacer pushes the search hint to the right edge
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label searchIcon = new Label("\uD83D\uDD0D");
        searchIcon.setStyle("-fx-font-size: 11px;");
        Label searchText = new Label("Search commands...");
        searchText.setStyle("-fx-font-size: 11px; -fx-text-fill: #777;");
        Label shortcutBadge = new Label(
                System.getProperty("os.name", "").toLowerCase().contains("mac")
                        ? "\u2318K" : "Ctrl+K");
        shortcutBadge.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;"
                + " -fx-border-color: #CCC; -fx-border-radius: 3;"
                + " -fx-background-color: #F5F5F5; -fx-background-radius: 3;"
                + " -fx-padding: 1 4 1 4;");

        HBox searchHint = new HBox(4, searchIcon, searchText, shortcutBadge);
        searchHint.setId("toolbarSearchHint");
        searchHint.setAlignment(javafx.geometry.Pos.CENTER);
        searchHint.setStyle("-fx-padding: 2 8 2 8; -fx-cursor: hand;"
                + " -fx-border-color: #DDD; -fx-border-radius: 4;"
                + " -fx-background-color: #FAFAFA; -fx-background-radius: 4;");
        searchHint.setCursor(Cursor.HAND);
        searchHint.setOnMouseClicked(e -> {
            if (onSearchClicked != null) {
                onSearchClicked.run();
            }
        });

        getItems().addAll(selectButton, new Separator(),
                cldVarButton, causalLinkButton, new Separator(),
                stockButton, flowButton, auxButton, lookupButton, moduleButton, infoLinkButton,
                new Separator(), commentButton,
                new Separator(), loopsButton, new Separator(), validateButton,
                spacer, searchHint);
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
     * Deselects the Loops toggle button without firing the toggle callback.
     */
    public void deactivateLoopToggle() {
        loopsButton.setSelected(false);
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

    /**
     * Sets a callback invoked when the search hint is clicked,
     * typically to open the command palette.
     */
    public void setOnSearchClicked(Runnable callback) {
        this.onSearchClicked = callback;
    }
}
