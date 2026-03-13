package systems.courant.sd.demo.waterfall;

import systems.courant.sd.Simulation;
import systems.courant.sd.measure.Quantity;
import systems.courant.sd.measure.units.time.TimeUnits;
import systems.courant.sd.model.Model;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkforceTest {

    @Test
    void shouldGrowWorkforceThroughHiring() {
        Workforce workforce = new Workforce();

        Model model = new Model("Workforce Test");
        model.addModule(workforce.getModule());

        Simulation sim = new Simulation(model, TimeUnits.DAY,
                new Quantity(180, TimeUnits.DAY));
        sim.execute();

        // Started at 6 (2 new + 4 experienced), should grow toward the need of 30
        assertTrue(workforce.getTotalWorkforce().getValue() > 6,
                "Workforce should grow from initial 6");
    }

    @Test
    void shouldTrackTrainingOverhead() {
        Workforce workforce = new Workforce();

        Model model = new Model("Training Test");
        model.addModule(workforce.getModule());

        Simulation sim = new Simulation(model, TimeUnits.DAY,
                new Quantity(30, TimeUnits.DAY));
        sim.execute();

        // Training overhead = 0.2 * newlyHired; with hiring happening, overhead should be > 0
        assertTrue(workforce.getDailyTrainingOverhead().getValue() > 0,
                "Training overhead should be positive while there are new hires");
    }

    @Test
    void shouldTrackFractionExperienced() {
        Workforce workforce = new Workforce();

        // Initial: 4 experienced / 6 total = 0.667
        double initialFraction = workforce.getFractionExperienced().getValue();
        assertTrue(initialFraction > 0.5 && initialFraction < 1.0,
                "Initial fraction experienced should be between 0.5 and 1.0");
    }

    @Test
    void shouldDrainExperiencedWorkforceViaResignation() {
        // High resignation rate: avgEmploymentDays = 10 (very short tenure)
        // No hiring (workforceNeed = 0), no new hires to assimilate
        Workforce workforce = new Workforce(0, 10, 0, 8, 16, 0.2, 10, 0.003);

        Model model = new Model("Resignation Test");
        model.addModule(workforce.getModule());

        double initialExperienced = model.getStocks().stream()
                .filter(s -> s.getName().equals("Experienced workers"))
                .findFirst().orElseThrow().getValue();

        Simulation sim = new Simulation(model, TimeUnits.DAY,
                new Quantity(30, TimeUnits.DAY));
        sim.execute();

        double finalExperienced = model.getStocks().stream()
                .filter(s -> s.getName().equals("Experienced workers"))
                .findFirst().orElseThrow().getValue();

        assertTrue(finalExperienced < initialExperienced,
                "Experienced workforce should decrease due to resignation: initial="
                        + initialExperienced + " final=" + finalExperienced);
    }

    @Test
    void shouldComputeCommunicationOverhead() {
        // Team of 6: overhead = 6 * 5 * 0.003 = 0.09
        Workforce workforce = new Workforce();

        double overhead = workforce.getCommunicationOverhead().getValue();
        double expected = 6 * 5 * 0.003;
        assertTrue(Math.abs(overhead - expected) < 0.001,
                "Communication overhead for team of 6 should be ~0.09, got " + overhead);
    }

    @Test
    void shouldIncreaseCommunicationOverheadWithTeamSize() {
        Workforce workforce = new Workforce();

        Model model = new Model("Comm Overhead Test");
        model.addModule(workforce.getModule());

        double initialOverhead = workforce.getCommunicationOverhead().getValue();

        // Run long enough for team to grow significantly
        Simulation sim = new Simulation(model, TimeUnits.DAY,
                new Quantity(180, TimeUnits.DAY));
        sim.execute();

        double laterOverhead = workforce.getCommunicationOverhead().getValue();
        assertTrue(laterOverhead > initialOverhead,
                "Communication overhead should increase as team grows: initial="
                        + initialOverhead + " later=" + laterOverhead);
    }
}
