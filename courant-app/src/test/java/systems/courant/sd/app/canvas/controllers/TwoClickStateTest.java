package systems.courant.sd.app.canvas.controllers;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TwoClickStateTest {

    @Test
    void shouldStartIdle() {
        TwoClickState state = new TwoClickState();
        assertThat(state.isPending()).isFalse();
        assertThat(state.source()).isNull();
    }

    @Test
    void shouldTransitionToPendingOnBegin() {
        TwoClickState state = new TwoClickState();
        state.begin("stock1", 10, 20, 15, 25);

        assertThat(state.isPending()).isTrue();
        assertThat(state.source()).isEqualTo("stock1");
        assertThat(state.sourceX()).isEqualTo(10);
        assertThat(state.sourceY()).isEqualTo(20);
        assertThat(state.rubberBandEndX()).isEqualTo(15);
        assertThat(state.rubberBandEndY()).isEqualTo(25);
    }

    @Test
    void shouldUpdateRubberBandWhenPending() {
        TwoClickState state = new TwoClickState();
        state.begin("src", 0, 0, 0, 0);
        state.updateRubberBand(50, 60);

        assertThat(state.rubberBandEndX()).isEqualTo(50);
        assertThat(state.rubberBandEndY()).isEqualTo(60);
    }

    @Test
    void shouldIgnoreRubberBandWhenIdle() {
        TwoClickState state = new TwoClickState();
        state.updateRubberBand(50, 60);

        assertThat(state.rubberBandEndX()).isEqualTo(0);
        assertThat(state.rubberBandEndY()).isEqualTo(0);
    }

    @Test
    void shouldResetToIdle() {
        TwoClickState state = new TwoClickState();
        state.begin("src", 10, 20, 30, 40);
        state.reset();

        assertThat(state.isPending()).isFalse();
        assertThat(state.source()).isNull();
        assertThat(state.sourceX()).isEqualTo(0);
        assertThat(state.sourceY()).isEqualTo(0);
    }

    @Test
    void shouldAllowNullSource() {
        TwoClickState state = new TwoClickState();
        state.begin(null, 5, 10, 15, 20);

        assertThat(state.isPending()).isTrue();
        assertThat(state.source()).isNull();
        assertThat(state.sourceX()).isEqualTo(5);
    }
}
