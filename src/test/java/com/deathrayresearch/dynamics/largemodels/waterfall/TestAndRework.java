package com.deathrayresearch.dynamics.largemodels.waterfall;

import com.deathrayresearch.dynamics.model.SubSystem;

import static com.deathrayresearch.dynamics.largemodels.waterfall.WaterfallSoftwareDevelopment.TESTING_AND_REWORK;

/**
 */

class TestAndRework {

    static SubSystem getTestAndReworkSubSystem() {
        SubSystem subSystem = new SubSystem(TESTING_AND_REWORK);

        return subSystem;
    }

}
