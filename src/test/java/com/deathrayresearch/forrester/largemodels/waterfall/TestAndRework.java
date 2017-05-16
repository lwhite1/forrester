package com.deathrayresearch.forrester.largemodels.waterfall;


import com.deathrayresearch.forrester.model.Module;

import static com.deathrayresearch.forrester.largemodels.waterfall.WaterfallSoftwareDevelopment.TESTING_AND_REWORK;

/**
 */

class TestAndRework {

    static Module getTestAndReworkSubSystem() {
        Module module = new Module(TESTING_AND_REWORK);

        return module;
    }

}
