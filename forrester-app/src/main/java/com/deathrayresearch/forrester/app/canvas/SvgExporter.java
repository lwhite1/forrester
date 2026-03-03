package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.ConnectorRoute;
import com.deathrayresearch.forrester.model.def.ConstantDef;
import com.deathrayresearch.forrester.model.def.ElementType;
import com.deathrayresearch.forrester.model.def.FlowDef;
import com.deathrayresearch.forrester.model.def.LookupTableDef;
import com.deathrayresearch.forrester.model.graph.FeedbackAnalysis;

import javafx.scene.paint.Color;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * Exports the diagram to an SVG file by translating canvas drawing operations
 * into SVG XML elements. Follows the same layer order as {@link CanvasRenderer}.
 */
public final class SvgExporter {

    private SvgExporter() {
    }

    /**
     * Exports the current diagram to an SVG file.
     *
     * @param canvasState  the canvas state containing element positions
     * @param editor       the model editor
     * @param connectors   the connector routes for info links
     * @param loopAnalysis optional feedback analysis (null if loop highlighting is off)
     * @param file         the output file
     * @throws IOException if writing fails
     */
    public static void export(CanvasState canvasState, ModelEditor editor,
                              List<ConnectorRoute> connectors,
                              FeedbackAnalysis loopAnalysis,
                              File file) throws IOException {
        ExportBounds.Bounds bounds = ExportBounds.compute(canvasState, editor);
        double minX = bounds.minX();
        double minY = bounds.minY();
        double width = bounds.width();
        double height = bounds.height();

        try (PrintWriter w = new PrintWriter(file, StandardCharsets.UTF_8)) {
            w.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            w.printf(Locale.US,
                    "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"%.1f %.1f %.1f %.1f\" " +
                    "width=\"%.1f\" height=\"%.1f\">%n",
                    minX, minY, width, height, width, height);

            // 1. Background
            w.printf(Locale.US, "  <rect x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" fill=\"%s\"/>%n",
                    minX, minY, width, height, svgColor(ColorPalette.BACKGROUND));

            // 2. Material flows
            writeMaterialFlows(w, canvasState, editor);

            // 3. Info links
            writeInfoLinks(w, canvasState, connectors);

            // 4. Loop edge highlights
            if (loopAnalysis != null) {
                writeLoopEdges(w, canvasState, connectors, editor, loopAnalysis);
            }

            // 5. Elements
            for (String name : canvasState.getDrawOrder()) {
                ElementType type = canvasState.getType(name);
                if (type == null) {
                    continue;
                }
                double cx = canvasState.getX(name);
                double cy = canvasState.getY(name);

                switch (type) {
                    case STOCK -> writeStock(w, name, editor.getStockUnit(name), cx, cy,
                            LayoutMetrics.effectiveWidth(canvasState, name),
                            LayoutMetrics.effectiveHeight(canvasState, name));
                    case FLOW -> {
                        String eq = editor.getFlowEquation(name);
                        writeFlow(w, name, eq, cx, cy);
                    }
                    case AUX -> writeAux(w, name, editor.getAuxEquation(name), cx, cy,
                            LayoutMetrics.effectiveWidth(canvasState, name),
                            LayoutMetrics.effectiveHeight(canvasState, name));
                    case CONSTANT -> {
                        ConstantDef cd = editor.getConstantByName(name);
                        double value = cd != null ? cd.value() : 0;
                        writeConstant(w, name, value, cx, cy,
                                LayoutMetrics.effectiveWidth(canvasState, name),
                                LayoutMetrics.effectiveHeight(canvasState, name));
                    }
                    case MODULE -> writeModule(w, name, cx, cy,
                            LayoutMetrics.effectiveWidth(canvasState, name),
                            LayoutMetrics.effectiveHeight(canvasState, name));
                    case LOOKUP -> {
                        LookupTableDef lt = editor.getLookupTableByName(name);
                        int pts = lt != null ? lt.xValues().length : 0;
                        writeLookup(w, name, pts, cx, cy,
                                LayoutMetrics.effectiveWidth(canvasState, name),
                                LayoutMetrics.effectiveHeight(canvasState, name));
                    }
                    default -> { }
                }
            }

            // 6. Loop participant highlights
            if (loopAnalysis != null) {
                for (String name : loopAnalysis.loopParticipants()) {
                    if (canvasState.hasElement(name)) {
                        writeLoopHighlight(w, canvasState, name);
                    }
                }
            }

            w.println("</svg>");
        }
    }

    // --- Material flows ---

    private static void writeMaterialFlows(PrintWriter w, CanvasState state, ModelEditor editor) {
        for (FlowDef flow : editor.getFlows()) {
            if (!state.hasElement(flow.name())) {
                continue;
            }
            double midX = state.getX(flow.name());
            double midY = state.getY(flow.name());

            double sourceX;
            double sourceY;
            boolean sourceIsCloud;

            if (flow.source() != null && state.hasElement(flow.source())) {
                FlowGeometry.Point2D edge = FlowGeometry.clipToElement(state, flow.source(), midX, midY);
                sourceX = edge.x();
                sourceY = edge.y();
                sourceIsCloud = false;
            } else {
                FlowGeometry.Point2D cloud = FlowEndpointCalculator.cloudPosition(
                        FlowEndpointCalculator.FlowEnd.SOURCE, flow, state);
                sourceX = cloud != null ? cloud.x() : midX - LayoutMetrics.CLOUD_OFFSET;
                sourceY = cloud != null ? cloud.y() : midY;
                sourceIsCloud = true;
            }

            double sinkX;
            double sinkY;
            boolean sinkIsCloud;

            if (flow.sink() != null && state.hasElement(flow.sink())) {
                FlowGeometry.Point2D edge = FlowGeometry.clipToElement(state, flow.sink(), midX, midY);
                sinkX = edge.x();
                sinkY = edge.y();
                sinkIsCloud = false;
            } else {
                FlowGeometry.Point2D cloud = FlowEndpointCalculator.cloudPosition(
                        FlowEndpointCalculator.FlowEnd.SINK, flow, state);
                sinkX = cloud != null ? cloud.x() : midX + LayoutMetrics.CLOUD_OFFSET;
                sinkY = cloud != null ? cloud.y() : midY;
                sinkIsCloud = true;
            }

            // Clouds
            if (sourceIsCloud) {
                writeCloud(w, sourceX, sourceY);
            }
            if (sinkIsCloud) {
                writeCloud(w, sinkX, sinkY);
            }

            // Source → diamond
            w.printf(Locale.US,
                    "  <line x1=\"%.2f\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\" " +
                    "stroke=\"%s\" stroke-width=\"%.1f\"/>%n",
                    sourceX, sourceY, midX, midY,
                    svgColor(ColorPalette.MATERIAL_FLOW), LayoutMetrics.MATERIAL_FLOW_WIDTH);

            // Diamond → sink
            w.printf(Locale.US,
                    "  <line x1=\"%.2f\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\" " +
                    "stroke=\"%s\" stroke-width=\"%.1f\"/>%n",
                    midX, midY, sinkX, sinkY,
                    svgColor(ColorPalette.MATERIAL_FLOW), LayoutMetrics.MATERIAL_FLOW_WIDTH);

            // Arrowhead at sink
            writeArrowhead(w, midX, midY, sinkX, sinkY,
                    LayoutMetrics.ARROWHEAD_LENGTH, LayoutMetrics.ARROWHEAD_WIDTH,
                    ColorPalette.MATERIAL_FLOW);
        }
    }

    // --- Info links ---

    private static void writeInfoLinks(PrintWriter w, CanvasState state,
                                       List<ConnectorRoute> connectors) {
        for (ConnectorRoute route : connectors) {
            if (!state.hasElement(route.from()) || !state.hasElement(route.to())) {
                continue;
            }

            double fromX = state.getX(route.from());
            double fromY = state.getY(route.from());
            double toX = state.getX(route.to());
            double toY = state.getY(route.to());

            FlowGeometry.Point2D cf = FlowGeometry.clipToElement(state, route.from(), toX, toY);
            FlowGeometry.Point2D ct = FlowGeometry.clipToElement(state, route.to(), fromX, fromY);

            w.printf(Locale.US,
                    "  <line x1=\"%.2f\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\" " +
                    "stroke=\"%s\" stroke-opacity=\"%.2f\" stroke-width=\"%.1f\" " +
                    "stroke-dasharray=\"%.0f %.0f\"/>%n",
                    cf.x(), cf.y(), ct.x(), ct.y(),
                    svgColor(ColorPalette.INFO_LINK), svgOpacity(ColorPalette.INFO_LINK),
                    LayoutMetrics.INFO_LINK_WIDTH,
                    LayoutMetrics.INFO_LINK_DASH_LENGTH, LayoutMetrics.INFO_LINK_DASH_GAP);

            writeArrowhead(w, cf.x(), cf.y(), ct.x(), ct.y(),
                    LayoutMetrics.INFO_ARROWHEAD_LENGTH, LayoutMetrics.INFO_ARROWHEAD_WIDTH,
                    ColorPalette.INFO_LINK);
        }
    }

    // --- Loop edges ---

    private static void writeLoopEdges(PrintWriter w, CanvasState state,
                                       List<ConnectorRoute> connectors,
                                       ModelEditor editor,
                                       FeedbackAnalysis loopAnalysis) {
        // Info link loop edges
        for (ConnectorRoute route : connectors) {
            if (!state.hasElement(route.from()) || !state.hasElement(route.to())) {
                continue;
            }
            if (!loopAnalysis.isLoopEdge(route.from(), route.to())) {
                continue;
            }

            double fromX = state.getX(route.from());
            double fromY = state.getY(route.from());
            double toX = state.getX(route.to());
            double toY = state.getY(route.to());

            FlowGeometry.Point2D cf = FlowGeometry.clipToElement(state, route.from(), toX, toY);
            FlowGeometry.Point2D ct = FlowGeometry.clipToElement(state, route.to(), fromX, fromY);

            w.printf(Locale.US,
                    "  <line x1=\"%.2f\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\" " +
                    "stroke=\"%s\" stroke-opacity=\"%.2f\" stroke-width=\"2.5\"/>%n",
                    cf.x(), cf.y(), ct.x(), ct.y(),
                    svgColor(ColorPalette.LOOP_EDGE), svgOpacity(ColorPalette.LOOP_EDGE));
        }

        // Material flow loop edges
        for (FlowDef flow : editor.getFlows()) {
            if (!state.hasElement(flow.name())) {
                continue;
            }
            double midX = state.getX(flow.name());
            double midY = state.getY(flow.name());

            if (flow.source() != null && state.hasElement(flow.source())
                    && loopAnalysis.isLoopEdge(flow.name(), flow.source())) {
                FlowGeometry.Point2D edge = FlowGeometry.clipToElement(
                        state, flow.source(), midX, midY);
                w.printf(Locale.US,
                        "  <line x1=\"%.2f\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\" " +
                        "stroke=\"%s\" stroke-opacity=\"%.2f\" stroke-width=\"2.5\"/>%n",
                        midX, midY, edge.x(), edge.y(),
                        svgColor(ColorPalette.LOOP_EDGE), svgOpacity(ColorPalette.LOOP_EDGE));
            }

            if (flow.sink() != null && state.hasElement(flow.sink())
                    && loopAnalysis.isLoopEdge(flow.name(), flow.sink())) {
                FlowGeometry.Point2D edge = FlowGeometry.clipToElement(
                        state, flow.sink(), midX, midY);
                w.printf(Locale.US,
                        "  <line x1=\"%.2f\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\" " +
                        "stroke=\"%s\" stroke-opacity=\"%.2f\" stroke-width=\"2.5\"/>%n",
                        midX, midY, edge.x(), edge.y(),
                        svgColor(ColorPalette.LOOP_EDGE), svgOpacity(ColorPalette.LOOP_EDGE));
            }
        }
    }

    // --- Elements ---

    private static void writeStock(PrintWriter w, String name, String unit,
                                   double cx, double cy, double width, double height) {
        double x = cx - width / 2;
        double y = cy - height / 2;
        double r = LayoutMetrics.STOCK_CORNER_RADIUS;

        // Fill
        w.printf(Locale.US,
                "  <rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" rx=\"%.1f\" " +
                "fill=\"%s\"/>%n",
                x, y, width, height, r, svgColor(ColorPalette.STOCK_FILL));

        // Border
        w.printf(Locale.US,
                "  <rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" rx=\"%.1f\" " +
                "fill=\"none\" stroke=\"%s\" stroke-width=\"%.1f\"/>%n",
                x, y, width, height, r,
                svgColor(ColorPalette.STOCK_BORDER), LayoutMetrics.STOCK_BORDER_WIDTH);

        // Name centered
        w.printf(Locale.US,
                "  <text x=\"%.2f\" y=\"%.2f\" text-anchor=\"middle\" dominant-baseline=\"central\" " +
                "font-family=\"sans-serif\" font-size=\"13\" font-weight=\"bold\" fill=\"%s\">%s</text>%n",
                cx, cy, svgColor(ColorPalette.TEXT), escapeXml(name));

        // Unit badge
        if (unit != null && !unit.isBlank()) {
            w.printf(Locale.US,
                    "  <text x=\"%.2f\" y=\"%.2f\" text-anchor=\"end\" dominant-baseline=\"auto\" " +
                    "font-family=\"sans-serif\" font-size=\"9\" fill=\"%s\">%s</text>%n",
                    x + width - 6, y + height - 4,
                    svgColor(ColorPalette.TEXT_SECONDARY), escapeXml(unit));
        }
    }

    private static void writeFlow(PrintWriter w, String name, String equation,
                                  double cx, double cy) {
        double half = LayoutMetrics.FLOW_INDICATOR_SIZE / 2.0;

        // Diamond
        String points = String.format(Locale.US, "%.2f,%.2f %.2f,%.2f %.2f,%.2f %.2f,%.2f",
                cx, cy - half, cx + half, cy, cx, cy + half, cx - half, cy);
        w.printf(Locale.US,
                "  <polygon points=\"%s\" fill=\"%s\"/>%n",
                points, svgColor(ColorPalette.STOCK_FILL));
        w.printf(Locale.US,
                "  <polygon points=\"%s\" fill=\"none\" stroke=\"%s\" stroke-width=\"1.5\"/>%n",
                points, svgColor(ColorPalette.AUX_BORDER));

        // Name below diamond
        w.printf(Locale.US,
                "  <text x=\"%.2f\" y=\"%.2f\" text-anchor=\"middle\" dominant-baseline=\"hanging\" " +
                "font-family=\"sans-serif\" font-size=\"11\" fill=\"%s\">%s</text>%n",
                cx, cy + half + LayoutMetrics.FLOW_NAME_GAP,
                svgColor(ColorPalette.TEXT), escapeXml(name));

        // Equation below name
        if (ElementRenderer.isDisplayableEquation(equation)) {
            w.printf(Locale.US,
                    "  <text x=\"%.2f\" y=\"%.2f\" text-anchor=\"middle\" dominant-baseline=\"hanging\" " +
                    "font-family=\"sans-serif\" font-size=\"9\" fill=\"%s\">%s</text>%n",
                    cx, cy + half + LayoutMetrics.FLOW_EQUATION_GAP,
                    svgColor(ColorPalette.TEXT_SECONDARY), escapeXml(equation));
        }
    }

    private static void writeAux(PrintWriter w, String name, String equation,
                                 double cx, double cy, double width, double height) {
        double x = cx - width / 2;
        double y = cy - height / 2;
        double r = LayoutMetrics.AUX_CORNER_RADIUS;

        // Fill
        w.printf(Locale.US,
                "  <rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" rx=\"%.1f\" " +
                "fill=\"%s\"/>%n",
                x, y, width, height, r, svgColor(ColorPalette.STOCK_FILL));

        // Border
        w.printf(Locale.US,
                "  <rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" rx=\"%.1f\" " +
                "fill=\"none\" stroke=\"%s\" stroke-width=\"%.1f\"/>%n",
                x, y, width, height, r,
                svgColor(ColorPalette.AUX_BORDER), LayoutMetrics.AUX_BORDER_WIDTH);

        // "fx" badge
        w.printf(Locale.US,
                "  <text x=\"%.2f\" y=\"%.2f\" text-anchor=\"start\" dominant-baseline=\"hanging\" " +
                "font-family=\"sans-serif\" font-size=\"9\" fill=\"%s\">fx</text>%n",
                x + 5, y + 3, svgColor(ColorPalette.TEXT_SECONDARY));

        // Name centered
        w.printf(Locale.US,
                "  <text x=\"%.2f\" y=\"%.2f\" text-anchor=\"middle\" dominant-baseline=\"central\" " +
                "font-family=\"sans-serif\" font-size=\"12\" fill=\"%s\">%s</text>%n",
                cx, cy + LayoutMetrics.LABEL_NAME_OFFSET,
                svgColor(ColorPalette.TEXT), escapeXml(name));

        // Equation
        if (ElementRenderer.isDisplayableEquation(equation)) {
            w.printf(Locale.US,
                    "  <text x=\"%.2f\" y=\"%.2f\" text-anchor=\"middle\" dominant-baseline=\"central\" " +
                    "font-family=\"sans-serif\" font-size=\"9\" fill=\"%s\">%s</text>%n",
                    cx, cy + LayoutMetrics.LABEL_SUBLABEL_OFFSET,
                    svgColor(ColorPalette.TEXT_SECONDARY), escapeXml(equation));
        }
    }

    private static void writeConstant(PrintWriter w, String name, double value,
                                      double cx, double cy, double width, double height) {
        double x = cx - width / 2;
        double y = cy - height / 2;
        double r = LayoutMetrics.CONSTANT_CORNER_RADIUS;

        // Fill
        w.printf(Locale.US,
                "  <rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" rx=\"%.1f\" " +
                "fill=\"%s\"/>%n",
                x, y, width, height, r, svgColor(ColorPalette.STOCK_FILL));

        // Dashed border
        w.printf(Locale.US,
                "  <rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" rx=\"%.1f\" " +
                "fill=\"none\" stroke=\"%s\" stroke-width=\"%.1f\" " +
                "stroke-dasharray=\"%.0f %.0f\"/>%n",
                x, y, width, height, r,
                svgColor(ColorPalette.CONSTANT_BORDER), LayoutMetrics.CONSTANT_BORDER_WIDTH,
                LayoutMetrics.CONSTANT_DASH_LENGTH, LayoutMetrics.CONSTANT_DASH_GAP);

        // "pin" badge
        w.printf(Locale.US,
                "  <text x=\"%.2f\" y=\"%.2f\" text-anchor=\"start\" dominant-baseline=\"hanging\" " +
                "font-family=\"sans-serif\" font-size=\"9\" fill=\"%s\">pin</text>%n",
                x + 4, y + 3, svgColor(ColorPalette.TEXT_SECONDARY));

        // Name centered
        w.printf(Locale.US,
                "  <text x=\"%.2f\" y=\"%.2f\" text-anchor=\"middle\" dominant-baseline=\"central\" " +
                "font-family=\"sans-serif\" font-size=\"11\" fill=\"%s\">%s</text>%n",
                cx, cy + LayoutMetrics.LABEL_NAME_OFFSET,
                svgColor(ColorPalette.TEXT), escapeXml(name));

        // Value
        w.printf(Locale.US,
                "  <text x=\"%.2f\" y=\"%.2f\" text-anchor=\"middle\" dominant-baseline=\"central\" " +
                "font-family=\"sans-serif\" font-size=\"9\" fill=\"%s\">%s</text>%n",
                cx, cy + LayoutMetrics.LABEL_SUBLABEL_OFFSET,
                svgColor(ColorPalette.TEXT_SECONDARY), escapeXml(ElementRenderer.formatValue(value)));
    }

    private static void writeModule(PrintWriter w, String name,
                                    double cx, double cy, double width, double height) {
        double x = cx - width / 2;
        double y = cy - height / 2;
        double r = LayoutMetrics.MODULE_CORNER_RADIUS;

        // Fill
        w.printf(Locale.US,
                "  <rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" rx=\"%.1f\" " +
                "fill=\"%s\"/>%n",
                x, y, width, height, r, svgColor(ColorPalette.STOCK_FILL));

        // Border
        w.printf(Locale.US,
                "  <rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" rx=\"%.1f\" " +
                "fill=\"none\" stroke=\"%s\" stroke-width=\"%.1f\"/>%n",
                x, y, width, height, r,
                svgColor(ColorPalette.STOCK_BORDER), LayoutMetrics.MODULE_BORDER_WIDTH);

        // "mod" badge
        w.printf(Locale.US,
                "  <text x=\"%.2f\" y=\"%.2f\" text-anchor=\"start\" dominant-baseline=\"hanging\" " +
                "font-family=\"sans-serif\" font-size=\"9\" fill=\"%s\">mod</text>%n",
                x + 5, y + 3, svgColor(ColorPalette.TEXT_SECONDARY));

        // Name centered
        w.printf(Locale.US,
                "  <text x=\"%.2f\" y=\"%.2f\" text-anchor=\"middle\" dominant-baseline=\"central\" " +
                "font-family=\"sans-serif\" font-size=\"13\" font-weight=\"bold\" fill=\"%s\">%s</text>%n",
                cx, cy, svgColor(ColorPalette.TEXT), escapeXml(name));
    }

    private static void writeLookup(PrintWriter w, String name, int dataPoints,
                                     double cx, double cy, double width, double height) {
        double x = cx - width / 2;
        double y = cy - height / 2;
        double r = LayoutMetrics.LOOKUP_CORNER_RADIUS;

        // Fill
        w.printf(Locale.US,
                "  <rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" rx=\"%.1f\" " +
                "fill=\"%s\"/>%n",
                x, y, width, height, r, svgColor(ColorPalette.STOCK_FILL));

        // Dot-dash border
        w.printf(Locale.US,
                "  <rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" rx=\"%.1f\" " +
                "fill=\"none\" stroke=\"%s\" stroke-width=\"%.1f\" " +
                "stroke-dasharray=\"8 3 2 3\"/>%n",
                x, y, width, height, r,
                svgColor(ColorPalette.AUX_BORDER), LayoutMetrics.LOOKUP_BORDER_WIDTH);

        // "tbl" badge
        w.printf(Locale.US,
                "  <text x=\"%.2f\" y=\"%.2f\" text-anchor=\"start\" dominant-baseline=\"hanging\" " +
                "font-family=\"sans-serif\" font-size=\"9\" fill=\"%s\">tbl</text>%n",
                x + 4, y + 3, svgColor(ColorPalette.TEXT_SECONDARY));

        // Name centered
        w.printf(Locale.US,
                "  <text x=\"%.2f\" y=\"%.2f\" text-anchor=\"middle\" dominant-baseline=\"central\" " +
                "font-family=\"sans-serif\" font-size=\"11\" fill=\"%s\">%s</text>%n",
                cx, cy + LayoutMetrics.LABEL_NAME_OFFSET,
                svgColor(ColorPalette.TEXT), escapeXml(name));

        // Data point count
        w.printf(Locale.US,
                "  <text x=\"%.2f\" y=\"%.2f\" text-anchor=\"middle\" dominant-baseline=\"central\" " +
                "font-family=\"sans-serif\" font-size=\"9\" fill=\"%s\">%s</text>%n",
                cx, cy + LayoutMetrics.LABEL_SUBLABEL_OFFSET,
                svgColor(ColorPalette.TEXT_SECONDARY), dataPoints + " pts");
    }

    // --- Loop highlights ---

    private static void writeLoopHighlight(PrintWriter w, CanvasState state, String name) {
        ElementType type = state.getType(name);
        double cx = state.getX(name);
        double cy = state.getY(name);

        if (type == null || Double.isNaN(cx) || Double.isNaN(cy)) {
            return;
        }

        double glowPadding = 6;

        if (type == ElementType.FLOW) {
            double half = LayoutMetrics.FLOW_INDICATOR_SIZE / 2 + glowPadding;
            String points = String.format(Locale.US, "%.2f,%.2f %.2f,%.2f %.2f,%.2f %.2f,%.2f",
                    cx, cy - half, cx + half, cy, cx, cy + half, cx - half, cy);
            w.printf(Locale.US,
                    "  <polygon points=\"%s\" fill=\"%s\" fill-opacity=\"%.2f\" " +
                    "stroke=\"%s\" stroke-opacity=\"%.2f\" stroke-width=\"2.5\"/>%n",
                    points,
                    svgColor(ColorPalette.LOOP_FILL), svgOpacity(ColorPalette.LOOP_FILL),
                    svgColor(ColorPalette.LOOP_HIGHLIGHT), svgOpacity(ColorPalette.LOOP_HIGHLIGHT));
        } else {
            double halfW = LayoutMetrics.effectiveWidth(state, name) / 2 + glowPadding;
            double halfH = LayoutMetrics.effectiveHeight(state, name) / 2 + glowPadding;
            w.printf(Locale.US,
                    "  <rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" " +
                    "fill=\"%s\" fill-opacity=\"%.2f\" " +
                    "stroke=\"%s\" stroke-opacity=\"%.2f\" stroke-width=\"2.5\"/>%n",
                    cx - halfW, cy - halfH, halfW * 2, halfH * 2,
                    svgColor(ColorPalette.LOOP_FILL), svgOpacity(ColorPalette.LOOP_FILL),
                    svgColor(ColorPalette.LOOP_HIGHLIGHT), svgOpacity(ColorPalette.LOOP_HIGHLIGHT));
        }
    }

    // --- Shared helpers ---

    private static void writeCloud(PrintWriter w, double cx, double cy) {
        double r = LayoutMetrics.CLOUD_RADIUS;
        w.printf(Locale.US,
                "  <circle cx=\"%.2f\" cy=\"%.2f\" r=\"%.1f\" fill=\"none\" stroke=\"%s\" " +
                "stroke-width=\"%.1f\"/>%n",
                cx, cy, r, svgColor(ColorPalette.CLOUD), LayoutMetrics.CLOUD_LINE_WIDTH);
        w.printf(Locale.US,
                "  <text x=\"%.2f\" y=\"%.2f\" text-anchor=\"middle\" dominant-baseline=\"central\" " +
                "font-family=\"sans-serif\" font-size=\"9\" fill=\"%s\">~</text>%n",
                cx, cy, svgColor(ColorPalette.CLOUD));
    }

    private static void writeArrowhead(PrintWriter w, double fromX, double fromY,
                                       double toX, double toY,
                                       double length, double arrowWidth, Color color) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 1) {
            return;
        }

        double ux = dx / dist;
        double uy = dy / dist;

        double baseX = toX - ux * length;
        double baseY = toY - uy * length;

        double perpX = -uy * arrowWidth / 2;
        double perpY = ux * arrowWidth / 2;

        String points = String.format(Locale.US, "%.2f,%.2f %.2f,%.2f %.2f,%.2f",
                toX, toY, baseX + perpX, baseY + perpY, baseX - perpX, baseY - perpY);

        double opacity = svgOpacity(color);
        if (opacity < 1.0) {
            w.printf(Locale.US,
                    "  <polygon points=\"%s\" fill=\"%s\" fill-opacity=\"%.2f\"/>%n",
                    points, svgColor(color), opacity);
        } else {
            w.printf(Locale.US,
                    "  <polygon points=\"%s\" fill=\"%s\"/>%n",
                    points, svgColor(color));
        }
    }

    /**
     * Converts a JavaFX Color to an SVG hex color string (without opacity).
     */
    static String svgColor(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    /**
     * Returns the opacity component of a JavaFX Color.
     */
    static double svgOpacity(Color color) {
        return color.getOpacity();
    }

    static String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}
