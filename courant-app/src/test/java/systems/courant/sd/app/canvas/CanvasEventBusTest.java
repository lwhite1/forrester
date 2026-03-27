package systems.courant.sd.app.canvas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import systems.courant.sd.model.def.ValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("CanvasEventBus")
class CanvasEventBusTest {

    private CanvasEventBus bus;

    @BeforeEach
    void setUp() {
        bus = new CanvasEventBus();
    }

    @Nested
    @DisplayName("Publish and Subscribe")
    class PublishAndSubscribe {

        @Test
        void shouldInvokeSubscriberForMatchingEventType() {
            AtomicInteger count = new AtomicInteger();
            bus.subscribe(CanvasEvent.StatusChanged.class, e -> count.incrementAndGet());

            bus.publish(new CanvasEvent.StatusChanged());

            assertThat(count.get()).isEqualTo(1);
        }

        @Test
        void shouldInvokeMultipleSubscribersInOrder() {
            List<String> order = new ArrayList<>();
            bus.subscribe(CanvasEvent.StatusChanged.class, e -> order.add("first"));
            bus.subscribe(CanvasEvent.StatusChanged.class, e -> order.add("second"));
            bus.subscribe(CanvasEvent.StatusChanged.class, e -> order.add("third"));

            bus.publish(new CanvasEvent.StatusChanged());

            assertThat(order).containsExactly("first", "second", "third");
        }

        @Test
        void shouldNotInvokeSubscriberForDifferentEventType() {
            AtomicInteger statusCount = new AtomicInteger();
            AtomicInteger navCount = new AtomicInteger();
            bus.subscribe(CanvasEvent.StatusChanged.class, e -> statusCount.incrementAndGet());
            bus.subscribe(CanvasEvent.NavigationChanged.class, e -> navCount.incrementAndGet());

            bus.publish(new CanvasEvent.StatusChanged());

            assertThat(statusCount.get()).isEqualTo(1);
            assertThat(navCount.get()).isEqualTo(0);
        }

        @Test
        void shouldPassEventDataToSubscriber() {
            var result = new ValidationResult(List.of());
            var received = new Object() { ValidationResult value; };
            bus.subscribe(CanvasEvent.ValidationChanged.class, e -> received.value = e.result());

            bus.publish(new CanvasEvent.ValidationChanged(result));

            assertThat(received.value).isSameAs(result);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        void shouldNotThrowWhenPublishingWithNoSubscribers() {
            assertThatCode(() -> bus.publish(new CanvasEvent.StatusChanged()))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldContinueToNextSubscriberWhenOneThrows() {
            AtomicInteger count = new AtomicInteger();
            bus.subscribe(CanvasEvent.StatusChanged.class, e -> count.incrementAndGet());
            bus.subscribe(CanvasEvent.StatusChanged.class, e -> {
                throw new RuntimeException("subscriber failure");
            });
            bus.subscribe(CanvasEvent.StatusChanged.class, e -> count.incrementAndGet());

            bus.publish(new CanvasEvent.StatusChanged());

            assertThat(count.get()).isEqualTo(2);
        }
    }
}
