package systems.courant.sd.app;

import systems.courant.sd.app.canvas.AnalysisRunner;
import systems.courant.sd.app.canvas.ActivityLogPanel;
import systems.courant.sd.app.canvas.BreadcrumbBar;
import systems.courant.sd.app.canvas.CanvasToolBar;
import systems.courant.sd.app.canvas.Clipboard;
import systems.courant.sd.app.canvas.dialogs.ColumnMappingDialog;
import systems.courant.sd.app.canvas.CommandPalette;
import systems.courant.sd.app.canvas.dialogs.ContextHelpDialog;
import systems.courant.sd.app.canvas.LoopNavigatorBar;
import systems.courant.sd.app.canvas.DashboardPanel;
import systems.courant.sd.app.canvas.DiagramExporter;
import systems.courant.sd.app.canvas.ReportExporter;
import systems.courant.sd.app.canvas.dialogs.ExpressionLanguageDialog;
import systems.courant.sd.app.canvas.HelpContextResolver;
import systems.courant.sd.app.canvas.HelpTopic;
import systems.courant.sd.app.canvas.ModelCanvas;
import systems.courant.sd.app.canvas.ModelEditListener;
import systems.courant.sd.app.canvas.ModelEditor;
import systems.courant.sd.app.canvas.PropertiesPanel;
import systems.courant.sd.app.canvas.dialogs.QuickstartDialog;
import systems.courant.sd.app.canvas.dialogs.SirTutorialDialog;
import systems.courant.sd.app.canvas.dialogs.SupplyChainTutorialDialog;
import systems.courant.sd.app.canvas.dialogs.TutorialChooserDialog;
import systems.courant.sd.app.canvas.dialogs.KeyboardShortcutsDialog;
import systems.courant.sd.app.canvas.dialogs.SdConceptsDialog;
import systems.courant.sd.app.canvas.StatusBar;
import systems.courant.sd.app.canvas.UndoHistoryPopup;
import systems.courant.sd.app.canvas.UndoManager;
import systems.courant.sd.app.canvas.dialogs.ValidationDialog;
import systems.courant.sd.app.canvas.ZoomOverlay;
import systems.courant.sd.io.ReferenceDataCsvReader;
import systems.courant.sd.model.ModelMetadata;
import systems.courant.sd.model.def.ReferenceDataset;
import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ValidationResult;
import systems.courant.sd.model.def.ViewDef;
import systems.courant.sd.model.graph.AutoLayout;
import systems.courant.sd.model.graph.ElementSizes;
import systems.courant.sd.model.graph.FeedbackAnalysis;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * An independent editor window for a single Courant model.
 * Each window owns its own canvas, editor, undo stack, and file state.
 *
 * <p>File I/O is handled by {@link FileController} and simulation/analysis
 * by {@link SimulationController}.
 */
public class ModelWindow {

    private static final Logger log = LoggerFactory.getLogger(ModelWindow.class);

    private final Stage stage;
    private final CourantApp app;
    private final Clipboard clipboard;
    private ModelCanvas canvas;
    private ModelEditor editor;
    private StatusBar statusBar;
    private BreadcrumbBar breadcrumbBar;
    private PropertiesPanel propertiesPanel;
    private ActivityLogPanel activityLogPanel;
    private DashboardPanel dashboardPanel;
    private TabPane rightTabPane;
    private Tab dashboardTab;
    private Stage dashboardStage;
    private MenuItem popOutDashboardItem;
    private BorderPane root;
    private final UndoManager undoManager = new UndoManager();
    private MenuItem undoItem;
    private MenuItem redoItem;
    private ModelEditListener logListener;
    private ModelEditListener staleListener;
    private AnalysisRunner analysisRunner;
    private CommandPalette commandPalette;
    private CommandRegistry commandRegistry;
    private ZoomOverlay zoomOverlay;
    private HelpWindowManager helpWindows;
    private ContextHelpDialog contextHelpDialog;

    private FileController fileController;
    private SimulationController simulationController;
    private ModelEditListener dirtyListener;
    private volatile CompletableFuture<Void> pendingLayout = CompletableFuture.completedFuture(null);

    private SplitPane editorSplitPane;
    private VBox topContainer;
    private CanvasToolBar toolBar;
    private MenuBar menuBar;
    private LoopNavigatorBar loopNavigatorBar;
    private volatile boolean editorShown;
    private final List<MenuItem> editorOnlyItems = new ArrayList<>();
    private MenuItem validationIssuesItem;

    public ModelWindow(Stage stage, CourantApp app, Clipboard clipboard) {
        this.stage = stage;
        this.app = app;
        this.clipboard = clipboard;
        buildUI();
    }

    private void buildUI() {
        canvas = new ModelCanvas(clipboard);
        canvas.undo().setUndoManager(undoManager);

        statusBar = new StatusBar();
        analysisRunner = new AnalysisRunner(statusBar, this::showError);

        // Dashboard panel (created before controllers that reference it)
        dashboardPanel = new DashboardPanel();
        staleListener = createStaleListener();

        // Activity log panel (hidden by default) — created early so logListener
        // is available when fileController.newModel() triggers loadDefinition
        activityLogPanel = new ActivityLogPanel();
        activityLogPanel.setVisible(false);
        activityLogPanel.setManaged(false);
        logListener = activityLogPanel.createListener();

        // Controllers
        fileController = new FileController(stage, canvas,
                this::loadDefinition, this::updateTitle,
                msg -> showError("File Error", msg),
                this::fireLogEvent);
        simulationController = new SimulationController(canvas, analysisRunner,
                dashboardPanel, this::switchToDashboard, statusBar,
                msg -> showError("Error", msg),
                this::fireLogEvent);

        dashboardPanel.setRerunAction(simulationController::runSimulation);
        dashboardPanel.setOnVariableClicked(name -> {
            canvas.elements().selectElement(name);
            canvas.requestFocus();
        });
        dashboardPanel.setOnReferenceDataImported(dataset -> {
            canvas.getEditor().addReferenceDataset(dataset);
            simulationController.runSimulation();
        });

        configureToolBarAndNavigation();
        configureCanvasCallbacks();
        createRightPanel();

        menuBar = createMenuBar();
        topContainer = new VBox(menuBar, toolBar, loopNavigatorBar, breadcrumbBar);

        root = new BorderPane();
        root.setId("modelWindowRoot");

        configureStartScreen();

        Scene scene = new Scene(root, 1200, 800);
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> {
            if (!fileController.confirmDiscardChanges()) {
                event.consume();
                return;
            }
            close();
        });

        helpWindows = new HelpWindowManager(stage);
        commandPalette = new CommandPalette(this::buildCommands);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isShortcutDown() && event.getCode() == KeyCode.K) {
                commandPalette.show(stage);
                event.consume();
            }
            if (event.getCode() == KeyCode.F1) {
                showContextHelp();
                event.consume();
            }
        });

        updateTitle();

        // When the window gains focus (e.g. switching from another window),
        // give focus to the canvas so keyboard shortcuts (Ctrl+V, etc.) work
        // immediately without requiring a click first.
        stage.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                // Don't steal focus from MenuBar menus — doing so closes them
                boolean menuShowing = menuBar.getMenus().stream().anyMatch(Menu::isShowing);
                if (menuShowing) {
                    return;
                }
                Node focused = scene.getFocusOwner();
                if (focused == null || focused == root
                        || !(focused instanceof javafx.scene.control.TextInputControl)) {
                    canvas.requestFocus();
                }
            }
        });

        canvas.requestFocus();
    }

    private void configureToolBarAndNavigation() {
        toolBar = new CanvasToolBar();
        toolBar.setOnToolChanged(tool -> {
            canvas.setActiveTool(tool);
            statusBar.updateTool(tool);
        });
        loopNavigatorBar = new LoopNavigatorBar();
        loopNavigatorBar.setVisible(false);
        loopNavigatorBar.setManaged(false);
        loopNavigatorBar.setOnPrev(() -> {
            canvas.analysis().stepLoopBack();
            updateLoopNavigator();
            canvas.requestFocus();
        });
        loopNavigatorBar.setOnNext(() -> {
            canvas.analysis().stepLoopForward();
            updateLoopNavigator();
            canvas.requestFocus();
        });
        loopNavigatorBar.setOnShowAll(() -> {
            canvas.analysis().setActiveLoopIndex(-1);
            updateLoopNavigator();
            canvas.requestFocus();
        });
        loopNavigatorBar.setOnFilterChanged(filter -> {
            canvas.analysis().setLoopTypeFilter(filter);
            updateLoopNavigator();
            canvas.requestFocus();
        });
        toolBar.setOnLoopToggleChanged(active -> {
            canvas.analysis().setLoopHighlightActive(active);
            loopNavigatorBar.setVisible(active);
            loopNavigatorBar.setManaged(active);
            if (!active) {
                loopNavigatorBar.resetFilter();
            }
            updateLoopStatus();
            updateLoopNavigator();
        });
        toolBar.setOnValidateClicked(simulationController::validateModel);
        toolBar.setOnSearchClicked(() -> commandPalette.show(stage));
    }

    private void configureCanvasCallbacks() {
        canvas.setToolBar(toolBar);
        canvas.setOnStatusChanged(() -> {
            updateStatusBar();
            if (canvas.analysis().isLoopHighlightActive()) {
                updateLoopNavigator();
            }
            if (propertiesPanel != null) {
                propertiesPanel.updateSelection(canvas, canvas.getEditor());
            }
        });
        canvas.setOnPasteWarning(replaced -> {
            String names = String.join(", ", replaced);
            activityLogPanel.log("warning",
                    "Paste: " + replaced.size() + " reference"
                    + (replaced.size() == 1 ? "" : "s")
                    + " replaced with 0 (" + names + ")");
        });
        canvas.analysis().setOnValidationChanged(result -> {
            statusBar.updateValidation(result.errorCount(), result.warningCount());
            if (validationIssuesItem != null) {
                validationIssuesItem.setDisable(result.isClean());
            }
        });
        statusBar.setOnValidationClicked(this::showValidationDialog);

        breadcrumbBar = new BreadcrumbBar();
        breadcrumbBar.setOnNavigateTo(depth -> {
            canvas.navigation().navigateToDepth(depth);
            canvas.requestFocus();
        });
        canvas.navigation().setOnNavigationChanged(this::updateBreadcrumb);
    }

    private void createRightPanel() {
        Pane canvasPane = new Pane(canvas);
        canvas.widthProperty().bind(canvasPane.widthProperty());
        canvas.heightProperty().bind(canvasPane.heightProperty());
        canvas.setOverlayPane(canvasPane);

        zoomOverlay = new ZoomOverlay(canvas);
        zoomOverlay.anchorTo(canvasPane);

        propertiesPanel = new PropertiesPanel();
        propertiesPanel.setOnOpenExpressionHelp(() -> {
            ExpressionLanguageDialog eld = helpWindows.showOrBring(
                    ExpressionLanguageDialog.class, ExpressionLanguageDialog::new);
            eld.focusSdFunctions();
        });
        propertiesPanel.setModelSummaryActions(
                simulationController::runSimulation,
                simulationController::validateModel,
                simulationController::openSimulationSettings);

        // Right-side TabPane with Properties and Dashboard tabs
        rightTabPane = new TabPane();
        rightTabPane.setId("rightTabPane");
        rightTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Tab propertiesTab = new Tab("Properties", propertiesPanel);
        propertiesTab.setId("propertiesTab");
        dashboardTab = new Tab("Dashboard", dashboardPanel);
        dashboardTab.setId("dashboardTab");
        dashboardPanel.setDashboardTab(dashboardTab);
        rightTabPane.getTabs().addAll(propertiesTab, dashboardTab);

        editorSplitPane = new SplitPane(canvasPane, rightTabPane);
        editorSplitPane.setDividerPositions(0.75);
        SplitPane.setResizableWithParent(rightTabPane, false);
    }

    private void configureStartScreen() {
        StartScreen startScreen = new StartScreen();
        startScreen.setOnNewModel(() -> {
            showEditor();
            fileController.newModel();
            canvas.requestFocus();
        });
        startScreen.setOnOpenFile(() -> {
            showEditor();
            fileController.openFile();
            // If user cancelled the file chooser, still show the editor
            canvas.requestFocus();
        });
        startScreen.setOnTutorials(() -> {
            TutorialChooserDialog chooser = new TutorialChooserDialog();
            chooser.setOnGettingStarted(() -> {
                showEditor();
                fileController.newModel();
                helpWindows.showOrBring(QuickstartDialog.class, QuickstartDialog::new);
                canvas.requestFocus();
            });
            chooser.setOnSirTutorial(() -> {
                showEditor();
                fileController.newModel();
                helpWindows.showOrBring(SirTutorialDialog.class, SirTutorialDialog::new);
                canvas.requestFocus();
            });
            chooser.setOnSupplyChainTutorial(() -> {
                showEditor();
                fileController.newModel();
                helpWindows.showOrBring(SupplyChainTutorialDialog.class,
                        SupplyChainTutorialDialog::new);
                canvas.requestFocus();
            });
            chooser.show();
        });
        startScreen.setOnOpenExample((name, path) -> {
            showEditor();
            fileController.openExample(name, path);
            canvas.requestFocus();
        });

        toolBar.setVisible(false);
        toolBar.setManaged(false);
        root.setTop(topContainer);
        root.setCenter(startScreen);
        root.setBottom(statusBar);
    }

    /**
     * Transitions from the start screen to the full editor layout.
     * Safe to call multiple times; subsequent calls are no-ops.
     */
    private void showEditor() {
        if (editorShown) {
            return;
        }
        editorShown = true;
        toolBar.setVisible(true);
        toolBar.setManaged(true);
        root.setCenter(editorSplitPane);
        for (MenuItem item : editorOnlyItems) {
            item.setDisable(false);
        }
    }

    private MenuBar createMenuBar() {
        Menu fileMenu = createFileMenu();
        Menu editMenu = createEditMenu();
        Menu viewMenu = createViewMenu();
        Menu simulateMenu = createSimulateMenu();
        Menu helpMenu = createHelpMenu();

        return new MenuBar(fileMenu, editMenu, viewMenu, simulateMenu, helpMenu);
    }

    private Menu createFileMenu() {
        Menu fileMenu = new Menu("File");

        MenuItem newWindowItem = new MenuItem("New Window");
        newWindowItem.setId("menuNewWindow");
        newWindowItem.setAccelerator(new KeyCodeCombination(KeyCode.N,
                KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        newWindowItem.setOnAction(e -> app.openNewWindow());

        MenuItem newItem = new MenuItem("New");
        newItem.setId("menuNew");
        newItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN));
        newItem.setOnAction(e -> {
            showEditor();
            fileController.newModel();
        });

        MenuItem openItem = new MenuItem("Open...");
        openItem.setId("menuOpen");
        openItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN));
        openItem.setOnAction(e -> {
            showEditor();
            fileController.openFile();
        });

        MenuItem saveItem = new MenuItem("Save");
        saveItem.setId("menuSave");
        saveItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));
        saveItem.setOnAction(e -> fileController.save());

        MenuItem saveAsItem = new MenuItem("Save As...");
        saveAsItem.setId("menuSaveAs");
        saveAsItem.setAccelerator(new KeyCodeCombination(KeyCode.S,
                KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        saveAsItem.setOnAction(e -> fileController.saveAs());

        MenuItem exportItem = new MenuItem("Export Diagram...");
        exportItem.setId("menuExport");
        exportItem.setAccelerator(new KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN));
        exportItem.setOnAction(e -> DiagramExporter.exportDiagram(
                canvas.getCanvasState(), canvas.getEditor(),
                canvas.getConnectors(), canvas.analysis().getActiveLoopAnalysis(), stage,
                editor != null ? editor.getModelName() : null));

        MenuItem exportReportItem = new MenuItem("Export Report...");
        exportReportItem.setId("menuExportReport");
        exportReportItem.setOnAction(e -> ReportExporter.exportReport(
                canvas.getCanvasState(), canvas.getEditor(),
                canvas.getConnectors(), canvas.analysis().getActiveLoopAnalysis(), stage,
                editor != null ? editor.getModelName() : null));

        MenuItem closeItem = new MenuItem("Close");
        closeItem.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN));
        closeItem.setOnAction(e -> {
            if (fileController.confirmDiscardChanges()) {
                resetToStartScreen();
            }
        });

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> {
            if (fileController.confirmDiscardChanges()) {
                Platform.exit();
            }
        });

        Menu examplesMenu = fileController.buildExamplesMenu();

        MenuItem modelInfoItem = new MenuItem("Model Info\u2026");
        modelInfoItem.setOnAction(e -> showModelInfoDialog());

        MenuItem importRefDataItem = new MenuItem("Import Reference Data\u2026");
        importRefDataItem.setId("menuImportRefData");
        importRefDataItem.setOnAction(e -> importReferenceData());

        // Disable file items that require an open model
        closeItem.setDisable(true);
        saveItem.setDisable(true);
        saveAsItem.setDisable(true);
        exportItem.setDisable(true);
        exportReportItem.setDisable(true);
        modelInfoItem.setDisable(true);
        importRefDataItem.setDisable(true);
        editorOnlyItems.addAll(List.of(closeItem, saveItem, saveAsItem, exportItem, exportReportItem,
                modelInfoItem, importRefDataItem));

        fileMenu.getItems().addAll(newWindowItem, newItem, openItem, examplesMenu,
                new SeparatorMenuItem(), modelInfoItem, importRefDataItem,
                new SeparatorMenuItem(), saveItem, saveAsItem, exportItem, exportReportItem,
                new SeparatorMenuItem(), closeItem, exitItem);

        return fileMenu;
    }

    private Menu createEditMenu() {
        Menu editMenu = new Menu("Edit");

        undoItem = new MenuItem("Undo");
        undoItem.setId("menuUndo");
        undoItem.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN));
        undoItem.setOnAction(e -> {
            canvas.undo().performUndo();
            canvas.requestFocus();
        });
        undoItem.setDisable(true);

        redoItem = new MenuItem("Redo");
        redoItem.setId("menuRedo");
        redoItem.setAccelerator(new KeyCodeCombination(KeyCode.Z,
                KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        redoItem.setOnAction(e -> {
            canvas.undo().performRedo();
            canvas.requestFocus();
        });
        redoItem.setDisable(true);

        MenuItem undoHistoryItem = new MenuItem("Undo History\u2026");
        undoHistoryItem.setId("menuUndoHistory");
        undoHistoryItem.setOnAction(e -> showUndoHistoryPopup());

        MenuItem cutItem = new MenuItem("Cut");
        cutItem.setAccelerator(new KeyCodeCombination(KeyCode.X, KeyCombination.SHORTCUT_DOWN));
        cutItem.setOnAction(e -> {
            canvas.elements().cutSelection();
            canvas.requestFocus();
        });

        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN));
        copyItem.setOnAction(e -> {
            canvas.elements().copySelection();
            canvas.requestFocus();
        });

        MenuItem pasteItem = new MenuItem("Paste");
        pasteItem.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN));
        pasteItem.setOnAction(e -> {
            canvas.elements().pasteClipboard();
            canvas.requestFocus();
        });

        MenuItem selectAllItem = new MenuItem("Select All");
        selectAllItem.setAccelerator(new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN));
        selectAllItem.setOnAction(e -> {
            canvas.elements().selectAll();
            canvas.requestFocus();
        });

        editMenu.getItems().addAll(undoItem, redoItem, undoHistoryItem,
                new SeparatorMenuItem(),
                cutItem, copyItem, pasteItem, new SeparatorMenuItem(), selectAllItem);
        editMenu.setDisable(true);
        editorOnlyItems.add(editMenu);

        return editMenu;
    }

    private Menu createViewMenu() {
        Menu viewMenu = new Menu("View");

        CheckMenuItem activityLogItem = new CheckMenuItem("Activity Log");
        activityLogItem.setAccelerator(new KeyCodeCombination(KeyCode.L, KeyCombination.SHORTCUT_DOWN));
        activityLogItem.setOnAction(e -> {
            boolean show = activityLogItem.isSelected();
            activityLogPanel.setVisible(show);
            activityLogPanel.setManaged(show);
            if (show) {
                root.setLeft(activityLogPanel);
            } else {
                root.setLeft(null);
            }
        });

        popOutDashboardItem = new MenuItem("Pop Out Dashboard");
        popOutDashboardItem.setId("menuPopOutDashboard");
        popOutDashboardItem.setAccelerator(new KeyCodeCombination(KeyCode.D,
                KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        popOutDashboardItem.setOnAction(e -> {
            if (dashboardStage == null) {
                popOutDashboard();
            } else {
                dockDashboard();
            }
        });

        MenuItem commandPaletteItem = new MenuItem("Command Palette\u2026");
        commandPaletteItem.setAccelerator(new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN));
        commandPaletteItem.setOnAction(e -> commandPalette.show(stage));

        MenuItem zoomToFitItem = new MenuItem("Zoom to Fit");
        zoomToFitItem.setId("menuZoomToFit");
        zoomToFitItem.setAccelerator(new KeyCodeCombination(KeyCode.F,
                KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        zoomToFitItem.setOnAction(e -> { canvas.zoomToFit(); canvas.requestFocus(); });

        MenuItem resetZoomItem = new MenuItem("Reset Zoom");
        resetZoomItem.setId("menuResetZoom");
        resetZoomItem.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.SHORTCUT_DOWN));
        resetZoomItem.setOnAction(e -> { canvas.resetZoom(); canvas.requestFocus(); });

        validationIssuesItem = new MenuItem("Validation Issues\u2026");
        validationIssuesItem.setId("menuValidationIssues");
        validationIssuesItem.setDisable(true);
        validationIssuesItem.setOnAction(e -> showValidationDialog());

        CheckMenuItem hideAuxItem = new CheckMenuItem("Hide Variables");
        hideAuxItem.setId("menuHideVariables");
        hideAuxItem.setOnAction(e -> { canvas.setHideVariables(hideAuxItem.isSelected()); canvas.requestFocus(); });

        CheckMenuItem showDelayItem = new CheckMenuItem("Show Delay Indicators");
        showDelayItem.setId("menuShowDelayIndicators");
        showDelayItem.setOnAction(e -> {
            canvas.setShowDelayBadges(showDelayItem.isSelected()); canvas.requestFocus(); });

        CheckMenuItem hideInfoLinksItem = new CheckMenuItem("Hide Info Links");
        hideInfoLinksItem.setId("menuHideInfoLinks");
        hideInfoLinksItem.setOnAction(e -> {
            canvas.setHideInfoLinks(hideInfoLinksItem.isSelected()); canvas.requestFocus(); });

        viewMenu.getItems().addAll(commandPaletteItem, new SeparatorMenuItem(),
                zoomToFitItem, resetZoomItem, new SeparatorMenuItem(),
                hideAuxItem, hideInfoLinksItem, showDelayItem, new SeparatorMenuItem(),
                validationIssuesItem, new SeparatorMenuItem(),
                activityLogItem, popOutDashboardItem);
        viewMenu.setDisable(true);
        editorOnlyItems.add(viewMenu);

        return viewMenu;
    }

    private Menu createSimulateMenu() {
        Menu simulateMenu = new Menu("Simulate");

        MenuItem settingsItem = new MenuItem("Simulation Settings...");
        settingsItem.setOnAction(e -> simulationController.openSimulationSettings());

        MenuItem runItem = new MenuItem("Run Simulation");
        runItem.setId("menuRunSimulation");
        runItem.setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN));
        runItem.setOnAction(e -> simulationController.runSimulation());

        MenuItem validateItem = new MenuItem("Validate Model");
        validateItem.setAccelerator(new KeyCodeCombination(KeyCode.B, KeyCombination.SHORTCUT_DOWN));
        validateItem.setOnAction(e -> simulationController.validateModel());

        MenuItem extremeCondItem = new MenuItem("Extreme Conditions...");
        extremeCondItem.setOnAction(e -> simulationController.runExtremeConditionTest());

        MenuItem sweepItem = new MenuItem("Parameter Sweep...");
        sweepItem.setOnAction(e -> simulationController.runParameterSweep());

        MenuItem multiSweepItem = new MenuItem("Multi-Parameter Sweep...");
        multiSweepItem.setOnAction(e -> simulationController.runMultiParameterSweep());

        MenuItem monteCarloItem = new MenuItem("Monte Carlo...");
        monteCarloItem.setOnAction(e -> simulationController.runMonteCarlo());

        MenuItem optimizeItem = new MenuItem("Optimize...");
        optimizeItem.setOnAction(e -> simulationController.runOptimization());

        MenuItem calibrateItem = new MenuItem("Calibrate...");
        calibrateItem.setOnAction(e -> simulationController.runCalibration());

        simulateMenu.getItems().addAll(settingsItem, runItem,
                new SeparatorMenuItem(), validateItem, extremeCondItem,
                new SeparatorMenuItem(), sweepItem, multiSweepItem, monteCarloItem, optimizeItem,
                calibrateItem);
        simulateMenu.setDisable(true);
        editorOnlyItems.add(simulateMenu);

        return simulateMenu;
    }

    private Menu createHelpMenu() {
        Menu helpMenu = new Menu("Help");

        MenuItem contextHelpItem = new MenuItem("Context Help");
        contextHelpItem.setAccelerator(new KeyCodeCombination(KeyCode.F1));
        contextHelpItem.setOnAction(e -> showContextHelp());

        MenuItem gettingStartedItem = new MenuItem("Getting Started\u2026");
        gettingStartedItem.setOnAction(e ->
                helpWindows.showOrBring(QuickstartDialog.class, QuickstartDialog::new));

        MenuItem sirTutorialItem = new MenuItem("Tutorial: SIR Epidemic\u2026");
        sirTutorialItem.setOnAction(e ->
                helpWindows.showOrBring(SirTutorialDialog.class, SirTutorialDialog::new));

        MenuItem supplyChainTutorialItem = new MenuItem("Tutorial: Supply Chain\u2026");
        supplyChainTutorialItem.setOnAction(e ->
                helpWindows.showOrBring(SupplyChainTutorialDialog.class,
                        SupplyChainTutorialDialog::new));

        MenuItem sdConceptsItem = new MenuItem("SD Concepts");
        sdConceptsItem.setOnAction(e ->
                helpWindows.showOrBring(SdConceptsDialog.class, SdConceptsDialog::new));

        MenuItem exprLangItem = new MenuItem("Expression Language");
        exprLangItem.setOnAction(e ->
                helpWindows.showOrBring(ExpressionLanguageDialog.class, ExpressionLanguageDialog::new));

        MenuItem shortcutsItem = new MenuItem("Keyboard Shortcuts");
        shortcutsItem.setOnAction(e ->
                helpWindows.showOrBring(KeyboardShortcutsDialog.class, KeyboardShortcutsDialog::new));

        MenuItem aboutItem = new MenuItem("About Courant");
        aboutItem.setOnAction(e -> {
            Alert about = new Alert(Alert.AlertType.INFORMATION);
            about.setTitle("About Courant");
            about.setHeaderText("Courant");
            about.setContentText("A visual System Dynamics modeling environment.\nVersion "
                    + AppVersion.get());
            about.showAndWait();
        });

        helpMenu.getItems().addAll(contextHelpItem,
                new SeparatorMenuItem(),
                gettingStartedItem, sirTutorialItem, supplyChainTutorialItem,
                new SeparatorMenuItem(), sdConceptsItem, exprLangItem,
                new SeparatorMenuItem(), shortcutsItem,
                new SeparatorMenuItem(), aboutItem);

        return helpMenu;
    }

    void loadDefinition(ModelDefinition def, String displayName) {
        showEditor();
        detachEditorListeners();
        if (dirtyListener == null) {
            dirtyListener = fileController.createDirtyListener();
        }
        editor = new ModelEditor();
        editor.addListener(logListener);
        editor.addListener(staleListener);
        editor.addListener(dirtyListener);
        editor.loadFrom(def);
        var editorSnapshot = this.editor;

        if (def.stocks().isEmpty() && def.flows().isEmpty()
                && def.variables().isEmpty()) {
            // CLD or empty model — use embedded view if available
            ViewDef view;
            if (!def.views().isEmpty() && !def.cldVariables().isEmpty()) {
                view = def.views().getFirst();
            } else {
                view = new ViewDef("Main", List.of(), List.of(), List.of());
            }
            applyView(editorSnapshot, view, displayName);
            pendingLayout = CompletableFuture.completedFuture(null);
        } else {
            // Run auto-layout on a background thread to keep the UI responsive.
            // ELK initialization on first use and layout of large models can
            // take over a second on a cold JVM.
            var layoutFuture = new CompletableFuture<Void>();
            pendingLayout = layoutFuture;
            statusBar.showProgress("Computing layout\u2026");
            canvas.setDisable(true);
            Thread layoutThread = new Thread(() -> {
                try {
                    var sizeOverrides = systems.courant.sd.app.canvas.LayoutMetrics
                            .computeSizeOverrides(def);
                    ViewDef view = AutoLayout.layout(def, sizeOverrides);
                    Platform.runLater(() -> {
                        canvas.setDisable(false);
                        statusBar.clearProgress();
                        applyView(editorSnapshot, view, displayName);
                        layoutFuture.complete(null);
                    });
                } catch (Exception e) {
                    log.error("Auto-layout failed", e);
                    Platform.runLater(() -> {
                        canvas.setDisable(false);
                        statusBar.clearProgress();
                        showError("Layout Error",
                                "Auto-layout failed: " + e.getMessage());
                    });
                    layoutFuture.completeExceptionally(e);
                }
            }, "auto-layout");
            layoutThread.setDaemon(true);
            layoutThread.start();
        }
    }

    private void applyView(ModelEditor ed, ViewDef view, String displayName) {
        canvas.navigation().clearNavigation();
        canvas.clearSparklines();
        canvas.setModel(ed, view);
        undoManager.clear();
        fileController.setDirty(false);
        if (dashboardPanel != null) {
            dashboardPanel.clear();
        }
        updateTitle();
        if (displayName != null) {
            fireLogEvent(l -> l.onModelOpened(displayName));
        }
    }

    private void switchToDashboard() {
        if (dashboardStage != null) {
            dashboardStage.toFront();
            dashboardStage.requestFocus();
        } else if (rightTabPane != null && rightTabPane.getTabs().contains(dashboardTab)) {
            rightTabPane.getSelectionModel().select(dashboardTab);
        }
    }

    private void popOutDashboard() {
        if (dashboardStage != null) {
            return;
        }
        rightTabPane.getTabs().remove(dashboardTab);

        dashboardStage = new Stage();
        dashboardStage.setTitle("Dashboard \u2014 " + (editor != null ? editor.getModelName() : "Courant"));
        dashboardStage.initOwner(stage);

        BorderPane dashRoot = new BorderPane(dashboardPanel);
        Scene dashScene = new Scene(dashRoot, 600, 500);
        dashboardStage.setScene(dashScene);

        dashboardStage.setOnHidden(e -> {
            if (dashboardStage != null) {
                dockDashboard();
            }
        });

        dashboardStage.show();
        popOutDashboardItem.setText("Dock Dashboard");
    }

    private void dockDashboard() {
        if (dashboardStage == null) {
            return;
        }
        Stage stageToClose = dashboardStage;
        dashboardStage = null;

        BorderPane dashRoot = (BorderPane) stageToClose.getScene().getRoot();
        dashRoot.setCenter(null);

        dashboardTab.setContent(dashboardPanel);
        if (!rightTabPane.getTabs().contains(dashboardTab)) {
            rightTabPane.getTabs().add(dashboardTab);
        }

        stageToClose.setOnHidden(null);
        stageToClose.close();

        popOutDashboardItem.setText("Pop Out Dashboard");
    }

    private void fireLogEvent(Consumer<ModelEditListener> event) {
        if (logListener != null) {
            event.accept(logListener);
        }
    }

    private ModelEditListener createStaleListener() {
        return new ModelEditListener() {
            private void markStale() {
                dashboardPanel.markStale();
                canvas.markSparklinesStale();
            }

            @Override
            public void onElementAdded(String name, String typeName) {
                markStale();
            }

            @Override
            public void onElementRemoved(String name) {
                markStale();
            }

            @Override
            public void onElementRenamed(String oldName, String newName) {
                markStale();
            }

            @Override
            public void onEquationChanged(String elementName) {
                markStale();
            }

        };
    }

    private void showContextHelp() {
        int dashboardIndex = rightTabPane.getTabs().indexOf(dashboardTab);
        Node focusOwner = stage.getScene() != null ? stage.getScene().getFocusOwner() : null;
        HelpTopic topic = HelpContextResolver.resolve(focusOwner, canvas,
                rightTabPane, dashboardIndex);
        if (contextHelpDialog == null || !contextHelpDialog.isShowing()) {
            contextHelpDialog = new ContextHelpDialog();
            contextHelpDialog.initOwner(stage);
        }
        contextHelpDialog.showTopic(topic);
        if (!contextHelpDialog.isShowing()) {
            contextHelpDialog.show();
        } else {
            HelpWindowManager.bringToFront(contextHelpDialog);
        }
    }

    private void showUndoHistoryPopup() {
        UndoManager activeUndo = canvas.undo().getUndoManager();
        if (activeUndo == null || !activeUndo.canUndo()) {
            return;
        }
        List<String> labels = activeUndo.undoLabels();
        UndoHistoryPopup popup = new UndoHistoryPopup(labels, depth -> {
            canvas.undo().performUndoTo(depth);
            canvas.requestFocus();
            updateStatusBar();
        });
        popup.showBelow(stage, stage.getX() + 60, stage.getY() + 80);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message != null ? message : "An unexpected error occurred.");
        alert.showAndWait();
    }

    private void updateStatusBar() {
        if (statusBar == null) {
            return;
        }
        ModelEditor activeEditor = canvas.getEditor();
        if (activeEditor == null) {
            return;
        }
        statusBar.updateSelection(canvas.getSelectionCount());
        statusBar.updateElements(
                activeEditor.getStocks().size(),
                activeEditor.getFlows().size(),
                activeEditor.getVariables().size(),
                0,
                activeEditor.getModules().size(),
                activeEditor.getCldVariables().size(),
                activeEditor.getCausalLinks().size());
        statusBar.updateZoom(canvas.getZoomScale());
        if (zoomOverlay != null) {
            zoomOverlay.updateZoom(canvas.getZoomScale());
        }

        UndoManager activeUndo = canvas.undo().getUndoManager();
        if (undoItem != null) {
            undoItem.setDisable(activeUndo == null || !activeUndo.canUndo());
        }
        if (redoItem != null) {
            redoItem.setDisable(activeUndo == null || !activeUndo.canRedo());
        }
    }

    private void updateLoopStatus() {
        if (statusBar == null) {
            return;
        }
        if (canvas.analysis().isLoopHighlightActive()) {
            FeedbackAnalysis analysis = canvas.analysis().getLoopAnalysis();
            int count = analysis != null ? analysis.loopCount() : 0;
            statusBar.updateLoops(count);
        } else {
            statusBar.clearLoops();
        }
    }

    private void updateLoopNavigator() {
        if (loopNavigatorBar == null) {
            return;
        }
        loopNavigatorBar.update(canvas.analysis().getLoopAnalysis(), canvas.analysis().getActiveLoopIndex(),
                canvas.analysis().getLoopTypeFilter(), canvas.analysis().getFilteredLoopCount());
    }

    private void showValidationDialog() {
        ValidationResult result = canvas.analysis().getLastValidationResult();
        if (result.isClean()) {
            return;
        }
        ValidationDialog.showOrUpdate(result, canvas.elements()::selectElement, stage);
    }

    private void importReferenceData() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import Reference Data (CSV)");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        java.io.File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }
        try {
            String name = file.getName();
            int dot = name.lastIndexOf('.');
            if (dot > 0) {
                name = name.substring(0, dot);
            }
            ReferenceDataset rawDataset = ReferenceDataCsvReader.read(
                    file.toPath(), name);

            // Show column mapping dialog
            ModelDefinition def = canvas.getEditor().toModelDefinition();
            List<String> modelVarNames = new java.util.ArrayList<>();
            def.stocks().forEach(s -> modelVarNames.add(s.name()));
            def.variables().forEach(v -> modelVarNames.add(v.name()));

            ColumnMappingDialog mappingDialog = new ColumnMappingDialog(
                    rawDataset, modelVarNames);
            mappingDialog.initOwner(stage);
            ReferenceDataset mapped = mappingDialog.showAndWait().orElse(null);
            if (mapped == null) {
                return;
            }

            canvas.getEditor().addReferenceDataset(mapped);
            simulationController.runSimulation();
            log.info("Imported reference data '{}' ({} rows, {} variables)",
                    mapped.name(), mapped.size(), mapped.variableNames().size());
        } catch (java.io.IOException ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Import Error");
            alert.setHeaderText("Failed to import reference data");
            alert.setContentText(ex.getMessage());
            alert.initOwner(stage);
            alert.showAndWait();
        }
    }

    private void showModelInfoDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Model Info");
        dialog.setHeaderText(null);
        dialog.initOwner(stage);

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        TextField nameField = new TextField(editor.getModelName());
        nameField.setPrefColumnCount(30);
        TextArea commentArea = new TextArea(
                editor.getModelComment() != null ? editor.getModelComment() : "");
        commentArea.setPrefRowCount(4);
        commentArea.setPrefColumnCount(30);

        ModelMetadata meta = editor.getMetadata();
        TextField authorField = new TextField(meta != null ? nullToEmpty(meta.author()) : "");
        authorField.setPrefColumnCount(30);
        TextField sourceField = new TextField(meta != null ? nullToEmpty(meta.source()) : "");
        sourceField.setPrefColumnCount(30);
        TextField licenseField = new TextField(meta != null ? nullToEmpty(meta.license()) : "");
        licenseField.setPrefColumnCount(30);
        TextField urlField = new TextField(meta != null ? nullToEmpty(meta.url()) : "");
        urlField.setPrefColumnCount(30);

        int row = 0;
        grid.add(new Label("Name:"), 0, row);
        grid.add(nameField, 1, row++);
        grid.add(new Label("Comment:"), 0, row);
        grid.add(commentArea, 1, row++);
        grid.add(new Label("Author:"), 0, row);
        grid.add(authorField, 1, row++);
        grid.add(new Label("Source:"), 0, row);
        grid.add(sourceField, 1, row++);
        grid.add(new Label("License:"), 0, row);
        grid.add(licenseField, 1, row++);
        grid.add(new Label("URL:"), 0, row);
        grid.add(urlField, 1, row);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(nameField::requestFocus);

        dialog.showAndWait()
                .filter(button -> button == ButtonType.OK)
                .ifPresent(button -> {
                    String newName = nameField.getText().trim();
                    editor.setModelName(newName.isEmpty() ? "Untitled" : newName);
                    editor.setModelComment(commentArea.getText().trim());

                    String author = emptyToNull(authorField.getText());
                    String source = emptyToNull(sourceField.getText());
                    String license = emptyToNull(licenseField.getText());
                    String url = emptyToNull(urlField.getText());
                    if (author != null || source != null || license != null || url != null) {
                        editor.setMetadata(ModelMetadata.builder()
                                .author(author).source(source)
                                .license(license).url(url)
                                .build());
                    } else {
                        editor.setMetadata(null);
                    }
                    updateTitle();
                });
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String emptyToNull(String s) {
        return s == null || s.trim().isEmpty() ? null : s.trim();
    }

    private void updateTitle() {
        if (!editorShown) {
            stage.setTitle("Courant");
            return;
        }
        String name;
        if (editor != null && !"Untitled".equals(editor.getModelName())) {
            name = editor.getModelName();
        } else if (fileController != null && fileController.getCurrentFile() != null) {
            Path fn = fileController.getCurrentFile().getFileName();
            name = fn != null ? fn.toString() : fileController.getCurrentFile().toString();
        } else {
            name = "Untitled";
        }
        String dirtySuffix = (fileController != null && fileController.isDirty()) ? " \u2022" : "";
        String moduleSuffix = canvas != null && canvas.navigation().isInsideModule()
                ? " [" + canvas.navigation().getCurrentModuleName() + "]"
                : "";
        stage.setTitle("Courant \u2014 " + name + dirtySuffix + moduleSuffix);
        if (dashboardStage != null) {
            dashboardStage.setTitle("Dashboard \u2014 " + name);
        }
    }

    private void updateBreadcrumb() {
        if (breadcrumbBar != null && canvas.getEditor() != null) {
            breadcrumbBar.update(canvas.navigation().getNavigationPath());
        }
        updateTitle();
        updateStatusBar();
    }

    Path getCurrentFile() {
        return fileController.getCurrentFile();
    }

    void setCurrentFile(Path path) {
        fileController.setCurrentFile(path);
    }

    ModelEditor getEditor() {
        return editor;
    }

    boolean isDirty() {
        return fileController.isDirty();
    }

    FileController getFileController() {
        return fileController;
    }

    ModelCanvas getCanvas() {
        return canvas;
    }

    /**
     * Returns a future that completes when the most recent background
     * auto-layout finishes and {@code applyView} has been called on the
     * FX thread.  Already-complete if the last load used the synchronous path.
     */
    CompletableFuture<Void> layoutFuture() {
        return pendingLayout;
    }

    private void buildRegistry() {
        commandRegistry = new CommandRegistry();
        addBuildCommands();
        addSimulateCommands();
        addViewCommands();
        addEditCommands();
        addFileCommands();
        addHelpCommands();
    }

    private void addBuildCommands() {
        commandRegistry.add("Add Stock", "Build",
                () -> switchToolAndFocus(CanvasToolBar.Tool.PLACE_STOCK));
        commandRegistry.add("Add Flow", "Build",
                () -> switchToolAndFocus(CanvasToolBar.Tool.PLACE_FLOW));
        commandRegistry.add("Add Variable", "Build",
                () -> switchToolAndFocus(CanvasToolBar.Tool.PLACE_VARIABLE));
        commandRegistry.add("Add Module", "Build",
                () -> switchToolAndFocus(CanvasToolBar.Tool.PLACE_MODULE));
        commandRegistry.add("Add Lookup Table", "Build",
                () -> switchToolAndFocus(CanvasToolBar.Tool.PLACE_LOOKUP));
        commandRegistry.add("Add CLD Variable", "Build",
                () -> switchToolAndFocus(CanvasToolBar.Tool.PLACE_CLD_VARIABLE));
        commandRegistry.add("Draw Causal Link", "Build",
                () -> switchToolAndFocus(CanvasToolBar.Tool.PLACE_CAUSAL_LINK));
        commandRegistry.add("Draw Info Link", "Build",
                () -> switchToolAndFocus(CanvasToolBar.Tool.PLACE_INFO_LINK));
        commandRegistry.add("Add Comment", "Build",
                () -> switchToolAndFocus(CanvasToolBar.Tool.PLACE_COMMENT));
        commandRegistry.add("Select Tool", "Build",
                () -> switchToolAndFocus(CanvasToolBar.Tool.SELECT));
    }

    private void addSimulateCommands() {
        commandRegistry.add("Run Simulation", "Simulate", simulationController::runSimulation);
        commandRegistry.add("Validate Model", "Simulate", simulationController::validateModel);
        commandRegistry.add("Simulation Settings", "Simulate",
                simulationController::openSimulationSettings);
        commandRegistry.add("Extreme Conditions", "Simulate",
                simulationController::runExtremeConditionTest);
        commandRegistry.add("Parameter Sweep", "Simulate",
                simulationController::runParameterSweep);
        commandRegistry.add("Multi-Parameter Sweep", "Simulate",
                simulationController::runMultiParameterSweep);
        commandRegistry.add("Monte Carlo", "Simulate", simulationController::runMonteCarlo);
        commandRegistry.add("Optimize", "Simulate", simulationController::runOptimization);
        commandRegistry.add("Calibrate", "Simulate", simulationController::runCalibration);
    }

    private void addViewCommands() {
        commandRegistry.add("Validation Issues", "View", this::showValidationDialog);
        commandRegistry.add("Zoom to Fit", "View", () -> {
            canvas.zoomToFit(); canvas.requestFocus(); });
        commandRegistry.add("Reset Zoom", "View", () -> {
            canvas.resetZoom(); canvas.requestFocus(); });
        commandRegistry.add("Zoom In", "View", () -> {
            canvas.zoomIn(); canvas.requestFocus(); });
        commandRegistry.add("Zoom Out", "View", () -> {
            canvas.zoomOut(); canvas.requestFocus(); });
        commandRegistry.add("Toggle Hide Variables", "View", () -> {
            canvas.setHideVariables(!canvas.isHideVariables()); canvas.requestFocus(); });
        commandRegistry.add("Toggle Hide Info Links", "View", () -> {
            canvas.setHideInfoLinks(!canvas.isHideInfoLinks()); canvas.requestFocus(); });
        commandRegistry.add("Toggle Delay Indicators", "View", () -> {
            canvas.setShowDelayBadges(!canvas.isShowDelayBadges()); canvas.requestFocus(); });
        commandRegistry.add("Toggle Activity Log", "View", () -> {
            boolean show = !activityLogPanel.isVisible();
            activityLogPanel.setVisible(show);
            activityLogPanel.setManaged(show);
            if (show) { root.setLeft(activityLogPanel); } else { root.setLeft(null); }
        });
        commandRegistry.add("Pop Out / Dock Dashboard", "View", () -> {
            if (dashboardStage == null) { popOutDashboard(); } else { dockDashboard(); }
        });
    }

    private void addEditCommands() {
        commandRegistry.add("Undo", "Edit", () -> {
            canvas.undo().performUndo(); canvas.requestFocus(); });
        commandRegistry.add("Redo", "Edit", () -> {
            canvas.undo().performRedo(); canvas.requestFocus(); });
        commandRegistry.add("Undo History", "Edit", this::showUndoHistoryPopup);
        commandRegistry.add("Cut", "Edit", () -> {
            canvas.elements().cutSelection(); canvas.requestFocus(); });
        commandRegistry.add("Copy", "Edit", () -> {
            canvas.elements().copySelection(); canvas.requestFocus(); });
        commandRegistry.add("Paste", "Edit", () -> {
            canvas.elements().pasteClipboard(); canvas.requestFocus(); });
        commandRegistry.add("Select All", "Edit", () -> {
            canvas.elements().selectAll(); canvas.requestFocus(); });
    }

    private void addFileCommands() {
        commandRegistry.add("New Model", "File", fileController::newModel);
        commandRegistry.add("New Window", "File", () -> app.openNewWindow());
        commandRegistry.add("Open Model", "File", fileController::openFile);
        commandRegistry.add("Save", "File", fileController::save);
        commandRegistry.add("Save As", "File", fileController::saveAs);
        commandRegistry.add("Export Diagram", "File", () -> DiagramExporter.exportDiagram(
                canvas.getCanvasState(), canvas.getEditor(),
                canvas.getConnectors(), canvas.analysis().getActiveLoopAnalysis(), stage,
                editor != null ? editor.getModelName() : null));
        commandRegistry.add("Model Info", "File", this::showModelInfoDialog);
    }

    private void addHelpCommands() {
        commandRegistry.add("Context Help", "Help", this::showContextHelp);
        commandRegistry.add("Getting Started", "Help",
                () -> helpWindows.showOrBring(QuickstartDialog.class, QuickstartDialog::new));
        commandRegistry.add("Tutorial: SIR Epidemic", "Help",
                () -> helpWindows.showOrBring(SirTutorialDialog.class, SirTutorialDialog::new));
        commandRegistry.add("Tutorial: Supply Chain", "Help",
                () -> helpWindows.showOrBring(SupplyChainTutorialDialog.class,
                        SupplyChainTutorialDialog::new));
        commandRegistry.add("SD Concepts", "Help",
                () -> helpWindows.showOrBring(SdConceptsDialog.class, SdConceptsDialog::new));
        commandRegistry.add("Expression Language", "Help",
                () -> helpWindows.showOrBring(ExpressionLanguageDialog.class,
                        ExpressionLanguageDialog::new));
        commandRegistry.add("Keyboard Shortcuts", "Help",
                () -> helpWindows.showOrBring(KeyboardShortcutsDialog.class,
                        KeyboardShortcutsDialog::new));
        commandRegistry.add("About Courant", "Help", () -> {
            Alert about = new Alert(Alert.AlertType.INFORMATION);
            about.setTitle("About Courant");
            about.setHeaderText("Courant");
            about.setContentText("A visual System Dynamics modeling environment.\nVersion "
                    + AppVersion.get());
            about.showAndWait();
        });
    }

    private List<CommandPalette.Command> buildCommands() {
        if (commandRegistry == null) {
            buildRegistry();
        }
        List<CommandPalette.Command> commands = new ArrayList<>(commandRegistry.toPaletteCommands());
        addElementNavigationCommands(commands);
        return commands;
    }

    private void addElementNavigationCommands(List<CommandPalette.Command> commands) {
        for (String name : canvas.getCanvasState().getDrawOrder()) {
            ElementType type = canvas.getCanvasState().getType(name).orElse(null);
            String category = formatElementType(type);
            commands.add(new CommandPalette.Command(name, category, () -> {
                canvas.elements().selectElement(name);
                canvas.requestFocus();
            }));
        }
    }

    private void switchToolAndFocus(CanvasToolBar.Tool tool) {
        canvas.switchTool(tool);
        canvas.requestFocus();
    }

    private static String formatElementType(ElementType type) {
        if (type == null) {
            return "Element";
        }
        return switch (type) {
            case STOCK -> "Stock";
            case FLOW -> "Flow";
            case AUX -> "Variable";
            case MODULE -> "Module";
            case LOOKUP -> "Lookup Table";
            case CLD_VARIABLE -> "CLD Variable";
            case COMMENT -> "Comment";
        };
    }

    /**
     * Resets the window back to the start screen, tearing down the current editor
     * session without closing the window. Callers must check for unsaved changes
     * via {@link FileController#confirmDiscardChanges()} before calling this method.
     */
    void resetToStartScreen() {
        detachEditorListeners();
        editor = null;

        // Cancel pending analysis tasks and create a fresh runner
        if (analysisRunner != null) {
            analysisRunner.shutdown();
        }
        analysisRunner = new AnalysisRunner(statusBar, this::showError);
        simulationController.setAnalysisRunner(analysisRunner);

        // Clear undo history
        undoManager.clear();

        // Close auxiliary windows
        if (contextHelpDialog != null) {
            contextHelpDialog.close();
            contextHelpDialog = null;
        }
        if (dashboardStage != null) {
            dashboardStage.setOnHidden(null);
            dashboardStage.close();
            dashboardStage = null;
        }

        // Clear canvas and dashboard state
        canvas.navigation().clearNavigation();
        canvas.clearSparklines();
        if (dashboardPanel != null) {
            dashboardPanel.clear();
        }
        statusBar.clearProgress();

        // Reset file state
        fileController.setCurrentFile(null);
        fileController.setDirty(false);

        // Switch UI back to start screen
        editorShown = false;
        for (MenuItem item : editorOnlyItems) {
            item.setDisable(true);
        }
        configureStartScreen();
        updateTitle();
    }

    private void detachEditorListeners() {
        if (editor != null) {
            if (logListener != null) {
                editor.removeListener(logListener);
            }
            if (staleListener != null) {
                editor.removeListener(staleListener);
            }
            if (dirtyListener != null) {
                editor.removeListener(dirtyListener);
            }
        }
    }

    /**
     * Closes this window unconditionally. Callers must check for unsaved changes
     * via {@link FileController#confirmDiscardChanges()} before calling this method.
     * The CourantApp will be notified via the stage's onHidden handler.
     */
    public void close() {
        detachEditorListeners();
        if (analysisRunner != null) {
            analysisRunner.shutdown();
        }
        undoManager.close();
        if (contextHelpDialog != null) {
            contextHelpDialog.close();
            contextHelpDialog = null;
        }
        helpWindows.closeAll();
        if (dashboardStage != null) {
            dashboardStage.setOnHidden(null);
            dashboardStage.close();
            dashboardStage = null;
        }
        stage.close();
    }
}
