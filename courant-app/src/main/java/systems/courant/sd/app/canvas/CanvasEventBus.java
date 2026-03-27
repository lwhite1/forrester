package systems.courant.sd.app.canvas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Lightweight publish-subscribe event bus for canvas component coordination.
 *
 * <p>Components publish {@link CanvasEvent} instances, and all registered
 * subscribers for that event type are invoked in registration order. If a
 * subscriber throws, the exception is logged and remaining subscribers
 * still execute (error isolation).</p>
 *
 * <p>This bus is designed for single-threaded use on the JavaFX Application
 * Thread. It does not provide thread safety guarantees.</p>
 */
public final class CanvasEventBus {

    private static final Logger logger = LoggerFactory.getLogger(CanvasEventBus.class);

    private final Map<Class<? extends CanvasEvent>, List<Consumer<? extends CanvasEvent>>>
            subscribers = new LinkedHashMap<>();

    /**
     * Subscribes a handler to events of the given type. The handler will be
     * invoked each time an event of that exact type is published.
     *
     * @param eventType the event class to subscribe to
     * @param handler   the handler to invoke when the event is published
     * @param <E>       the event type
     */
    public <E extends CanvasEvent> void subscribe(Class<E> eventType, Consumer<E> handler) {
        subscribers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
    }

    /**
     * Publishes an event to all subscribers registered for its concrete type.
     * Subscribers are invoked in registration order. If a subscriber throws,
     * the exception is logged and remaining subscribers still execute.
     *
     * @param event the event to publish
     */
    @SuppressWarnings("unchecked")
    public void publish(CanvasEvent event) {
        List<Consumer<? extends CanvasEvent>> handlers = subscribers.get(event.getClass());
        if (handlers == null) {
            return;
        }
        for (Consumer<? extends CanvasEvent> handler : handlers) {
            try {
                ((Consumer<CanvasEvent>) handler).accept(event);
            } catch (Exception e) {
                logger.error("Event subscriber failed for {}: {}",
                        event.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }
}
