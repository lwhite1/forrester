package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.ConnectorRoute;
import com.deathrayresearch.forrester.model.def.ConstantDef;
import com.deathrayresearch.forrester.model.def.ElementPlacement;
import com.deathrayresearch.forrester.model.def.FlowDef;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ViewDef;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Canvas component that renders a model using the Layered Flow Diagram visual language.
 * Holds references to a {@link ModelDefinition} and {@link ViewDef} and redraws on resize.
 */
public class ModelCanvas extends Canvas {

    private ModelDefinition definition;
    private ViewDef view;

    public ModelCanvas() {
        widthProperty().addListener(observable -> redraw());
        heightProperty().addListener(observable -> redraw());
    }

    /**
     * Sets the model and view data, triggering a redraw.
     */
    public void setModel(ModelDefinition definition, ViewDef view) {
        this.definition = definition;
        this.view = view;
        redraw();
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public double prefWidth(double height) {
        return getWidth();
    }

    @Override
    public double prefHeight(double width) {
        return getHeight();
    }

    /**
     * Redraws the entire canvas: background, connections, then elements.
     */
    private void redraw() {
        double w = getWidth();
        double h = getHeight();
        GraphicsContext gc = getGraphicsContext2D();

        // Clear and fill background
        gc.clearRect(0, 0, w, h);
        gc.setFill(ColorPalette.BACKGROUND);
        gc.fillRect(0, 0, w, h);

        if (definition == null || view == null) {
            return;
        }

        // Build lookup maps for element positions and types
        Map<String, ElementPlacement> placementMap = new HashMap<>();
        for (ElementPlacement ep : view.elements()) {
            placementMap.put(ep.name(), ep);
        }

        // Build lookup for constant values
        Map<String, ConstantDef> constantMap = new HashMap<>();
        for (ConstantDef c : definition.constants()) {
            constantMap.put(c.name(), c);
        }

        // Build lookup for stock units
        Map<String, String> stockUnitMap = new HashMap<>();
        for (var s : definition.stocks()) {
            stockUnitMap.put(s.name(), s.unit());
        }

        // 1. Draw connections first (behind elements)
        drawMaterialFlows(gc, placementMap);
        drawInfoLinks(gc, placementMap);

        // 2. Draw elements on top
        for (ElementPlacement ep : view.elements()) {
            switch (ep.type()) {
                case "stock" -> {
                    String unit = stockUnitMap.get(ep.name());
                    ElementRenderer.drawStock(gc, ep.name(), unit,
                            ep.x() - LayoutMetrics.STOCK_WIDTH / 2,
                            ep.y() - LayoutMetrics.STOCK_HEIGHT / 2,
                            LayoutMetrics.STOCK_WIDTH, LayoutMetrics.STOCK_HEIGHT);
                }
                case "flow" -> ElementRenderer.drawFlow(gc, ep.name(), ep.x(), ep.y());
                case "aux" -> ElementRenderer.drawAux(gc, ep.name(),
                        ep.x() - LayoutMetrics.AUX_WIDTH / 2,
                        ep.y() - LayoutMetrics.AUX_HEIGHT / 2,
                        LayoutMetrics.AUX_WIDTH, LayoutMetrics.AUX_HEIGHT);
                case "constant" -> {
                    ConstantDef cd = constantMap.get(ep.name());
                    double value = cd != null ? cd.value() : 0;
                    ElementRenderer.drawConstant(gc, ep.name(), value,
                            ep.x() - LayoutMetrics.CONSTANT_WIDTH / 2,
                            ep.y() - LayoutMetrics.CONSTANT_HEIGHT / 2,
                            LayoutMetrics.CONSTANT_WIDTH, LayoutMetrics.CONSTANT_HEIGHT);
                }
                default -> { }
            }
        }
    }

    /**
     * Draws material flow arrows between stocks based on flow definitions.
     */
    private void drawMaterialFlows(GraphicsContext gc, Map<String, ElementPlacement> placementMap) {
        for (FlowDef flow : definition.flows()) {
            ElementPlacement source = flow.source() != null ? placementMap.get(flow.source()) : null;
            ElementPlacement sink = flow.sink() != null ? placementMap.get(flow.sink()) : null;

            double sourceX = Double.NaN;
            double sourceY = Double.NaN;
            double sinkX = Double.NaN;
            double sinkY = Double.NaN;

            // Clip to stock border edges (right edge of source, left edge of sink)
            if (source != null) {
                sourceX = source.x() + LayoutMetrics.STOCK_WIDTH / 2;
                sourceY = source.y();
            }
            if (sink != null) {
                sinkX = sink.x() - LayoutMetrics.STOCK_WIDTH / 2;
                sinkY = sink.y();
            }

            ConnectionRenderer.drawMaterialFlow(gc, sourceX, sourceY, sinkX, sinkY, flow.name());
        }
    }

    /**
     * Draws info link dashed arrows based on connector routes.
     */
    private void drawInfoLinks(GraphicsContext gc, Map<String, ElementPlacement> placementMap) {
        for (ConnectorRoute route : view.connectors()) {
            ElementPlacement from = placementMap.get(route.from());
            ElementPlacement to = placementMap.get(route.to());
            if (from == null || to == null) {
                continue;
            }

            // Compute border-clipped endpoints
            double fromX = from.x();
            double fromY = from.y();
            double toX = to.x();
            double toY = to.y();

            double fromW = LayoutMetrics.widthFor(from.type()) / 2;
            double fromH = LayoutMetrics.heightFor(from.type()) / 2;
            double toW = LayoutMetrics.widthFor(to.type()) / 2;
            double toH = LayoutMetrics.heightFor(to.type()) / 2;

            double[] clippedFrom = clipToBorder(fromX, fromY, fromW, fromH, toX, toY);
            double[] clippedTo = clipToBorder(toX, toY, toW, toH, fromX, fromY);

            ConnectionRenderer.drawInfoLink(gc, clippedFrom[0], clippedFrom[1],
                    clippedTo[0], clippedTo[1]);
        }
    }

    /**
     * Clips a line from the center of a rectangle toward a target point,
     * returning the intersection with the rectangle border.
     */
    private static double[] clipToBorder(double cx, double cy, double halfW, double halfH,
                                         double targetX, double targetY) {
        double dx = targetX - cx;
        double dy = targetY - cy;
        if (dx == 0 && dy == 0) {
            return new double[]{cx, cy};
        }

        // Find the scaling factor for the ray to hit the rectangle border
        double scaleX = halfW > 0 ? Math.abs(halfW / dx) : Double.MAX_VALUE;
        double scaleY = halfH > 0 ? Math.abs(halfH / dy) : Double.MAX_VALUE;
        double scale = Math.min(scaleX, scaleY);

        return new double[]{cx + dx * scale, cy + dy * scale};
    }
}
