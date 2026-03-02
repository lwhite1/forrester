package com.deathrayresearch.forrester.demo.waterfall;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.dimensionless.DimensionlessUnits;
import com.deathrayresearch.forrester.measure.units.item.ItemUnits;
import com.deathrayresearch.forrester.measure.units.time.TimeUnits;
import com.deathrayresearch.forrester.model.Model;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Variable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SoftwareProductionTest {

    private SoftwareProduction createModule(double staffing, double qaStaffing,
                                            double fractionExp, double projectSize) {
        Variable devResources = new Variable("Dev Resources", ItemUnits.PEOPLE, () -> staffing);
        Variable qaResources = new Variable("QA Resources", ItemUnits.PEOPLE, () -> qaStaffing);
        Variable fracExp = new Variable("Fraction Experienced",
                DimensionlessUnits.DIMENSIONLESS, () -> fractionExp);

        return new SoftwareProduction(devResources, qaResources, fracExp,
                projectSize, 0.80, 1.0, 0.5, 0.05, 0.40, 1.5);
    }

    @Test
    void shouldCompleteTasksOverTime() {
        SoftwareProduction sp = createModule(10, 2, 0.8, 500);

        Model model = new Model("Completion Test");
        model.addModule(sp.getModule());

        Simulation sim = new Simulation(model, TimeUnits.DAY,
                new Quantity(100, TimeUnits.DAY));
        sim.execute();

        Stock tasksCompleted = sp.getModule().getStock("Tasks Completed");
        Stock tasksRemaining = sp.getModule().getStock("Tasks Remaining");

        assertTrue(tasksCompleted.getValue() > 0,
                "Tasks completed should be positive after 100 days");
        assertTrue(tasksRemaining.getValue() < 500,
                "Tasks remaining should decrease from initial 500");
    }

    @Test
    void shouldGenerateUndiscoveredRework() {
        // With FCC = 0.80 * experience multiplier, some work will have errors
        SoftwareProduction sp = createModule(10, 2, 0.8, 500);

        Model model = new Model("Rework Generation Test");
        model.addModule(sp.getModule());

        Simulation sim = new Simulation(model, TimeUnits.DAY,
                new Quantity(30, TimeUnits.DAY));
        sim.execute();

        Stock undiscoveredRework = sp.getModule().getStock("Undiscovered Rework");
        assertTrue(undiscoveredRework.getValue() > 0,
                "Undiscovered rework should accumulate when FCC < 1");
    }

    @Test
    void shouldDiscoverReworkOverTime() {
        // Low QA staffing (0.1) so discovered rework accumulates faster than it's fixed
        SoftwareProduction sp = createModule(10, 0.1, 0.8, 500);

        Model model = new Model("Rework Discovery Test");
        model.addModule(sp.getModule());

        // Run long enough for rework to accumulate and be discovered
        Simulation sim = new Simulation(model, TimeUnits.DAY,
                new Quantity(100, TimeUnits.DAY));
        sim.execute();

        Stock reworkToDo = sp.getModule().getStock("Rework to Do");
        assertTrue(reworkToDo.getValue() > 0,
                "Rework to Do should be positive as errors are discovered");
    }

    @Test
    void shouldIncreaseReworkDiscoveryWithCompletion() {
        // With high staffing and small project, completion fraction rises quickly
        // → rework discovery fraction should increase
        SoftwareProduction sp = createModule(20, 5, 1.0, 100);

        Model model = new Model("Discovery Fraction Test");
        model.addModule(sp.getModule());

        Simulation sim = new Simulation(model, TimeUnits.DAY,
                new Quantity(60, TimeUnits.DAY));
        sim.execute();

        // The rework discovery fraction variable should be above the base (0.05)
        Variable discoveryFrac = sp.getModule().getVariable("Rework Discovery Fraction");
        assertTrue(discoveryFrac.getValue() > 0.05,
                "Rework discovery fraction should increase above base as project progresses");
    }

    @Test
    void shouldMakeReworkMoreExpensiveLateInProject() {
        SoftwareProduction sp = createModule(20, 5, 1.0, 100);

        Model model = new Model("Integration Tax Test");
        model.addModule(sp.getModule());

        Simulation sim = new Simulation(model, TimeUnits.DAY,
                new Quantity(60, TimeUnits.DAY));
        sim.execute();

        Variable multiplier = sp.getModule().getVariable("Integration Effort Multiplier");
        assertTrue(multiplier.getValue() > 1.0,
                "Integration effort multiplier should exceed 1.0 as project progresses");
    }

    @Test
    void shouldEventuallyCompleteProject() {
        // With enough staffing and time, all tasks should be completed
        SoftwareProduction sp = createModule(10, 5, 0.8, 200);

        Model model = new Model("Project Completion Test");
        model.addModule(sp.getModule());

        Simulation sim = new Simulation(model, TimeUnits.DAY,
                new Quantity(365, TimeUnits.DAY));
        sim.execute();

        Stock tasksRemaining = sp.getModule().getStock("Tasks Remaining");
        Stock tasksCompleted = sp.getModule().getStock("Tasks Completed");

        // With 200 tasks and reasonable staffing for a year, project should be mostly done
        assertTrue(tasksRemaining.getValue() < 10,
                "Most tasks should be done after a year: remaining=" + tasksRemaining.getValue());
        assertTrue(tasksCompleted.getValue() > 150,
                "Most tasks should be correctly completed: completed=" + tasksCompleted.getValue());
    }

    @Test
    void shouldProduceMoreErrorsWithInexperiencedTeam() {
        SoftwareProduction expTeam = createModule(10, 2, 1.0, 500);
        SoftwareProduction newTeam = createModule(10, 2, 0.0, 500);

        Model expModel = new Model("Exp Team");
        expModel.addModule(expTeam.getModule());
        Simulation expSim = new Simulation(expModel, TimeUnits.DAY,
                new Quantity(60, TimeUnits.DAY));
        expSim.execute();

        Model newModel = new Model("New Team");
        newModel.addModule(newTeam.getModule());
        Simulation newSim = new Simulation(newModel, TimeUnits.DAY,
                new Quantity(60, TimeUnits.DAY));
        newSim.execute();

        double expRework = expTeam.getModule().getStock("Undiscovered Rework").getValue()
                + expTeam.getModule().getStock("Rework to Do").getValue();
        double newRework = newTeam.getModule().getStock("Undiscovered Rework").getValue()
                + newTeam.getModule().getStock("Rework to Do").getValue();

        assertTrue(newRework > expRework,
                "Inexperienced team should generate more rework: exp=" + expRework
                        + " new=" + newRework);
    }
}
