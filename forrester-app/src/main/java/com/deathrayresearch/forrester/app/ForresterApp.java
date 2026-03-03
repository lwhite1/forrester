package com.deathrayresearch.forrester.app;

import com.deathrayresearch.forrester.app.canvas.BreadcrumbBar;
import com.deathrayresearch.forrester.app.canvas.CanvasToolBar;
import com.deathrayresearch.forrester.app.canvas.ModelCanvas;
import com.deathrayresearch.forrester.app.canvas.ModelEditor;
import com.deathrayresearch.forrester.app.canvas.SimulationResultsDialog;
import com.deathrayresearch.forrester.app.canvas.SimulationRunner;
import com.deathrayresearch.forrester.app.canvas.SimulationSettingsDialog;
import com.deathrayresearch.forrester.app.canvas.StatusBar;
import com.deathrayresearch.forrester.app.canvas.UndoManager;
import com.deathrayresearch.forrester.io.json.ModelDefinitionSerializer;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModelDefinitionBuilder;
import com.deathrayresearch.forrester.model.def.SimulationSettings;
import com.deathrayresearch.forrester.model.def.ViewDef;
import com.deathrayresearch.forrester.model.graph.AutoLayout;
import com.deathrayresearch.forrester.model.graph.FeedbackAnalysis;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
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
 * JavaFX entry point for the Forrester visual editor.
 * Provides a canvas-based system dynamics model editor with element creation,
 * inline editing, flow connections, and file save/load.
 */
public class ForresterApp extends Application {

    private Stage stage;
    private ModelCanvas canvas;
    private ModelEditor editor;
    private StatusBar statusBar;
    private BreadcrumbBar breadcrumbBar;
    private Path currentFile;
    private final UndoManager undoManager = new UndoManager();
    private MenuItem undoItem;
    private MenuItem redoItem;

    private final ModelDefinitionSerializer serializer = new ModelDefinitionSerializer();

    @Override
    public void start(Stage stage) {
        this.stage = stage;

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
        canvas.setToolBar(toolBar);
        canvas.setOnStatusChanged(this::updateStatusBar);

        breadcrumbBar = new BreadcrumbBar();
        breadcrumbBar.setOnNavigateTo(depth -> {
            canvas.navigateToDepth(depth);
            canvas.requestFocus();
        });
        canvas.setOnNavigationChanged(this::updateBreadcrumb);

        // Start with an empty canvas
        newModel();

        // Wrap canvas in a Pane so it can bind to available space in the center region
        Pane canvasPane = new Pane(canvas);
        canvas.widthProperty().bind(canvasPane.widthProperty());
        canvas.heightProperty().bind(canvasPane.heightProperty());
        canvas.setOverlayPane(canvasPane);

        MenuBar menuBar = createMenuBar();
        VBox topContainer = new VBox(menuBar, toolBar, breadcrumbBar);

        BorderPane root = new BorderPane();
        root.setTop(topContainer);
        root.setCenter(canvasPane);
        root.setBottom(statusBar);

        Scene scene = new Scene(root, 1200, 800);
        stage.setScene(scene);
        updateTitle();
        stage.show();

        // Ensure canvas gets focus for keyboard events
        canvas.requestFocus();
    }

    private MenuBar createMenuBar() {
        Menu fileMenu = new Menu("File");

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

        MenuItem closeItem = new MenuItem("Close");
        closeItem.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN));
        closeItem.setOnAction(e -> newModel());

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> stage.close());

        fileMenu.getItems().addAll(newItem, openItem, new SeparatorMenuItem(),
                saveItem, saveAsItem, new SeparatorMenuItem(),
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

        simulateMenu.getItems().addAll(settingsItem, runItem);

        return new MenuBar(fileMenu, editMenu, simulateMenu);
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
            chooser.setInitialDirectory(currentFile.getParent().toFile());
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
        if (breadcrumbBar != null) {
            breadcrumbBar.update(canvas.getNavigationPath());
        }
        updateTitle();
        updateStatusBar();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
