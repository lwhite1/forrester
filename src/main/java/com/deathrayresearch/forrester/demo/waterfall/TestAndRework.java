package com.deathrayresearch.forrester.demo.waterfall;


import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.Unit;
import com.deathrayresearch.forrester.measure.units.item.ItemUnit;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Formula;
import com.deathrayresearch.forrester.model.Module;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Variable;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.demo.waterfall.WaterfallSoftwareDevelopmentDemo.TESTING_AND_REWORK;

/**
 * Test and rework subsystem for the waterfall software project model.
 *
 * <p>Models the defect lifecycle: errors are generated at a constant rate into a latent
 * defects stock, discovered at 10% per day into known defects, and fixed at 20% per day
 * into a fixed defects stock. The three-stock pipeline (latent → known → fixed) captures
 * the delay between defect injection and resolution that drives rework dynamics.
 */
class TestAndRework {

    private static final Unit ERRORS = new ItemUnit("Error");
    private static final Unit ERRORS_PER_TASK = new ItemUnit("Errors per task");

    static Module getTestAndReworkSubSystem() {

        Module module = new Module(TESTING_AND_REWORK);

        Stock latentDefects = new Stock("Latent defects", 0.0, ERRORS);
        Stock knownDefects = new Stock("Known defects", 0.0, ERRORS);
        Stock fixedDefects = new Stock("Fixed defects", 0.0, ERRORS);

        Variable nominalErrorsCommittedPerTask =
                new Variable("Nominal errors committed per task",
                        ERRORS_PER_TASK,
                        new Formula() {
                            @Override
                            public double getCurrentValue() {
                                return 0;
                            }
                        });



        Flow errorGenerationFlow = Flow.create("New Errors", DAY, () ->
                new Quantity(10, ERRORS));

        Flow errorDiscoveryFlow = Flow.create("Errors discovered", DAY, () -> {
            double discoveryFraction = 0.1;
            return new Quantity(latentDefects.getValue() * discoveryFraction, ERRORS);
        });

        Flow errorFixedFlow = Flow.create("Errors fixed", DAY, () -> {
            double fixFraction = 0.2;
            return new Quantity(knownDefects.getValue() * fixFraction, ERRORS);
        });

        latentDefects.addInflow(errorGenerationFlow);
        latentDefects.addOutflow(errorDiscoveryFlow);

        knownDefects.addInflow(errorDiscoveryFlow);
        knownDefects.addOutflow(errorFixedFlow);

        fixedDefects.addInflow(errorFixedFlow);

        module.addStock(latentDefects);
        module.addStock(knownDefects);
        module.addStock(fixedDefects);

        return module;
    }

}
