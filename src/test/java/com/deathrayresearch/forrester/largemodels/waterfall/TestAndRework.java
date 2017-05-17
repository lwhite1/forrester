package com.deathrayresearch.forrester.largemodels.waterfall;


import com.deathrayresearch.forrester.largemodels.waterfall.units.Errors;
import com.deathrayresearch.forrester.largemodels.waterfall.units.ErrorsPerTask;
import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Formula;
import com.deathrayresearch.forrester.model.Module;
import com.deathrayresearch.forrester.model.Stock;
import com.deathrayresearch.forrester.model.Variable;
import com.deathrayresearch.forrester.rate.Rate;
import com.deathrayresearch.forrester.rate.RatePerDay;

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



        Rate errorGenerationRate = new RatePerDay() {
            @Override
            protected Quantity quantityPerDay() {
                return null;
            }
        };

        Flow errorGenerationFlow = new Flow("New errors", errorGenerationRate);

        Rate errorDiscoveryRate = new RatePerDay() {
            @Override
            protected Quantity quantityPerDay() {
                return null;
            }
        };

        Flow errorDiscoveryFlow = new Flow("Discovered errors", errorDiscoveryRate);

        Rate errorFixRate = new RatePerDay() {
            @Override
            protected Quantity quantityPerDay() {
                return null;
            }
        };

        Flow errorFixFlow = new Flow("Fixed errors", errorFixRate);


        latentDefects.addInflow(errorGenerationFlow);
        latentDefects.addOutflow(errorDiscoveryFlow);

        knownDefects.addInflow(errorDiscoveryFlow);
        knownDefects.addOutflow(errorFixFlow);

        fixedDefects.addInflow(errorFixFlow);

        module.addStock(latentDefects);
        module.addStock(knownDefects);
        module.addStock(fixedDefects);

        return module;
    }

}
