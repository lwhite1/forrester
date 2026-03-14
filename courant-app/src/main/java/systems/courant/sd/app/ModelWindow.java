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
import java.util.function.Consumer;
import java.util.function.Supplier;

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
    private ZoomOverlay zoomOverlay;
    private Stage quickstartWindow;
    private Stage sirTutorialWindow;
    private Stage supplyChainTutorialWindow;
    private Stage sdConceptsWindow;
    private Stage exprLangWindow;
    private Stage shortcutsWindow;
    private ContextHelpDialog contextHelpDialog;

    private FileController fileController;
    private SimulationController simulationController;
    private ModelEditListener dirtyListener;

    private SplitPane editorSplitPane;
    private VBox topContainer;
    private CanvasToolBar toolBar;
    private MenuBar menuBar;
    private LoopNavigatorBar loopNavigatorBar;
    private boolean editorShown;
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
        canvas.setUndoManager(undoManager);

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
            canvas.selectElement(name);
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
            canvas.stepLoopBack();
            updateLoopNavigator();
            canvas.requestFocus();
        });
        loopNavigatorBar.setOnNext(() -> {
            canvas.stepLoopForward();
            updateLoopNavigator();
            canvas.requestFocus();
        });
        loopNavigatorBar.setOnShowAll(() -> {
            canvas.setActiveLoopIndex(-1);
            updateLoopNavigator();
            canvas.requestFocus();
        });
        loopNavigatorBar.setOnFilterChanged(filter -> {
            canvas.setLoopTypeFilter(filter);
            updateLoopNavigator();
            canvas.requestFocus();
        });
        toolBar.setOnLoopToggleChanged(active -> {
            canvas.setLoopHighlightActive(active);
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
            if (canvas.isLoopHighlightActive()) {
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
        canvas.setOnValidationChanged(result -> {
            statusBar.updateValidation(result.errorCount(), result.warningCount());
            if (validationIssuesItem != null) {
                validationIssuesItem.setDisable(result.isClean());
            }
        });
        statusBar.setOnValidationClicked(this::showValidationDialog);

        breadcrumbBar = new BreadcrumbBar();
        breadcrumbBar.setOnNavigateTo(depth -> {
            canvas.navigateToDepth(depth);
            canvas.requestFocus();
        });
        canvas.setOnNavigationChanged(this::updateBreadcrumb);
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
            exprLangWindow = showHelpWindow(exprLangWindow, ExpressionLanguageDialog::new);
            if (exprLangWindow instanceof ExpressionLanguageDialog eld) {
                eld.focusSdFunctions();
            }
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
                quickstartWindow = showHelpWindow(quickstartWindow, QuickstartDialog::new);
                canvas.requestFocus();
            });
            chooser.setOnSirTutorial(() -> {
                showEditor();
                fileController.newModel();
                sirTutorialWindow = showHelpWindow(sirTutorialWindow, SirTutorialDialog::new);
                canvas.requestFocus();
            });
            chooser.setOnSupplyChainTutorial(() -> {
                showEditor();
                fileController.newModel();
                supplyChainTutorialWindow = showHelpWindow(supplyChainTutorialWindow,
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
                canvas.getConnectors(), canvas.getActiveLoopAnalysis(), stage,
                editor != null ? editor.getModelName() : null));

        MenuItem exportReportItem = new MenuItem("Export Report...");
        exportReportItem.setId("menuExportReport");
        exportReportItem.setOnAction(e -> ReportExporter.exportReport(
                canvas.getCanvasState(), canvas.getEditor(),
                canvas.getConnectors(), canvas.getActiveLoopAnalysis(), stage,
                editor != null ? editor.getModelName() : null));

        MenuItem closeItem = new MenuItem("Close");
        closeItem.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN));
        closeItem.setOnAction(e -> {
            if (fileController.confirmDiscardChanges()) {
                close();
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
        saveItem.setDisable(true);
        saveAsItem.setDisable(true);
        exportItem.setDisable(true);
        exportReportItem.setDisable(true);
        modelInfoItem.setDisable(true);
        importRefDataItem.setDisable(true);
        editorOnlyItems.addAll(List.of(saveItem, saveAsItem, exportItem, exportReportItem,
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
            canvas.performUndo();
            canvas.requestFocus();
        });
        undoItem.setDisable(true);

        redoItem = new MenuItem("Redo");
        redoItem.setId("menuRedo");
        redoItem.setAccelerator(new KeyCodeCombination(KeyCode.Z,
                KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        redoItem.setOnAction(e -> {
            canvas.performRedo();
            canvas.requestFocus();
        });
        redoItem.setDisable(true);

        MenuItem undoHistoryItem = new MenuItem("Undo History\u2026");
        undoHistoryItem.setId("menuUndoHistory");
        undoHistoryItem.setOnAction(e -> showUndoHistoryPopup());

        MenuItem cutItem = new MenuItem("Cut");
        cutItem.setAccelerator(new KeyCodeCombination(KeyCode.X, KeyCombination.SHORTCUT_DOWN));
        cutItem.setOnAction(e -> {
            canvas.cutSelection();
            canvas.requestFocus();
        });

        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN));
        copyItem.setOnAction(e -> {
            canvas.copySelection();
            canvas.requestFocus();
        });

        MenuItem pasteItem = new MenuItem("Paste");
        pasteItem.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN));
        pasteItem.setOnAction(e -> {
            canvas.pasteClipboard();
            canvas.requestFocus();
        });

        MenuItem selectAllItem = new MenuItem("Select All");
        selectAllItem.setAccelerator(new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN));
        selectAllItem.setOnAction(e -> {
            canvas.selectAll();
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
        zoomToFitItem.setOnAction(e -> {
            canvas.zoomToFit();
            canvas.requestFocus();
        });

        MenuItem resetZoomItem = new MenuItem("Reset Zoom");
        resetZoomItem.setId("menuResetZoom");
        resetZoomItem.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.SHORTCUT_DOWN));
        resetZoomItem.setOnAction(e -> {
            canvas.resetZoom();
            canvas.requestFocus();
        });

        validationIssuesItem = new MenuItem("Validation Issues\u2026");
        validationIssuesItem.setId("menuValidationIssues");
        validationIssuesItem.setDisable(true);
        validationIssuesItem.setOnAction(e -> showValidationDialog());

        CheckMenuItem hideAuxItem = new CheckMenuItem("Hide Variables");
        hideAuxItem.setId("menuHideVariables");
        hideAuxItem.setOnAction(e -> {
            canvas.setHideVariables(hideAuxItem.isSelected());
            canvas.requestFocus();
        });

        CheckMenuItem showDelayItem = new CheckMenuItem("Show Delay Indicators");
        showDelayItem.setId("menuShowDelayIndicators");
        showDelayItem.setOnAction(e -> {
            canvas.setShowDelayBadges(showDelayItem.isSelected());
            canvas.requestFocus();
        });

        CheckMenuItem hideInfoLinksItem = new CheckMenuItem("Hide Info Links");
        hideInfoLinksItem.setId("menuHideInfoLinks");
        hideInfoLinksItem.setOnAction(e -> {
            canvas.setHideInfoLinks(hideInfoLinksItem.isSelected());
            canvas.requestFocus();
        });

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
        gettingStartedItem.setOnAction(e -> {
            quickstartWindow = showHelpWindow(quickstartWindow, QuickstartDialog::new);
        });

        MenuItem sirTutorialItem = new MenuItem("Tutorial: SIR Epidemic\u2026");
        sirTutorialItem.setOnAction(e -> {
            sirTutorialWindow = showHelpWindow(sirTutorialWindow, SirTutorialDialog::new);
        });

        MenuItem supplyChainTutorialItem = new MenuItem("Tutorial: Supply Chain\u2026");
        supplyChainTutorialItem.setOnAction(e -> {
            supplyChainTutorialWindow = showHelpWindow(supplyChainTutorialWindow,
                    SupplyChainTutorialDialog::new);
        });

        MenuItem sdConceptsItem = new MenuItem("SD Concepts");
        sdConceptsItem.setOnAction(e -> {
            sdConceptsWindow = showHelpWindow(sdConceptsWindow, SdConceptsDialog::new);
        });

        MenuItem exprLangItem = new MenuItem("Expression Language");
        exprLangItem.setOnAction(e -> {
            exprLangWindow = showHelpWindow(exprLangWindow, ExpressionLanguageDialog::new);
        });

        MenuItem shortcutsItem = new MenuItem("Keyboard Shortcuts");
        shortcutsItem.setOnAction(e -> {
            shortcutsWindow = showHelpWindow(shortcutsWindow, KeyboardShortcutsDialog::new);
        });

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
        if (editor != null) {
            editor.removeListener(logListener);
            editor.removeListener(staleListener);
            if (dirtyListener != null) {
                editor.removeListener(dirtyListener);
            }
        }
        if (dirtyListener == null) {
            dirtyListener = fileController.createDirtyListener();
        }
        editor = new ModelEditor();
        editor.addListener(logListener);
        editor.addListener(staleListener);
        editor.addListener(dirtyListener);
        editor.loadFrom(def);

        ViewDef view;
        if (def.stocks().isEmpty() && def.flows().isEmpty()
                && def.variables().isEmpty()) {
            // CLD or empty model — use embedded view if available
            if (!def.views().isEmpty() && !def.cldVariables().isEmpty()) {
                view = def.views().getFirst();
            } else {
                view = new ViewDef("Main", List.of(), List.of(), List.of());
            }
        } else {
            view = AutoLayout.layout(def);
        }

        canvas.clearNavigation();
        canvas.clearSparklines();
        canvas.setModel(editor, view);
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
            bringToFront(contextHelpDialog);
        }
    }

    private Stage showHelpWindow(Stage existing, Supplier<? extends Stage> factory) {
        if (existing != null && existing.isShowing()) {
            bringToFront(existing);
            return existing;
        }
        Stage window = factory.get();
        window.initOwner(stage);
        window.show();
        return window;
    }

    /**
     * Forces a window to the front, working around platform-specific focus-stealing
     * prevention by briefly toggling alwaysOnTop.
     */
    private static void bringToFront(Stage window) {
        window.setAlwaysOnTop(true);
        window.toFront();
        window.requestFocus();
        Platform.runLater(() -> window.setAlwaysOnTop(false));
    }

    private void showUndoHistoryPopup() {
        UndoManager activeUndo = canvas.getUndoManager();
        if (activeUndo == null || !activeUndo.canUndo()) {
            return;
        }
        List<String> labels = activeUndo.undoLabels();
        UndoHistoryPopup popup = new UndoHistoryPopup(labels, depth -> {
            canvas.performUndoTo(depth);
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

        UndoManager activeUndo = canvas.getUndoManager();
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
        if (canvas.isLoopHighlightActive()) {
            FeedbackAnalysis analysis = canvas.getLoopAnalysis();
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
        loopNavigatorBar.update(canvas.getLoopAnalysis(), canvas.getActiveLoopIndex(),
                canvas.getLoopTypeFilter(), canvas.getFilteredLoopCount());
    }

    private void showValidationDialog() {
        ValidationResult result = canvas.getLastValidationResult();
        if (result.isClean()) {
            return;
        }
        ValidationDialog.showOrUpdate(result, canvas::selectElement, stage);
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
        String moduleSuffix = canvas != null && canvas.isInsideModule()
                ? " [" + canvas.getCurrentModuleName() + "]"
                : "";
        stage.setTitle("Courant \u2014 " + name + dirtySuffix + moduleSuffix);
        if (dashboardStage != null) {
            dashboardStage.setTitle("Dashboard \u2014 " + name);
        }
    }

    private void updateBreadcrumb() {
        if (breadcrumbBar != null && canvas.getEditor() != null) {
            breadcrumbBar.update(canvas.getNavigationPath());
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

    private List<CommandPalette.Command> buildCommands() {
        List<CommandPalette.Command> commands = new ArrayList<>();

        // Build tools
        commands.add(cmd("Add Stock", "Build",
                () -> switchToolAndFocus(CanvasToolBar.Tool.PLACE_STOCK)));
        commands.add(cmd("Add Flow", "Build",
                () -> switchToolAndFocus(CanvasToolBar.Tool.PLACE_FLOW)));
        commands.add(cmd("Add Variable", "Build",
                () -> switchToolAndFocus(CanvasToolBar.Tool.PLACE_VARIABLE)));
        commands.add(cmd("Add Module", "Build",
                () -> switchToolAndFocus(CanvasToolBar.Tool.PLACE_MODULE)));
        commands.add(cmd("Add Lookup Table", "Build",
                () -> switchToolAndFocus(CanvasToolBar.Tool.PLACE_LOOKUP)));
        commands.add(cmd("Add CLD Variable", "Build",
                () -> switchToolAndFocus(CanvasToolBar.Tool.PLACE_CLD_VARIABLE)));
        commands.add(cmd("Draw Causal Link", "Build",
                () -> switchToolAndFocus(CanvasToolBar.Tool.PLACE_CAUSAL_LINK)));
        commands.add(cmd("Draw Info Link", "Build",
                () -> switchToolAndFocus(CanvasToolBar.Tool.PLACE_INFO_LINK)));
        commands.add(cmd("Add Comment", "Build",
                () -> switchToolAndFocus(CanvasToolBar.Tool.PLACE_COMMENT)));
        commands.add(cmd("Select Tool", "Build",
                () -> switchToolAndFocus(CanvasToolBar.Tool.SELECT)));

        // Simulate
        commands.add(cmd("Run Simulation", "Simulate", simulationController::runSimulation));
        commands.add(cmd("Validate Model", "Simulate", simulationController::validateModel));
        commands.add(cmd("Simulation Settings", "Simulate", simulationController::openSimulationSettings));
        commands.add(cmd("Extreme Conditions", "Simulate", simulationController::runExtremeConditionTest));
        commands.add(cmd("Parameter Sweep", "Simulate", simulationController::runParameterSweep));
        commands.add(cmd("Multi-Parameter Sweep", "Simulate", simulationController::runMultiParameterSweep));
        commands.add(cmd("Monte Carlo", "Simulate", simulationController::runMonteCarlo));
        commands.add(cmd("Optimize", "Simulate", simulationController::runOptimization));
        commands.add(cmd("Calibrate", "Simulate", simulationController::runCalibration));

        // View
        commands.add(cmd("Validation Issues", "View", this::showValidationDialog));
        commands.add(cmd("Zoom to Fit", "View", () -> {
            canvas.zoomToFit();
            canvas.requestFocus();
        }));
        commands.add(cmd("Reset Zoom", "View", () -> {
            canvas.resetZoom();
            canvas.requestFocus();
        }));
        commands.add(cmd("Zoom In", "View", () -> {
            canvas.zoomIn();
            canvas.requestFocus();
        }));
        commands.add(cmd("Zoom Out", "View", () -> {
            canvas.zoomOut();
            canvas.requestFocus();
        }));
        commands.add(cmd("Toggle Hide Variables", "View", () -> {
            canvas.setHideVariables(!canvas.isHideVariables());
            canvas.requestFocus();
        }));
        commands.add(cmd("Toggle Hide Info Links", "View", () -> {
            canvas.setHideInfoLinks(!canvas.isHideInfoLinks());
            canvas.requestFocus();
        }));
        commands.add(cmd("Toggle Delay Indicators", "View", () -> {
            canvas.setShowDelayBadges(!canvas.isShowDelayBadges());
            canvas.requestFocus();
        }));
        commands.add(cmd("Toggle Activity Log", "View", () -> {
            boolean show = !activityLogPanel.isVisible();
            activityLogPanel.setVisible(show);
            activityLogPanel.setManaged(show);
            if (show) {
                root.setLeft(activityLogPanel);
            } else {
                root.setLeft(null);
            }
        }));
        commands.add(cmd("Pop Out / Dock Dashboard", "View", () -> {
            if (dashboardStage == null) {
                popOutDashboard();
            } else {
                dockDashboard();
            }
        }));

        // Edit
        commands.add(cmd("Undo", "Edit", () -> {
            canvas.performUndo();
            canvas.requestFocus();
        }));
        commands.add(cmd("Redo", "Edit", () -> {
            canvas.performRedo();
            canvas.requestFocus();
        }));
        commands.add(cmd("Undo History", "Edit", this::showUndoHistoryPopup));
        commands.add(cmd("Cut", "Edit", () -> {
            canvas.cutSelection();
            canvas.requestFocus();
        }));
        commands.add(cmd("Copy", "Edit", () -> {
            canvas.copySelection();
            canvas.requestFocus();
        }));
        commands.add(cmd("Paste", "Edit", () -> {
            canvas.pasteClipboard();
            canvas.requestFocus();
        }));
        commands.add(cmd("Select All", "Edit", () -> {
            canvas.selectAll();
            canvas.requestFocus();
        }));

        // File
        commands.add(cmd("New Model", "File", fileController::newModel));
        commands.add(cmd("New Window", "File", () -> app.openNewWindow()));
        commands.add(cmd("Open Model", "File", fileController::openFile));
        commands.add(cmd("Save", "File", fileController::save));
        commands.add(cmd("Save As", "File", fileController::saveAs));
        commands.add(cmd("Export Diagram", "File", () -> DiagramExporter.exportDiagram(
                canvas.getCanvasState(), canvas.getEditor(),
                canvas.getConnectors(), canvas.getActiveLoopAnalysis(), stage,
                editor != null ? editor.getModelName() : null)));
        commands.add(cmd("Model Info", "File", this::showModelInfoDialog));

        // Help (reuse tracked windows, same as menu items)
        commands.add(cmd("Context Help", "Help", this::showContextHelp));
        commands.add(cmd("Getting Started", "Help",
                () -> quickstartWindow = showHelpWindow(quickstartWindow, QuickstartDialog::new)));
        commands.add(cmd("Tutorial: SIR Epidemic", "Help",
                () -> sirTutorialWindow = showHelpWindow(sirTutorialWindow, SirTutorialDialog::new)));
        commands.add(cmd("Tutorial: Supply Chain", "Help",
                () -> supplyChainTutorialWindow = showHelpWindow(supplyChainTutorialWindow,
                        SupplyChainTutorialDialog::new)));
        commands.add(cmd("SD Concepts", "Help",
                () -> sdConceptsWindow = showHelpWindow(sdConceptsWindow, SdConceptsDialog::new)));
        commands.add(cmd("Expression Language", "Help",
                () -> exprLangWindow = showHelpWindow(exprLangWindow, ExpressionLanguageDialog::new)));
        commands.add(cmd("Keyboard Shortcuts", "Help",
                () -> shortcutsWindow = showHelpWindow(shortcutsWindow, KeyboardShortcutsDialog::new)));
        commands.add(cmd("About Courant", "Help", () -> {
            Alert about = new Alert(Alert.AlertType.INFORMATION);
            about.setTitle("About Courant");
            about.setHeaderText("Courant");
            about.setContentText("A visual System Dynamics modeling environment.\nVersion "
                    + AppVersion.get());
            about.showAndWait();
        }));

        // Dynamic: model element names for navigation
        for (String name : canvas.getCanvasState().getDrawOrder()) {
            ElementType type = canvas.getCanvasState().getType(name).orElse(null);
            String category = formatElementType(type);
            commands.add(cmd(name, category, () -> {
                canvas.selectElement(name);
                canvas.requestFocus();
            }));
        }

        return commands;
    }

    private void switchToolAndFocus(CanvasToolBar.Tool tool) {
        canvas.switchTool(tool);
        canvas.requestFocus();
    }

    private static CommandPalette.Command cmd(String name, String category, Runnable action) {
        return new CommandPalette.Command(name, category, action);
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
     * Closes this window unconditionally. Callers must check for unsaved changes
     * via {@link FileController#confirmDiscardChanges()} before calling this method.
     * The CourantApp will be notified via the stage's onHidden handler.
     */
    public void close() {
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
        if (analysisRunner != null) {
            analysisRunner.shutdown();
        }
        undoManager.close();
        if (contextHelpDialog != null) {
            contextHelpDialog.close();
            contextHelpDialog = null;
        }
        if (dashboardStage != null) {
            dashboardStage.setOnHidden(null);
            dashboardStage.close();
            dashboardStage = null;
        }
        stage.close();
    }
}
