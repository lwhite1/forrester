package systems.courant.shrewd.io;

import systems.courant.shrewd.demo.agile.AgileSoftwareDevelopmentDemo;
import systems.courant.shrewd.demo.waterfall.WaterfallSoftwareDevelopmentDemo;
import systems.courant.shrewd.model.Model;
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
        Model model = AgileSoftwareDevelopmentDemo.getModel();
        String report = ModelReport.create(model);
        assertThat(report).isNotBlank();
        assertThat(report).contains("Model Report");
        assertThat(report).contains(model.getName());
        assertThat(report).contains("Stocks:");
    }
}
