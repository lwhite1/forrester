package systems.courant.sd.sweep;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SweepResult and MultiSweepResult defensive copies (#449)")
class SweepResultDefensiveCopyTest {

    private static RunResult dummyRunResult() {
        return new RunResult(Map.of("p", 1.0));
    }

    @Test
    @DisplayName("SweepResult should not be affected by mutation of the source list")
    void sweepResultDefensiveCopy() {
        List<RunResult> mutable = new ArrayList<>();
        mutable.add(dummyRunResult());

        SweepResult result = new SweepResult("param", mutable);
        assertThat(result.getRunCount()).isEqualTo(1);

        mutable.add(dummyRunResult());
        assertThat(result.getRunCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("SweepResult.getResults() should be unmodifiable")
    void sweepResultUnmodifiable() {
        SweepResult result = new SweepResult("param", List.of(dummyRunResult()));
        assertThatThrownBy(() -> result.getResults().add(dummyRunResult()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("MultiSweepResult should not be affected by mutation of source lists")
    void multiSweepResultDefensiveCopy() {
        List<String> names = new ArrayList<>();
        names.add("p1");
        List<RunResult> runs = new ArrayList<>();
        runs.add(dummyRunResult());

        MultiSweepResult result = new MultiSweepResult(names, runs);
        assertThat(result.getParameterNames()).containsExactly("p1");
        assertThat(result.getRunCount()).isEqualTo(1);

        names.add("p2");
        runs.add(dummyRunResult());
        assertThat(result.getParameterNames()).containsExactly("p1");
        assertThat(result.getRunCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("MultiSweepResult.getResults() should be unmodifiable")
    void multiSweepResultUnmodifiable() {
        MultiSweepResult result = new MultiSweepResult(
                List.of("p1"), List.of(dummyRunResult()));
        assertThatThrownBy(() -> result.getResults().add(dummyRunResult()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("MultiSweepResult.getParameterNames() should be unmodifiable")
    void multiSweepParameterNamesUnmodifiable() {
        MultiSweepResult result = new MultiSweepResult(
                List.of("p1"), List.of(dummyRunResult()));
        assertThatThrownBy(() -> result.getParameterNames().add("p2"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
