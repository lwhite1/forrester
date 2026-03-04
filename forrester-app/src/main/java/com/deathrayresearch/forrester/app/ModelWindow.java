package com.deathrayresearch.forrester.app;

import com.deathrayresearch.forrester.app.canvas.BreadcrumbBar;
import com.deathrayresearch.forrester.app.canvas.CanvasToolBar;
import com.deathrayresearch.forrester.app.canvas.DiagramExporter;
import com.deathrayresearch.forrester.app.canvas.ModelCanvas;
import com.deathrayresearch.forrester.app.canvas.ModelEditor;
import com.deathrayresearch.forrester.app.canvas.PropertiesPanel;
import com.deathrayresearch.forrester.app.canvas.SimulationResultsDialog;
import com.deathrayresearch.forrester.app.canvas.SimulationRunner;
import com.deathrayresearch.forrester.app.canvas.SimulationSettingsDialog;
import com.deathrayresearch.forrester.app.canvas.StatusBar;
import com.deathrayresearch.forrester.app.canvas.UndoManager;
import com.deathrayresearch.forrester.app.canvas.ValidationDialog;
import com.deathrayresearch.forrester.io.json.ModelDefinitionSerializer;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModelDefinitionBuilder;
import com.deathrayresearch.forrester.model.def.ModelValidator;
import com.deathrayresearch.forrester.model.def.SimulationSettings;
import com.deathrayresearch.forrester.model.def.ValidationResult;
import com.deathrayresearch.forrester.model.def.ViewDef;
import com.deathrayresearch.forrester.model.graph.AutoLayout;
import com.deathrayresearch.forrester.model.graph.FeedbackAnalysis;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * An independent editor window for a single Forrester model.
 * Each window owns its own canvas, editor, undo stack, and file state.
 */
public class ModelWindow {

    private final Stage stage;
    private final ForresterApp app;
    private ModelCanvas canvas;
    private ModelEditor editor;
    private StatusBar statusBar;
    private BreadcrumbBar breadcrumbBar;
    private PropertiesPanel propertiesPanel;
    private Path currentFile;
    private final UndoManager undoManager = new UndoManager();
    private MenuItem undoItem;
    private MenuItem redoItem;

    private final ModelDefinitionSerializer serializer = new ModelDefinitionSerializer();

    public ModelWindow(Stage stage, ForresterApp app) {
        this.stage = stage;
        this.app = app;
        buildUI();
    }

    private void buildUI() {
        editor = new ModelEditor();
        canvas = new ModelCanvas();
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

        newModel();

        Pane canvasPane = new Pane(canvas);
        canvas.widthProperty().bind(canvasPane.widthProperty());
        canvas.heightProperty().bind(canvasPane.heightProperty());
        canvas.setOverlayPane(canvasPane);

        propertiesPanel = new PropertiesPanel();

        SplitPane splitPane = new SplitPane(canvasPane, propertiesPanel);
        splitPane.setDividerPositions(0.75);
        SplitPane.setResizableWithParent(propertiesPanel, false);

        MenuBar menuBar = createMenuBar();
        VBox topContainer = new VBox(menuBar, toolBar, breadcrumbBar);

        BorderPane root = new BorderPane();
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

        fileMenu.getItems().addAll(newWindowItem, newItem, openItem, new SeparatorMenuItem(),
                saveItem, saveAsItem, exportItem, new SeparatorMenuItem(),
                closeItem, exitItem);

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

        Menu simulateMenu = new Menu("Simulate");

        MenuItem settingsItem = new MenuItem("Simulation Settings...");
        settingsItem.setOnAction(e -> openSimulationSettings());

        MenuItem runItem = new MenuItem("Run Simulation");
        runItem.setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN));
        runItem.setOnAction(e -> runSimulation());

        MenuItem validateItem = new MenuItem("Validate Model");
        validateItem.setAccelerator(new KeyCodeCombination(KeyCode.B, KeyCombination.SHORTCUT_DOWN));
        validateItem.setOnAction(e -> validateModel());

        simulateMenu.getItems().addAll(settingsItem, runItem, new SeparatorMenuItem(), validateItem);

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

        return new MenuBar(fileMenu, editMenu, simulateMenu, helpMenu);
    }

    private void newModel() {
        ModelDefinition empty = new ModelDefinitionBuilder()
                .name("Untitled")
                .build();
        ViewDef emptyView = new ViewDef("Main", List.of(), List.of(), List.of());

        editor = new ModelEditor();
        editor.loadFrom(empty);
        canvas.clearNavigation();
        canvas.setModel(editor, emptyView);
        undoManager.clear();
        currentFile = null;
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
            editor = new ModelEditor();
            editor.loadFrom(def);

            ViewDef view;
            if (!def.views().isEmpty()) {
                view = def.views().get(0);
            } else {
                view = AutoLayout.layout(def);
            }

            canvas.clearNavigation();
            canvas.setModel(editor, view);
            undoManager.clear();
            currentFile = file.toPath();
            updateTitle();
        } catch (IOException ex) {
            showError("Open Error", "Failed to open file: " + ex.getMessage());
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

    private void runSimulation() {
        SimulationSettings settings = editor.getSimulationSettings();
        if (settings == null) {
            SimulationSettingsDialog dialog = new SimulationSettingsDialog(null);
            Optional<SimulationSettings> result = dialog.showAndWait();
            if (result.isEmpty()) {
                return;
            }
            settings = result.get();
            editor.setSimulationSettings(settings);
        }

        ModelDefinition def = canvas.toModelDefinition();
        SimulationSettings finalSettings = settings;

        javafx.concurrent.Task<SimulationRunner.SimulationResult> task =
                new javafx.concurrent.Task<>() {
            @Override
            protected SimulationRunner.SimulationResult call() {
                SimulationRunner runner = new SimulationRunner();
                return runner.run(def, finalSettings);
            }
        };

        task.setOnSucceeded(e -> {
            SimulationResultsDialog resultsDialog = new SimulationResultsDialog(task.getValue());
            resultsDialog.show();
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            showError("Simulation Error", ex != null ? ex.getMessage() : null);
        });

        Thread thread = new Thread(task, "simulation-runner");
        thread.setDaemon(true);
        thread.start();
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
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            showError("Validation Error", ex != null ? ex.getMessage() : null);
        });

        Thread thread = new Thread(task, "model-validator");
        thread.setDaemon(true);
        thread.start();
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
