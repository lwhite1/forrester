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
                new Quantity(200, TimeUnits.DAY));
        sim.execute();

        // Verify tasks were completed
        double tasksCompleted = model.getStocks().stream()
                .filter(s -> s.getName().equals("Tasks Completed"))
                .findFirst()
                .orElseThrow()
                .getValue();
        assertTrue(tasksCompleted > 0, "Tasks should have been completed");

        // Verify tasks remaining decreased
        double tasksRemaining = model.getStocks().stream()
                .filter(s -> s.getName().equals("Tasks Remaining"))
                .findFirst()
                .orElseThrow()
                .getValue();
        assertTrue(tasksRemaining < 500, "Tasks remaining should decrease from initial 500");

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

        // Verify significant tasks completed in 200 days (rework clears ~day 175)
        assertTrue(tasksCompleted > 100,
                "Significant tasks should be completed in 200 days");
    }

    @Test
    void shouldHaveAllModuleStocksInModel() {
        WaterfallSoftwareDevelopmentDemo demo = new WaterfallSoftwareDevelopmentDemo();
        Model model = demo.getModel();

        // 3 modules should be registered
        assertTrue(model.getModules().size() == 3, "Model should have 3 modules");

        // Workforce stocks
        assertTrue(model.getStockNames().contains("Newly hired"),
                "Model should contain Workforce stocks");
        assertTrue(model.getStockNames().contains("Experienced workers"),
                "Model should contain Workforce stocks");

        // SoftwareProduction stocks
        assertTrue(model.getStockNames().contains("Tasks Remaining"),
                "Model should contain SoftwareProduction stocks");
        assertTrue(model.getStockNames().contains("Tasks Completed"),
                "Model should contain SoftwareProduction stocks");
        assertTrue(model.getStockNames().contains("Undiscovered Rework"),
                "Model should contain SoftwareProduction stocks");
        assertTrue(model.getStockNames().contains("Rework to Do"),
                "Model should contain SoftwareProduction stocks");
    }

    @Test
    void shouldHaveAllModuleVariablesInModel() {
        WaterfallSoftwareDevelopmentDemo demo = new WaterfallSoftwareDevelopmentDemo();
        Model model = demo.getModel();

        assertTrue(model.getVariable("Total Workforce").isPresent(),
                "Model should contain Workforce variable");
        assertTrue(model.getVariable("Communication overhead").isPresent(),
                "Model should contain Workforce communication overhead variable");
        assertTrue(model.getVariable("Daily resources for software production").isPresent(),
                "Model should contain StaffAllocation variable");
        assertTrue(model.getVariable("Fraction Correct and Complete").isPresent(),
                "Model should contain SoftwareProduction FCC variable");
        assertTrue(model.getVariable("Development Productivity").isPresent(),
                "Model should contain SoftwareProduction productivity variable");
    }
}
