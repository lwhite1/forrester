package systems.courant.sd.demo.waterfall;

import systems.courant.sd.Simulation;
import systems.courant.sd.event.EventHandler;
import systems.courant.sd.event.TimeStepEvent;
import systems.courant.sd.measure.Quantity;
import systems.courant.sd.measure.units.dimensionless.DimensionlessUnits;
import systems.courant.sd.measure.units.item.ItemUnits;
import systems.courant.sd.measure.units.time.TimeUnits;
import systems.courant.sd.model.Flow;
import systems.courant.sd.model.Model;
import systems.courant.sd.model.Module;
import systems.courant.sd.model.Stock;
import systems.courant.sd.model.Variable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
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

        Stock tasksCompleted = sp.getModule().getStock("Tasks Completed").orElseThrow();
        Stock tasksRemaining = sp.getModule().getStock("Tasks Remaining").orElseThrow();

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

        Stock undiscoveredRework = sp.getModule().getStock("Undiscovered Rework").orElseThrow();
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

        Stock reworkToDo = sp.getModule().getStock("Rework to Do").orElseThrow();
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
        Variable discoveryFrac = sp.getModule().getVariable("Rework Discovery Fraction").orElseThrow();
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

        Variable multiplier = sp.getModule().getVariable("Integration Effort Multiplier").orElseThrow();
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

        Stock tasksRemaining = sp.getModule().getStock("Tasks Remaining").orElseThrow();
        Stock tasksCompleted = sp.getModule().getStock("Tasks Completed").orElseThrow();

        // With 200 tasks and reasonable staffing for a year, project should be mostly done
        assertTrue(tasksRemaining.getValue() < 10,
                "Most tasks should be done after a year: remaining=" + tasksRemaining.getValue());
        assertTrue(tasksCompleted.getValue() > 150,
                "Most tasks should be correctly completed: completed=" + tasksCompleted.getValue());
    }

    @Test
    void shouldConserveTasksInDevelopmentSplit() {
        SoftwareProduction sp = createModule(10, 2, 0.8, 500);
        Module mod = sp.getModule();

        Model model = new Model("Conservation Test");
        model.addModule(mod);

        // Find the three development split flows
        Flow development = findFlow(model, "Development");
        Flow correctDev = findFlow(model, "Correct Development");
        Flow errorInjection = findFlow(model, "Error Injection");

        AtomicBoolean violated = new AtomicBoolean(false);
        Simulation sim = new Simulation(model, TimeUnits.DAY,
                new Quantity(100, TimeUnits.DAY));
        sim.addEventHandler(new EventHandler() {
            @Override
            public void handleTimeStepEvent(TimeStepEvent event) {
                int step = (int) event.getStep();
                double total = development.getHistoryAtTimeStep(step);
                double correct = correctDev.getHistoryAtTimeStep(step);
                double errors = errorInjection.getHistoryAtTimeStep(step);
                if (total > 0 && Math.abs((correct + errors) - total) > 1e-9) {
                    violated.set(true);
                }
            }
        });
        sim.execute();

        assertThat(violated.get()).as("Development split should conserve tasks at every step").isFalse();
    }

    @Test
    void shouldConserveTasksInReworkSplit() {
        SoftwareProduction sp = createModule(10, 2, 0.8, 500);
        Module mod = sp.getModule();

        Model model = new Model("Rework Conservation Test");
        model.addModule(mod);

        Flow rework = findFlow(model, "Rework");
        Flow correctRework = findFlow(model, "Correct Rework");
        Flow reworkErrors = findFlow(model, "Rework Errors");

        AtomicBoolean violated = new AtomicBoolean(false);
        Simulation sim = new Simulation(model, TimeUnits.DAY,
                new Quantity(100, TimeUnits.DAY));
        sim.addEventHandler(new EventHandler() {
            @Override
            public void handleTimeStepEvent(TimeStepEvent event) {
                int step = (int) event.getStep();
                double total = rework.getHistoryAtTimeStep(step);
                double correct = correctRework.getHistoryAtTimeStep(step);
                double errors = reworkErrors.getHistoryAtTimeStep(step);
                if (total > 0 && Math.abs((correct + errors) - total) > 1e-9) {
                    violated.set(true);
                }
            }
        });
        sim.execute();

        assertThat(violated.get()).as("Rework split should conserve tasks at every step").isFalse();
    }

    private static Flow findFlow(Model model, String name) {
        return model.getFlows().stream()
                .filter(f -> f.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Flow '" + name + "' not found"));
    }

    @Test
    @DisplayName("Split flows should conserve tasks regardless of evaluation order (#465)")
    void shouldConserveTasksRegardlessOfEvaluationOrder() {
        // Evaluate sibling flows BEFORE the parent flow to verify no order dependency.
        // Before #465, correctDevelopment/errorInjection read from a cache populated by
        // developmentOutflow — evaluating them first would read stale (zero) values.
        SoftwareProduction sp = createModule(10, 2, 0.8, 500);
        Module mod = sp.getModule();

        Flow development = mod.getFlows().stream()
                .filter(f -> f.getName().equals("Development")).findFirst().orElseThrow();
        Flow correctDev = mod.getFlows().stream()
                .filter(f -> f.getName().equals("Correct Development")).findFirst().orElseThrow();
        Flow errorInjection = mod.getFlows().stream()
                .filter(f -> f.getName().equals("Error Injection")).findFirst().orElseThrow();
        Flow rework = mod.getFlows().stream()
                .filter(f -> f.getName().equals("Rework")).findFirst().orElseThrow();
        Flow correctRework = mod.getFlows().stream()
                .filter(f -> f.getName().equals("Correct Rework")).findFirst().orElseThrow();
        Flow reworkErrors = mod.getFlows().stream()
                .filter(f -> f.getName().equals("Rework Errors")).findFirst().orElseThrow();

        // Evaluate siblings BEFORE their parent — this is the reverse of the old assumed order
        Quantity correctDevQ = correctDev.flowPerTimeUnit(TimeUnits.DAY);
        Quantity errorInjQ = errorInjection.flowPerTimeUnit(TimeUnits.DAY);
        Quantity devQ = development.flowPerTimeUnit(TimeUnits.DAY);

        double devTotal = devQ.getValue();
        double splitSum = correctDevQ.getValue() + errorInjQ.getValue();
        assertThat(Math.abs(splitSum - devTotal))
                .as("Development split must conserve: total=%f, correct+error=%f", devTotal, splitSum)
                .isLessThan(1e-9);

        // Same check for rework split
        Quantity correctRwQ = correctRework.flowPerTimeUnit(TimeUnits.DAY);
        Quantity rwErrorsQ = reworkErrors.flowPerTimeUnit(TimeUnits.DAY);
        Quantity rwQ = rework.flowPerTimeUnit(TimeUnits.DAY);

        double rwTotal = rwQ.getValue();
        double rwSplitSum = correctRwQ.getValue() + rwErrorsQ.getValue();
        assertThat(Math.abs(rwSplitSum - rwTotal))
                .as("Rework split must conserve: total=%f, correct+error=%f", rwTotal, rwSplitSum)
                .isLessThan(1e-9);
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

        double expRework = expTeam.getModule().getStock("Undiscovered Rework").orElseThrow().getValue()
                + expTeam.getModule().getStock("Rework to Do").orElseThrow().getValue();
        double newRework = newTeam.getModule().getStock("Undiscovered Rework").orElseThrow().getValue()
                + newTeam.getModule().getStock("Rework to Do").orElseThrow().getValue();

        assertTrue(newRework > expRework,
                "Inexperienced team should generate more rework: exp=" + expRework
                        + " new=" + newRework);
    }
}
