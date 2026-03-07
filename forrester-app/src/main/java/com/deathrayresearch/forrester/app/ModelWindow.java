package com.deathrayresearch.forrester.app;

import com.deathrayresearch.forrester.app.canvas.AnalysisRunner;
import com.deathrayresearch.forrester.app.canvas.ActivityLogPanel;
import com.deathrayresearch.forrester.app.canvas.BreadcrumbBar;
import com.deathrayresearch.forrester.app.canvas.CanvasToolBar;
import com.deathrayresearch.forrester.app.canvas.Clipboard;
import com.deathrayresearch.forrester.app.canvas.CommandPalette;
import com.deathrayresearch.forrester.app.canvas.DashboardPanel;
import com.deathrayresearch.forrester.app.canvas.DiagramExporter;
import com.deathrayresearch.forrester.app.canvas.ExpressionLanguageDialog;
import com.deathrayresearch.forrester.app.canvas.ModelCanvas;
import com.deathrayresearch.forrester.app.canvas.ModelDefinitionFactory;
import com.deathrayresearch.forrester.app.canvas.ModelEditListener;
import com.deathrayresearch.forrester.app.canvas.ModelEditor;
import com.deathrayresearch.forrester.app.canvas.MonteCarloDialog;
import com.deathrayresearch.forrester.app.canvas.MultiParameterSweepDialog;
import com.deathrayresearch.forrester.app.canvas.OptimizerDialog;
import com.deathrayresearch.forrester.app.canvas.ParameterSweepDialog;
import com.deathrayresearch.forrester.app.canvas.PropertiesPanel;
import com.deathrayresearch.forrester.app.canvas.QuickstartDialog;
import com.deathrayresearch.forrester.app.canvas.KeyboardShortcutsDialog;
import com.deathrayresearch.forrester.app.canvas.SdConceptsDialog;
import com.deathrayresearch.forrester.app.canvas.SimulationRunner;
import com.deathrayresearch.forrester.app.canvas.SimulationSettingsDialog;
import com.deathrayresearch.forrester.app.canvas.StatusBar;
import com.deathrayresearch.forrester.app.canvas.UndoHistoryPopup;
import com.deathrayresearch.forrester.app.canvas.UndoManager;
import com.deathrayresearch.forrester.app.canvas.ValidationDialog;
import com.deathrayresearch.forrester.io.ImportResult;
import com.deathrayresearch.forrester.io.ModelImporter;
import com.deathrayresearch.forrester.io.json.ModelDefinitionSerializer;
import com.deathrayresearch.forrester.io.vensim.VensimExporter;
import com.deathrayresearch.forrester.io.vensim.VensimImporter;
import com.deathrayresearch.forrester.io.xmile.XmileExporter;
import com.deathrayresearch.forrester.io.xmile.XmileImporter;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.def.ConstantDef;
import com.deathrayresearch.forrester.model.def.ElementType;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModelDefinitionBuilder;
import com.deathrayresearch.forrester.model.def.ModelValidator;
import com.deathrayresearch.forrester.model.def.SimulationSettings;
import com.deathrayresearch.forrester.model.def.StockDef;
import com.deathrayresearch.forrester.model.def.ValidationResult;
import com.deathrayresearch.forrester.model.def.ViewDef;
import com.deathrayresearch.forrester.model.graph.AutoLayout;
import com.deathrayresearch.forrester.model.graph.FeedbackAnalysis;
import com.deathrayresearch.forrester.sweep.MonteCarlo;
import com.deathrayresearch.forrester.sweep.MonteCarloResult;
import com.deathrayresearch.forrester.sweep.Objectives;
import com.deathrayresearch.forrester.sweep.ObjectiveFunction;
import com.deathrayresearch.forrester.sweep.OptimizationAlgorithm;
import com.deathrayresearch.forrester.sweep.OptimizationResult;
import com.deathrayresearch.forrester.sweep.Optimizer;
import com.deathrayresearch.forrester.sweep.MultiParameterSweep;
import com.deathrayresearch.forrester.sweep.MultiSweepResult;
import com.deathrayresearch.forrester.sweep.ParameterSweep;
import com.deathrayresearch.forrester.sweep.SamplingMethod;
import com.deathrayresearch.forrester.sweep.SweepResult;

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
import javafx.util.Pair;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * An independent editor window for a single Forrester model.
 * Each window owns its own canvas, editor, undo stack, and file state.
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
    private Path currentFile;
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

    private final ModelDefinitionSerializer serializer = new ModelDefinitionSerializer();

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

        CanvasToolBar toolBar = new CanvasToolBar();
        toolBar.setOnToolChanged(tool -> {
            canvas.setActiveTool(tool);
            statusBar.updateTool(tool);
        });
        toolBar.setOnLoopToggleChanged(active -> {
            canvas.setLoopHighlightActive(active);
            updateLoopStatus();
        });
        toolBar.setOnValidateClicked(this::validateModel);
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

        // Activity log panel (hidden by default)
        activityLogPanel = new ActivityLogPanel();
        activityLogPanel.setVisible(false);
        activityLogPanel.setManaged(false);
        logListener = activityLogPanel.createListener();

        // Dashboard panel
        dashboardPanel = new DashboardPanel();
        dashboardPanel.setRerunAction(this::runSimulation);
        staleListener = createStaleListener();

        newModel();

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
        newItem.setOnAction(e -> newModel());

        MenuItem openItem = new MenuItem("Open...");
        openItem.setId("menuOpen");
        openItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN));
        openItem.setOnAction(e -> openFile());

        MenuItem saveItem = new MenuItem("Save");
        saveItem.setId("menuSave");
        saveItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));
        saveItem.setOnAction(e -> save());

        MenuItem saveAsItem = new MenuItem("Save As...");
        saveAsItem.setId("menuSaveAs");
        saveAsItem.setAccelerator(new KeyCodeCombination(KeyCode.S,
                KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        saveAsItem.setOnAction(e -> saveAs());

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

        Menu examplesMenu = buildExamplesMenu();

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
        settingsItem.setOnAction(e -> openSimulationSettings());

        MenuItem runItem = new MenuItem("Run Simulation");
        runItem.setId("menuRunSimulation");
        runItem.setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN));
        runItem.setOnAction(e -> runSimulation());

        MenuItem validateItem = new MenuItem("Validate Model");
        validateItem.setAccelerator(new KeyCodeCombination(KeyCode.B, KeyCombination.SHORTCUT_DOWN));
        validateItem.setOnAction(e -> validateModel());

        MenuItem sweepItem = new MenuItem("Parameter Sweep...");
        sweepItem.setOnAction(e -> runParameterSweep());

        MenuItem multiSweepItem = new MenuItem("Multi-Parameter Sweep...");
        multiSweepItem.setOnAction(e -> runMultiParameterSweep());

        MenuItem monteCarloItem = new MenuItem("Monte Carlo...");
        monteCarloItem.setOnAction(e -> runMonteCarlo());

        MenuItem optimizeItem = new MenuItem("Optimize...");
        optimizeItem.setOnAction(e -> runOptimization());

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

    private void newModel() {
        ModelDefinition empty = new ModelDefinitionBuilder()
                .name("Untitled")
                .build();
        loadDefinition(empty, null);
        currentFile = null;
        updateTitle();
    }

    void loadDefinition(ModelDefinition def, String displayName) {
        if (editor != null) {
            editor.removeListener(logListener);
            editor.removeListener(staleListener);
        }
        editor = new ModelEditor();
        editor.addListener(logListener);
        editor.addListener(staleListener);
        editor.loadFrom(def);

        ViewDef view;
        if (def.stocks().isEmpty() && def.flows().isEmpty()
                && def.auxiliaries().isEmpty() && def.constants().isEmpty()) {
            view = new ViewDef("Main", List.of(), List.of(), List.of());
        } else {
            view = AutoLayout.layout(def);
        }

        canvas.clearNavigation();
        canvas.setModel(editor, view);
        undoManager.clear();
        if (dashboardPanel != null) {
            dashboardPanel.clear();
        }
        updateTitle();
        if (displayName != null) {
            fireLogEvent(l -> l.onModelOpened(displayName));
        }
    }

    private void openFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Model");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(
                        "All Supported Models", "*.json", "*.mdl", "*.xmile", "*.stmx", "*.itmx"),
                new FileChooser.ExtensionFilter("Forrester Model (*.json)", "*.json"),
                new FileChooser.ExtensionFilter("Vensim Model (*.mdl)", "*.mdl"),
                new FileChooser.ExtensionFilter("XMILE Model (*.xmile, *.stmx, *.itmx)",
                        "*.xmile", "*.stmx", "*.itmx"));
        LastDirectoryStore.applyOpenDirectory(chooser);
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }
        LastDirectoryStore.recordOpenDirectory(file);

        String name = file.getName();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')).toLowerCase() : "";

        try {
            ModelDefinition def;
            switch (ext) {
                case ".mdl" -> def = importModel(new VensimImporter(), file.toPath(), name);
                case ".xmile", ".stmx", ".itmx" -> def = importModel(new XmileImporter(), file.toPath(), name);
                case ".json" -> def = serializer.fromFile(file.toPath());
                default -> {
                    if (ext.isEmpty()) {
                        showError("Open Error",
                                "Cannot determine file format (no file extension).");
                    } else {
                        showError("Open Error",
                                "Unsupported file format: " + ext);
                    }
                    return;
                }
            }

            loadDefinition(def, name);

            // For native JSON files, track the file for Save; for imports, force Save As
            currentFile = ".json".equals(ext) ? file.toPath() : null;
            updateTitle();
        } catch (IOException ex) {
            showError("Open Error", "Failed to open file: " + ex.getMessage());
        } catch (RuntimeException ex) {
            showError("Open Error", "Failed to parse file: " + ex.getMessage());
        }
    }

    private Menu buildExamplesMenu() {
        Menu menu = new Menu("Open Example");
        try (InputStream in = getClass().getResourceAsStream("/models/catalog.json")) {
            if (in == null) {
                MenuItem empty = new MenuItem("(no examples found)");
                empty.setDisable(true);
                menu.getItems().add(empty);
                return menu;
            }
            ObjectMapper om = new ObjectMapper();
            JsonNode root = om.readTree(in);
            JsonNode models = root.get("models");
            if (models == null || !models.isArray()) {
                MenuItem empty = new MenuItem("(no examples found)");
                empty.setDisable(true);
                menu.getItems().add(empty);
                return menu;
            }

            Map<String, Menu> categoryMenus = new LinkedHashMap<>();
            for (JsonNode model : models) {
                String name = model.path("name").asText(null);
                String category = model.path("category").asText(null);
                String path = model.path("path").asText(null);
                if (name == null || category == null || path == null) {
                    log.warn("Skipping malformed example entry: {}", model);
                    continue;
                }

                Menu categoryMenu = categoryMenus.computeIfAbsent(category, c -> new Menu(c));
                MenuItem item = new MenuItem(name);
                item.setOnAction(e -> openExample(name, path));
                categoryMenu.getItems().add(item);
            }
            menu.getItems().addAll(categoryMenus.values());
        } catch (Exception ex) {
            log.warn("Failed to load examples catalog", ex);
            MenuItem empty = new MenuItem("(no examples found)");
            empty.setDisable(true);
            menu.getItems().add(empty);
        }
        return menu;
    }

    private void openExample(String name, String resourcePath) {
        try (InputStream in = getClass().getResourceAsStream("/models/" + resourcePath)) {
            if (in == null) {
                showError("Open Example", "Example resource not found: " + resourcePath);
                return;
            }
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            ModelDefinition def = serializer.fromJson(json);

            loadDefinition(def, name);
            currentFile = null;
            updateTitle();
        } catch (Exception ex) {
            showError("Open Example", "Failed to load example: " + ex.getMessage());
        }
    }

    private void save() {
        if (currentFile != null) {
            saveToFile(currentFile);
        } else {
            saveAs();
        }
    }

    private void saveAs() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Model");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Forrester Model (*.json)", "*.json"),
                new FileChooser.ExtensionFilter("Vensim Model (*.mdl)", "*.mdl"),
                new FileChooser.ExtensionFilter("XMILE Model (*.xmile, *.stmx, *.itmx)",
                        "*.xmile", "*.stmx", "*.itmx"));
        if (currentFile != null) {
            Path parentDir = currentFile.getParent();
            if (parentDir != null) {
                chooser.setInitialDirectory(parentDir.toFile());
            }
            Path fn = currentFile.getFileName();
            if (fn != null) {
                chooser.setInitialFileName(fn.toString());
            }
        } else {
            LastDirectoryStore.applySaveDirectory(chooser);
        }
        File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }
        LastDirectoryStore.recordSaveDirectory(file);

        String name = file.getName();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')).toLowerCase() : "";

        try {
            ModelDefinition def = canvas.toModelDefinition();
            switch (ext) {
                case ".mdl" -> {
                    VensimExporter.toFile(def, file.toPath());
                    fireLogEvent(l -> l.onModelSaved(name));
                }
                case ".xmile", ".stmx", ".itmx" -> {
                    XmileExporter.toFile(def, file.toPath());
                    fireLogEvent(l -> l.onModelSaved(name));
                }
                default -> {
                    currentFile = file.toPath();
                    saveToFile(currentFile);
                    return;
                }
            }
        } catch (IOException ex) {
            showError("Save Error", "Failed to save file: " + ex.getMessage());
        } catch (RuntimeException ex) {
            showError("Save Error", "Failed to export file: " + ex.getMessage());
        }
    }

    private void saveToFile(Path path) {
        try {
            ModelDefinition def = canvas.toModelDefinition();
            serializer.toFile(def, path);
            updateTitle();
            fireLogEvent(l -> l.onModelSaved(path.getFileName().toString()));
        } catch (IOException ex) {
            showError("Save Error", "Failed to save file: " + ex.getMessage());
        }
    }

    private void openSimulationSettings() {
        SimulationSettingsDialog dialog = new SimulationSettingsDialog(
                editor.getSimulationSettings());
        Optional<SimulationSettings> result = dialog.showAndWait();
        result.ifPresent(settings -> editor.setSimulationSettings(settings));
    }

    private SimulationSettings ensureSettings() {
        SimulationSettings settings = editor.getSimulationSettings();
        if (settings == null) {
            SimulationSettingsDialog dialog = new SimulationSettingsDialog(null);
            Optional<SimulationSettings> result = dialog.showAndWait();
            if (result.isEmpty()) {
                return null;
            }
            settings = result.get();
            editor.setSimulationSettings(settings);
        }
        return settings;
    }

    private void runSimulation() {
        SimulationSettings settings = ensureSettings();
        if (settings == null) {
            return;
        }

        ModelDefinition def = canvas.toModelDefinition();
        SimulationSettings finalSettings = settings;

        analysisRunner.run("Simulating...",
                () -> new SimulationRunner().run(def, finalSettings),
                result -> {
                    dashboardPanel.showSimulationResult(result);
                    switchToDashboard();
                    fireLogEvent(ModelEditListener::onSimulationRun);
                },
                "Simulation Error");
    }

    private void runParameterSweep() {
        SimulationSettings settings = ensureSettings();
        if (settings == null) {
            return;
        }

        ModelEditor activeEditor = canvas.getEditor();
        List<String> constantNames = activeEditor.getConstants().stream()
                .map(ConstantDef::name).toList();
        List<String> trackableNames = new ArrayList<>();
        activeEditor.getStocks().forEach(s -> trackableNames.add(s.name()));
        activeEditor.getFlows().forEach(f -> trackableNames.add(f.name()));
        activeEditor.getAuxiliaries().forEach(a -> trackableNames.add(a.name()));

        if (constantNames.isEmpty()) {
            showError("Parameter Sweep", "Model has no constants to sweep.");
            return;
        }

        ParameterSweepDialog dialog = new ParameterSweepDialog(constantNames, trackableNames);
        Optional<ParameterSweepDialog.Config> configOpt = dialog.showAndWait();
        if (configOpt.isEmpty()) {
            return;
        }

        ParameterSweepDialog.Config config = configOpt.get();
        ModelDefinition def = canvas.toModelDefinition();
        SimulationSettings finalSettings = settings;

        analysisRunner.run("Running sweep...",
                () -> {
                    TimeUnit timeStep = ModelDefinitionFactory.resolveTimeStep(finalSettings);
                    Quantity duration = ModelDefinitionFactory.resolveDuration(finalSettings);

                    return ParameterSweep.builder()
                            .parameterName(config.parameterName())
                            .parameterValues(ParameterSweep.linspace(
                                    config.start(), config.end(), config.step()))
                            .compiledModelFactory(
                                    ModelDefinitionFactory.createSingleParamFactory(
                                            def, finalSettings, config.parameterName()))
                            .timeStep(timeStep)
                            .duration(duration)
                            .build()
                            .execute();
                },
                result -> {
                    dashboardPanel.showSweepResult(result, config.parameterName());
                    switchToDashboard();
                    fireLogEvent(l -> l.onAnalysisRun("Parameter Sweep",
                            config.parameterName() + " [" + config.start() + ".." + config.end() + "]"));
                },
                "Sweep Error");
    }

    private void runMultiParameterSweep() {
        SimulationSettings settings = ensureSettings();
        if (settings == null) {
            return;
        }

        ModelEditor activeEditor = canvas.getEditor();
        List<String> constantNames = activeEditor.getConstants().stream()
                .map(ConstantDef::name).toList();

        if (constantNames.size() < 2) {
            showError("Multi-Parameter Sweep", "Model needs at least 2 constants to sweep.");
            return;
        }

        MultiParameterSweepDialog dialog = new MultiParameterSweepDialog(constantNames);
        Optional<MultiParameterSweepDialog.Config> configOpt = dialog.showAndWait();
        if (configOpt.isEmpty()) {
            return;
        }

        MultiParameterSweepDialog.Config config = configOpt.get();
        ModelDefinition def = canvas.toModelDefinition();
        SimulationSettings finalSettings = settings;

        analysisRunner.run("Running multi-parameter sweep...",
                () -> {
                    TimeUnit timeStep = ModelDefinitionFactory.resolveTimeStep(finalSettings);
                    Quantity duration = ModelDefinitionFactory.resolveDuration(finalSettings);

                    MultiParameterSweep.Builder builder = MultiParameterSweep.builder()
                            .compiledModelFactory(
                                    ModelDefinitionFactory.createFactory(def, finalSettings))
                            .timeStep(timeStep)
                            .duration(duration);

                    for (MultiParameterSweepDialog.ParamConfig p : config.parameters()) {
                        builder.parameter(p.name(),
                                ParameterSweep.linspace(p.start(), p.end(), p.step()));
                    }

                    return builder.build().execute();
                },
                result -> {
                    dashboardPanel.showMultiSweepResult(result);
                    switchToDashboard();
                    String paramSummary = config.parameters().stream()
                            .map(MultiParameterSweepDialog.ParamConfig::name)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("");
                    fireLogEvent(l -> l.onAnalysisRun("Multi-Parameter Sweep", paramSummary));
                },
                "Multi-Sweep Error");
    }

    private void runMonteCarlo() {
        SimulationSettings settings = ensureSettings();
        if (settings == null) {
            return;
        }

        ModelEditor activeEditor = canvas.getEditor();
        List<String> constantNames = activeEditor.getConstants().stream()
                .map(ConstantDef::name).toList();

        if (constantNames.isEmpty()) {
            showError("Monte Carlo", "Model has no constants to vary.");
            return;
        }

        MonteCarloDialog dialog = new MonteCarloDialog(constantNames);
        Optional<MonteCarloDialog.Config> configOpt = dialog.showAndWait();
        if (configOpt.isEmpty()) {
            return;
        }

        MonteCarloDialog.Config config = configOpt.get();
        ModelDefinition def = canvas.toModelDefinition();
        SimulationSettings finalSettings = settings;

        analysisRunner.run(
                "Running Monte Carlo (" + config.iterations() + " iterations)...",
                () -> {
                    TimeUnit timeStep = ModelDefinitionFactory.resolveTimeStep(finalSettings);
                    Quantity duration = ModelDefinitionFactory.resolveDuration(finalSettings);

                    MonteCarlo.Builder builder = MonteCarlo.builder()
                            .compiledModelFactory(
                                    ModelDefinitionFactory.createFactory(def, finalSettings))
                            .iterations(config.iterations())
                            .sampling("RANDOM".equals(config.samplingMethod())
                                    ? SamplingMethod.RANDOM : SamplingMethod.LATIN_HYPERCUBE)
                            .seed(config.seed())
                            .timeStep(timeStep)
                            .duration(duration);

                    for (MonteCarloDialog.ParameterConfig p : config.parameters()) {
                        if (p.distribution() == MonteCarloDialog.DistributionType.NORMAL) {
                            builder.parameter(p.name(), new NormalDistribution(p.param1(), p.param2()));
                        } else {
                            builder.parameter(p.name(), new UniformRealDistribution(p.param1(), p.param2()));
                        }
                    }

                    return builder.build().execute();
                },
                result -> {
                    dashboardPanel.showMonteCarloResult(result);
                    switchToDashboard();
                    fireLogEvent(l -> l.onAnalysisRun("Monte Carlo",
                            config.iterations() + " iterations, " + config.parameters().size() + " params"));
                },
                "Monte Carlo Error");
    }

    private void runOptimization() {
        SimulationSettings settings = ensureSettings();
        if (settings == null) {
            return;
        }

        ModelEditor activeEditor = canvas.getEditor();
        List<String> constantNames = activeEditor.getConstants().stream()
                .map(ConstantDef::name).toList();
        List<String> stockNames = activeEditor.getStocks().stream()
                .map(StockDef::name).toList();

        if (constantNames.isEmpty()) {
            showError("Optimize", "Model has no constants to optimize.");
            return;
        }
        if (stockNames.isEmpty()) {
            showError("Optimize", "Model has no stocks for objective evaluation.");
            return;
        }

        OptimizerDialog dialog = new OptimizerDialog(constantNames, stockNames);
        Optional<OptimizerDialog.Config> configOpt = dialog.showAndWait();
        if (configOpt.isEmpty()) {
            return;
        }

        OptimizerDialog.Config config = configOpt.get();
        ModelDefinition def = canvas.toModelDefinition();
        SimulationSettings finalSettings = settings;

        analysisRunner.run(
                "Optimizing (" + config.maxEvaluations() + " max evals)...",
                () -> {
                    TimeUnit timeStep = ModelDefinitionFactory.resolveTimeStep(finalSettings);
                    Quantity duration = ModelDefinitionFactory.resolveDuration(finalSettings);

                    ObjectiveFunction objective = switch (config.objectiveType()) {
                        case MINIMIZE -> Objectives.minimize(config.targetVariable());
                        case MAXIMIZE -> Objectives.maximize(config.targetVariable());
                        case TARGET -> Objectives.target(config.targetVariable(), config.targetValue());
                        case MINIMIZE_PEAK -> Objectives.minimizePeak(config.targetVariable());
                    };

                    OptimizationAlgorithm algorithm = switch (config.algorithm()) {
                        case "BOBYQA" -> OptimizationAlgorithm.BOBYQA;
                        case "CMAES" -> OptimizationAlgorithm.CMAES;
                        default -> OptimizationAlgorithm.NELDER_MEAD;
                    };

                    Optimizer.Builder builder = Optimizer.builder()
                            .compiledModelFactory(
                                    ModelDefinitionFactory.createFactory(def, finalSettings))
                            .objective(objective)
                            .algorithm(algorithm)
                            .maxEvaluations(config.maxEvaluations())
                            .timeStep(timeStep)
                            .duration(duration);

                    for (OptimizerDialog.ParamConfig p : config.parameters()) {
                        if (Double.isNaN(p.initialGuess())) {
                            builder.parameter(p.name(), p.lower(), p.upper());
                        } else {
                            builder.parameter(p.name(), p.lower(), p.upper(), p.initialGuess());
                        }
                    }

                    return builder.build().execute();
                },
                result -> {
                    dashboardPanel.showOptimizationResult(result);
                    switchToDashboard();
                    fireLogEvent(l -> l.onAnalysisRun("Optimization",
                            config.algorithm() + ", " + config.parameters().size() + " params"));
                },
                "Optimization Error");
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
        // Remove dashboard tab from the right pane
        rightTabPane.getTabs().remove(dashboardTab);

        // Create a new stage with the dashboard panel
        dashboardStage = new Stage();
        dashboardStage.setTitle("Dashboard \u2014 " + (editor != null ? editor.getModelName() : "Forrester"));
        dashboardStage.initOwner(stage);

        BorderPane dashRoot = new BorderPane(dashboardPanel);
        Scene dashScene = new Scene(dashRoot, 600, 500);
        dashboardStage.setScene(dashScene);

        // Dock back when the dashboard window is closed via its own close button
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

        // Remove dashboard from the pop-out scene
        BorderPane dashRoot = (BorderPane) stageToClose.getScene().getRoot();
        dashRoot.setCenter(null);

        // Re-add the dashboard tab to the right pane
        dashboardTab.setContent(dashboardPanel);
        if (!rightTabPane.getTabs().contains(dashboardTab)) {
            rightTabPane.getTabs().add(dashboardTab);
        }

        // Close the pop-out window (clear handler first to prevent recursion)
        stageToClose.setOnHidden(null);
        stageToClose.close();

        popOutDashboardItem.setText("Pop Out Dashboard");
    }

    private void validateModel() {
        ModelDefinition def = canvas.toModelDefinition();

        analysisRunner.run(
                () -> ModelValidator.validate(def),
                result -> {
                    statusBar.updateValidation(result.errorCount(), result.warningCount());
                    ValidationDialog dialog = new ValidationDialog(result, canvas::selectElement);
                    dialog.show();
                    fireLogEvent(l -> l.onValidation(result.errorCount(), result.warningCount()));
                },
                "Validation Error");
    }

    private void fireLogEvent(java.util.function.Consumer<ModelEditListener> event) {
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

    /**
     * Shows a singleton help window. If already open, brings it to front.
     * If closed, creates a new one. Sets owner so it stays above the main window.
     */
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
        // Show below the menu bar area
        popup.showBelow(stage, stage.getX() + 60, stage.getY() + 80);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message != null ? message : "An unexpected error occurred.");
        alert.showAndWait();
    }

    void showImportWarnings(String fileName, List<String> warnings) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Import Warnings");
        alert.setHeaderText("Warnings while importing " + fileName);
        alert.setContentText(String.join("\n", warnings));
        alert.showAndWait();
    }

    private ModelDefinition importModel(ModelImporter importer, Path path, String displayName)
            throws IOException {
        ImportResult result = importer.importModel(path);
        if (!result.isClean()) {
            showImportWarnings(displayName, result.warnings());
        }
        return result.definition();
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
        Dialog<Pair<String, String>> dialog = new Dialog<>();
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

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Comment:"), 0, 1);
        grid.add(commentArea, 1, 1);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(nameField::requestFocus);

        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return new Pair<>(nameField.getText().trim(), commentArea.getText().trim());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            String newName = result.getKey().isEmpty() ? "Untitled" : result.getKey();
            editor.setModelName(newName);
            editor.setModelComment(result.getValue());
            updateTitle();
        });
    }

    private void updateTitle() {
        String name;
        if (editor != null && !"Untitled".equals(editor.getModelName())) {
            name = editor.getModelName();
        } else if (currentFile != null) {
            Path fn = currentFile.getFileName();
            name = fn != null ? fn.toString() : currentFile.toString();
        } else {
            name = "Untitled";
        }
        String moduleSuffix = canvas.isInsideModule()
                ? " [" + canvas.getCurrentModuleName() + "]"
                : "";
        stage.setTitle("Forrester \u2014 " + name + moduleSuffix);
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
        return currentFile;
    }

    void setCurrentFile(Path path) {
        this.currentFile = path;
        updateTitle();
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
        commands.add(cmd("Run Simulation", "Simulate", this::runSimulation));
        commands.add(cmd("Validate Model", "Simulate", this::validateModel));
        commands.add(cmd("Simulation Settings", "Simulate", this::openSimulationSettings));
        commands.add(cmd("Parameter Sweep", "Simulate", this::runParameterSweep));
        commands.add(cmd("Multi-Parameter Sweep", "Simulate", this::runMultiParameterSweep));
        commands.add(cmd("Monte Carlo", "Simulate", this::runMonteCarlo));
        commands.add(cmd("Optimize", "Simulate", this::runOptimization));

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
        commands.add(cmd("New Model", "File", this::newModel));
        commands.add(cmd("New Window", "File", () -> app.openNewWindow()));
        commands.add(cmd("Open Model", "File", this::openFile));
        commands.add(cmd("Save", "File", this::save));
        commands.add(cmd("Save As", "File", this::saveAs));
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
            ElementType type = canvas.getCanvasState().getType(name);
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
