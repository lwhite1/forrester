package systems.courant.sd.integration;

import systems.courant.sd.Simulation;
import systems.courant.sd.model.Stock;
import systems.courant.sd.model.Variable;
import systems.courant.sd.model.compile.CompiledModel;
import systems.courant.sd.model.compile.ModelCompiler;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModelDefinitionBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("TimeSeries integration (#481)")
class TimeSeriesIntegrationTest {

    @Test
    @DisplayName("should drive a stock with an exogenous time-series input")
    void shouldDriveStockWithTimeSeries() {
        // External data: orders arrive at rate [10, 20, 30, 20, 10] over time 0-4
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("ExogenousInput")
                .defaultSimulation("Day", 4, "Day")
                .timeSeries("order_rate",
                        new double[]{0, 1, 2, 3, 4},
                        new double[]{10, 20, 30, 20, 10},
                        "Thing/Day")
                .stock("Backlog", 0.0, "Thing")
                .flow(new FlowDef("orders_in", "order_rate", "Day", null, "Backlog"))
                .build();

        CompiledModel compiled = new ModelCompiler().compile(def);
        Simulation sim = compiled.createSimulation();
        sim.execute();

        // After simulation, check the stock value
        Stock backlog = compiled.getModel().getStocks().stream()
                .filter(s -> s.getName().equals("Backlog"))
                .findFirst().orElseThrow();

        // With dt=1, 5 steps (0→4): rate=10+20+30+20+10 = 90
        assertThat(backlog.getValue()).isCloseTo(90.0, within(1.0));
    }

    @Test
    @DisplayName("should allow time-series variable to be referenced in equations")
    void shouldAllowTimeSeriesInEquations() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("DerivedFromTimeSeries")
                .defaultSimulation("Day", 3, "Day")
                .timeSeries("temperature",
                        new double[]{0, 1, 2, 3},
                        new double[]{20, 25, 30, 28},
                        null)
                .variable("doubled_temp", "temperature * 2", "Dmnl")
                .build();

        CompiledModel compiled = new ModelCompiler().compile(def);
        Simulation sim = compiled.createSimulation();
        sim.execute();

        // After simulation at time=3, temperature=28, doubled_temp=56
        Variable doubled = compiled.getModel().getVariable("doubled_temp").orElseThrow();
        assertThat(doubled.getValue()).isCloseTo(56.0, within(1.0));
    }
}
