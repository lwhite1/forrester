package systems.courant.forrester.io;

import systems.courant.forrester.model.Constant;
import systems.courant.forrester.model.Flow;
import systems.courant.forrester.model.Model;
import systems.courant.forrester.model.Module;
import systems.courant.forrester.model.Stock;
import systems.courant.forrester.model.Variable;
import systems.courant.forrester.measure.Quantity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static systems.courant.forrester.measure.Units.MINUTE;
import static systems.courant.forrester.measure.Units.THING;

class ModelReportTest {

    private Model model;

    @BeforeEach
    void setUp() {
        model = new Model("TestModel");
    }

    @Nested
    @DisplayName("Basic model structure")
    class BasicStructure {

        @Test
        void shouldIncludeModelName() {
            String report = ModelReport.create(model);

            assertThat(report).contains("Model: TestModel");
        }

        @Test
        void shouldListStockWithNoFlows() {
            Stock stock = new Stock("Water", 100, THING);
            model.addStock(stock);

            String report = ModelReport.create(model);

            assertThat(report).contains("Stock: Water");
            assertThat(report).contains("No Inflows");
            assertThat(report).contains("No Outflows");
        }

        @Test
        void shouldListStockWithInflows() {
            Stock stock = new Stock("Tank", 50, THING);
            Flow inflow = Flow.create("Fill", MINUTE, () -> new Quantity(5.0, THING));
            stock.addInflow(inflow);
            model.addStock(stock);
            model.addFlow(inflow);

            String report = ModelReport.create(model);

            assertThat(report).contains("Stock: Tank");
            assertThat(report).contains("Inflows:");
            assertThat(report).contains("Fill");
            assertThat(report).contains("No Outflows");
        }

        @Test
        void shouldListStockWithOutflows() {
            Stock stock = new Stock("Tank", 50, THING);
            Flow outflow = Flow.create("Drain", MINUTE, () -> new Quantity(2.0, THING));
            stock.addOutflow(outflow);
            model.addStock(stock);
            model.addFlow(outflow);

            String report = ModelReport.create(model);

            assertThat(report).contains("Stock: Tank");
            assertThat(report).contains("No Inflows");
            assertThat(report).contains("Outflows:");
            assertThat(report).contains("Drain");
        }

        @Test
        void shouldListStockWithBothInflowsAndOutflows() {
            Stock stock = new Stock("Tank", 50, THING);
            Flow inflow = Flow.create("Fill", MINUTE, () -> new Quantity(5.0, THING));
            Flow outflow = Flow.create("Drain", MINUTE, () -> new Quantity(2.0, THING));
            stock.addInflow(inflow);
            stock.addOutflow(outflow);
            model.addStock(stock);
            model.addFlow(inflow);
            model.addFlow(outflow);

            String report = ModelReport.create(model);

            assertThat(report).contains("Inflows:");
            assertThat(report).contains("Fill");
            assertThat(report).contains("Outflows:");
            assertThat(report).contains("Drain");
        }

        @Test
        void shouldListConstants() {
            Constant c = new Constant("Gravity", THING, 9.81);
            model.addConstant(c);

            String report = ModelReport.create(model);

            assertThat(report).contains("Constants:");
            assertThat(report).contains("Gravity");
        }

        @Test
        void shouldListVariables() {
            Variable v = new Variable("Rate", THING, () -> 0.5);
            model.addVariable(v);

            String report = ModelReport.create(model);

            assertThat(report).contains("Variables:");
            assertThat(report).contains("Rate");
        }
    }

    @Nested
    @DisplayName("Module hierarchy")
    class ModuleHierarchy {

        @Test
        void shouldListModuleWithContents() {
            Module module = new Module("Engine");
            Stock s = new Stock("Fuel", 10, THING);
            module.addStock(s);
            Constant c = new Constant("Efficiency", THING, 0.9);
            module.addConstant(c);
            model.addModulePreserved(module);

            String report = ModelReport.create(model);

            assertThat(report).contains("Modules:");
            assertThat(report).contains("Module: Engine");
            assertThat(report).contains("Stock: Fuel");
            assertThat(report).contains("Constant: Efficiency");
        }

        @Test
        void shouldListNestedSubModules() {
            Module parent = new Module("Parent");
            Module child = new Module("Child");
            Stock s = new Stock("Item", 5, THING);
            child.addStock(s);
            parent.addSubModule(child);
            model.addModulePreserved(parent);

            String report = ModelReport.create(model);

            assertThat(report).contains("Module: Parent");
            assertThat(report).contains("Module: Child");
            assertThat(report).contains("Stock: Item");
        }

        @Test
        void shouldDetectModuleCycle() {
            // Create a module that references itself via submodule with same name
            Module module = new Module("Loop");
            Module duplicate = new Module("Loop");
            module.addSubModule(duplicate);
            model.addModulePreserved(module);

            String report = ModelReport.create(model);

            assertThat(report).contains("cycle detected, skipping");
        }
    }

    @Nested
    @DisplayName("Empty model")
    class EmptyModel {

        @Test
        void shouldProduceReportForEmptyModel() {
            String report = ModelReport.create(model);

            assertThat(report)
                    .contains("Model: TestModel")
                    .contains("Stocks:")
                    .contains("Constants:")
                    .contains("Variables:");
            assertThat(report).doesNotContain("Modules:");
        }
    }
}
