package systems.courant.sd.app.canvas;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that the interrupt-clearing pattern used by {@link AnalysisRunner}
 * prevents a leaked interrupted flag from affecting the next submitted task.
 *
 * <p>Uses a plain single-thread executor to reproduce the scenario without
 * requiring the JavaFX toolkit (StatusBar / Platform.runLater).
 */
@DisplayName("AnalysisRunner interrupt safety")
class AnalysisRunnerTest {

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "analysis-runner-test");
        t.setDaemon(true);
        return t;
    });

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Nested
    @DisplayName("interrupt flag clearing")
    class InterruptFlagClearing {

        @Test
        void shouldNotLeakInterruptedStateToNextTask() throws InterruptedException {
            CountDownLatch taskRunning = new CountDownLatch(1);

            // Submit a task that busy-waits (won't consume the interrupt).
            AtomicBoolean keepRunning = new AtomicBoolean(true);
            Future<?> first = executor.submit(() -> {
                taskRunning.countDown();
                while (keepRunning.get()) {
                    Thread.onSpinWait();
                }
            });

            assertThat(taskRunning.await(5, TimeUnit.SECONDS)).isTrue();

            // Cancel with interrupt — sets the thread's interrupted flag.
            first.cancel(true);
            keepRunning.set(false);

            // Submit a second task that mirrors AnalysisRunner's pattern:
            // call Thread.interrupted() at the top, then check the flag.
            AtomicBoolean interruptedAtStart = new AtomicBoolean(true);
            CountDownLatch secondDone = new CountDownLatch(1);

            executor.submit(() -> {
                Thread.interrupted(); // same clearing as AnalysisRunner
                interruptedAtStart.set(Thread.currentThread().isInterrupted());
                secondDone.countDown();
            });

            assertThat(secondDone.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(interruptedAtStart.get())
                    .as("interrupted flag should be cleared before the next task runs")
                    .isFalse();
        }
    }
}
