package com.deathrayresearch.forrester.demo.waterfall;

import com.deathrayresearch.forrester.measure.units.item.ItemUnits;
import com.deathrayresearch.forrester.model.Variable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StaffAllocationTest {

    @Test
    void shouldSplitResourcesBetweenDevAndQA() {
        Variable totalWorkforce = new Variable("Total Workforce", ItemUnits.PEOPLE, () -> 20.0);
        Variable trainingOverhead = new Variable("Training Overhead", ItemUnits.PEOPLE, () -> 2.0);

        StaffAllocation staffAllocation = new StaffAllocation(totalWorkforce, trainingOverhead);

        double production = staffAllocation.getDailyResourcesForProduction().getValue();
        double qa = staffAllocation.getDailyResourcesForQA().getValue();

        assertTrue(production > 0, "Production resources should be positive");
        assertTrue(qa > 0, "QA resources should be positive");
        // Production should be larger than QA (QA planned fraction is 15%)
        assertTrue(production > qa,
                "Production resources should exceed QA resources");
    }

    @Test
    void shouldDeductTrainingOverhead() {
        Variable totalWorkforce = new Variable("Total Workforce", ItemUnits.PEOPLE, () -> 10.0);
        Variable noOverhead = new Variable("Training Overhead", ItemUnits.PEOPLE, () -> 0.0);
        Variable someOverhead = new Variable("Training Overhead", ItemUnits.PEOPLE, () -> 3.0);

        StaffAllocation noOverheadAllocation = new StaffAllocation(totalWorkforce, noOverhead);
        StaffAllocation withOverheadAllocation = new StaffAllocation(totalWorkforce, someOverhead);

        double noOverheadProduction = noOverheadAllocation.getDailyResourcesForProduction().getValue();
        double withOverheadProduction = withOverheadAllocation.getDailyResourcesForProduction().getValue();

        assertTrue(noOverheadProduction > withOverheadProduction,
                "Production resources should decrease when training overhead increases");
    }
}
