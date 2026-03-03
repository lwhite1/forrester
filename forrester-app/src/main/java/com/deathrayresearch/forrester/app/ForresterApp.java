package com.deathrayresearch.forrester.app;

import com.deathrayresearch.forrester.app.canvas.CanvasToolBar;
import com.deathrayresearch.forrester.app.canvas.ModelCanvas;
import com.deathrayresearch.forrester.app.canvas.ModelEditor;
import com.deathrayresearch.forrester.io.json.ModelDefinitionSerializer;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModelDefinitionBuilder;
import com.deathrayresearch.forrester.model.def.ViewDef;
import com.deathrayresearch.forrester.model.graph.AutoLayout;

import javafx.application.Application;
import javafx.scene.Scene;
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

/**
 * JavaFX entry point for the Forrester visual editor.
 * Provides a canvas-based system dynamics model editor with element creation,
 * inline editing, flow connections, and file save/load.
 */
public class ForresterApp extends Application {

    private Stage stage;
    private ModelCanvas canvas;
    private ModelEditor editor;
    private Path currentFile;

    private final ModelDefinitionSerializer serializer = new ModelDefinitionSerializer();

    @Override
    public void start(Stage stage) {
        this.stage = stage;

        editor = new ModelEditor();
        canvas = new ModelCanvas();

        CanvasToolBar toolBar = new CanvasToolBar();
        toolBar.setOnToolChanged(canvas::setActiveTool);
        canvas.setToolBar(toolBar);

        // Start with an empty canvas
        newModel();

        // Wrap canvas in a Pane so it can bind to available space in the center region
        Pane canvasPane = new Pane(canvas);
        canvas.widthProperty().bind(canvasPane.widthProperty());
        canvas.heightProperty().bind(canvasPane.heightProperty());
        canvas.setOverlayPane(canvasPane);

        MenuBar menuBar = createMenuBar();
        VBox topContainer = new VBox(menuBar, toolBar);

        BorderPane root = new BorderPane();
        root.setTop(topContainer);
        root.setCenter(canvasPane);

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

        return new MenuBar(fileMenu);
    }

    private void newModel() {
        ModelDefinition empty = new ModelDefinitionBuilder()
                .name("Untitled")
                .build();
        ViewDef emptyView = new ViewDef("Main", List.of(), List.of(), List.of());

        editor = new ModelEditor();
        editor.loadFrom(empty);
        canvas.setModel(editor, emptyView);
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

            canvas.setModel(editor, view);
            currentFile = file.toPath();
            updateTitle();
        } catch (IOException ex) {
            ex.printStackTrace();
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
            ex.printStackTrace();
        }
    }

    private void updateTitle() {
        String fileName = currentFile != null
                ? currentFile.getFileName().toString()
                : "Untitled";
        stage.setTitle("Forrester \u2014 " + fileName);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
