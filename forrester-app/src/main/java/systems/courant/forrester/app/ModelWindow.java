package systems.courant.forrester.app;

import systems.courant.forrester.app.canvas.AnalysisRunner;
import systems.courant.forrester.app.canvas.ActivityLogPanel;
import systems.courant.forrester.app.canvas.BreadcrumbBar;
import systems.courant.forrester.app.canvas.CanvasToolBar;
import systems.courant.forrester.app.canvas.Clipboard;
import systems.courant.forrester.app.canvas.CommandPalette;
import systems.courant.forrester.app.canvas.DashboardPanel;
import systems.courant.forrester.app.canvas.DiagramExporter;
import systems.courant.forrester.app.canvas.ExpressionLanguageDialog;
import systems.courant.forrester.app.canvas.ModelCanvas;
import systems.courant.forrester.app.canvas.ModelEditListener;
import systems.courant.forrester.app.canvas.ModelEditor;
import systems.courant.forrester.app.canvas.PropertiesPanel;
import systems.courant.forrester.app.canvas.QuickstartDialog;
import systems.courant.forrester.app.canvas.KeyboardShortcutsDialog;
import systems.courant.forrester.app.canvas.SdConceptsDialog;
import systems.courant.forrester.app.canvas.StatusBar;
import systems.courant.forrester.app.canvas.UndoHistoryPopup;
import systems.courant.forrester.app.canvas.UndoManager;
import systems.courant.forrester.model.ModelMetadata;
import systems.courant.forrester.model.def.ElementType;
import systems.courant.forrester.model.def.ModelDefinition;
import systems.courant.forrester.model.def.ViewDef;
import systems.courant.forrester.model.graph.AutoLayout;
import systems.courant.forrester.model.graph.FeedbackAnalysis;

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
import javafx.stage.Stage;
import javafx.util.Pair;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * An independent editor window for a single Forrester model.
 * Each window owns its own canvas, editor, undo stack, and file state.
 *
 * <p>File I/O is handled by {@link FileController} and simulation/analysis
 * by {@link SimulationController}.
 */
public class ModelWindow {

    private static final Logger log = LoggerFactory.getLogger(ModelWindow.class);

    private final Stage stage;
    private final ForresterApp app;
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
    private Stage quickstartWindow;
    private Stage sdConceptsWindow;
    private Stage exprLangWindow;
    private Stage shortcutsWindow;

    private FileController fileController;
    private SimulationController simulationController;
    private ModelEditListener dirtyListener;

    public ModelWindow(Stage stage, ForresterApp app, Clipboard clipboard) {
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

        CanvasToolBar toolBar = new CanvasToolBar();
        toolBar.setOnToolChanged(tool -> {
            canvas.setActiveTool(tool);
            statusBar.updateTool(tool);
        });
        toolBar.setOnLoopToggleChanged(active -> {
            canvas.setLoopHighlightActive(active);
            updateLoopStatus();
        });
        toolBar.setOnValidateClicked(simulationController::validateModel);
        canvas.setToolBar(toolBar);
        canvas.setOnStatusChanged(() -> {
            updateStatusBar();
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

        breadcrumbBar = new BreadcrumbBar();
        breadcrumbBar.setOnNavigateTo(depth -> {
            canvas.navigateToDepth(depth);
            canvas.requestFocus();
        });
        canvas.setOnNavigationChanged(this::updateBreadcrumb);

        fileController.newModel();

        Pane canvasPane = new Pane(canvas);
        canvas.widthProperty().bind(canvasPane.widthProperty());
        canvas.heightProperty().bind(canvasPane.heightProperty());
        canvas.setOverlayPane(canvasPane);

        propertiesPanel = new PropertiesPanel();

        // Right-side TabPane with Properties and Dashboard tabs
        rightTabPane = new TabPane();
        rightTabPane.setId("rightTabPane");
        rightTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Tab propertiesTab = new Tab("Properties", propertiesPanel);
        propertiesTab.setId("propertiesTab");
        dashboardTab = new Tab("Dashboard", dashboardPanel);
        dashboardTab.setId("dashboardTab");
        rightTabPane.getTabs().addAll(propertiesTab, dashboardTab);

        SplitPane splitPane = new SplitPane(canvasPane, rightTabPane);
        splitPane.setDividerPositions(0.75);
        SplitPane.setResizableWithParent(rightTabPane, false);

        MenuBar menuBar = createMenuBar();
        VBox topContainer = new VBox(menuBar, toolBar, breadcrumbBar);

        root = new BorderPane();
        root.setId("modelWindowRoot");
        root.setTop(topContainer);
        root.setCenter(splitPane);
        root.setBottom(statusBar);

        Scene scene = new Scene(root, 1200, 800);
        stage.setScene(scene);

        commandPalette = new CommandPalette(this::buildCommands);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isShortcutDown() && event.getCode() == KeyCode.K) {
                commandPalette.show(stage);
                event.consume();
            }
        });

        updateTitle();

        // When the window gains focus (e.g. switching from another window),
        // give focus to the canvas so keyboard shortcuts (Ctrl+V, etc.) work
        // immediately without requiring a click first.
        stage.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                Node focused = scene.getFocusOwner();
                if (focused == null || focused == root
                        || !(focused instanceof javafx.scene.control.TextInputControl)) {
                    canvas.requestFocus();
                }
            }
        });

        canvas.requestFocus();
    }

    private MenuBar createMenuBar() {
        Menu fileMenu = new Menu("File");

        MenuItem newWindowItem = new MenuItem("New Window");
        newWindowItem.setId("menuNewWindow");
        newWindowItem.setAccelerator(new KeyCodeCombination(KeyCode.N,
                KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        newWindowItem.setOnAction(e -> app.openNewWindow());

        MenuItem newItem = new MenuItem("New");
        newItem.setId("menuNew");
        newItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN));
        newItem.setOnAction(e -> fileController.newModel());

        MenuItem openItem = new MenuItem("Open...");
        openItem.setId("menuOpen");
        openItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN));
        openItem.setOnAction(e -> fileController.openFile());

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

        MenuItem closeItem = new MenuItem("Close");
        closeItem.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN));
        closeItem.setOnAction(e -> close());

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> Platform.exit());

        Menu examplesMenu = fileController.buildExamplesMenu();

        MenuItem modelInfoItem = new MenuItem("Model Info\u2026");
        modelInfoItem.setOnAction(e -> showModelInfoDialog());

        fileMenu.getItems().addAll(newWindowItem, newItem, openItem, examplesMenu,
                new SeparatorMenuItem(), modelInfoItem,
                new SeparatorMenuItem(), saveItem, saveAsItem, exportItem,
                new SeparatorMenuItem(), closeItem, exitItem);

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

        // View menu
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

        viewMenu.getItems().addAll(commandPaletteItem, new SeparatorMenuItem(),
                activityLogItem, popOutDashboardItem);

        // Simulate menu
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

        MenuItem sweepItem = new MenuItem("Parameter Sweep...");
        sweepItem.setOnAction(e -> simulationController.runParameterSweep());

        MenuItem multiSweepItem = new MenuItem("Multi-Parameter Sweep...");
        multiSweepItem.setOnAction(e -> simulationController.runMultiParameterSweep());

        MenuItem monteCarloItem = new MenuItem("Monte Carlo...");
        monteCarloItem.setOnAction(e -> simulationController.runMonteCarlo());

        MenuItem optimizeItem = new MenuItem("Optimize...");
        optimizeItem.setOnAction(e -> simulationController.runOptimization());

        simulateMenu.getItems().addAll(settingsItem, runItem,
                new SeparatorMenuItem(), validateItem,
                new SeparatorMenuItem(), sweepItem, multiSweepItem, monteCarloItem, optimizeItem);

        Menu helpMenu = new Menu("Help");

        MenuItem gettingStartedItem = new MenuItem("Getting Started\u2026");
        gettingStartedItem.setOnAction(e -> {
            quickstartWindow = showHelpWindow(quickstartWindow, QuickstartDialog::new);
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

        MenuItem aboutItem = new MenuItem("About Forrester");
        aboutItem.setOnAction(e -> {
            Alert about = new Alert(Alert.AlertType.INFORMATION);
            about.setTitle("About Forrester");
            about.setHeaderText("Forrester");
            about.setContentText("A visual System Dynamics modeling environment.\nVersion 0.1");
            about.showAndWait();
        });

        helpMenu.getItems().addAll(gettingStartedItem, sdConceptsItem, exprLangItem,
                new SeparatorMenuItem(), shortcutsItem,
                new SeparatorMenuItem(), aboutItem);

        return new MenuBar(fileMenu, editMenu, viewMenu, simulateMenu, helpMenu);
    }

    void loadDefinition(ModelDefinition def, String displayName) {
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
                && def.auxiliaries().isEmpty() && def.constants().isEmpty()) {
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
        dashboardStage.setTitle("Dashboard \u2014 " + (editor != null ? editor.getModelName() : "Forrester"));
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
            @Override
            public void onElementAdded(String name, String typeName) {
                dashboardPanel.markStale();
            }

            @Override
            public void onElementRemoved(String name) {
                dashboardPanel.markStale();
            }

            @Override
            public void onElementRenamed(String oldName, String newName) {
                dashboardPanel.markStale();
            }

            @Override
            public void onEquationChanged(String elementName) {
                dashboardPanel.markStale();
            }

            @Override
            public void onConstantChanged(String name) {
                dashboardPanel.markStale();
            }
        };
    }

    private Stage showHelpWindow(Stage existing, Supplier<? extends Stage> factory) {
        if (existing != null && existing.isShowing()) {
            existing.toFront();
            existing.requestFocus();
            return existing;
        }
        Stage window = factory.get();
        window.initOwner(stage);
        window.show();
        return window;
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
                activeEditor.getAuxiliaries().size(),
                activeEditor.getConstants().size(),
                activeEditor.getModules().size(),
                activeEditor.getCldVariables().size(),
                activeEditor.getCausalLinks().size());
        statusBar.updateZoom(canvas.getZoomScale());

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
        stage.setTitle("Forrester \u2014 " + name + dirtySuffix + moduleSuffix);
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
        commands.add(cmd("Add Auxiliary", "Build",
                () -> switchToolAndFocus(CanvasToolBar.Tool.PLACE_AUX)));
        commands.add(cmd("Add Constant", "Build",
                () -> switchToolAndFocus(CanvasToolBar.Tool.PLACE_CONSTANT)));
        commands.add(cmd("Add Module", "Build",
                () -> switchToolAndFocus(CanvasToolBar.Tool.PLACE_MODULE)));
        commands.add(cmd("Add Lookup Table", "Build",
                () -> switchToolAndFocus(CanvasToolBar.Tool.PLACE_LOOKUP)));
        commands.add(cmd("Add CLD Variable", "Build",
                () -> switchToolAndFocus(CanvasToolBar.Tool.PLACE_CLD_VARIABLE)));
        commands.add(cmd("Draw Causal Link", "Build",
                () -> switchToolAndFocus(CanvasToolBar.Tool.PLACE_CAUSAL_LINK)));
        commands.add(cmd("Select Tool", "Build",
                () -> switchToolAndFocus(CanvasToolBar.Tool.SELECT)));

        // Simulate
        commands.add(cmd("Run Simulation", "Simulate", simulationController::runSimulation));
        commands.add(cmd("Validate Model", "Simulate", simulationController::validateModel));
        commands.add(cmd("Simulation Settings", "Simulate", simulationController::openSimulationSettings));
        commands.add(cmd("Parameter Sweep", "Simulate", simulationController::runParameterSweep));
        commands.add(cmd("Multi-Parameter Sweep", "Simulate", simulationController::runMultiParameterSweep));
        commands.add(cmd("Monte Carlo", "Simulate", simulationController::runMonteCarlo));
        commands.add(cmd("Optimize", "Simulate", simulationController::runOptimization));

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

        // Help
        commands.add(cmd("Getting Started", "Help",
                () -> new QuickstartDialog().show()));
        commands.add(cmd("SD Concepts", "Help",
                () -> new SdConceptsDialog().show()));
        commands.add(cmd("Expression Language", "Help",
                () -> new ExpressionLanguageDialog().show()));
        commands.add(cmd("Keyboard Shortcuts", "Help",
                () -> new KeyboardShortcutsDialog().show()));

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
            case AUX -> "Auxiliary";
            case CONSTANT -> "Constant";
            case MODULE -> "Module";
            case LOOKUP -> "Lookup Table";
            case CLD_VARIABLE -> "CLD Variable";
        };
    }

    /**
     * Closes this window. The ForresterApp will be notified via the stage's onHidden handler.
     */
    public void close() {
        if (!fileController.confirmDiscardChanges()) {
            return;
        }
        if (editor != null) {
            if (logListener != null) {
                editor.removeListener(logListener);
            }
            if (staleListener != null) {
                editor.removeListener(staleListener);
            }
        }
        if (analysisRunner != null) {
            analysisRunner.shutdown();
        }
        if (dashboardStage != null) {
            dashboardStage.setOnHidden(null);
            dashboardStage.close();
            dashboardStage = null;
        }
        stage.close();
    }
}
