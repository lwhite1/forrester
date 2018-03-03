package com.deathrayresearch.forrester.largemodels.waterfall;


import com.deathrayresearch.forrester.largemodels.waterfall.units.Errors;
import com.deathrayresearch.forrester.largemodels.waterfall.units.ErrorsPerTask;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.model.Formula;
import com.deathrayresearch.forrester.model.Module;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Variable;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.flows.FlowPerDay;

import static com.deathrayresearch.forrester.largemodels.waterfall.WaterfallSoftwareDevelopment.TESTING_AND_REWORK;

/**
 */
class TestAndRework {

    static Module getTestAndReworkSubSystem() {

        Module module = new Module(TESTING_AND_REWORK);

        Stock latentDefects = new Stock("Latent defects", 0.0, Errors.getInstance());
        Stock knownDefects = new Stock("Known defects", 0.0, Errors.getInstance());
        Stock fixedDefects = new Stock("Fixed defects", 0.0, Errors.getInstance());

        Variable nominalErrorsCommittedPerTask =
                new Variable("Nominal errors committed per task",
                        ErrorsPerTask.getInstance(),
                        new Formula() {
                            @Override
                            public double getCurrentValue() {
                                return 0;
                            }
                        });



        Flow errorGenerationFlow = new FlowPerDay("New Errors") {

            @Override
            protected Quantity quantityPerDay() {
                return new Quantity(10, Errors.getInstance());
            }
        };

        Flow errorDiscoveryFlow = new FlowPerDay("Errors discovered") {
            @Override
            protected Quantity quantityPerDay() {
                return new Quantity(3, Errors.getInstance());
            }
        };

        Flow errorFixedFlow = new FlowPerDay("Errors fixed") {
            @Override
            protected Quantity quantityPerDay() {
                return new Quantity(1, Errors.getInstance());
            }
        };

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
