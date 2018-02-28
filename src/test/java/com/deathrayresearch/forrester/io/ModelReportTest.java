package com.deathrayresearch.forrester.io;

import com.deathrayresearch.forrester.largemodels.agile.AgileSoftwareDevelopment;
import com.deathrayresearch.forrester.largemodels.waterfall.WaterfallSoftwareDevelopment;
import com.deathrayresearch.forrester.model.Model;
import org.junit.Test;

public class ModelReportTest {

    @Test
    public void produceModelReport1() {

        Model model = new WaterfallSoftwareDevelopment().getModel();
        System.out.println(ModelReport.create(model));
    }

    @Test
    public void produceModelReport2() {

        Model model = new AgileSoftwareDevelopment().getModel();
        System.out.println(ModelReport.create(model));
    }
}