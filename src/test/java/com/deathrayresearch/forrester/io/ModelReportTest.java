package com.deathrayresearch.forrester.io;

import com.deathrayresearch.forrester.demo.agile.AgileSoftwareDevelopmentDemo;
import com.deathrayresearch.forrester.demo.waterfall.WaterfallSoftwareDevelopmentDemo;
import com.deathrayresearch.forrester.model.Model;
import org.junit.Test;

public class ModelReportTest {

    @Test
    public void produceModelReport1() {

        Model model = new WaterfallSoftwareDevelopmentDemo().getModel();
        System.out.println(ModelReport.create(model));
    }

    @Test
    public void produceModelReport2() {

        Model model = new AgileSoftwareDevelopmentDemo().getModel();
        System.out.println(ModelReport.create(model));
    }
}