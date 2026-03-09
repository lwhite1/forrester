package systems.courant.forrester.app;

import systems.courant.forrester.app.canvas.ModelCanvas;
import systems.courant.forrester.app.canvas.ModelEditListener;
import systems.courant.forrester.io.ImportResult;
import systems.courant.forrester.io.ModelImporter;
import systems.courant.forrester.io.json.ModelDefinitionSerializer;
import systems.courant.forrester.io.vensim.VensimExporter;
import systems.courant.forrester.io.vensim.VensimImporter;
import systems.courant.forrester.io.xmile.XmileExporter;
import systems.courant.forrester.io.xmile.XmileImporter;
import systems.courant.forrester.model.def.ModelDefinition;
import systems.courant.forrester.model.def.ModelDefinitionBuilder;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Handles file operations (new, open, save, import/export) for a {@link ModelWindow}.
 */
final class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private final Stage stage;
    private final ModelDefinitionSerializer serializer = new ModelDefinitionSerializer();
    private final BiConsumer<ModelDefinition, String> loadDefinition;
    private final Runnable updateTitle;
    private final Consumer<String> showError;
    private final Consumer<Consumer<ModelEditListener>> fireLogEvent;
    private final ModelCanvas canvas;

    private Path currentFile;
    private boolean dirty;

    FileController(Stage stage,
                   ModelCanvas canvas,
                   BiConsumer<ModelDefinition, String> loadDefinition,
                   Runnable updateTitle,
                   Consumer<String> showError,
                   Consumer<Consumer<ModelEditListener>> fireLogEvent) {
        this.stage = stage;
        this.canvas = canvas;
        this.loadDefinition = loadDefinition;
        this.updateTitle = updateTitle;
        this.showError = showError;
        this.fireLogEvent = fireLogEvent;
    }

    void newModel() {
        if (!confirmDiscardChanges()) {
            return;
        }
        ModelDefinition empty = new ModelDefinitionBuilder()
                .name("Untitled")
                .build();
        loadDefinition.accept(empty, null);
        currentFile = null;
        updateTitle.run();
    }

    void openFile() {
        if (!confirmDiscardChanges()) {
            return;
        }
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
                        showError.accept("Cannot determine file format (no file extension).");
                    } else {
                        showError.accept("Unsupported file format: " + ext);
                    }
                    return;
                }
            }

            loadDefinition.accept(def, name);

            // For native JSON files, track the file for Save; for imports, force Save As
            currentFile = ".json".equals(ext) ? file.toPath() : null;
            updateTitle.run();
        } catch (IOException ex) {
            showError.accept("Failed to open file: " + ex.getMessage());
        } catch (RuntimeException ex) {
            showError.accept("Failed to parse file: " + ex.getMessage());
        }
    }

    Menu buildExamplesMenu() {
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
        } catch (IOException ex) {
            log.warn("Failed to load examples catalog", ex);
            MenuItem empty = new MenuItem("(no examples found)");
            empty.setDisable(true);
            menu.getItems().add(empty);
        }
        return menu;
    }

    void save() {
        if (currentFile != null) {
            saveToFile(currentFile);
        } else {
            saveAs();
        }
    }

    void saveAs() {
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
                    fireLogEvent.accept(l -> l.onModelSaved(name));
                }
                case ".xmile", ".stmx", ".itmx" -> {
                    XmileExporter.toFile(def, file.toPath());
                    fireLogEvent.accept(l -> l.onModelSaved(name));
                }
                default -> {
                    currentFile = file.toPath();
                    saveToFile(currentFile);
                    return;
                }
            }
        } catch (IOException ex) {
            showError.accept("Failed to save file: " + ex.getMessage());
        } catch (RuntimeException ex) {
            showError.accept("Failed to export file: " + ex.getMessage());
        }
    }

    boolean confirmDiscardChanges() {
        if (!dirty) {
            return true;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("You have unsaved changes.");
        alert.setContentText("Do you want to discard your changes?");
        alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        alert.initOwner(stage);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    void markDirty() {
        if (!dirty) {
            dirty = true;
            updateTitle.run();
        }
    }

    ModelEditListener createDirtyListener() {
        return new ModelEditListener() {
            @Override
            public void onElementAdded(String name, String typeName) {
                markDirty();
            }

            @Override
            public void onElementRemoved(String name) {
                markDirty();
            }

            @Override
            public void onElementRenamed(String oldName, String newName) {
                markDirty();
            }

            @Override
            public void onEquationChanged(String elementName) {
                markDirty();
            }

            @Override
            public void onConstantChanged(String name) {
                markDirty();
            }
        };
    }

    boolean isDirty() {
        return dirty;
    }

    void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    Path getCurrentFile() {
        return currentFile;
    }

    void setCurrentFile(Path path) {
        this.currentFile = path;
        updateTitle.run();
    }

    void openExample(String name, String resourcePath) {
        try (InputStream in = getClass().getResourceAsStream("/models/" + resourcePath)) {
            if (in == null) {
                showError.accept("Example resource not found: " + resourcePath);
                return;
            }
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            ModelDefinition def = serializer.fromJson(json);

            loadDefinition.accept(def, name);
            currentFile = null;
            updateTitle.run();
        } catch (IOException ex) {
            showError.accept("Failed to load example: " + ex.getMessage());
        }
    }

    private void saveToFile(Path path) {
        try {
            ModelDefinition def = canvas.toModelDefinition();
            serializer.toFile(def, path);
            dirty = false;
            updateTitle.run();
            fireLogEvent.accept(l -> l.onModelSaved(path.getFileName().toString()));
        } catch (IOException ex) {
            showError.accept("Failed to save file: " + ex.getMessage());
        }
    }

    private ModelDefinition importModel(ModelImporter importer, Path path, String displayName)
            throws IOException {
        ImportResult result = importer.importModel(path);
        if (!result.isClean()) {
            showImportWarnings(displayName, result.warnings());
        }
        return result.definition();
    }

    private void showImportWarnings(String fileName, List<String> warnings) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Import Warnings");
        alert.setHeaderText("Warnings while importing " + fileName);
        alert.setContentText(String.join("\n", warnings));
        alert.showAndWait();
    }
}
