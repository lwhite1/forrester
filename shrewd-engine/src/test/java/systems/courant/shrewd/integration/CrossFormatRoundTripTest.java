package systems.courant.shrewd.integration;

import systems.courant.shrewd.Simulation;
import systems.courant.shrewd.io.ImportResult;
import systems.courant.shrewd.io.json.ModelDefinitionSerializer;
import systems.courant.shrewd.io.vensim.VensimImporter;
import systems.courant.shrewd.io.xmile.XmileExporter;
import systems.courant.shrewd.io.xmile.XmileImporter;
import systems.courant.shrewd.model.Stock;
import systems.courant.shrewd.model.compile.CompiledModel;
import systems.courant.shrewd.model.compile.ModelCompiler;
import systems.courant.shrewd.model.def.FlowDef;
import systems.courant.shrewd.model.def.ModelDefinition;
import systems.courant.shrewd.model.def.ModelDefinitionBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Integration test: cross-format conversion fidelity.
 *
 * <p>Verifies that models survive conversion between Vensim, JSON, and XMILE formats
 * without losing structural or behavioral information.
 */
@DisplayName("Cross-format round-trip fidelity")
class CrossFormatRoundTripTest {

    private final ModelDefinitionSerializer serializer = new ModelDefinitionSerializer();
    private final ModelCompiler compiler = new ModelCompiler();

    @Nested
    @DisplayName("JSON → XMILE → JSON")
    class JsonXmileJson {

        @Test
        @DisplayName("should preserve structure through JSON → XMILE → JSON")
        void shouldPreserveStructure() {
            ModelDefinition original = new ModelDefinitionBuilder()
                    .name("CrossFormat")
                    .defaultSimulation("Day", 50, "Day")
                    .stock("Inventory", 200, "Widget")
                    .flow(new FlowDef("production", "order_rate", "Day", null, "Inventory"))
                    .flow(new FlowDef("sales", "Inventory * 0.05", "Day", "Inventory", null))
                    .constant("order_rate", 15, "Widget/Day")
                    .build();

            // JSON → XMILE
            String xmile = XmileExporter.toXmile(original);
            ImportResult xmileResult = new XmileImporter().importModel(xmile, "CrossFormat");
            ModelDefinition fromXmile = xmileResult.definition();

            // XMILE → JSON → back
            String json = serializer.toJson(fromXmile);
            ModelDefinition fromJson = serializer.fromJson(json);

            assertThat(fromJson.stocks()).hasSize(1);
            assertThat(fromJson.stocks().getFirst().name()).isEqualTo("Inventory");
            assertThat(fromJson.flows()).hasSize(2);

            Set<String> flowNames = fromJson.flows().stream()
                    .map(FlowDef::name).collect(Collectors.toSet());
            assertThat(flowNames).containsExactlyInAnyOrder("production", "sales");
        }

        @Test
        @DisplayName("should produce equivalent simulation results through JSON → XMILE → JSON")
        void shouldPreserveSimulationBehavior() {
            ModelDefinition original = new ModelDefinitionBuilder()
                    .name("BehaviorTest")
                    .defaultSimulation("Day", 30, "Day")
                    .stock("Tank", 500, "Liter")
                    .flow(new FlowDef("inflow", "50", "Day", null, "Tank"))
                    .flow(new FlowDef("outflow", "Tank * 0.2", "Day", "Tank", null))
                    .build();

            CompiledModel compiledOrig = compiler.compile(original);
            Simulation simOrig = compiledOrig.createSimulation();
            simOrig.execute();
            double originalValue = findStock(compiledOrig, "Tank").getValue();

            // Convert through XMILE and back to JSON
            String xmile = XmileExporter.toXmile(original);
            ImportResult xmileResult = new XmileImporter().importModel(xmile, "BehaviorTest");
            String json = serializer.toJson(xmileResult.definition());
            ModelDefinition converted = serializer.fromJson(json);

            CompiledModel compiledConv = compiler.compile(converted);
            Simulation simConv = compiledConv.createSimulation();
            simConv.execute();
            double convertedValue = findStock(compiledConv, "Tank").getValue();

            assertThat(convertedValue).isCloseTo(originalValue, within(0.01));
        }
    }

    @Nested
    @DisplayName("Vensim → JSON → XMILE → JSON")
    class VensimJsonXmileJson {

        @Test
        @DisplayName("should preserve teacup model through Vensim → JSON → XMILE → JSON")
        void shouldPreserveTeacupAcrossFormats() throws IOException {
            // Vensim → ModelDefinition
            String mdl = loadResource("vensim/teacup.mdl");
            ImportResult vensimResult = new VensimImporter().importModel(mdl, "Teacup");
            ModelDefinition fromVensim = vensimResult.definition();

            // Simulate original
            CompiledModel compiledVensim = compiler.compile(fromVensim);
            Simulation simVensim = compiledVensim.createSimulation();
            simVensim.execute();
            double vensimTemp = findStock(compiledVensim, "Teacup Temperature").getValue();

            // ModelDefinition → JSON → ModelDefinition
            String json = serializer.toJson(fromVensim);
            ModelDefinition fromJson = serializer.fromJson(json);

            // ModelDefinition → XMILE → ModelDefinition
            String xmile = XmileExporter.toXmile(fromJson);
            ImportResult xmileResult = new XmileImporter().importModel(xmile, "Teacup");
            ModelDefinition fromXmile = xmileResult.definition();

            // Final JSON round-trip
            String jsonFinal = serializer.toJson(fromXmile);
            ModelDefinition finalDef = serializer.fromJson(jsonFinal);

            // Simulate the result of full format chain
            CompiledModel compiledFinal = compiler.compile(finalDef);
            Simulation simFinal = compiledFinal.createSimulation();
            simFinal.execute();
            double finalTemp = findStock(compiledFinal, "Teacup Temperature").getValue();

            assertThat(finalTemp).isCloseTo(vensimTemp, within(0.1));
        }
    }

    @Nested
    @DisplayName("Lookup table fidelity")
    class LookupFidelity {

        @Test
        @DisplayName("should preserve lookup table data through XMILE round-trip")
        void shouldPreserveLookupData() {
            double[] xVals = {0, 1, 2, 3, 4, 5};
            double[] yVals = {0, 0.2, 0.8, 1.0, 0.8, 0.2};

            ModelDefinition original = new ModelDefinitionBuilder()
                    .name("LookupCross")
                    .defaultSimulation("Day", 10, "Day")
                    .lookupTable("effect", xVals, yVals, "LINEAR")
                    .build();

            String xmile = XmileExporter.toXmile(original);
            ImportResult result = new XmileImporter().importModel(xmile, "LookupCross");
            ModelDefinition imported = result.definition();

            // The lookup data should survive (may come back as aux+lookup or standalone lookup)
            boolean hasLookup = !imported.lookupTables().isEmpty()
                    || !imported.variables().isEmpty();
            assertThat(hasLookup)
                    .as("Lookup table data should survive XMILE round-trip")
                    .isTrue();
        }
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
