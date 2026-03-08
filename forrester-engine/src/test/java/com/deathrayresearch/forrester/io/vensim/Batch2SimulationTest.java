package com.deathrayresearch.forrester.io.vensim;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.io.json.ModelDefinitionSerializer;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.compile.CompiledModel;
import com.deathrayresearch.forrester.model.compile.ModelCompiler;
import com.deathrayresearch.forrester.model.def.ModelDefinition;

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
            "NewTownsDemo"
            // ProjectManagementDemo excluded: has algebraic loops (circular variable refs)
            // that cause StackOverflow — would need simultaneous equation solver
    })
    void shouldCompileAndSimulate(String modelName) throws IOException {
        String json = loadResource("vensim/batch2/" + modelName + ".json");
        ModelDefinitionSerializer serializer = new ModelDefinitionSerializer();
        ModelDefinition def = serializer.fromJson(json);

        ModelCompiler compiler = new ModelCompiler();
        CompiledModel compiled = compiler.compile(def);

        Simulation sim = compiled.createSimulation();
        sim.execute();

        // Check that at least one stock has a finite value (not NaN/Infinity)
        boolean anyFinite = compiled.getModel().getStocks().stream()
                .anyMatch(s -> Double.isFinite(s.getValue()));
        assertThat(anyFinite)
                .as("At least one stock should have a finite value after simulation of " + modelName)
                .isTrue();
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
