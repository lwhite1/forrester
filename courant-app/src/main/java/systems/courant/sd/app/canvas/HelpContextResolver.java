package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ElementType;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javafx.stage.Window;

import systems.courant.sd.app.canvas.dialogs.ContextHelpDialog;

/**
 * Stateless utility that resolves the current UI context to a {@link HelpTopic}.
 *
 * <p>Implements a priority chain:
 * <ol>
 *   <li>Equation field focused → expression language help</li>
 *   <li>Single element selected → help for that element type</li>
 *   <li>Tool active on toolbar → help for that element type</li>
 *   <li>Loop highlighting active → feedback loops help</li>
 *   <li>Causal trace active → causal tracing help</li>
 *   <li>Inside module → module help</li>
 *   <li>Dashboard tab selected → simulation results help</li>
 *   <li>Default → overview</li>
 * </ol>
 */
public final class HelpContextResolver {

    /** Cached help dialog per owner window. Uses WeakHashMap so closed windows are GC'd. */
    private static final Map<Window, WeakReference<ContextHelpDialog>> helpDialogCache =
            new WeakHashMap<>();

    private HelpContextResolver() {
    }

    /**
     * Resolves the current context to a help topic by extracting state from the canvas.
     *
     * @param focusOwner        the currently focused node (may be null)
     * @param canvas            the model canvas
     * @param rightTabPane      the right-side tab pane (Properties/Dashboard)
     * @param dashboardTabIndex the index of the dashboard tab in rightTabPane
     * @return the most relevant help topic for the current context
     */
    public static HelpTopic resolve(Node focusOwner,
                                    ModelCanvas canvas,
                                    TabPane rightTabPane,
                                    int dashboardTabIndex) {
        // Extract single selected element type
        Set<String> selection = canvas.getSelectedElementNames();
        ElementType selectedType = null;
        if (selection.size() == 1) {
            selectedType = canvas.getSelectedElementType(selection.iterator().next());
        }

        boolean dashboardSelected = rightTabPane != null
                && rightTabPane.getSelectionModel().getSelectedIndex() == dashboardTabIndex;

        return resolve(
                isEquationFieldFocused(focusOwner),
                selectedType,
                canvas.getActiveTool(),
                canvas.isLoopHighlightActive(),
                canvas.isTraceActive(),
                canvas.isInsideModule(),
                dashboardSelected);
    }

    /**
     * Resolves the current context to a help topic from primitive state values.
     * This overload is testable without a JavaFX toolkit.
     *
     * @param equationFocused    true if an equation field is focused
     * @param selectedType       the type of the single selected element (null if none or multiple)
     * @param activeTool         the currently active toolbar tool
     * @param loopHighlightActive true if loop highlighting is active
     * @param traceActive        true if causal tracing is active
     * @param insideModule       true if navigated inside a module
     * @param dashboardSelected  true if the dashboard tab is selected
     * @return the most relevant help topic
     */
    public static HelpTopic resolve(boolean equationFocused,
                                    ElementType selectedType,
                                    CanvasToolBar.Tool activeTool,
                                    boolean loopHighlightActive,
                                    boolean traceActive,
                                    boolean insideModule,
                                    boolean dashboardSelected) {
        // 1. Equation field focused
        if (equationFocused) {
            return HelpTopic.EXPRESSION_LANGUAGE;
        }

        // 2. Single element selected → element type help
        if (selectedType != null) {
            HelpTopic topic = topicForElementType(selectedType);
            if (topic != null) {
                return topic;
            }
        }

        // 3. Tool active on toolbar
        HelpTopic toolTopic = topicForTool(activeTool);
        if (toolTopic != null) {
            return toolTopic;
        }

        // 4. Loop highlighting active
        if (loopHighlightActive) {
            return HelpTopic.FEEDBACK_LOOPS;
        }

        // 5. Causal trace active
        if (traceActive) {
            return HelpTopic.CAUSAL_TRACE;
        }

        // 6. Inside module
        if (insideModule) {
            return HelpTopic.MODULE;
        }

        // 7. Dashboard tab selected
        if (dashboardSelected) {
            return HelpTopic.SIMULATION_RESULTS;
        }

        // 8. Default
        return HelpTopic.OVERVIEW;
    }

    /**
     * Returns the help topic for a specific dialog class name.
     * Used by modal dialogs that have their own F1 handler.
     *
     * @param dialogClassName the simple class name of the dialog
     * @return the matching help topic, or {@link HelpTopic#OVERVIEW} if unknown
     */
    public static HelpTopic topicForDialog(String dialogClassName) {
        return switch (dialogClassName) {
            case "SimulationSettingsDialog" -> HelpTopic.SIMULATION_SETTINGS;
            case "MonteCarloDialog" -> HelpTopic.MONTE_CARLO;
            case "OptimizerDialog" -> HelpTopic.OPTIMIZATION;
            case "CalibrateDialog" -> HelpTopic.CALIBRATION;
            case "ParameterSweepDialog" -> HelpTopic.PARAMETER_SWEEP;
            case "MultiParameterSweepDialog" -> HelpTopic.MULTI_SWEEP;
            case "DefinePortsDialog" -> HelpTopic.MODULE_PORTS;
            case "ExpressionLanguageDialog" -> HelpTopic.EXPRESSION_LANGUAGE;
            case "SdConceptsDialog" -> HelpTopic.OVERVIEW;
            case "BindingConfigDialog" -> HelpTopic.MODULE_PORTS;
            case "ValidationDialog" -> HelpTopic.VALIDATION;
            case "ExtremeConditionDialog" -> HelpTopic.EXTREME_CONDITION;
            case "ColumnMappingDialog" -> HelpTopic.COLUMN_MAPPING;
            default -> HelpTopic.OVERVIEW;
        };
    }

    /**
     * Maps an {@link ElementType} to the corresponding help topic.
     */
    static HelpTopic topicForElementType(ElementType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case STOCK -> HelpTopic.STOCK;
            case FLOW -> HelpTopic.FLOW;
            case AUX -> HelpTopic.VARIABLE;
            case LOOKUP -> HelpTopic.LOOKUP;
            case MODULE -> HelpTopic.MODULE;
            case CLD_VARIABLE -> HelpTopic.CLD_VARIABLE;
            case COMMENT -> HelpTopic.COMMENT;
        };
    }

    static HelpTopic topicForTool(CanvasToolBar.Tool tool) {
        if (tool == null || tool == CanvasToolBar.Tool.SELECT) {
            return null;
        }
        return switch (tool) {
            case PLACE_STOCK -> HelpTopic.STOCK;
            case PLACE_FLOW -> HelpTopic.FLOW;
            case PLACE_VARIABLE -> HelpTopic.VARIABLE;
            case PLACE_LOOKUP -> HelpTopic.LOOKUP;
            case PLACE_MODULE -> HelpTopic.MODULE;
            case PLACE_CLD_VARIABLE -> HelpTopic.CLD_VARIABLE;
            case PLACE_CAUSAL_LINK -> HelpTopic.CAUSAL_LOOPS;
            case PLACE_INFO_LINK -> HelpTopic.MODULE;
            case PLACE_COMMENT -> HelpTopic.COMMENT;
            case SELECT -> null;
        };
    }

    /**
     * Installs an F1 key handler on a {@link Dialog} so that pressing F1
     * opens context help for that dialog type.
     *
     * <p>Modal dialogs have their own Stage, so they don't receive the
     * Scene-level F1 filter installed in ModelWindow. This method bridges
     * the gap by adding a filter directly on the dialog pane.
     *
     * @param dialog the dialog to install the handler on
     */
    public static void installF1Handler(Dialog<?> dialog) {
        dialog.getDialogPane().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.F1) {
                openHelpForDialog(dialog);
                event.consume();
            }
        });
    }

    /**
     * Adds a visible "?" help button and an F1 key handler to the given dialog.
     * The help button appears on the left side of the button bar. Clicking it
     * (or pressing F1) opens the context help viewer for the dialog's topic.
     *
     * <p>This is the preferred method for wiring help into dialogs, replacing
     * the older {@link #installF1Handler(Dialog)} which only supported F1.
     *
     * @param dialog the dialog to add the help button and F1 handler to
     */
    public static void addHelpButton(Dialog<?> dialog) {
        installF1Handler(dialog);

        ButtonType helpType = new ButtonType("?", ButtonBar.ButtonData.HELP);
        dialog.getDialogPane().getButtonTypes().add(helpType);

        Button helpButton = (Button) dialog.getDialogPane().lookupButton(helpType);
        helpButton.setId("dialogHelpButton");
        helpButton.setTooltip(new Tooltip("Help (F1)"));
        helpButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            openHelpForDialog(dialog);
        });
    }

    private static void openHelpForDialog(Dialog<?> dialog) {
        String className = dialog.getClass().getSimpleName();
        HelpTopic topic = topicForDialog(className);
        Window owner = dialog.getDialogPane().getScene().getWindow();

        // Reuse cached dialog for this owner window, or create a new one
        ContextHelpDialog helpDialog = null;
        WeakReference<ContextHelpDialog> ref = helpDialogCache.get(owner);
        if (ref != null) {
            helpDialog = ref.get();
        }
        if (helpDialog == null) {
            helpDialog = new ContextHelpDialog();
            helpDialog.initOwner(owner);
            helpDialogCache.put(owner, new WeakReference<>(helpDialog));
        }
        helpDialog.showTopic(topic);
        helpDialog.show();
        helpDialog.toFront();
    }

    private static boolean isEquationFieldFocused(Node focusOwner) {
        if (focusOwner == null) {
            return false;
        }
        // Walk up the scene graph looking for an ancestor with the equation field ID
        Node node = focusOwner;
        while (node != null) {
            String id = node.getId();
            if (id != null && id.startsWith("equationField")) {
                return true;
            }
            // CodeAreaEquationField wraps a CodeArea — check class name
            String className = node.getClass().getSimpleName();
            if ("CodeArea".equals(className) || "CodeAreaEquationField".equals(className)) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }
}
