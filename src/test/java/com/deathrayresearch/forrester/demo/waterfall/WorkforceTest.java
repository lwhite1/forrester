package com.deathrayresearch.forrester.demo.waterfall;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.time.TimeUnits;
import com.deathrayresearch.forrester.model.Model;
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
}
