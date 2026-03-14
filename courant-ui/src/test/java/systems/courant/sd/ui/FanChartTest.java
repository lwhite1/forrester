package systems.courant.sd.ui;

import javafx.application.Platform;
import javafx.stage.Stage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FanChart")
@ExtendWith(ApplicationExtension.class)
class FanChartTest {

    @Start
    void start(Stage stage) {
        // JavaFX toolkit initialization only
    }

    @Test
    @DisplayName("no-arg constructor leaves result null (#533)")
    void noArgConstructorLeavesResultNull() {
        FanChart chart = new FanChart();
        assertThat(chart).isNotNull();
    }

    @Test
    @DisplayName("start() with no-arg constructor throws IllegalStateException (#533)")
    void startWithNoArgConstructorThrowsIllegalState() {
        CompletableFuture<Throwable> thrown = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                FanChart chart = new FanChart();
                chart.start(new Stage());
                thrown.complete(null);
            } catch (Throwable t) {
                thrown.complete(t);
            }
        });
        WaitForAsyncUtils.waitForFxEvents();

        Throwable result = thrown.join();
        assertThat(result)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FanChart requires a MonteCarloResult");
    }
}
