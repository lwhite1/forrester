package com.deathrayresearch.forrester.demo.waterfall;

import com.deathrayresearch.forrester.Simulation;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.time.TimeUnits;
import com.deathrayresearch.forrester.model.Model;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TestAndReworkTest {

    @Test
    void shouldGenerateAndDiscoverDefects() {
        TestAndRework testAndRework = new TestAndRework();

        Model model = new Model("Defect Test");
        model.addModule(testAndRework.getModule());

        Simulation sim = new Simulation(model, TimeUnits.DAY,
                new Quantity(60, TimeUnits.DAY));
        sim.execute();

        double latent = testAndRework.getModule().getStock("Latent defects").getValue();
        double known = testAndRework.getModule().getStock("Known defects").getValue();
        double fixed = testAndRework.getModule().getStock("Fixed defects").getValue();

        assertTrue(latent > 0, "Latent defects should accumulate");
        assertTrue(known > 0, "Some defects should be discovered");
        assertTrue(fixed > 0, "Some defects should be fixed");
    }

    @Test
    void shouldFlowDefectsThroughPipeline() {
        TestAndRework testAndRework = new TestAndRework();

        Model model = new Model("Pipeline Test");
        model.addModule(testAndRework.getModule());

        Simulation sim = new Simulation(model, TimeUnits.DAY,
                new Quantity(180, TimeUnits.DAY));
        sim.execute();

        double fixed = testAndRework.getModule().getStock("Fixed defects").getValue();

        // After 180 days of generating 10 errors/day, a significant number should be fixed
        assertTrue(fixed > 100, "Fixed defects should be substantial after 180 days");
    }
}
