package systems.courant.sd.io.vensim;

import systems.courant.sd.Simulation;
import systems.courant.sd.io.json.ModelDefinitionSerializer;
import systems.courant.sd.model.Stock;
import systems.courant.sd.model.compile.CompiledModel;
import systems.courant.sd.model.compile.ModelCompiler;
import systems.courant.sd.model.def.ModelDefinition;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke-tests batch 2 imported models through compile + simulate.
 * Catches NaN propagation, infinite loops, and runtime exceptions.
 */
class Batch2SimulationTest {

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "CholeraDemo",
            "DeradicalizationDemo",
            "EVsLithiumDemo",
            "EnergyTransitionDemo",
            "Flu2RegionsDemo",
            "GlobalizationDemo",
            "HigherEducationDemo",
            "HousingMarketDemo",
            "NewTownsDemo",
            "ProjectManagementDemo"
    })
    void shouldCompileAndSimulate(String modelName) throws IOException {
        String json = loadResource("vensim/batch2/" + modelName + ".json");
        ModelDefinitionSerializer serializer = new ModelDefinitionSerializer();
        ModelDefinition def = serializer.fromJson(json);

        ModelCompiler compiler = new ModelCompiler();
        CompiledModel compiled = compiler.compile(def);

        Simulation sim = compiled.createSimulation();
        sim.execute();

        // Check that ALL stocks have finite values (not NaN/Infinity)
        for (Stock stock : compiled.getModel().getStocks()) {
            assertThat(Double.isFinite(stock.getValue()))
                    .as("Stock '%s' should have a finite value after simulation of %s, but was %s",
                            stock.getName(), modelName, stock.getValue())
                    .isTrue();
        }
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
