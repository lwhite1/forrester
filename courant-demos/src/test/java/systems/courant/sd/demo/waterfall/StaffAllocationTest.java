package systems.courant.sd.demo.waterfall;

import systems.courant.sd.measure.units.dimensionless.DimensionlessUnits;
import systems.courant.sd.measure.units.item.ItemUnits;
import systems.courant.sd.model.Variable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaffAllocationTest {

    @Test
    void shouldSplitResourcesBetweenDevAndQA() {
        Variable totalWorkforce = new Variable("Total Workforce", ItemUnits.PEOPLE, () -> 20.0);
        Variable trainingOverhead = new Variable("Training Overhead", ItemUnits.PEOPLE, () -> 2.0);
        Variable commOverhead = new Variable("Comm Overhead",
                DimensionlessUnits.DIMENSIONLESS, () -> 0.5);

        StaffAllocation staffAllocation =
                new StaffAllocation(totalWorkforce, trainingOverhead, commOverhead);

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
        Variable zeroComm = new Variable("Comm Overhead",
                DimensionlessUnits.DIMENSIONLESS, () -> 0.0);

        StaffAllocation noOverheadAllocation =
                new StaffAllocation(totalWorkforce, noOverhead, zeroComm);
        StaffAllocation withOverheadAllocation =
                new StaffAllocation(totalWorkforce, someOverhead, zeroComm);

        double noOverheadProduction =
                noOverheadAllocation.getDailyResourcesForProduction().getValue();
        double withOverheadProduction =
                withOverheadAllocation.getDailyResourcesForProduction().getValue();

        assertTrue(noOverheadProduction > withOverheadProduction,
                "Production resources should decrease when training overhead increases");
    }

    @Test
    void shouldDeductCommunicationOverhead() {
        Variable totalWorkforce = new Variable("Total Workforce", ItemUnits.PEOPLE, () -> 20.0);
        Variable trainingOverhead = new Variable("Training Overhead", ItemUnits.PEOPLE, () -> 1.0);
        Variable noComm = new Variable("No Comm",
                DimensionlessUnits.DIMENSIONLESS, () -> 0.0);
        Variable highComm = new Variable("High Comm",
                DimensionlessUnits.DIMENSIONLESS, () -> 3.0);

        StaffAllocation noCommAllocation =
                new StaffAllocation(totalWorkforce, trainingOverhead, noComm);
        StaffAllocation highCommAllocation =
                new StaffAllocation(totalWorkforce, trainingOverhead, highComm);

        double noCommProduction =
                noCommAllocation.getDailyResourcesForProduction().getValue();
        double highCommProduction =
                highCommAllocation.getDailyResourcesForProduction().getValue();

        assertTrue(noCommProduction > highCommProduction,
                "Production resources should decrease when communication overhead increases");
    }

    @Test
    void shouldComputeQAFromNetAvailableNotGrossStaffing() {
        // Team of 28, overhead of ~4.5 from training + communication
        Variable totalWorkforce = new Variable("Total Workforce", ItemUnits.PEOPLE, () -> 28.0);
        Variable trainingOverhead = new Variable("Training", ItemUnits.PEOPLE, () -> 2.0);
        Variable commOverhead = new Variable("Comm", DimensionlessUnits.DIMENSIONLESS, () -> 2.5);

        double plannedQAFraction = 0.15;
        double overheadLoss = 0.10;

        StaffAllocation alloc = new StaffAllocation(
                totalWorkforce, trainingOverhead, commOverhead,
                plannedQAFraction, overheadLoss);

        double qa = alloc.getDailyResourcesForQA().getValue();
        double production = alloc.getDailyResourcesForProduction().getValue();
        double totalUsed = qa + production;

        // QA fraction of net resources should not exceed planned fraction
        double actualQAFraction = qa / totalUsed;
        assertThat(actualQAFraction).isLessThanOrEqualTo(plannedQAFraction + 0.001);
    }
}
