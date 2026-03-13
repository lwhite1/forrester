package systems.courant.sd.model.def;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimulationSettingsTest {

    @Test
    void shouldDefaultDtToOne() {
        var settings = new SimulationSettings("Day", 100, "Day");
        assertThat(settings.dt()).isEqualTo(1.0);
    }

    @Test
    void shouldAcceptExplicitDt() {
        var settings = new SimulationSettings("Month", 120, "Month", 0.25);
        assertThat(settings.dt()).isEqualTo(0.25);
        assertThat(settings.timeStep()).isEqualTo("Month");
        assertThat(settings.duration()).isEqualTo(120.0);
        assertThat(settings.durationUnit()).isEqualTo("Month");
    }

    @Test
    void shouldRejectNonPositiveDt() {
        assertThatThrownBy(() -> new SimulationSettings("Day", 100, "Day", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dt");
        assertThatThrownBy(() -> new SimulationSettings("Day", 100, "Day", -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dt");
    }

    @Test
    void shouldRejectNaNDt() {
        assertThatThrownBy(() -> new SimulationSettings("Day", 100, "Day", Double.NaN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dt");
    }

    @Test
    void shouldRejectInfiniteDt() {
        assertThatThrownBy(() -> new SimulationSettings("Day", 100, "Day", Double.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dt");
    }
}
