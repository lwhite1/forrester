package com.deathrayresearch.forrester.largemodels.waterfall;


import com.deathrayresearch.forrester.model.SubSystem;

import static com.deathrayresearch.forrester.largemodels.waterfall.WaterfallSoftwareDevelopment.TESTING_AND_REWORK;

/**
 */

class TestAndRework {

    static SubSystem getTestAndReworkSubSystem() {
        SubSystem subSystem = new SubSystem(TESTING_AND_REWORK);

        return subSystem;
    }

}
