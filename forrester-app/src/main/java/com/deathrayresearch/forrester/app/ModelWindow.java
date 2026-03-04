package com.deathrayresearch.forrester.app;

import com.deathrayresearch.forrester.app.canvas.ActivityLogPanel;
import com.deathrayresearch.forrester.app.canvas.BreadcrumbBar;
import com.deathrayresearch.forrester.app.canvas.CanvasToolBar;
import com.deathrayresearch.forrester.app.canvas.Clipboard;
import com.deathrayresearch.forrester.app.canvas.DashboardPanel;
import com.deathrayresearch.forrester.app.canvas.DiagramExporter;
import com.deathrayresearch.forrester.app.canvas.ModelCanvas;
import com.deathrayresearch.forrester.app.canvas.ModelDefinitionFactory;
import com.deathrayresearch.forrester.app.canvas.ModelEditListener;
import com.deathrayresearch.forrester.app.canvas.ModelEditor;
import com.deathrayresearch.forrester.app.canvas.MonteCarloDialog;
import com.deathrayresearch.forrester.app.canvas.MultiParameterSweepDialog;
import com.deathrayresearch.forrester.app.canvas.OptimizerDialog;
import com.deathrayresearch.forrester.app.canvas.ParameterSweepDialog;
import com.deathrayresearch.forrester.app.canvas.PropertiesPanel;
import com.deathrayresearch.forrester.app.canvas.SimulationRunner;
import com.deathrayresearch.forrester.app.canvas.SimulationSettingsDialog;
import com.deathrayresearch.forrester.app.canvas.StatusBar;
import com.deathrayresearch.forrester.app.canvas.UndoManager;
import com.deathrayresearch.forrester.app.canvas.ValidationDialog;
import com.deathrayresearch.forrester.io.json.ModelDefinitionSerializer;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.def.ConstantDef;
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
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

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
import java.util.function.DoubleFunction;
import java.util.function.Function;

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
    private BorderPane root;
    private Path currentFile;
    private final UndoManager undoManager = new UndoManager();
    private MenuItem undoItem;
    private MenuItem redoItem;
    private ModelEditListener logListener;

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

        newModel();

        Pane canvasPane = new Pane(canvas);
        canvas.widthProperty().bind(canvasPane.widthProperty());
        canvas.heightProperty().bind(canvasPane.heightProperty());
        canvas.setOverlayPane(canvasPane);

        propertiesPanel = new PropertiesPanel();

        // Right-side TabPane with Properties and Dashboard tabs
        rightTabPane = new TabPane();
        rightTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Tab propertiesTab = new Tab("Properties", propertiesPanel);
        Tab dashboardTab = new Tab("Dashboard", dashboardPanel);
        rightTabPane.getTabs().addAll(propertiesTab, dashboardTab);

        SplitPane splitPane = new SplitPane(canvasPane, rightTabPane);
        splitPane.setDividerPositions(0.75);
        SplitPane.setResizableWithParent(rightTabPane, false);

        MenuBar menuBar = createMenuBar();
        VBox topContainer = new VBox(menuBar, toolBar, breadcrumbBar);

        root = new BorderPane();
        root.setTop(topContainer);
        root.setCenter(splitPane);
        root.setBottom(statusBar);

        Scene scene = new Scene(root, 1200, 800);
        stage.setScene(scene);
        updateTitle();

        canvas.requestFocus();
    }

    private MenuBar createMenuBar() {
        Menu fileMenu = new Menu("File");

        MenuItem newWindowItem = new MenuItem("New Window");
        newWindowItem.setAccelerator(new KeyCodeCombination(KeyCode.N,
                KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        newWindowItem.setOnAction(e -> app.openNewWindow());

        MenuItem newItem = new MenuItem("New");
        newItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN));
        newItem.setOnAction(e -> newModel());

        MenuItem openItem = new MenuItem("Open...");
        openItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN));
        openItem.setOnAction(e -> openFile());

        MenuItem saveItem = new MenuItem("Save");
        saveItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));
        saveItem.setOnAction(e -> save());

        MenuItem saveAsItem = new MenuItem("Save As...");
        saveAsItem.setAccelerator(new KeyCodeCombination(KeyCode.S,
                KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        saveAsItem.setOnAction(e -> saveAs());

        MenuItem exportItem = new MenuItem("Export Diagram...");
        exportItem.setAccelerator(new KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN));
        exportItem.setOnAction(e -> DiagramExporter.exportDiagram(
                canvas.getCanvasState(), canvas.getEditor(),
                canvas.getConnectors(), canvas.getActiveLoopAnalysis(), stage));

        MenuItem closeItem = new MenuItem("Close");
        closeItem.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN));
        closeItem.setOnAction(e -> close());

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> Platform.exit());

        Menu examplesMenu = buildExamplesMenu();

        fileMenu.getItems().addAll(newWindowItem, newItem, openItem, examplesMenu,
                new SeparatorMenuItem(), saveItem, saveAsItem, exportItem,
                new SeparatorMenuItem(), closeItem, exitItem);

        Menu editMenu = new Menu("Edit");

        undoItem = new MenuItem("Undo");
        undoItem.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN));
        undoItem.setOnAction(e -> {
            canvas.performUndo();
            canvas.requestFocus();
        });
        undoItem.setDisable(true);

        redoItem = new MenuItem("Redo");
        redoItem.setAccelerator(new KeyCodeCombination(KeyCode.Z,
                KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        redoItem.setOnAction(e -> {
            canvas.performRedo();
            canvas.requestFocus();
        });
        redoItem.setDisable(true);

        MenuItem selectAllItem = new MenuItem("Select All");
        selectAllItem.setAccelerator(new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN));
        selectAllItem.setOnAction(e -> {
            canvas.selectAll();
            canvas.requestFocus();
        });

        editMenu.getItems().addAll(undoItem, redoItem, new SeparatorMenuItem(), selectAllItem);

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

        viewMenu.getItems().add(activityLogItem);

        // Simulate menu
        Menu simulateMenu = new Menu("Simulate");

        MenuItem settingsItem = new MenuItem("Simulation Settings...");
        settingsItem.setOnAction(e -> openSimulationSettings());

        MenuItem runItem = new MenuItem("Run Simulation");
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

        MenuItem gettingStartedItem = new MenuItem("Getting Started");
        gettingStartedItem.setDisable(true);

        MenuItem sdConceptsItem = new MenuItem("SD Concepts");
        sdConceptsItem.setDisable(true);

        MenuItem shortcutsItem = new MenuItem("Keyboard Shortcuts");
        shortcutsItem.setDisable(true);

        MenuItem aboutItem = new MenuItem("About Forrester");
        aboutItem.setOnAction(e -> {
            Alert about = new Alert(Alert.AlertType.INFORMATION);
            about.setTitle("About Forrester");
            about.setHeaderText("Forrester");
            about.setContentText("A visual System Dynamics modeling environment.\nVersion 0.1");
            about.showAndWait();
        });

        helpMenu.getItems().addAll(gettingStartedItem, sdConceptsItem,
                new SeparatorMenuItem(), shortcutsItem,
                new SeparatorMenuItem(), aboutItem);

        return new MenuBar(fileMenu, editMenu, viewMenu, simulateMenu, helpMenu);
    }

    private void newModel() {
        ModelDefinition empty = new ModelDefinitionBuilder()
                .name("Untitled")
                .build();
        ViewDef emptyView = new ViewDef("Main", List.of(), List.of(), List.of());

        if (editor != null) {
            editor.removeListener(logListener);
        }
        editor = new ModelEditor();
        editor.addListener(logListener);
        editor.loadFrom(empty);
        canvas.clearNavigation();
        canvas.setModel(editor, emptyView);
        undoManager.clear();
        currentFile = null;
        if (dashboardPanel != null) {
            dashboardPanel.clear();
        }
        updateTitle();
    }

    private void openFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open Forrester Model");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Forrester Model (*.json)", "*.json"));
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }

        try {
            ModelDefinition def = serializer.fromFile(file.toPath());
            if (editor != null) {
                editor.removeListener(logListener);
            }
            editor = new ModelEditor();
            editor.addListener(logListener);
            editor.loadFrom(def);

            ViewDef view;
            if (!def.views().isEmpty()) {
                view = def.views().getFirst();
            } else {
                view = AutoLayout.layout(def);
            }

            canvas.clearNavigation();
            canvas.setModel(editor, view);
            undoManager.clear();
            currentFile = file.toPath();
            if (dashboardPanel != null) {
                dashboardPanel.clear();
            }
            updateTitle();
            fireLogEvent(l -> l.onModelOpened(file.getName()));
        } catch (IOException ex) {
            showError("Open Error", "Failed to open file: " + ex.getMessage());
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

            if (editor != null) {
                editor.removeListener(logListener);
            }
            editor = new ModelEditor();
            editor.addListener(logListener);
            editor.loadFrom(def);

            ViewDef view;
            if (!def.views().isEmpty()) {
                view = def.views().getFirst();
            } else {
                view = AutoLayout.layout(def);
            }

            canvas.clearNavigation();
            canvas.setModel(editor, view);
            undoManager.clear();
            currentFile = null;
            if (dashboardPanel != null) {
                dashboardPanel.clear();
            }
            updateTitle();
            fireLogEvent(l -> l.onModelOpened(name));
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
        chooser.setTitle("Save Forrester Model");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Forrester Model (*.json)", "*.json"));
        if (currentFile != null) {
            if (currentFile.getParent() != null) {
                chooser.setInitialDirectory(currentFile.getParent().toFile());
            }
            chooser.setInitialFileName(currentFile.getFileName().toString());
        }
        File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }

        currentFile = file.toPath();
        saveToFile(currentFile);
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

        statusBar.showProgress("Simulating...");

        javafx.concurrent.Task<SimulationRunner.SimulationResult> task =
                new javafx.concurrent.Task<>() {
            @Override
            protected SimulationRunner.SimulationResult call() {
                SimulationRunner runner = new SimulationRunner();
                return runner.run(def, finalSettings);
            }
        };

        task.setOnSucceeded(e -> {
            statusBar.clearProgress();
            dashboardPanel.showSimulationResult(task.getValue());
            switchToDashboard();
            fireLogEvent(ModelEditListener::onSimulationRun);
        });

        task.setOnFailed(e -> {
            statusBar.clearProgress();
            Throwable ex = task.getException();
            showError("Simulation Error", ex != null ? ex.getMessage() : null);
        });

        Thread thread = new Thread(task, "simulation-runner");
        thread.setDaemon(true);
        thread.start();
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

        statusBar.showProgress("Running sweep...");

        javafx.concurrent.Task<SweepResult> task = new javafx.concurrent.Task<>() {
            @Override
            protected SweepResult call() {
                TimeUnit timeStep = ModelDefinitionFactory.resolveTimeStep(finalSettings);
                Quantity duration = ModelDefinitionFactory.resolveDuration(finalSettings);
                DoubleFunction<Model> factory = ModelDefinitionFactory.createSingleParamFactory(
                        def, finalSettings, config.parameterName());

                return ParameterSweep.builder()
                        .parameterName(config.parameterName())
                        .parameterValues(ParameterSweep.linspace(
                                config.start(), config.end(), config.step()))
                        .modelFactory(factory)
                        .timeStep(timeStep)
                        .duration(duration)
                        .build()
                        .execute();
            }
        };

        task.setOnSucceeded(e -> {
            statusBar.clearProgress();
            dashboardPanel.showSweepResult(task.getValue(), config.parameterName());
            switchToDashboard();
            fireLogEvent(l -> l.onAnalysisRun("Parameter Sweep",
                    config.parameterName() + " [" + config.start() + ".." + config.end() + "]"));
        });

        task.setOnFailed(e -> {
            statusBar.clearProgress();
            Throwable ex = task.getException();
            showError("Sweep Error", ex != null ? ex.getMessage() : null);
        });

        Thread thread = new Thread(task, "parameter-sweep");
        thread.setDaemon(true);
        thread.start();
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

        statusBar.showProgress("Running multi-parameter sweep...");

        javafx.concurrent.Task<MultiSweepResult> task = new javafx.concurrent.Task<>() {
            @Override
            protected MultiSweepResult call() {
                TimeUnit timeStep = ModelDefinitionFactory.resolveTimeStep(finalSettings);
                Quantity duration = ModelDefinitionFactory.resolveDuration(finalSettings);
                Function<Map<String, Double>, Model> factory =
                        ModelDefinitionFactory.createFactory(def, finalSettings);

                MultiParameterSweep.Builder builder = MultiParameterSweep.builder()
                        .modelFactory(factory)
                        .timeStep(timeStep)
                        .duration(duration);

                for (MultiParameterSweepDialog.ParamConfig p : config.parameters()) {
                    builder.parameter(p.name(),
                            ParameterSweep.linspace(p.start(), p.end(), p.step()));
                }

                return builder.build().execute();
            }
        };

        task.setOnSucceeded(e -> {
            statusBar.clearProgress();
            dashboardPanel.showMultiSweepResult(task.getValue());
            switchToDashboard();
            String paramSummary = config.parameters().stream()
                    .map(MultiParameterSweepDialog.ParamConfig::name)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            fireLogEvent(l -> l.onAnalysisRun("Multi-Parameter Sweep", paramSummary));
        });

        task.setOnFailed(e -> {
            statusBar.clearProgress();
            Throwable ex = task.getException();
            showError("Multi-Sweep Error", ex != null ? ex.getMessage() : null);
        });

        Thread thread = new Thread(task, "multi-parameter-sweep");
        thread.setDaemon(true);
        thread.start();
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

        statusBar.showProgress("Running Monte Carlo (" + config.iterations() + " iterations)...");

        javafx.concurrent.Task<MonteCarloResult> task = new javafx.concurrent.Task<>() {
            @Override
            protected MonteCarloResult call() {
                TimeUnit timeStep = ModelDefinitionFactory.resolveTimeStep(finalSettings);
                Quantity duration = ModelDefinitionFactory.resolveDuration(finalSettings);
                Function<Map<String, Double>, Model> factory =
                        ModelDefinitionFactory.createFactory(def, finalSettings);

                MonteCarlo.Builder builder = MonteCarlo.builder()
                        .modelFactory(factory)
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
            }
        };

        task.setOnSucceeded(e -> {
            statusBar.clearProgress();
            dashboardPanel.showMonteCarloResult(task.getValue());
            switchToDashboard();
            fireLogEvent(l -> l.onAnalysisRun("Monte Carlo",
                    config.iterations() + " iterations, " + config.parameters().size() + " params"));
        });

        task.setOnFailed(e -> {
            statusBar.clearProgress();
            Throwable ex = task.getException();
            showError("Monte Carlo Error", ex != null ? ex.getMessage() : null);
        });

        Thread thread = new Thread(task, "monte-carlo");
        thread.setDaemon(true);
        thread.start();
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

        statusBar.showProgress("Optimizing (" + config.maxEvaluations() + " max evals)...");

        javafx.concurrent.Task<OptimizationResult> task = new javafx.concurrent.Task<>() {
            @Override
            protected OptimizationResult call() {
                TimeUnit timeStep = ModelDefinitionFactory.resolveTimeStep(finalSettings);
                Quantity duration = ModelDefinitionFactory.resolveDuration(finalSettings);
                Function<Map<String, Double>, Model> factory =
                        ModelDefinitionFactory.createFactory(def, finalSettings);

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
                        .modelFactory(factory)
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
            }
        };

        task.setOnSucceeded(e -> {
            statusBar.clearProgress();
            dashboardPanel.showOptimizationResult(task.getValue());
            switchToDashboard();
            fireLogEvent(l -> l.onAnalysisRun("Optimization",
                    config.algorithm() + ", " + config.parameters().size() + " params"));
        });

        task.setOnFailed(e -> {
            statusBar.clearProgress();
            Throwable ex = task.getException();
            showError("Optimization Error", ex != null ? ex.getMessage() : null);
        });

        Thread thread = new Thread(task, "optimizer");
        thread.setDaemon(true);
        thread.start();
    }

    private void switchToDashboard() {
        if (rightTabPane != null && rightTabPane.getTabs().size() > 1) {
            rightTabPane.getSelectionModel().select(1);
        }
    }

    private void validateModel() {
        ModelDefinition def = canvas.toModelDefinition();

        javafx.concurrent.Task<ValidationResult> task = new javafx.concurrent.Task<>() {
            @Override
            protected ValidationResult call() {
                return ModelValidator.validate(def);
            }
        };

        task.setOnSucceeded(e -> {
            ValidationResult result = task.getValue();
            statusBar.updateValidation(result.errorCount(), result.warningCount());
            ValidationDialog dialog = new ValidationDialog(result, canvas::selectElement);
            dialog.show();
            fireLogEvent(l -> l.onValidation(result.errorCount(), result.warningCount()));
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            showError("Validation Error", ex != null ? ex.getMessage() : null);
        });

        Thread thread = new Thread(task, "model-validator");
        thread.setDaemon(true);
        thread.start();
    }

    private void fireLogEvent(java.util.function.Consumer<ModelEditListener> event) {
        if (logListener != null) {
            event.accept(logListener);
        }
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
                activeEditor.getModules().size());
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

    private void updateTitle() {
        String fileName = currentFile != null
                ? currentFile.getFileName().toString()
                : "Untitled";
        String moduleSuffix = canvas.isInsideModule()
                ? " [" + canvas.getCurrentModuleName() + "]"
                : "";
        stage.setTitle("Forrester \u2014 " + fileName + moduleSuffix);
    }

    private void updateBreadcrumb() {
        if (breadcrumbBar != null && canvas.getEditor() != null) {
            breadcrumbBar.update(canvas.getNavigationPath());
        }
        updateTitle();
        updateStatusBar();
    }

    /**
     * Closes this window. The ForresterApp will be notified via the stage's onHidden handler.
     */
    public void close() {
        stage.close();
    }
}
