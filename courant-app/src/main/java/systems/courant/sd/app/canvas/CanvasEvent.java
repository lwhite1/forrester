package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.ValidationResult;

/**
 * Events published on the {@link CanvasEventBus} by canvas components.
 * Sealed so that all event types are known at compile time.
 *
 * <p>New events can be added here without modifying publishers or
 * subscribers of existing event types.</p>
 */
public sealed interface CanvasEvent {

    /** Canvas status changed: selection, zoom, tool, or element counts. */
    record StatusChanged() implements CanvasEvent {}

    /** Module navigation depth changed (drill-in or navigate-back). */
    record NavigationChanged() implements CanvasEvent {}

    /** Background validation completed with updated results. */
    record ValidationChanged(ValidationResult result) implements CanvasEvent {}
}
