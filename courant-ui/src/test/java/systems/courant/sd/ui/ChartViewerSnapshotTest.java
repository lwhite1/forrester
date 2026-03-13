package systems.courant.sd.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that ChartViewerApplication.snapshot() captures an atomic, independent
 * copy of the static state, so concurrent callers cannot corrupt each other.
 */
@DisplayName("ChartViewerApplication snapshot isolation")
@ExtendWith(ApplicationExtension.class)
class ChartViewerSnapshotTest {

    @BeforeEach
    void setUp() {
        ChartViewerApplication.reset();
    }

    @Test
    @DisplayName("snapshot captures current state independently of later mutations")
    void shouldIsolateSnapshotFromLaterMutations() {
        ChartViewerApplication.addSeries(List.of("A"), List.of());
        ChartViewerApplication.addValues(List.of(1.0), List.of(), 0);
        ChartViewerApplication.setSize(1024, 768);

        ChartViewerApplication.ChartData snap = ChartViewerApplication.snapshot();

        // Mutate static state after snapshot
        ChartViewerApplication.reset();
        ChartViewerApplication.addSeries(List.of("B", "C"), List.of());
        ChartViewerApplication.setSize(640, 480);

        // Snapshot should still reflect the original state
        assertThat(snap.series()).hasSize(1);
        assertThat(snap.series().getFirst().getName()).isEqualTo("A");
        assertThat(snap.series().getFirst().getData()).hasSize(1);
        assertThat(snap.width()).isEqualTo(1024);
        assertThat(snap.height()).isEqualTo(768);
    }

    @Test
    @DisplayName("snapshot taken before next caller mutates state is unaffected")
    void shouldIsolateSequentialCallers() {
        // Caller 1 accumulates data and snapshots
        ChartViewerApplication.addSeries(List.of("Alpha"), List.of());
        ChartViewerApplication.addValues(List.of(10.0), List.of(), 0);
        ChartViewerApplication.ChartData snap1 = ChartViewerApplication.snapshot();

        // Caller 2 resets and accumulates different data
        ChartViewerApplication.reset();
        ChartViewerApplication.addSeries(List.of("Beta"), List.of());
        ChartViewerApplication.addValues(List.of(20.0), List.of(), 0);
        ChartViewerApplication.ChartData snap2 = ChartViewerApplication.snapshot();

        // snap1 still holds Alpha/10.0
        assertThat(snap1.series()).hasSize(1);
        assertThat(snap1.series().getFirst().getName()).isEqualTo("Alpha");
        assertThat(snap1.series().getFirst().getData().getFirst().getYValue().doubleValue())
                .isEqualTo(10.0);

        // snap2 holds Beta/20.0
        assertThat(snap2.series()).hasSize(1);
        assertThat(snap2.series().getFirst().getName()).isEqualTo("Beta");
        assertThat(snap2.series().getFirst().getData().getFirst().getYValue().doubleValue())
                .isEqualTo(20.0);
    }

    @Test
    @DisplayName("snapshot title and xAxisLabel match what was set")
    void shouldCaptureTitle() {
        ChartViewerApplication.addSeries(List.of("X"), List.of());

        ChartViewerApplication.ChartData snap = ChartViewerApplication.snapshot();

        // Default title is empty, xAxisLabel is empty
        assertThat(snap.title()).isEmpty();
        assertThat(snap.xAxisLabel()).isEmpty();
    }

    @Test
    @DisplayName("snapshot after reset returns empty series")
    void shouldReturnEmptyAfterReset() {
        ChartViewerApplication.addSeries(List.of("A", "B"), List.of());
        ChartViewerApplication.reset();

        ChartViewerApplication.ChartData snap = ChartViewerApplication.snapshot();

        assertThat(snap.series()).isEmpty();
        assertThat(snap.width()).isEqualTo(800);
        assertThat(snap.height()).isEqualTo(600);
    }

}
