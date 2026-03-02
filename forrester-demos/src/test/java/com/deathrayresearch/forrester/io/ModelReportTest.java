package com.deathrayresearch.forrester.io;

import com.deathrayresearch.forrester.demo.agile.AgileSoftwareDevelopmentDemo;
import com.deathrayresearch.forrester.demo.waterfall.WaterfallSoftwareDevelopmentDemo;
import com.deathrayresearch.forrester.model.Model;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ModelReport")
class ModelReportTest {

    @Test
    void shouldProduceReportForWaterfallModel() {
        Model model = new WaterfallSoftwareDevelopmentDemo().getModel();
        String report = ModelReport.create(model);
        assertThat(report).isNotBlank();
        assertThat(report).contains("Model Report");
        assertThat(report).contains(model.getName());
        assertThat(report).contains("Stocks:");
    }

    @Test
    void shouldProduceReportForAgileModel() {
        Model model = new AgileSoftwareDevelopmentDemo().getModel();
        String report = ModelReport.create(model);
        assertThat(report).isNotBlank();
        assertThat(report).contains("Model Report");
        assertThat(report).contains(model.getName());
        assertThat(report).contains("Stocks:");
    }
}
