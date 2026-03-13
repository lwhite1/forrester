package systems.courant.sd.app.models;

import systems.courant.sd.Simulation;
import systems.courant.sd.io.json.ModelDefinitionSerializer;
import systems.courant.sd.model.Stock;
import systems.courant.sd.model.compile.CompiledModel;
import systems.courant.sd.model.compile.ModelCompiler;
import systems.courant.sd.model.def.ModelDefinition;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.Assumptions.assumeThat;

@DisplayName("Canonical example models")
class ExampleModelGeneratorTest {

    private final ModelCompiler compiler = new ModelCompiler();
    private final ModelDefinitionSerializer serializer = new ModelDefinitionSerializer();

    // -------------------------------------------------------------------
    // Model metadata: id, category, description, difficulty, tags
    // -------------------------------------------------------------------

    record ModelEntry(
            String id,
            String category,
            String description,
            String difficulty,
            List<String> tags,
            ModelDefinition definition
    ) {
        @Override
        public String toString() {
            return id;
        }
    }

    static Stream<ModelEntry> allModels() {
        return Stream.of(
                new ModelEntry("exponential-growth", "introductory",
                        "Population growing with constant birth and death rates",
                        "introductory", List.of("positive-feedback", "exponential"),
                        CanonicalModels.exponentialGrowth()),
                new ModelEntry("bathtub", "introductory",
                        "Water drains at a fixed rate; inflow begins after a delay",
                        "introductory", List.of("stock-and-flow", "step-function"),
                        CanonicalModels.bathtub()),
                new ModelEntry("goal-seeking", "introductory",
                        "Inventory adjusts toward a goal via negative feedback",
                        "introductory", List.of("negative-feedback", "goal-seeking"),
                        CanonicalModels.goalSeeking()),
                new ModelEntry("coffee-cooling", "introductory",
                        "Coffee temperature decays toward room temperature",
                        "introductory", List.of("negative-feedback", "exponential-decay"),
                        CanonicalModels.coffeeCooling()),
                new ModelEntry("sir-epidemic", "epidemiology",
                        "Classic Susceptible-Infectious-Recovered compartmental model",
                        "intermediate", List.of("feedback", "nonlinear"),
                        CanonicalModels.sirEpidemic()),
                new ModelEntry("predator-prey", "ecology",
                        "Lotka-Volterra model of predator-prey oscillation",
                        "intermediate", List.of("oscillation", "nonlinear"),
                        CanonicalModels.predatorPrey()),
                new ModelEntry("s-shaped-growth", "population",
                        "Logistic population growth limited by carrying capacity",
                        "introductory", List.of("s-curve", "carrying-capacity"),
                        CanonicalModels.sShapedGrowth()),
                new ModelEntry("inventory-oscillation", "supply-chain",
                        "Car dealership inventory with perception and delivery delays",
                        "advanced", List.of("delay", "oscillation", "supply-chain"),
                        CanonicalModels.inventoryOscillation())
        );
    }

    // -------------------------------------------------------------------
    // Always-run tests
    // -------------------------------------------------------------------

    @ParameterizedTest(name = "{0}")
    @MethodSource("allModels")
    @DisplayName("compiles and simulates with finite stock values")
    void shouldCompileAndSimulate(ModelEntry entry) {
        CompiledModel compiled = compiler.compile(entry.definition());
        Simulation sim = compiled.createSimulation();
        sim.execute();

        for (Stock stock : compiled.getModel().getStocks()) {
            assertThat(stock.getValue())
                    .as("Stock '%s' should be finite", stock.getName())
                    .isFinite();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allModels")
    @DisplayName("round-trips through JSON with identical simulation results")
    void shouldRoundTrip(ModelEntry entry) {
        // Original
        CompiledModel compiled1 = compiler.compile(entry.definition());
        Simulation sim1 = compiled1.createSimulation();
        sim1.execute();

        // Serialize → deserialize
        String json = serializer.toJson(entry.definition());
        ModelDefinition deserialized = serializer.fromJson(json);

        // Re-compile and simulate
        CompiledModel compiled2 = compiler.compile(deserialized);
        Simulation sim2 = compiled2.createSimulation();
        sim2.execute();

        // Compare stock values
        List<Stock> stocks1 = compiled1.getModel().getStocks();
        List<Stock> stocks2 = compiled2.getModel().getStocks();
        assertThat(stocks2).hasSameSizeAs(stocks1);
        for (Stock s1 : stocks1) {
            Stock s2 = findStock(stocks2, s1.getName());
            assertThat(s2.getValue())
                    .as("Stock '%s' values should match after round-trip", s1.getName())
                    .isCloseTo(s1.getValue(), within(0.001));
        }
    }

    // -------------------------------------------------------------------
    // On-demand generation (mvn test -Dgenerate.models=true)
    // -------------------------------------------------------------------

    @Test
    @DisplayName("generate model JSON files")
    void generateModelFiles() throws IOException {
        assumeThat(Boolean.getBoolean("generate.models"))
                .as("Run with -Dgenerate.models=true to generate JSON files")
                .isTrue();

        Path resourcesDir = Path.of("src/main/resources/models");
        Files.createDirectories(resourcesDir);

        allModels().forEach(entry -> {
            try {
                ModelDefinition withLayout = CanonicalModels.addAutoLayout(entry.definition());
                Path categoryDir = resourcesDir.resolve(entry.category());
                Files.createDirectories(categoryDir);
                Path file = categoryDir.resolve(entry.id() + ".json");
                serializer.toFile(withLayout, file);
            } catch (IOException e) {
                throw new RuntimeException("Failed to write " + entry.id(), e);
            }
        });
    }

    @Test
    @DisplayName("generate catalog.json")
    void generateCatalog() throws IOException {
        assumeThat(Boolean.getBoolean("generate.models"))
                .as("Run with -Dgenerate.models=true to generate catalog")
                .isTrue();

        Path resourcesDir = Path.of("src/main/resources/models");
        Files.createDirectories(resourcesDir);

        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        ObjectNode root = mapper.createObjectNode();
        ArrayNode modelsArray = root.putArray("models");

        allModels().forEach(entry -> {
            ModelDefinition def = entry.definition();
            ObjectNode model = modelsArray.addObject();
            model.put("id", entry.id());
            model.put("name", def.name());
            model.put("description", entry.description());
            model.put("category", entry.category());
            model.put("difficulty", entry.difficulty());
            model.put("path", entry.category() + "/" + entry.id() + ".json");

            ArrayNode tagsArray = model.putArray("tags");
            entry.tags().forEach(tagsArray::add);

            ObjectNode elements = model.putObject("elements");
            elements.put("stocks", def.stocks().size());
            elements.put("flows", def.flows().size());
            elements.put("variables", def.variables().size());
            elements.put("parameters", def.parameterNames().size());
        });

        mapper.writeValue(resourcesDir.resolve("catalog.json").toFile(), root);
    }

    // -------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------

    private Stock findStock(List<Stock> stocks, String name) {
        return stocks.stream()
                .filter(s -> s.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Stock not found: " + name));
    }
}
