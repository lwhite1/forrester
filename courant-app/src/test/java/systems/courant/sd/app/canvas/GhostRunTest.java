package systems.courant.sd.app.canvas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GhostRun")
class GhostRunTest {

    @Nested
    @DisplayName("generateName")
    class GenerateName {

        @Test
        void shouldIncludeRunNumberWhenNoParamsChanged() {
            Map<String, Double> params = Map.of("rate", 0.1);
            String name = GhostRun.generateName(3, params, params);
            assertThat(name).startsWith("Run 3 (");
        }

        @Test
        void shouldFallbackToTimestampWhenParamsEmpty() {
            String name = GhostRun.generateName(1, Map.of(), Map.of());
            assertThat(name).startsWith("Run 1 (");
        }

        @Test
        void shouldFallbackToTimestampWhenParamsNull() {
            String name = GhostRun.generateName(2, null, null);
            assertThat(name).startsWith("Run 2 (");
        }

        @Test
        void shouldShowChangedParameter() {
            Map<String, Double> prev = Map.of("rate", 0.1);
            Map<String, Double> curr = Map.of("rate", 0.2);
            String name = GhostRun.generateName(2, curr, prev);
            assertThat(name).isEqualTo("Run 2: rate=0.2000");
        }

        @Test
        void shouldShowMultipleChangedParameters() {
            Map<String, Double> prev = orderedMap("rate", 0.1, "delay", 3.0);
            Map<String, Double> curr = orderedMap("rate", 0.2, "delay", 5.0);
            String name = GhostRun.generateName(3, curr, prev);
            assertThat(name).isEqualTo("Run 3: rate=0.2000, delay=5");
        }

        @Test
        void shouldTruncateWhenMoreThanTwoParamsChanged() {
            Map<String, Double> prev = orderedMap("a", 1.0, "b", 2.0, "c", 3.0);
            Map<String, Double> curr = orderedMap("a", 10.0, "b", 20.0, "c", 30.0);
            String name = GhostRun.generateName(4, curr, prev);
            assertThat(name).contains("(+1 more)");
            assertThat(name).startsWith("Run 4: ");
        }

        @Test
        void shouldDetectNewParameterAsChanged() {
            Map<String, Double> prev = Map.of("rate", 0.1);
            Map<String, Double> curr = orderedMap("rate", 0.1, "capacity", 500.0);
            String name = GhostRun.generateName(2, curr, prev);
            assertThat(name).isEqualTo("Run 2: capacity=500");
        }

        @Test
        void shouldShowAllParamsAsChangedWhenNoPreviousRun() {
            Map<String, Double> curr = Map.of("rate", 0.1);
            String name = GhostRun.generateName(1, curr, Map.of());
            assertThat(name).isEqualTo("Run 1: rate=0.1000");
        }
    }

    @Nested
    @DisplayName("tooltipText")
    class TooltipText {

        @Test
        void shouldShowNameOnlyWhenNoParams() {
            GhostRun ghost = new GhostRun(dummyResult(), "Run 1", 0, Map.of());
            assertThat(ghost.tooltipText()).isEqualTo("Run 1");
        }

        @Test
        void shouldShowNameAndParamsWhenPresent() {
            GhostRun ghost = new GhostRun(dummyResult(), "Run 2: rate=0.1", 0,
                    Map.of("rate", 0.1));
            String tooltip = ghost.tooltipText();
            assertThat(tooltip).contains("Run 2: rate=0.1");
            assertThat(tooltip).contains("rate = 0.1");
        }
    }

    @Nested
    @DisplayName("withName")
    class WithName {

        @Test
        void shouldReturnNewInstanceWithUpdatedName() {
            GhostRun original = new GhostRun(dummyResult(), "Run 1", 2,
                    Map.of("rate", 0.1));
            GhostRun renamed = original.withName("Baseline");

            assertThat(renamed.name()).isEqualTo("Baseline");
            assertThat(renamed.colorIndex()).isEqualTo(2);
            assertThat(renamed.parameters()).isEqualTo(Map.of("rate", 0.1));
            assertThat(renamed.result()).isSameAs(original.result());
        }
    }

    @Nested
    @DisplayName("immutability")
    class Immutability {

        @Test
        void shouldDefensivelyCopyParameters() {
            Map<String, Double> mutable = new LinkedHashMap<>();
            mutable.put("rate", 0.1);
            GhostRun ghost = new GhostRun(dummyResult(), "Run 1", 0, mutable);

            mutable.put("extra", 99.0);
            assertThat(ghost.parameters()).doesNotContainKey("extra");
        }

        @Test
        void shouldHandleNullParameters() {
            GhostRun ghost = new GhostRun(dummyResult(), "Run 1", 0, null);
            assertThat(ghost.parameters()).isEmpty();
        }
    }

    private static SimulationRunner.SimulationResult dummyResult() {
        return new SimulationRunner.SimulationResult(
                List.of("Step", "Stock1"),
                List.of(new double[]{0, 100}, new double[]{1, 110})
        );
    }

    private static Map<String, Double> orderedMap(String k1, double v1,
                                                   String k2, double v2) {
        Map<String, Double> map = new LinkedHashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return map;
    }

    private static Map<String, Double> orderedMap(String k1, double v1,
                                                   String k2, double v2,
                                                   String k3, double v3) {
        Map<String, Double> map = new LinkedHashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }
}
