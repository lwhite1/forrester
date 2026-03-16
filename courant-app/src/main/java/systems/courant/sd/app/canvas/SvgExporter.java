package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.VariableDef;
import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.def.ConnectorRoute;
import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.graph.FeedbackAnalysis;
import systems.courant.sd.model.graph.FeedbackAnalysis.CausalLoop;

import javafx.scene.paint.Color;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import systems.courant.sd.app.canvas.renderers.CanvasRenderer;
import systems.courant.sd.app.canvas.renderers.ElementRenderer;

/**
 * Exports the diagram to an SVG file by translating canvas drawing operations
 * into SVG XML elements. Follows the same layer order as {@link CanvasRenderer}.
 */
public final class SvgExporter {

    private SvgExporter() {
    }

    /**
     * Returns the diagram as an SVG string, or null if the diagram is empty.
     */
    public static String toSvgString(CanvasState canvasState, ModelEditor editor,
                                     List<ConnectorRoute> connectors,
                                     FeedbackAnalysis loopAnalysis) {
        if (canvasState.getDrawOrder().isEmpty()) {
            return null;
        }
        StringWriter sw = new StringWriter(4096);
        try (PrintWriter w = new PrintWriter(sw)) {
            writeSvg(w, canvasState, editor, connectors, loopAnalysis);
        }
        return sw.toString();
    }

    public static void export(CanvasState canvasState, ModelEditor editor,
                              List<ConnectorRoute> connectors,
                              FeedbackAnalysis loopAnalysis,
                              File file) throws IOException {
        if (canvasState.getDrawOrder().isEmpty()) {
            return;
        }
        try (PrintWriter w = new PrintWriter(file, StandardCharsets.UTF_8)) {
            writeSvg(w, canvasState, editor, connectors, loopAnalysis);
        }
    }

    private static void writeSvg(PrintWriter w, CanvasState canvasState, ModelEditor editor,
                                 List<ConnectorRoute> connectors,
                                 FeedbackAnalysis loopAnalysis) {
        ExportBounds.Bounds bounds = ExportBounds.compute(canvasState, editor);
        double minX = bounds.minX();
        double minY = bounds.minY();
        double width = bounds.width();
        double height = bounds.height();

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

        // 3b. Causal links
        writeCausalLinks(w, canvasState, editor);

        // 4. Loop edge highlights
        if (loopAnalysis != null) {
            writeLoopEdges(w, canvasState, connectors, editor, loopAnalysis);
        }

        // 5. Elements
        for (String name : canvasState.getDrawOrder()) {
            ElementType type = canvasState.getType(name).orElse(null);
            if (type == null) {
                continue;
            }
            double cx = canvasState.getX(name);
            double cy = canvasState.getY(name);

            switch (type) {
                case STOCK -> writeStock(w, name, editor.getStockUnit(name).orElse(null), cx, cy,
                        LayoutMetrics.effectiveWidth(canvasState, name),
                        LayoutMetrics.effectiveHeight(canvasState, name));
                case FLOW -> {
                    String eq = editor.getFlowEquation(name).orElse(null);
                    writeFlow(w, name, eq, cx, cy);
                }
                case AUX -> {
                    boolean isLiteral = editor.getVariableByName(name)
                            .map(VariableDef::isLiteral).orElse(false);
                    writeVariable(w, name, editor.getVariableEquation(name).orElse(null),
                            isLiteral, cx, cy,
                            LayoutMetrics.effectiveWidth(canvasState, name),
                            LayoutMetrics.effectiveHeight(canvasState, name));
                }
                case MODULE -> writeModule(w, name, cx, cy,
                        LayoutMetrics.effectiveWidth(canvasState, name),
                        LayoutMetrics.effectiveHeight(canvasState, name));
                case CLD_VARIABLE -> writeCldVariable(w, name, cx, cy,
                        LayoutMetrics.effectiveWidth(canvasState, name),
                        LayoutMetrics.effectiveHeight(canvasState, name));
                case LOOKUP -> writeLookup(w, name, cx, cy,
                        LayoutMetrics.effectiveWidth(canvasState, name),
                        LayoutMetrics.effectiveHeight(canvasState, name));
                case COMMENT -> {
                    var commentDef = editor.getCommentByName(name);
                    String text = commentDef != null ? commentDef.text() : "";
                    writeComment(w, text, cx, cy,
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

            // Source → diamond (no arrowhead — full length)
            w.printf(Locale.US,
                    "  <line x1=\"%.2f\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\" " +
                    "stroke=\"%s\" stroke-width=\"%.1f\"/>%n",
                    sourceX, sourceY, midX, midY,
                    svgColor(ColorPalette.MATERIAL_FLOW), LayoutMetrics.MATERIAL_FLOW_WIDTH);

            // Diamond → sink: stop line at arrowhead base
            double sinkDx = sinkX - midX;
            double sinkDy = sinkY - midY;
            double sinkDist = Math.sqrt(sinkDx * sinkDx + sinkDy * sinkDy);
            double lineEndX = sinkX;
            double lineEndY = sinkY;
            if (sinkDist > LayoutMetrics.ARROWHEAD_LENGTH) {
                double ux = sinkDx / sinkDist;
                double uy = sinkDy / sinkDist;
                lineEndX = sinkX - ux * LayoutMetrics.ARROWHEAD_LENGTH;
                lineEndY = sinkY - uy * LayoutMetrics.ARROWHEAD_LENGTH;
            }
            w.printf(Locale.US,
                    "  <line x1=\"%.2f\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\" " +
                    "stroke=\"%s\" stroke-width=\"%.1f\"/>%n",
                    midX, midY, lineEndX, lineEndY,
                    svgColor(ColorPalette.MATERIAL_FLOW), LayoutMetrics.MATERIAL_FLOW_WIDTH);

            // Arrowhead fills gap from lineEnd to sink
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

            // Stop line at arrowhead base
            double ldx = ct.x() - cf.x();
            double ldy = ct.y() - cf.y();
            double ldist = Math.sqrt(ldx * ldx + ldy * ldy);
            double ltx = ct.x();
            double lty = ct.y();
            if (ldist > LayoutMetrics.INFO_ARROWHEAD_LENGTH) {
                double lux = ldx / ldist;
                double luy = ldy / ldist;
                ltx = ct.x() - lux * LayoutMetrics.INFO_ARROWHEAD_LENGTH;
                lty = ct.y() - luy * LayoutMetrics.INFO_ARROWHEAD_LENGTH;
            }

            w.printf(Locale.US,
                    "  <line x1=\"%.2f\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\" " +
                    "stroke=\"%s\" stroke-opacity=\"%.2f\" stroke-width=\"%.1f\" " +
                    "stroke-dasharray=\"%.0f %.0f\"/>%n",
                    cf.x(), cf.y(), ltx, lty,
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
                    "stroke=\"%s\" stroke-opacity=\"%.2f\" stroke-width=\"%.1f\"/>%n",
                    cf.x(), cf.y(), ct.x(), ct.y(),
                    svgColor(ColorPalette.LOOP_EDGE), svgOpacity(ColorPalette.LOOP_EDGE),
                    LayoutMetrics.LOOP_EDGE_LINE_WIDTH);
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
                        "stroke=\"%s\" stroke-opacity=\"%.2f\" stroke-width=\"%.1f\"/>%n",
                        midX, midY, edge.x(), edge.y(),
                        svgColor(ColorPalette.LOOP_EDGE), svgOpacity(ColorPalette.LOOP_EDGE),
                        LayoutMetrics.LOOP_EDGE_LINE_WIDTH);
            }

            if (flow.sink() != null && state.hasElement(flow.sink())
                    && loopAnalysis.isLoopEdge(flow.name(), flow.sink())) {
                FlowGeometry.Point2D edge = FlowGeometry.clipToElement(
                        state, flow.sink(), midX, midY);
                w.printf(Locale.US,
                        "  <line x1=\"%.2f\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\" " +
                        "stroke=\"%s\" stroke-opacity=\"%.2f\" stroke-width=\"%.1f\"/>%n",
                        midX, midY, edge.x(), edge.y(),
                        svgColor(ColorPalette.LOOP_EDGE), svgOpacity(ColorPalette.LOOP_EDGE),
                        LayoutMetrics.LOOP_EDGE_LINE_WIDTH);
            }
        }

        // Causal link loop edges
        for (CausalLinkDef link : editor.getCausalLinks()) {
            if (!state.hasElement(link.from()) || !state.hasElement(link.to())) {
                continue;
            }
            if (!loopAnalysis.isLoopEdge(link.from(), link.to())) {
                continue;
            }

            double fromX = state.getX(link.from());
            double fromY = state.getY(link.from());
            double toX = state.getX(link.to());
            double toY = state.getY(link.to());

            FlowGeometry.Point2D cf = FlowGeometry.clipToElement(state, link.from(), toX, toY);
            FlowGeometry.Point2D ct = FlowGeometry.clipToElement(state, link.to(), fromX, fromY);

            w.printf(Locale.US,
                    "  <line x1=\"%.2f\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\" " +
                    "stroke=\"%s\" stroke-opacity=\"%.2f\" stroke-width=\"%.1f\"/>%n",
                    cf.x(), cf.y(), ct.x(), ct.y(),
                    svgColor(ColorPalette.LOOP_EDGE), svgOpacity(ColorPalette.LOOP_EDGE),
                    LayoutMetrics.LOOP_EDGE_LINE_WIDTH);
        }

        // Loop type labels (CLD loops only, not S&F feedback groups)
        for (CausalLoop loop : loopAnalysis.causalLoops()) {
            if (loop.type() == FeedbackAnalysis.LoopType.INDETERMINATE) {
                continue;
            }
            double sumX = 0;
            double sumY = 0;
            int count = 0;
            for (String name : loop.path()) {
                if (state.hasElement(name)) {
                    sumX += state.getX(name);
                    sumY += state.getY(name);
                    count++;
                }
            }
            if (count > 0) {
                double cx = sumX / count;
                double cy = sumY / count;
                Color color = switch (loop.type()) {
                    case REINFORCING -> ColorPalette.LOOP_REINFORCING;
                    case BALANCING -> ColorPalette.LOOP_BALANCING;
                    case INDETERMINATE -> ColorPalette.LOOP_INDETERMINATE;
                };
                w.printf(Locale.US,
                        "  <text x=\"%.2f\" y=\"%.2f\" text-anchor=\"middle\" " +
                        "dominant-baseline=\"central\" font-weight=\"bold\" font-size=\"%.0f\" " +
                        "fill=\"%s\">%s</text>%n",
                        cx, cy, LayoutMetrics.LOOP_LABEL_FONT_SIZE, svgColor(color), escapeXml(loop.label()));
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

        // Name centered (truncated to fit)
        String stockLabel = ElementRenderer.truncate(name, LayoutMetrics.STOCK_NAME_FONT, width - 12);
        w.printf(Locale.US,
                "  <text x=\"%.2f\" y=\"%.2f\" text-anchor=\"middle\" dominant-baseline=\"central\" " +
                "font-family=\"sans-serif\" font-size=\"%.0f\" font-weight=\"bold\" fill=\"%s\">%s</text>%n",
                cx, cy, LayoutMetrics.STOCK_NAME_FONT_SIZE, svgColor(ColorPalette.TEXT), escapeXml(stockLabel));

        // Unit badge
        if (unit != null && !unit.isBlank()) {
            w.printf(Locale.US,
                    "  <text x=\"%.2f\" y=\"%.2f\" text-anchor=\"end\" dominant-baseline=\"auto\" " +
                    "font-family=\"sans-serif\" font-size=\"%.0f\" fill=\"%s\">%s</text>%n",
                    x + width - 6, y + height - 4,
                    LayoutMetrics.BADGE_FONT_SIZE, svgColor(ColorPalette.TEXT_SECONDARY), escapeXml(unit));
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
                points, svgColor(ColorPalette.ELEMENT_FILL));
        w.printf(Locale.US,
                "  <polygon points=\"%s\" fill=\"none\" stroke=\"%s\" stroke-width=\"1.5\"/>%n",
                points, svgColor(ColorPalette.AUX_BORDER));

        // Name below diamond (truncated)
        String flowLabel = ElementRenderer.truncate(name, LayoutMetrics.FLOW_NAME_FONT,
                LayoutMetrics.FLOW_LABEL_MAX_WIDTH);
        w.printf(Locale.US,
                "  <text x=\"%.2f\" y=\"%.2f\" text-anchor=\"middle\" dominant-baseline=\"hanging\" " +
                "font-family=\"sans-serif\" font-size=\"%.0f\" fill=\"%s\">%s</text>%n",
                cx, cy + half + LayoutMetrics.FLOW_NAME_GAP,
                LayoutMetrics.FLOW_NAME_FONT_SIZE, svgColor(ColorPalette.TEXT), escapeXml(flowLabel));
    }

    private static void writeVariable(PrintWriter w, String name, String equation,
                                 boolean isLiteral, double cx, double cy,
                                 double width, double height) {
        double x = cx - width / 2;
        double y = cy - height / 2;
        double r = LayoutMetrics.AUX_CORNER_RADIUS;

        // Fill — subtle gray, no border
        w.printf(Locale.US,
                "  <rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" rx=\"%.1f\" " +
                "fill=\"%s\"/>%n",
                x, y, width, height, r, svgColor(ColorPalette.VARIABLE_FILL));

        // Badge: value for literals, "fx" for formulas
        String badge;
        if (isLiteral && equation != null) {
            try {
                double value = Double.parseDouble(equation.strip());
                badge = ElementRenderer.formatValue(value);
            } catch (NumberFormatException e) {
                badge = equation.strip();
            }
        } else {
            badge = ElementRenderer.BADGE_FORMULA;
        }
        w.printf(Locale.US,
                "  <text x=\"%.2f\" y=\"%.2f\" text-anchor=\"start\" dominant-baseline=\"hanging\" " +
                "font-family=\"sans-serif\" font-size=\"%.0f\" fill=\"%s\">%s</text>%n",
                x + 5, y + 3, LayoutMetrics.BADGE_FONT_SIZE, svgColor(ColorPalette.TEXT_SECONDARY), escapeXml(badge));

        // Name centered (truncated to fit)
        String auxLabel = ElementRenderer.truncate(name, LayoutMetrics.AUX_NAME_FONT, width - 20);
        w.printf(Locale.US,
                "  <text x=\"%.2f\" y=\"%.2f\" text-anchor=\"middle\" dominant-baseline=\"central\" " +
                "font-family=\"sans-serif\" font-size=\"%.0f\" fill=\"%s\">%s</text>%n",
                cx, cy, LayoutMetrics.AUX_NAME_FONT_SIZE, svgColor(ColorPalette.TEXT), escapeXml(auxLabel));
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
                x, y, width, height, r, svgColor(ColorPalette.ELEMENT_FILL));

        // Border
        w.printf(Locale.US,
                "  <rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" rx=\"%.1f\" " +
                "fill=\"none\" stroke=\"%s\" stroke-width=\"%.1f\"/>%n",
                x, y, width, height, r,
                svgColor(ColorPalette.STOCK_BORDER), LayoutMetrics.MODULE_BORDER_WIDTH);

        // Module badge
        w.printf(Locale.US,
                "  <text x=\"%.2f\" y=\"%.2f\" text-anchor=\"start\" dominant-baseline=\"hanging\" " +
                "font-family=\"sans-serif\" font-size=\"%.0f\" fill=\"%s\">%s</text>%n",
                x + 5, y + 3, LayoutMetrics.BADGE_FONT_SIZE,
                svgColor(ColorPalette.TEXT_SECONDARY), ElementRenderer.BADGE_MODULE);

        // Name centered (truncated to fit)
        String modLabel = ElementRenderer.truncate(name, LayoutMetrics.MODULE_NAME_FONT, width - 12);
        w.printf(Locale.US,
                "  <text x=\"%.2f\" y=\"%.2f\" text-anchor=\"middle\" dominant-baseline=\"central\" " +
                "font-family=\"sans-serif\" font-size=\"%.0f\" font-weight=\"bold\" fill=\"%s\">%s</text>%n",
                cx, cy, LayoutMetrics.MODULE_NAME_FONT_SIZE, svgColor(ColorPalette.TEXT), escapeXml(modLabel));
    }

    private static void writeLookup(PrintWriter w, String name,
                                     double cx, double cy, double width, double height) {
        double x = cx - width / 2;
        double y = cy - height / 2;
        double r = LayoutMetrics.LOOKUP_CORNER_RADIUS;

        // Fill — subtle gray, no border
        w.printf(Locale.US,
                "  <rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" rx=\"%.1f\" " +
                "fill=\"%s\"/>%n",
                x, y, width, height, r, svgColor(ColorPalette.LOOKUP_FILL));

        // Table badge
        w.printf(Locale.US,
                "  <text x=\"%.2f\" y=\"%.2f\" text-anchor=\"start\" dominant-baseline=\"hanging\" " +
                "font-family=\"sans-serif\" font-size=\"%.0f\" fill=\"%s\">%s</text>%n",
                x + 4, y + 3, LayoutMetrics.BADGE_FONT_SIZE,
                svgColor(ColorPalette.TEXT_SECONDARY), ElementRenderer.BADGE_LOOKUP);

        // Name centered vertically (truncated to fit)
        String lookupLabel = ElementRenderer.truncate(name, LayoutMetrics.LOOKUP_NAME_FONT, width - 16);
        w.printf(Locale.US,
                "  <text x=\"%.2f\" y=\"%.2f\" text-anchor=\"middle\" dominant-baseline=\"central\" " +
                "font-family=\"sans-serif\" font-size=\"%.0f\" fill=\"%s\">%s</text>%n",
                cx, cy,
                LayoutMetrics.LOOKUP_NAME_FONT_SIZE, svgColor(ColorPalette.TEXT), escapeXml(lookupLabel));
    }

    private static void writeCldVariable(PrintWriter w, String name,
                                          double cx, double cy, double width, double height) {
        // Plain text only — no rectangle, matching standard CLD notation
        String label = ElementRenderer.truncate(name, LayoutMetrics.AUX_NAME_FONT, width - 12);
        w.printf(Locale.US,
                "  <text x=\"%.2f\" y=\"%.2f\" text-anchor=\"middle\" dominant-baseline=\"central\" " +
                "font-family=\"sans-serif\" font-size=\"%.0f\" fill=\"%s\">%s</text>%n",
                cx, cy, LayoutMetrics.AUX_NAME_FONT_SIZE, svgColor(ColorPalette.TEXT), escapeXml(label));
    }

    private static void writeComment(PrintWriter w, String text,
                                      double cx, double cy, double width, double height) {
        double x = cx - width / 2;
        double y = cy - height / 2;
        double r = LayoutMetrics.COMMENT_CORNER_RADIUS;

        // Left accent bar
        w.printf(Locale.US,
                "  <rect x=\"%.2f\" y=\"%.2f\" width=\"%.1f\" height=\"%.2f\" " +
                "fill=\"%s\" fill-opacity=\"%.2f\"/>%n",
                x, y + r, LayoutMetrics.COMMENT_ACCENT_WIDTH, height - r * 2,
                svgColor(ColorPalette.COMMENT_ACCENT), svgOpacity(ColorPalette.COMMENT_ACCENT));

        if (text != null && !text.isBlank()) {
            String display = ElementRenderer.truncate(
                    text, LayoutMetrics.COMMENT_TEXT_FONT, width - 12);
            w.printf(Locale.US,
                    "  <text x=\"%.2f\" y=\"%.2f\" text-anchor=\"start\" dominant-baseline=\"hanging\" " +
                    "font-family=\"sans-serif\" font-size=\"%.0f\" fill=\"%s\">%s</text>%n",
                    x + 6, y + 6, LayoutMetrics.COMMENT_TEXT_FONT_SIZE,
                    svgColor(ColorPalette.COMMENT_TEXT), escapeXml(display));
        }
    }

    // --- Causal links ---

    private static void writeCausalLinks(PrintWriter w, CanvasState state, ModelEditor editor) {
        List<CausalLinkDef> allLinks = editor.getCausalLinks();

        for (CausalLinkDef link : allLinks) {
            if (!state.hasElement(link.from()) || !state.hasElement(link.to())) {
                continue;
            }

            double fromX = state.getX(link.from());
            double fromY = state.getY(link.from());

            // Self-loop
            if (link.from().equals(link.to())) {
                double halfW = LayoutMetrics.effectiveWidth(state, link.from()) / 2;
                double halfH = LayoutMetrics.effectiveHeight(state, link.from()) / 2;
                double[] lp = CausalLinkGeometry.selfLoopPoints(fromX, fromY, halfW, halfH);

                // Cubic Bézier path
                w.printf(Locale.US,
                        "  <path d=\"M %.2f %.2f C %.2f %.2f, %.2f %.2f, %.2f %.2f\" " +
                        "fill=\"none\" stroke=\"%s\" stroke-width=\"%.1f\"/>%n",
                        lp[0], lp[1], lp[2], lp[3], lp[4], lp[5], lp[6], lp[7],
                        svgColor(ColorPalette.CAUSAL_LINK), LayoutMetrics.CAUSAL_LINK_WIDTH);

                // Arrowhead at end
                double[] tan = CausalLinkGeometry.tangentCubic(
                        lp[0], lp[1], lp[2], lp[3], lp[4], lp[5], lp[6], lp[7], 1.0);
                writeArrowheadFromTangent(w, lp[6], lp[7], tan[0], tan[1],
                        LayoutMetrics.CAUSAL_ARROWHEAD_LENGTH, LayoutMetrics.CAUSAL_ARROWHEAD_WIDTH,
                        ColorPalette.CAUSAL_LINK);

                // Polarity label at top of loop
                double[] midPt = CausalLinkGeometry.evaluateCubic(
                        lp[0], lp[1], lp[2], lp[3], lp[4], lp[5], lp[6], lp[7], 0.5);
                writePolarityLabel(w, link.polarity(), midPt[0], midPt[1] - 10);
                continue;
            }

            double toX = state.getX(link.to());
            double toY = state.getY(link.to());

            CausalLinkGeometry.ControlPoint cp = CausalLinkGeometry.controlPoint(
                    fromX, fromY, toX, toY, link.from(), link.to(), allLinks);

            FlowGeometry.Point2D cf = FlowGeometry.clipToElement(state, link.from(), cp.x(), cp.y());
            FlowGeometry.Point2D ct = FlowGeometry.clipToElement(state, link.to(), cp.x(), cp.y());

            // Quadratic Bézier path
            w.printf(Locale.US,
                    "  <path d=\"M %.2f %.2f Q %.2f %.2f, %.2f %.2f\" " +
                    "fill=\"none\" stroke=\"%s\" stroke-width=\"%.1f\"/>%n",
                    cf.x(), cf.y(), cp.x(), cp.y(), ct.x(), ct.y(),
                    svgColor(ColorPalette.CAUSAL_LINK), LayoutMetrics.CAUSAL_LINK_WIDTH);

            // Arrowhead oriented along curve tangent at endpoint
            double[] tan = CausalLinkGeometry.tangent(
                    cf.x(), cf.y(), cp.x(), cp.y(), ct.x(), ct.y(), 1.0);
            writeArrowheadFromTangent(w, ct.x(), ct.y(), tan[0], tan[1],
                    LayoutMetrics.CAUSAL_ARROWHEAD_LENGTH, LayoutMetrics.CAUSAL_ARROWHEAD_WIDTH,
                    ColorPalette.CAUSAL_LINK);

            // Polarity label at t=0.8 on the curve, offset perpendicular
            double dx = ct.x() - cf.x();
            double dy = ct.y() - cf.y();
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist > 1) {
                double[] labelPt = CausalLinkGeometry.evaluate(
                        cf.x(), cf.y(), cp.x(), cp.y(), ct.x(), ct.y(), 0.8);
                double[] labelTan = CausalLinkGeometry.tangent(
                        cf.x(), cf.y(), cp.x(), cp.y(), ct.x(), ct.y(), 0.8);
                double tanDist = Math.sqrt(labelTan[0] * labelTan[0] + labelTan[1] * labelTan[1]);
                if (tanDist > 0) {
                    double perpX = -labelTan[1] / tanDist;
                    double perpY = labelTan[0] / tanDist;
                    double perpOffset = 12;
                    writePolarityLabel(w, link.polarity(),
                            labelPt[0] + perpX * perpOffset,
                            labelPt[1] + perpY * perpOffset);
                }
            }
        }
    }

    private static void writePolarityLabel(PrintWriter w, CausalLinkDef.Polarity polarity,
                                            double x, double y) {
        Color labelColor = switch (polarity) {
            case POSITIVE -> ColorPalette.CAUSAL_POSITIVE;
            case NEGATIVE -> ColorPalette.CAUSAL_NEGATIVE;
            case UNKNOWN -> ColorPalette.CAUSAL_UNKNOWN;
        };
        w.printf(Locale.US,
                "  <text x=\"%.2f\" y=\"%.2f\" text-anchor=\"middle\" " +
                "dominant-baseline=\"central\" font-weight=\"bold\" font-size=\"%.0f\" " +
                "fill=\"%s\">%s</text>%n",
                x, y, LayoutMetrics.CAUSAL_POLARITY_FONT_SIZE, svgColor(labelColor), escapeXml(polarity.symbol()));
    }

    private static void writeArrowheadFromTangent(PrintWriter w,
                                                   double tipX, double tipY,
                                                   double tanX, double tanY,
                                                   double length, double width,
                                                   Color color) {
        double dist = Math.sqrt(tanX * tanX + tanY * tanY);
        if (dist < 1e-9) {
            return;
        }
        double ux = tanX / dist;
        double uy = tanY / dist;
        double baseX = tipX - ux * length;
        double baseY = tipY - uy * length;
        double perpX = -uy * width / 2;
        double perpY = ux * width / 2;

        w.printf(Locale.US,
                "  <polygon points=\"%.2f,%.2f %.2f,%.2f %.2f,%.2f\" fill=\"%s\"/>%n",
                tipX, tipY,
                baseX + perpX, baseY + perpY,
                baseX - perpX, baseY - perpY,
                svgColor(color));
    }

    // --- Loop highlights ---

    private static void writeLoopHighlight(PrintWriter w, CanvasState state, String name) {
        ElementType type = state.getType(name).orElse(null);
        double cx = state.getX(name);
        double cy = state.getY(name);

        if (type == null || Double.isNaN(cx) || Double.isNaN(cy)) {
            return;
        }

        double glowPadding = LayoutMetrics.LOOP_GLOW_PADDING;

        if (type == ElementType.FLOW) {
            double half = LayoutMetrics.FLOW_INDICATOR_SIZE / 2 + glowPadding;
            String points = String.format(Locale.US, "%.2f,%.2f %.2f,%.2f %.2f,%.2f %.2f,%.2f",
                    cx, cy - half, cx + half, cy, cx, cy + half, cx - half, cy);
            w.printf(Locale.US,
                    "  <polygon points=\"%s\" fill=\"%s\" fill-opacity=\"%.2f\" " +
                    "stroke=\"%s\" stroke-opacity=\"%.2f\" stroke-width=\"%.1f\"/>%n",
                    points,
                    svgColor(ColorPalette.LOOP_FILL), svgOpacity(ColorPalette.LOOP_FILL),
                    svgColor(ColorPalette.LOOP_HIGHLIGHT), svgOpacity(ColorPalette.LOOP_HIGHLIGHT),
                    LayoutMetrics.LOOP_GLOW_LINE_WIDTH);
        } else {
            double halfW = LayoutMetrics.effectiveWidth(state, name) / 2 + glowPadding;
            double halfH = LayoutMetrics.effectiveHeight(state, name) / 2 + glowPadding;
            w.printf(Locale.US,
                    "  <rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" " +
                    "fill=\"%s\" fill-opacity=\"%.2f\" " +
                    "stroke=\"%s\" stroke-opacity=\"%.2f\" stroke-width=\"%.1f\"/>%n",
                    cx - halfW, cy - halfH, halfW * 2, halfH * 2,
                    svgColor(ColorPalette.LOOP_FILL), svgOpacity(ColorPalette.LOOP_FILL),
                    svgColor(ColorPalette.LOOP_HIGHLIGHT), svgOpacity(ColorPalette.LOOP_HIGHLIGHT),
                    LayoutMetrics.LOOP_GLOW_LINE_WIDTH);
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
                "font-family=\"sans-serif\" font-size=\"%.0f\" fill=\"%s\">~</text>%n",
                cx, cy, LayoutMetrics.BADGE_FONT_SIZE, svgColor(ColorPalette.CLOUD));
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
