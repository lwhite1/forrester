package com.deathrayresearch.forrester.app;

import com.deathrayresearch.forrester.app.canvas.Clipboard;
import com.deathrayresearch.forrester.app.canvas.ModelEditor;
import com.deathrayresearch.forrester.io.ImportResult;
import com.deathrayresearch.forrester.io.vensim.VensimExporter;
import com.deathrayresearch.forrester.io.vensim.VensimImporter;
import com.deathrayresearch.forrester.io.xmile.XmileExporter;
import com.deathrayresearch.forrester.io.xmile.XmileImporter;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModelDefinitionBuilder;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ModelWindow Import/Export (TestFX)")
@ExtendWith(ApplicationExtension.class)
class ModelWindowImportExportFxTest {

    private ModelWindow window;
    private Stage stage;

    @Start
    void start(Stage stage) {
        this.stage = stage;
        ForresterApp app = new ForresterApp();
        window = new ModelWindow(stage, app, new Clipboard());
        stage.show();
    }

    private static Path resourcePath(String name) {
        try {
            return Path.of(ModelWindowImportExportFxTest.class.getResource("/" + name).toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    // --- Vensim Import ---

    @Test
    @DisplayName("Vensim teacup import populates canvas with 1 stock and 1 flow")
    void shouldLoadVensimTeacupModel(FxRobot robot) throws IOException {
        ImportResult result = new VensimImporter().importModel(resourcePath("vensim/teacup.mdl"));

        Platform.runLater(() -> {
            window.loadDefinition(result.definition(), "teacup.mdl");
            window.setCurrentFile(null);
        });
        WaitForAsyncUtils.waitForFxEvents();

        ModelEditor editor = window.getEditor();
        assertThat(editor.getStocks()).hasSize(1);
        assertThat(editor.getStocks().getFirst().name()).isEqualTo("Teacup_Temperature");
        assertThat(editor.getFlows()).hasSize(1);
        assertThat(window.getCurrentFile()).isNull();
    }

    @Test
    @DisplayName("Vensim SIR import populates canvas with 3 stocks and 3 net flows")
    void shouldLoadVensimSirModel(FxRobot robot) throws IOException {
        ImportResult result = new VensimImporter().importModel(resourcePath("vensim/sir.mdl"));

        Platform.runLater(() -> window.loadDefinition(result.definition(), "sir.mdl"));
        WaitForAsyncUtils.waitForFxEvents();

        ModelEditor editor = window.getEditor();
        assertThat(editor.getStocks()).hasSize(3);
        assertThat(editor.getFlows()).hasSize(3);
    }

    // --- XMILE Import ---

    @Test
    @DisplayName("XMILE teacup import populates canvas with stock and flow")
    void shouldLoadXmileTeacupModel(FxRobot robot) throws IOException {
        ImportResult result = new XmileImporter()
                .importModel(resourcePath("xmile/teacup.xmile"));

        Platform.runLater(() -> {
            window.loadDefinition(result.definition(), "teacup.xmile");
            window.setCurrentFile(null);
        });
        WaitForAsyncUtils.waitForFxEvents();

        ModelEditor editor = window.getEditor();
        assertThat(editor.getStocks()).hasSize(1);
        assertThat(editor.getStocks().getFirst().name()).isEqualTo("Teacup_Temperature");
        assertThat(editor.getFlows()).hasSize(1);
        assertThat(editor.getFlows().getFirst().name()).isEqualTo("Heat_Loss_to_Room");
        assertThat(window.getCurrentFile()).isNull();
    }

    @Test
    @DisplayName("XMILE SIR import populates canvas with 3 stocks and 2 flows")
    void shouldLoadXmileSirModel(FxRobot robot) throws IOException {
        ImportResult result = new XmileImporter()
                .importModel(resourcePath("xmile/sir.xmile"));

        Platform.runLater(() -> window.loadDefinition(result.definition(), "sir.xmile"));
        WaitForAsyncUtils.waitForFxEvents();

        ModelEditor editor = window.getEditor();
        assertThat(editor.getStocks()).hasSize(3);
        assertThat(editor.getFlows()).hasSize(2);
    }

    // --- Export Round-Trip ---

    @Test
    @DisplayName("Export to Vensim .mdl and reimport preserves model structure")
    void shouldRoundTripVensim(FxRobot robot, @TempDir Path tempDir) throws IOException {
        ImportResult initial = new XmileImporter()
                .importModel(resourcePath("xmile/teacup.xmile"));

        Platform.runLater(() -> window.loadDefinition(initial.definition(), "teacup.xmile"));
        WaitForAsyncUtils.waitForFxEvents();

        ModelDefinition canvasDef = window.getCanvas().toModelDefinition();
        Path mdlFile = tempDir.resolve("teacup-export.mdl");
        VensimExporter.toFile(canvasDef, mdlFile);
        assertThat(Files.exists(mdlFile)).isTrue();
        assertThat(Files.size(mdlFile)).isGreaterThan(0);

        ImportResult reimported = new VensimImporter().importModel(mdlFile);
        assertThat(reimported.definition().stocks()).hasSize(1);
        assertThat(reimported.definition().stocks().getFirst().name())
                .isEqualTo("Teacup_Temperature");
    }

    @Test
    @DisplayName("Export to XMILE and reimport preserves model structure")
    void shouldRoundTripXmile(FxRobot robot, @TempDir Path tempDir) throws IOException {
        ImportResult initial = new VensimImporter()
                .importModel(resourcePath("vensim/teacup.mdl"));

        Platform.runLater(() -> window.loadDefinition(initial.definition(), "teacup.mdl"));
        WaitForAsyncUtils.waitForFxEvents();

        ModelDefinition canvasDef = window.getCanvas().toModelDefinition();
        Path xmileFile = tempDir.resolve("teacup-export.xmile");
        XmileExporter.toFile(canvasDef, xmileFile);
        assertThat(Files.exists(xmileFile)).isTrue();
        assertThat(Files.size(xmileFile)).isGreaterThan(0);

        ImportResult reimported = new XmileImporter().importModel(xmileFile);
        assertThat(reimported.definition().stocks()).hasSize(1);
        assertThat(reimported.definition().stocks().getFirst().name())
                .isEqualTo("Teacup_Temperature");
    }

    // --- Title and State ---

    @Test
    @DisplayName("After import, title shows model name from definition")
    void shouldShowModelNameAfterImport(FxRobot robot) throws IOException {
        ImportResult result = new VensimImporter()
                .importModel(resourcePath("vensim/teacup.mdl"));

        Platform.runLater(() -> {
            window.loadDefinition(result.definition(), "teacup.mdl");
            window.setCurrentFile(null);
        });
        WaitForAsyncUtils.waitForFxEvents();

        assertThat(stage.getTitle()).contains("teacup");
    }

    @Test
    @DisplayName("Status bar shows element counts after import")
    void shouldShowElementCountsAfterImport(FxRobot robot) throws IOException {
        ImportResult result = new VensimImporter()
                .importModel(resourcePath("vensim/sir.mdl"));

        Platform.runLater(() -> window.loadDefinition(result.definition(), "sir.mdl"));
        WaitForAsyncUtils.waitForFxEvents();

        Label elementsLabel = robot.lookup("#statusElements").queryAs(Label.class);
        assertThat(elementsLabel.getText()).contains("3 stocks");
        assertThat(elementsLabel.getText()).contains("3 flows");
    }

    @Test
    @DisplayName("loadDefinition replaces previous model cleanly")
    void shouldReplaceModelCleanly(FxRobot robot) throws IOException {
        ImportResult sir = new VensimImporter()
                .importModel(resourcePath("vensim/sir.mdl"));
        Platform.runLater(() -> window.loadDefinition(sir.definition(), "sir.mdl"));
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(window.getEditor().getStocks()).hasSize(3);

        ImportResult teacup = new XmileImporter()
                .importModel(resourcePath("xmile/teacup.xmile"));
        Platform.runLater(() -> window.loadDefinition(teacup.definition(), "teacup.xmile"));
        WaitForAsyncUtils.waitForFxEvents();
        assertThat(window.getEditor().getStocks()).hasSize(1);
    }

    // --- Import Warnings ---

    @Test
    @DisplayName("Import result contains definition regardless of warnings")
    void shouldImportWithDefinition(FxRobot robot) throws IOException {
        ImportResult result = new VensimImporter()
                .importModel(resourcePath("vensim/teacup.mdl"));
        assertThat(result.definition()).isNotNull();
        assertThat(result.definition().stocks()).isNotEmpty();
    }

    @Test
    @DisplayName("ImportResult with warnings is not clean")
    void shouldBeNotCleanWithWarnings(FxRobot robot) {
        ImportResult dirty = new ImportResult(
                new ModelDefinitionBuilder().name("Test").build(),
                List.of("Unsupported element: group"));
        assertThat(dirty.isClean()).isFalse();
        assertThat(dirty.warnings()).containsExactly("Unsupported element: group");
    }
}
