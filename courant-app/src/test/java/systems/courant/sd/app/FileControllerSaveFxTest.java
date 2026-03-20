package systems.courant.sd.app;

import systems.courant.sd.app.canvas.Clipboard;
import systems.courant.sd.io.ImportResult;
import systems.courant.sd.io.vensim.VensimImporter;
import systems.courant.sd.io.xmile.XmileImporter;
import systems.courant.sd.model.def.ModelDefinition;

import javafx.application.Platform;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FileController save/export clears dirty flag (#422)")
@ExtendWith(ApplicationExtension.class)
class FileControllerSaveFxTest {

    private ModelWindow window;
    private Stage stage;

    @Start
    void start(Stage stage) {
        this.stage = stage;
        CourantApp app = new CourantApp();
        window = new ModelWindow(stage, app, new Clipboard());
        stage.show();
    }

    private static Path resourcePath(String name) {
        try {
            return Path.of(FileControllerSaveFxTest.class.getResource("/" + name).toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadTeacupModel() throws Exception {
        ImportResult result = new VensimImporter()
                .importModel(resourcePath("vensim/teacup.mdl"));
        Platform.runLater(() -> {
            window.loadDefinition(result.definition(), "teacup.mdl");
            window.setCurrentFile(null);
        });
        WaitForAsyncUtils.waitForFxEvents();
        window.layoutFuture().get(10, TimeUnit.SECONDS);
        WaitForAsyncUtils.waitForFxEvents();
    }

    @Test
    @DisplayName("saveToChosenFile with .mdl clears dirty flag but does not change currentFile (#1139)")
    void shouldClearDirtyAfterVensimExport(FxRobot robot, @TempDir Path tempDir) throws Exception {
        loadTeacupModel();

        Platform.runLater(() -> window.getFileController().markDirty());
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(window.isDirty()).isTrue();

        Path previousFile = window.getCurrentFile();
        File mdlFile = tempDir.resolve("exported.mdl").toFile();
        Platform.runLater(() -> window.getFileController().saveToChosenFile(mdlFile));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(window.isDirty()).isFalse();
        assertThat(window.getCurrentFile()).isEqualTo(previousFile);
        assertThat(Files.exists(mdlFile.toPath())).isTrue();
        assertThat(Files.size(mdlFile.toPath())).isGreaterThan(0);
    }

    @Test
    @DisplayName("saveToChosenFile with .xmile clears dirty flag but does not change currentFile (#1139)")
    void shouldClearDirtyAfterXmileExport(FxRobot robot, @TempDir Path tempDir) throws Exception {
        loadTeacupModel();

        Platform.runLater(() -> window.getFileController().markDirty());
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(window.isDirty()).isTrue();

        Path previousFile = window.getCurrentFile();
        File xmileFile = tempDir.resolve("exported.xmile").toFile();
        Platform.runLater(() -> window.getFileController().saveToChosenFile(xmileFile));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(window.isDirty()).isFalse();
        assertThat(window.getCurrentFile()).isEqualTo(previousFile);
        assertThat(Files.exists(xmileFile.toPath())).isTrue();
        assertThat(Files.size(xmileFile.toPath())).isGreaterThan(0);
    }

    @Test
    @DisplayName("saveToChosenFile with .stmx clears dirty flag but does not change currentFile (#1139)")
    void shouldClearDirtyAfterStmxExport(FxRobot robot, @TempDir Path tempDir) throws Exception {
        loadTeacupModel();

        Platform.runLater(() -> window.getFileController().markDirty());
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(window.isDirty()).isTrue();

        Path previousFile = window.getCurrentFile();
        File stmxFile = tempDir.resolve("exported.stmx").toFile();
        Platform.runLater(() -> window.getFileController().saveToChosenFile(stmxFile));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(window.isDirty()).isFalse();
        assertThat(window.getCurrentFile()).isEqualTo(previousFile);
    }

    @Test
    @DisplayName("saveToChosenFile with .json clears dirty flag and updates currentFile")
    void shouldClearDirtyAfterJsonSave(FxRobot robot, @TempDir Path tempDir) throws Exception {
        loadTeacupModel();

        Platform.runLater(() -> window.getFileController().markDirty());
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(window.isDirty()).isTrue();

        File jsonFile = tempDir.resolve("exported.json").toFile();
        Platform.runLater(() -> window.getFileController().saveToChosenFile(jsonFile));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(window.isDirty()).isFalse();
        assertThat(window.getCurrentFile()).isEqualTo(jsonFile.toPath());
        assertThat(Files.exists(jsonFile.toPath())).isTrue();
    }

    @Test
    @DisplayName("window title drops dirty indicator after export")
    void shouldUpdateTitleAfterExport(FxRobot robot, @TempDir Path tempDir) throws Exception {
        loadTeacupModel();

        Platform.runLater(() -> window.getFileController().markDirty());
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(stage.getTitle()).contains("\u2022");

        File mdlFile = tempDir.resolve("exported.mdl").toFile();
        Platform.runLater(() -> window.getFileController().saveToChosenFile(mdlFile));
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(stage.getTitle()).doesNotContain("\u2022");
    }

    @Test
    @DisplayName("save() after .mdl export does not overwrite the .mdl file with JSON (#1139)")
    void shouldNotCorruptMdlFileOnSubsequentSave(FxRobot robot, @TempDir Path tempDir) throws Exception {
        loadTeacupModel();

        // First, save as JSON so currentFile is set
        File jsonFile = tempDir.resolve("model.json").toFile();
        Platform.runLater(() -> window.getFileController().saveToChosenFile(jsonFile));
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(window.getCurrentFile()).isEqualTo(jsonFile.toPath());

        // Export to .mdl
        File mdlFile = tempDir.resolve("exported.mdl").toFile();
        Platform.runLater(() -> window.getFileController().saveToChosenFile(mdlFile));
        WaitForAsyncUtils.waitForFxEvents();

        // currentFile should still point to the JSON file
        assertThat(window.getCurrentFile()).isEqualTo(jsonFile.toPath());

        // Mark dirty and Ctrl+S (save())
        Platform.runLater(() -> {
            window.getFileController().markDirty();
            window.getFileController().save();
        });
        WaitForAsyncUtils.waitForFxEvents();

        // The .mdl file should still be valid Vensim, not JSON
        ImportResult reimported = new VensimImporter().importModel(mdlFile.toPath());
        assertThat(reimported.definition().stocks()).hasSize(1);
    }

    @Test
    @DisplayName("save() after .xmile export does not overwrite the .xmile file with JSON (#1139)")
    void shouldNotCorruptXmileFileOnSubsequentSave(FxRobot robot, @TempDir Path tempDir) throws Exception {
        loadTeacupModel();

        // First, save as JSON so currentFile is set
        File jsonFile = tempDir.resolve("model.json").toFile();
        Platform.runLater(() -> window.getFileController().saveToChosenFile(jsonFile));
        WaitForAsyncUtils.waitForFxEvents();

        // Export to .xmile
        File xmileFile = tempDir.resolve("exported.xmile").toFile();
        Platform.runLater(() -> window.getFileController().saveToChosenFile(xmileFile));
        WaitForAsyncUtils.waitForFxEvents();

        // currentFile should still point to the JSON file
        assertThat(window.getCurrentFile()).isEqualTo(jsonFile.toPath());

        // Mark dirty and Ctrl+S (save())
        Platform.runLater(() -> {
            window.getFileController().markDirty();
            window.getFileController().save();
        });
        WaitForAsyncUtils.waitForFxEvents();

        // The .xmile file should still be valid XMILE, not JSON
        ImportResult reimported = new XmileImporter().importModel(xmileFile.toPath());
        assertThat(reimported.definition().stocks()).hasSize(1);
    }

    @Test
    @DisplayName("exported Vensim file can be reimported after saveToChosenFile")
    void shouldProduceValidVensimFile(FxRobot robot, @TempDir Path tempDir) throws Exception {
        loadTeacupModel();

        File mdlFile = tempDir.resolve("roundtrip.mdl").toFile();
        Platform.runLater(() -> window.getFileController().saveToChosenFile(mdlFile));
        WaitForAsyncUtils.waitForFxEvents();

        ImportResult reimported = new VensimImporter().importModel(mdlFile.toPath());
        assertThat(reimported.definition().stocks()).hasSize(1);
        assertThat(reimported.definition().stocks().getFirst().name())
                .isEqualTo("Teacup Temperature");
    }

    @Test
    @DisplayName("exported XMILE file can be reimported after saveToChosenFile")
    void shouldProduceValidXmileFile(FxRobot robot, @TempDir Path tempDir) throws Exception {
        loadTeacupModel();

        File xmileFile = tempDir.resolve("roundtrip.xmile").toFile();
        Platform.runLater(() -> window.getFileController().saveToChosenFile(xmileFile));
        WaitForAsyncUtils.waitForFxEvents();

        ImportResult reimported = new XmileImporter().importModel(xmileFile.toPath());
        assertThat(reimported.definition().stocks()).hasSize(1);
    }
}
