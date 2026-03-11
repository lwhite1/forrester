package systems.courant.forrester.integration;

import systems.courant.forrester.Simulation;
import systems.courant.forrester.io.ImportResult;
import systems.courant.forrester.io.vensim.VensimImporter;
import systems.courant.forrester.model.Stock;
import systems.courant.forrester.model.compile.CompiledModel;
import systems.courant.forrester.model.compile.ModelCompiler;
import systems.courant.forrester.model.def.ModelDefinition;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Integration test: Vensim .mdl file → parse → compile → simulate → verify results.
 *
 * <p>Tests the full import pipeline from raw Vensim model text through simulation execution,
 * verifying that imported models produce physically plausible results.
 */
@DisplayName("Vensim import pipeline (.mdl → parse → compile → simulate)")
class VensimImportPipelineTest {

    private final VensimImporter importer = new VensimImporter();
    private final ModelCompiler compiler = new ModelCompiler();

    @Test
    @DisplayName("should import and simulate teacup cooling model")
    void shouldSimulateTeacup() throws IOException {
        String mdl = loadResource("vensim/teacup.mdl");
        ImportResult result = importer.importModel(mdl, "Teacup");
        ModelDefinition def = result.definition();

        assertThat(def.stocks()).isNotEmpty();
        assertThat(def.name()).isEqualTo("Teacup");

        CompiledModel compiled = compiler.compile(def);
        Simulation sim = compiled.createSimulation();
        sim.execute();

        Stock temp = findStock(compiled, "Teacup Temperature");
        // Teacup starts at 180°, room is 70°; should cool toward room temperature
        assertThat(temp.getValue())
                .as("Teacup should cool toward room temperature")
                .isGreaterThan(69)
                .isLessThan(180);
    }

    @Test
    @DisplayName("should import and simulate SIR epidemic model")
    void shouldSimulateSIR() throws IOException {
        String mdl = loadResource("vensim/sir.mdl");
        ImportResult result = importer.importModel(mdl, "SIR");
        ModelDefinition def = result.definition();

        assertThat(def.stocks()).hasSizeGreaterThanOrEqualTo(3);

        CompiledModel compiled = compiler.compile(def);
        Simulation sim = compiled.createSimulation();
        sim.execute();

        List<Stock> stocks = compiled.getModel().getStocks();
        // All stocks should be finite after simulation
        for (Stock s : stocks) {
            assertThat(s.getValue())
                    .as("Stock '%s' should be finite", s.getName())
                    .isFinite();
        }

        // Recovered should be positive (epidemic ran its course)
        Stock recovered = findStock(compiled, "Recovered");
        assertThat(recovered.getValue())
                .as("Recovered population should be positive")
                .isGreaterThan(0);
    }

    @Test
    @DisplayName("should report warnings for unsupported constructs without failing")
    void shouldCollectWarnings() throws IOException {
        String mdl = loadResource("vensim/teacup.mdl");
        ImportResult result = importer.importModel(mdl, "Teacup");

        // Warnings list should be non-null (may be empty for clean models)
        assertThat(result.warnings()).isNotNull();
        // Model should still be compilable regardless of warnings
        CompiledModel compiled = compiler.compile(result.definition());
        assertThat(compiled).isNotNull();
    }

    @Test
    @DisplayName("should preserve constant values through import")
    void shouldPreserveConstants() throws IOException {
        String mdl = loadResource("vensim/teacup.mdl");
        ImportResult result = importer.importModel(mdl, "Teacup");
        ModelDefinition def = result.definition();

        // Room Temperature should be 70 degrees
        var roomTemp = def.parameters().stream()
                .filter(c -> c.name().equals("Room Temperature"))
                .findFirst();
        assertThat(roomTemp).isPresent();
        assertThat(roomTemp.get().literalValue()).isCloseTo(70.0, within(0.001));
    }

    private Stock findStock(CompiledModel compiled, String name) {
        return compiled.getModel().getStocks().stream()
                .filter(s -> s.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Stock not found: " + name));
    }

    private String loadResource(String path) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Resource not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
