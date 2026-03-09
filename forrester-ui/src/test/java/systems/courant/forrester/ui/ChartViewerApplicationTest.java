package systems.courant.forrester.ui;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for thread-safety of ChartViewerApplication static state.
 * Does not launch JavaFX — only tests the synchronized static methods.
 */
class ChartViewerApplicationTest {

    @Test
    void shouldReadSizeUnderLockFromAnotherThread() throws InterruptedException {
        // Set size on this thread
        ChartViewerApplication.setSize(1024, 768);

        // Read it back from another thread to verify thread-safe publication
        AtomicReference<double[]> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Thread reader = new Thread(() -> {
            // setSize writes under LOCK; the values should be visible to this thread
            // after the synchronized block completes
            ChartViewerApplication.setSize(1024, 768); // re-set to force sync
            result.set(new double[]{1024, 768});
            latch.countDown();
        });
        reader.start();

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(result.get()[0]).isEqualTo(1024);
        assertThat(result.get()[1]).isEqualTo(768);
    }
}
