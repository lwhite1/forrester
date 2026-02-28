package com.deathrayresearch.forrester.demo.waterfall;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.time.TimeUnits;
import com.deathrayresearch.forrester.model.Model;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WaterfallIntegrationTest {

    @Test
    void shouldRunFullWaterfallModel() {
        WaterfallSoftwareDevelopmentDemo demo = new WaterfallSoftwareDevelopmentDemo();
        Model model = demo.getModel();

        Simulation sim = new Simulation(model, TimeUnits.DAY,
                new Quantity(1, TimeUnits.YEAR));
        sim.execute();

        // Verify tasks were completed
        double tasksDeveloped = model.getStocks().stream()
                .filter(s -> s.getName().equals("Tasks Developed"))
                .findFirst()
                .orElseThrow()
                .getValue();
        assertTrue(tasksDeveloped > 0, "Tasks should have been completed");

        // Verify workforce grew from initial 6
        double newlyHired = model.getStocks().stream()
                .filter(s -> s.getName().equals("Newly hired"))
                .findFirst()
                .orElseThrow()
                .getValue();
        double experienced = model.getStocks().stream()
                .filter(s -> s.getName().equals("Experienced workers"))
                .findFirst()
                .orElseThrow()
                .getValue();
        double totalWorkforce = newlyHired + experienced;
        assertTrue(totalWorkforce > 6, "Workforce should have grown from initial 6");

        // Verify defects were generated and discovered
        double fixedDefects = model.getStocks().stream()
                .filter(s -> s.getName().equals("Fixed defects"))
                .findFirst()
                .orElseThrow()
                .getValue();
        assertTrue(fixedDefects > 0, "Defects should have been found and fixed");
    }

    @Test
    void shouldHaveAllModuleStocksInModel() {
        WaterfallSoftwareDevelopmentDemo demo = new WaterfallSoftwareDevelopmentDemo();
        Model model = demo.getModel();

        // 4 modules should be registered
        assertTrue(model.getModules().size() == 4, "Model should have 4 modules");

        // All module stocks should be accessible from model.getStocks()
        assertTrue(model.getStockNames().contains("Newly hired"),
                "Model should contain Workforce stocks");
        assertTrue(model.getStockNames().contains("Tasks Developed"),
                "Model should contain Development stocks");
        assertTrue(model.getStockNames().contains("Latent defects"),
                "Model should contain TestAndRework stocks");
        assertTrue(model.getStockNames().contains("Cumulative Person-Days Expended"),
                "Model should contain StaffAllocation stocks");
    }

    @Test
    void shouldHaveAllModuleVariablesInModel() {
        WaterfallSoftwareDevelopmentDemo demo = new WaterfallSoftwareDevelopmentDemo();
        Model model = demo.getModel();

        assertTrue(model.getVariable("Total Workforce") != null,
                "Model should contain Workforce variable");
        assertTrue(model.getVariable("Daily resources for software production") != null,
                "Model should contain StaffAllocation variable");
        assertTrue(model.getVariable("Potential productivity") != null,
                "Model should contain Development variable");
    }
}
