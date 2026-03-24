package systems.courant.sd.app;

import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Builds the application {@link MenuBar} from a {@link CommandRegistry}.
 * Every action is defined once in the registry; this class handles menu
 * structure, separators, {@link CheckMenuItem} toggles, and editor-only
 * disable/enable state.
 */
final class MenuBarBuilder {

    /**
     * Holds the constructed menu bar and references to items that
     * {@link ModelWindow} needs to update dynamically after construction.
     */
    record Result(
            MenuBar menuBar,
            MenuItem undoItem,
            MenuItem redoItem,
            MenuItem undoHistoryItem,
            MenuItem popOutDashboardItem,
            MenuItem validationIssuesItem,
            List<MenuItem> editorOnlyItems
    ) {}

    private final CommandRegistry registry;
    private final Menu examplesMenu;
    private final Consumer<Boolean> onActivityLogToggle;
    private final Consumer<Boolean> onHideVariablesToggle;
    private final Consumer<Boolean> onShowDelayToggle;
    private final Consumer<Boolean> onHideInfoLinksToggle;

    private final List<MenuItem> editorOnlyItems = new ArrayList<>();
    private MenuItem undoItem;
    private MenuItem redoItem;
    private MenuItem undoHistoryItem;
    private MenuItem popOutDashboardItem;
    private MenuItem validationIssuesItem;

    MenuBarBuilder(CommandRegistry registry, Menu examplesMenu,
                   Consumer<Boolean> onActivityLogToggle,
                   Consumer<Boolean> onHideVariablesToggle,
                   Consumer<Boolean> onShowDelayToggle,
                   Consumer<Boolean> onHideInfoLinksToggle) {
        this.registry = registry;
        this.examplesMenu = examplesMenu;
        this.onActivityLogToggle = onActivityLogToggle;
        this.onHideVariablesToggle = onHideVariablesToggle;
        this.onShowDelayToggle = onShowDelayToggle;
        this.onHideInfoLinksToggle = onHideInfoLinksToggle;
    }

    Result build() {
        Menu fileMenu = buildFileMenu();
        Menu editMenu = buildEditMenu();
        Menu viewMenu = buildViewMenu();
        Menu layoutMenu = buildLayoutMenu();
        Menu simulateMenu = buildSimulateMenu();
        Menu helpMenu = buildHelpMenu();

        MenuBar menuBar = new MenuBar(fileMenu, editMenu, viewMenu,
                layoutMenu, simulateMenu, helpMenu);
        return new Result(menuBar, undoItem, redoItem, undoHistoryItem,
                popOutDashboardItem, validationIssuesItem,
                List.copyOf(editorOnlyItems));
    }

    private Menu buildFileMenu() {
        Menu fileMenu = new Menu("File");

        MenuItem newWindowItem = registry.toMenuItem("New Window");
        MenuItem newItem = registry.toMenuItem("New Model", "New");
        MenuItem openItem = registry.toMenuItem("Open Model", "Open...");
        MenuItem saveItem = registry.toMenuItem("Save");
        MenuItem saveAsItem = registry.toMenuItem("Save As", "Save As...");
        MenuItem exportItem = registry.toMenuItem("Export Diagram",
                "Export Diagram...");
        MenuItem exportReportItem = registry.toMenuItem("Export Report",
                "Export Report...");
        MenuItem modelInfoItem = registry.toMenuItem("Model Info",
                "Model Info\u2026");
        MenuItem importRefDataItem = registry.toMenuItem(
                "Import Reference Data", "Import Reference Data\u2026");
        MenuItem closeItem = registry.toMenuItem("Close");
        MenuItem exitItem = registry.toMenuItem("Exit");

        // Disable items that require an open model
        closeItem.setDisable(true);
        saveItem.setDisable(true);
        saveAsItem.setDisable(true);
        exportItem.setDisable(true);
        exportReportItem.setDisable(true);
        modelInfoItem.setDisable(true);
        importRefDataItem.setDisable(true);
        editorOnlyItems.addAll(List.of(closeItem, saveItem, saveAsItem,
                exportItem, exportReportItem, modelInfoItem,
                importRefDataItem));

        fileMenu.getItems().addAll(newWindowItem, newItem, openItem,
                examplesMenu,
                new SeparatorMenuItem(), modelInfoItem, importRefDataItem,
                new SeparatorMenuItem(), saveItem, saveAsItem, exportItem,
                exportReportItem,
                new SeparatorMenuItem(), closeItem, exitItem);

        return fileMenu;
    }

    private Menu buildEditMenu() {
        Menu editMenu = new Menu("Edit");

        undoItem = registry.toMenuItem("Undo");
        undoItem.setDisable(true);
        redoItem = registry.toMenuItem("Redo");
        redoItem.setDisable(true);
        undoHistoryItem = registry.toMenuItem("Undo History",
                "Undo History\u2026");
        undoHistoryItem.setDisable(true);
        MenuItem cutItem = registry.toMenuItem("Cut");
        MenuItem copyItem = registry.toMenuItem("Copy");
        MenuItem pasteItem = registry.toMenuItem("Paste");
        MenuItem selectAllItem = registry.toMenuItem("Select All");

        editMenu.getItems().addAll(undoItem, redoItem, undoHistoryItem,
                new SeparatorMenuItem(),
                cutItem, copyItem, pasteItem,
                new SeparatorMenuItem(), selectAllItem);
        editMenu.setDisable(true);
        editorOnlyItems.add(editMenu);

        return editMenu;
    }

    private Menu buildViewMenu() {
        Menu viewMenu = new Menu("View");

        MenuItem commandPaletteItem = registry.toMenuItem("Command Palette",
                "Command Palette\u2026");
        MenuItem zoomToFitItem = registry.toMenuItem("Zoom to Fit");
        MenuItem resetZoomItem = registry.toMenuItem("Reset Zoom");

        validationIssuesItem = registry.toMenuItem("Validation Issues",
                "Validation Issues\u2026");
        validationIssuesItem.setDisable(true);

        CheckMenuItem activityLogItem = checkItem("Activity Log", null,
                new KeyCodeCombination(KeyCode.L,
                        KeyCombination.SHORTCUT_DOWN),
                onActivityLogToggle);

        CheckMenuItem hideAuxItem = checkItem("Hide Variables",
                "menuHideVariables", null, onHideVariablesToggle);

        CheckMenuItem showDelayItem = checkItem("Show Delay Indicators",
                "menuShowDelayIndicators", null, onShowDelayToggle);

        CheckMenuItem hideInfoLinksItem = checkItem("Hide Info Links",
                "menuHideInfoLinks", null, onHideInfoLinksToggle);

        popOutDashboardItem = registry.toMenuItem(
                "Pop Out / Dock Dashboard", "Pop Out Dashboard");

        viewMenu.getItems().addAll(commandPaletteItem,
                new SeparatorMenuItem(),
                zoomToFitItem, resetZoomItem,
                new SeparatorMenuItem(),
                hideAuxItem, hideInfoLinksItem, showDelayItem,
                new SeparatorMenuItem(),
                validationIssuesItem,
                new SeparatorMenuItem(),
                activityLogItem, popOutDashboardItem);
        viewMenu.setDisable(true);
        editorOnlyItems.add(viewMenu);

        return viewMenu;
    }

    private Menu buildLayoutMenu() {
        Menu layoutMenu = new Menu("Layout");

        Menu alignMenu = new Menu("Align");
        alignMenu.getItems().addAll(
                registry.toMenuItem("Align Left"),
                registry.toMenuItem("Align Center Horizontal",
                        "Align Center (Horizontal)"),
                registry.toMenuItem("Align Right"),
                new SeparatorMenuItem(),
                registry.toMenuItem("Align Top"),
                registry.toMenuItem("Align Center Vertical",
                        "Align Center (Vertical)"),
                registry.toMenuItem("Align Bottom"));

        Menu distributeMenu = new Menu("Distribute");
        distributeMenu.getItems().addAll(
                registry.toMenuItem("Distribute Horizontally"),
                registry.toMenuItem("Distribute Vertically"));

        MenuItem snapToGridItem = registry.toMenuItem("Snap to Grid");

        layoutMenu.getItems().addAll(alignMenu, distributeMenu,
                new SeparatorMenuItem(), snapToGridItem);
        layoutMenu.setDisable(true);
        editorOnlyItems.add(layoutMenu);

        return layoutMenu;
    }

    private Menu buildSimulateMenu() {
        Menu simulateMenu = new Menu("Simulate");

        MenuItem settingsItem = registry.toMenuItem("Simulation Settings",
                "Simulation Settings...");
        MenuItem runItem = registry.toMenuItem("Run Simulation");
        MenuItem validateItem = registry.toMenuItem("Validate Model");
        MenuItem extremeCondItem = registry.toMenuItem("Extreme Conditions",
                "Extreme Conditions...");
        MenuItem sweepItem = registry.toMenuItem("Parameter Sweep",
                "Parameter Sweep...");
        MenuItem multiSweepItem = registry.toMenuItem(
                "Multi-Parameter Sweep", "Multi-Parameter Sweep...");
        MenuItem monteCarloItem = registry.toMenuItem("Monte Carlo",
                "Monte Carlo...");
        MenuItem optimizeItem = registry.toMenuItem("Optimize",
                "Optimize...");
        MenuItem calibrateItem = registry.toMenuItem("Calibrate",
                "Calibrate...");

        simulateMenu.getItems().addAll(settingsItem, runItem,
                new SeparatorMenuItem(), validateItem, extremeCondItem,
                new SeparatorMenuItem(), sweepItem, multiSweepItem,
                monteCarloItem, optimizeItem, calibrateItem);
        simulateMenu.setDisable(true);
        editorOnlyItems.add(simulateMenu);

        return simulateMenu;
    }

    private Menu buildHelpMenu() {
        Menu helpMenu = new Menu("Help");

        MenuItem contextHelpItem = registry.toMenuItem("Context Help");

        Menu tutorialsMenu = new Menu("Tutorials");
        MenuItem gettingStartedItem = registry.toMenuItem("Getting Started",
                "Getting Started\u2026");
        MenuItem sirTutorialItem = registry.toMenuItem(
                "Tutorial: SIR Epidemic", "SIR Epidemic\u2026");
        MenuItem supplyChainItem = registry.toMenuItem(
                "Tutorial: Supply Chain", "Supply Chain\u2026");
        MenuItem cldTutorialItem = registry.toMenuItem(
                "Tutorial: Causal Loop Diagrams", "Causal Loop Diagrams\u2026");
        tutorialsMenu.getItems().addAll(gettingStartedItem, sirTutorialItem,
                supplyChainItem, cldTutorialItem);

        MenuItem sdConceptsItem = registry.toMenuItem("SD Concepts");
        MenuItem sdTerminologyItem = registry.toMenuItem("SD Terminology");
        MenuItem exprLangItem = registry.toMenuItem("Expression Language");
        MenuItem shortcutsItem = registry.toMenuItem("Keyboard Shortcuts");
        MenuItem aboutItem = registry.toMenuItem("About Courant");

        helpMenu.getItems().addAll(contextHelpItem,
                new SeparatorMenuItem(),
                tutorialsMenu,
                new SeparatorMenuItem(), sdConceptsItem, sdTerminologyItem,
                exprLangItem,
                new SeparatorMenuItem(), shortcutsItem,
                new SeparatorMenuItem(), aboutItem);

        return helpMenu;
    }

    private static CheckMenuItem checkItem(String text, String id,
                                           KeyCombination accelerator,
                                           Consumer<Boolean> onToggle) {
        CheckMenuItem item = new CheckMenuItem(text);
        if (id != null) {
            item.setId(id);
        }
        if (accelerator != null) {
            item.setAccelerator(accelerator);
        }
        item.setOnAction(e -> onToggle.accept(item.isSelected()));
        return item;
    }
}
