package systems.courant.shrewd.model.graph;

import systems.courant.shrewd.model.def.ElementType;

/**
 * Default element dimensions for layout purposes.
 * Mirrors the rendering dimensions in the app module's LayoutMetrics
 * so that the engine can set proper node sizes without depending on JavaFX.
 */
public record ElementSizes(
        double width,
        double height
) {

    public static final ElementSizes STOCK = new ElementSizes(140, 80);
    public static final ElementSizes FLOW = new ElementSizes(30, 30);
    public static final ElementSizes AUX = new ElementSizes(100, 55);
    public static final ElementSizes LOOKUP = new ElementSizes(100, 50);
    public static final ElementSizes MODULE = new ElementSizes(120, 70);

    /**
     * Returns the default size for a given element type.
     *
     * @param type the element type
     * @return the default width and height for that type
     */
    public static ElementSizes forType(ElementType type) {
        return switch (type) {
            case STOCK -> STOCK;
            case FLOW -> FLOW;
            case AUX -> AUX;
            case LOOKUP -> LOOKUP;
            case MODULE -> MODULE;
            case CLD_VARIABLE -> AUX;
            case COMMENT -> new ElementSizes(160, 80);
        };
    }
}
