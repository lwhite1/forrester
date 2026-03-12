package systems.courant.shrewd.app.canvas;

import systems.courant.shrewd.model.ModelMetadata;
import systems.courant.shrewd.model.def.VariableDef;
import systems.courant.shrewd.model.def.FlowDef;
import systems.courant.shrewd.model.def.LookupTableDef;
import systems.courant.shrewd.model.def.ModelDefinition;
import systems.courant.shrewd.model.def.ModuleInstanceDef;
import systems.courant.shrewd.model.def.SimulationSettings;
import systems.courant.shrewd.model.def.StockDef;
import systems.courant.shrewd.model.def.SubscriptDef;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Generates a self-contained HTML report from a {@link ModelDefinition} and optional
 * embedded SVG diagram. The output is a single HTML file with inline CSS that can be
 * opened in any browser and printed/saved as PDF.
 */
public final class ReportGenerator {

    /** Sections that can be included or excluded from the report. */
    public enum Section {
        MODEL_INFO,
        STOCKS,
        FLOWS,
        VARIABLES,
        LOOKUP_TABLES,
        SUBSCRIPTS,
        MODULES,
        SIMULATION_SETTINGS,
        DIAGRAM
    }

    private ReportGenerator() {
    }

    /**
     * Generates a complete HTML report with all sections.
     *
     * @param definition the model definition
     * @param svgDiagram optional SVG diagram content (null to omit diagram section)
     * @return self-contained HTML string
     */
    public static String generate(ModelDefinition definition, String svgDiagram) {
        return generate(definition, svgDiagram, EnumSet.allOf(Section.class));
    }

    /**
     * Generates an HTML report with selected sections.
     *
     * @param definition the model definition
     * @param svgDiagram optional SVG diagram content (null to omit diagram section)
     * @param sections   which sections to include
     * @return self-contained HTML string
     */
    public static String generate(ModelDefinition definition, String svgDiagram,
                                  Set<Section> sections) {
        StringBuilder html = new StringBuilder(8192);
        writeHeader(html, definition.name());

        html.append("<body>\n");
        html.append("<div class=\"container\">\n");

        html.append("<h1>").append(esc(definition.name())).append("</h1>\n");

        if (sections.contains(Section.MODEL_INFO)) {
            writeModelInfo(html, definition);
        }
        if (sections.contains(Section.DIAGRAM) && svgDiagram != null && !svgDiagram.isBlank()) {
            writeDiagram(html, svgDiagram);
        }
        if (sections.contains(Section.STOCKS) && !definition.stocks().isEmpty()) {
            writeStocks(html, definition.stocks());
        }
        if (sections.contains(Section.FLOWS) && !definition.flows().isEmpty()) {
            writeFlows(html, definition.flows());
        }
        if (sections.contains(Section.VARIABLES) && !definition.variables().isEmpty()) {
            writeVariables(html, definition.variables());
        }
        if (sections.contains(Section.LOOKUP_TABLES) && !definition.lookupTables().isEmpty()) {
            writeLookupTables(html, definition.lookupTables());
        }
        if (sections.contains(Section.SUBSCRIPTS) && !definition.subscripts().isEmpty()) {
            writeSubscripts(html, definition.subscripts());
        }
        if (sections.contains(Section.MODULES) && !definition.modules().isEmpty()) {
            writeModules(html, definition.modules());
        }
        if (sections.contains(Section.SIMULATION_SETTINGS) && definition.defaultSimulation() != null) {
            writeSimulationSettings(html, definition.defaultSimulation());
        }

        html.append("</div>\n");
        html.append("</body>\n</html>\n");
        return html.toString();
    }

    // ── Header & CSS ────────────────────────────────────────────────────

    private static void writeHeader(StringBuilder html, String title) {
        html.append("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                """);
        html.append("<title>").append(esc(title)).append(" — Model Report</title>\n");
        html.append(CSS);
        html.append("</head>\n");
    }

    // ── Sections ────────────────────────────────────────────────────────

    private static void writeModelInfo(StringBuilder html, ModelDefinition def) {
        html.append("<section class=\"model-info\">\n");
        html.append("<h2>Model Information</h2>\n");
        html.append("<table class=\"info-table\">\n");
        infoRow(html, "Name", def.name());
        if (def.comment() != null && !def.comment().isBlank()) {
            infoRow(html, "Description", def.comment());
        }
        ModelMetadata meta = def.metadata();
        if (meta != null) {
            if (meta.author() != null) {
                infoRow(html, "Author", meta.author());
            }
            if (meta.source() != null) {
                infoRow(html, "Source", meta.source());
            }
            if (meta.license() != null) {
                infoRow(html, "License", meta.license());
            }
        }
        infoRow(html, "Stocks", String.valueOf(def.stocks().size()));
        infoRow(html, "Flows", String.valueOf(def.flows().size()));
        infoRow(html, "Variables", String.valueOf(def.variables().size()));
        if (!def.lookupTables().isEmpty()) {
            infoRow(html, "Lookup Tables", String.valueOf(def.lookupTables().size()));
        }
        if (!def.subscripts().isEmpty()) {
            infoRow(html, "Subscripts", String.valueOf(def.subscripts().size()));
        }
        if (!def.modules().isEmpty()) {
            infoRow(html, "Modules", String.valueOf(def.modules().size()));
        }
        html.append("</table>\n");
        html.append("</section>\n\n");
    }

    private static void writeDiagram(StringBuilder html, String svgDiagram) {
        html.append("<section class=\"diagram\">\n");
        html.append("<h2>Model Diagram</h2>\n");
        html.append("<div class=\"diagram-container\">\n");
        // Strip XML declaration if present — it's not valid inside HTML
        String svg = svgDiagram;
        if (svg.startsWith("<?xml")) {
            int end = svg.indexOf("?>");
            if (end >= 0) {
                svg = svg.substring(end + 2).stripLeading();
            }
        }
        html.append(svg).append('\n');
        html.append("</div>\n");
        html.append("</section>\n\n");
    }

    private static void writeStocks(StringBuilder html, List<StockDef> stocks) {
        html.append("<section>\n");
        html.append("<h2>Stocks</h2>\n");
        html.append("<table class=\"element-table\">\n");
        html.append("<thead><tr><th>Name</th><th>Initial Value</th><th>Unit</th>");
        html.append("<th>Comment</th></tr></thead>\n");
        html.append("<tbody>\n");
        for (StockDef s : stocks) {
            html.append("<tr>");
            html.append("<td class=\"name\">").append(esc(s.name())).append("</td>");
            String initVal = s.initialExpression() != null && !s.initialExpression().isBlank()
                    ? s.initialExpression()
                    : String.valueOf(s.initialValue());
            html.append("<td class=\"code\">").append(esc(initVal)).append("</td>");
            html.append("<td>").append(esc(s.unit() != null ? s.unit() : "")).append("</td>");
            html.append("<td>").append(esc(s.comment() != null ? s.comment() : "")).append("</td>");
            html.append("</tr>\n");
        }
        html.append("</tbody></table>\n");
        html.append("</section>\n\n");
    }

    private static void writeFlows(StringBuilder html, List<FlowDef> flows) {
        html.append("<section>\n");
        html.append("<h2>Flows</h2>\n");
        html.append("<table class=\"element-table\">\n");
        html.append("<thead><tr><th>Name</th><th>Equation</th><th>From</th><th>To</th>");
        html.append("<th>Comment</th></tr></thead>\n");
        html.append("<tbody>\n");
        for (FlowDef f : flows) {
            html.append("<tr>");
            html.append("<td class=\"name\">").append(esc(f.name())).append("</td>");
            html.append("<td class=\"code\">").append(esc(f.equation() != null ? f.equation() : "")).append("</td>");
            html.append("<td>").append(esc(f.source() != null ? f.source() : "(external)")).append("</td>");
            html.append("<td>").append(esc(f.sink() != null ? f.sink() : "(external)")).append("</td>");
            html.append("<td>").append(esc(f.comment() != null ? f.comment() : "")).append("</td>");
            html.append("</tr>\n");
        }
        html.append("</tbody></table>\n");
        html.append("</section>\n\n");
    }

    private static void writeVariables(StringBuilder html, List<VariableDef> variables) {
        // Split into constants (literal-valued) and computed variables
        List<VariableDef> constants = variables.stream().filter(VariableDef::isLiteral).toList();
        List<VariableDef> computed = variables.stream().filter(a -> !a.isLiteral()).toList();

        if (!constants.isEmpty()) {
            html.append("<section>\n");
            html.append("<h2>Constants</h2>\n");
            html.append("<table class=\"element-table\">\n");
            html.append("<thead><tr><th>Name</th><th>Value</th><th>Unit</th>");
            html.append("<th>Comment</th></tr></thead>\n");
            html.append("<tbody>\n");
            for (VariableDef a : constants) {
                html.append("<tr>");
                html.append("<td class=\"name\">").append(esc(a.name())).append("</td>");
                html.append("<td class=\"code\">").append(esc(a.equation())).append("</td>");
                html.append("<td>").append(esc(a.unit() != null ? a.unit() : "")).append("</td>");
                html.append("<td>").append(esc(a.comment() != null ? a.comment() : "")).append("</td>");
                html.append("</tr>\n");
            }
            html.append("</tbody></table>\n");
            html.append("</section>\n\n");
        }

        if (!computed.isEmpty()) {
            html.append("<section>\n");
            html.append("<h2>Variables</h2>\n");
            html.append("<table class=\"element-table\">\n");
            html.append("<thead><tr><th>Name</th><th>Equation</th><th>Unit</th>");
            html.append("<th>Comment</th></tr></thead>\n");
            html.append("<tbody>\n");
            for (VariableDef a : computed) {
                html.append("<tr>");
                html.append("<td class=\"name\">").append(esc(a.name())).append("</td>");
                html.append("<td class=\"code\">").append(esc(a.equation())).append("</td>");
                html.append("<td>").append(esc(a.unit() != null ? a.unit() : "")).append("</td>");
                html.append("<td>").append(esc(a.comment() != null ? a.comment() : "")).append("</td>");
                html.append("</tr>\n");
            }
            html.append("</tbody></table>\n");
            html.append("</section>\n\n");
        }
    }

    private static void writeLookupTables(StringBuilder html, List<LookupTableDef> lookups) {
        html.append("<section>\n");
        html.append("<h2>Lookup Tables</h2>\n");
        html.append("<table class=\"element-table\">\n");
        html.append("<thead><tr><th>Name</th><th>Points</th><th>X Range</th>");
        html.append("<th>Y Range</th><th>Comment</th></tr></thead>\n");
        html.append("<tbody>\n");
        for (LookupTableDef lt : lookups) {
            double[] x = lt.xValues();
            double[] y = lt.yValues();
            html.append("<tr>");
            html.append("<td class=\"name\">").append(esc(lt.name())).append("</td>");
            html.append("<td>").append(x.length).append("</td>");
            if (x.length > 0) {
                html.append("<td>").append(formatNumber(x[0])).append(" – ")
                        .append(formatNumber(x[x.length - 1])).append("</td>");
                double yMin = Double.MAX_VALUE;
                double yMax = -Double.MAX_VALUE;
                for (double v : y) {
                    if (v < yMin) {
                        yMin = v;
                    }
                    if (v > yMax) {
                        yMax = v;
                    }
                }
                html.append("<td>").append(formatNumber(yMin)).append(" – ")
                        .append(formatNumber(yMax)).append("</td>");
            } else {
                html.append("<td></td><td></td>");
            }
            html.append("<td>").append(esc(lt.comment() != null ? lt.comment() : "")).append("</td>");
            html.append("</tr>\n");
        }
        html.append("</tbody></table>\n");
        html.append("</section>\n\n");
    }

    private static void writeSubscripts(StringBuilder html, List<SubscriptDef> subscripts) {
        html.append("<section>\n");
        html.append("<h2>Subscripts</h2>\n");
        html.append("<table class=\"element-table\">\n");
        html.append("<thead><tr><th>Name</th><th>Labels</th></tr></thead>\n");
        html.append("<tbody>\n");
        for (SubscriptDef s : subscripts) {
            html.append("<tr>");
            html.append("<td class=\"name\">").append(esc(s.name())).append("</td>");
            html.append("<td>").append(esc(String.join(", ", s.labels()))).append("</td>");
            html.append("</tr>\n");
        }
        html.append("</tbody></table>\n");
        html.append("</section>\n\n");
    }

    private static void writeModules(StringBuilder html, List<ModuleInstanceDef> modules) {
        html.append("<section>\n");
        html.append("<h2>Modules</h2>\n");
        html.append("<table class=\"element-table\">\n");
        html.append("<thead><tr><th>Name</th><th>Inputs</th><th>Outputs</th></tr></thead>\n");
        html.append("<tbody>\n");
        for (ModuleInstanceDef m : modules) {
            html.append("<tr>");
            html.append("<td class=\"name\">").append(esc(m.instanceName())).append("</td>");
            int inputs = m.definition().moduleInterface() != null
                    ? m.definition().moduleInterface().inputs().size() : 0;
            int outputs = m.definition().moduleInterface() != null
                    ? m.definition().moduleInterface().outputs().size() : 0;
            html.append("<td>").append(inputs).append("</td>");
            html.append("<td>").append(outputs).append("</td>");
            html.append("</tr>\n");
        }
        html.append("</tbody></table>\n");
        html.append("</section>\n\n");
    }

    private static void writeSimulationSettings(StringBuilder html, SimulationSettings sim) {
        html.append("<section>\n");
        html.append("<h2>Simulation Settings</h2>\n");
        html.append("<table class=\"info-table\">\n");
        infoRow(html, "Time Step", sim.timeStep());
        infoRow(html, "Duration", formatNumber(sim.duration()) + " " + sim.durationUnit());
        if (sim.dt() != 1.0) {
            infoRow(html, "DT (fractional)", formatNumber(sim.dt()));
        }
        html.append("</table>\n");
        html.append("</section>\n\n");
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private static void infoRow(StringBuilder html, String label, String value) {
        html.append("<tr><th>").append(esc(label)).append("</th><td>")
                .append(esc(value)).append("</td></tr>\n");
    }

    static String esc(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String formatNumber(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    // ── CSS ──────────────────────────────────────────────────────────────

    private static final String CSS = """
            <style>
            * { box-sizing: border-box; margin: 0; padding: 0; }
            body {
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                line-height: 1.6;
                color: #1a1a1a;
                background: #fff;
                padding: 2rem;
            }
            .container { max-width: 960px; margin: 0 auto; }
            h1 {
                font-size: 1.8rem;
                border-bottom: 2px solid #2c5282;
                padding-bottom: 0.5rem;
                margin-bottom: 1.5rem;
                color: #2c5282;
            }
            h2 {
                font-size: 1.2rem;
                color: #2c5282;
                margin-top: 1.5rem;
                margin-bottom: 0.75rem;
                border-bottom: 1px solid #e2e8f0;
                padding-bottom: 0.3rem;
            }
            section { margin-bottom: 1.5rem; }
            table { width: 100%; border-collapse: collapse; margin-bottom: 1rem; }
            .info-table th {
                text-align: left;
                width: 140px;
                padding: 0.3rem 0.75rem;
                color: #4a5568;
                font-weight: 600;
                vertical-align: top;
            }
            .info-table td { padding: 0.3rem 0.75rem; }
            .element-table th {
                text-align: left;
                padding: 0.4rem 0.75rem;
                background: #f7fafc;
                border-bottom: 2px solid #e2e8f0;
                font-weight: 600;
                color: #4a5568;
                font-size: 0.85rem;
            }
            .element-table td {
                padding: 0.35rem 0.75rem;
                border-bottom: 1px solid #edf2f7;
                font-size: 0.9rem;
            }
            .element-table tbody tr:hover { background: #f7fafc; }
            .name { font-weight: 600; }
            .code { font-family: "SFMono-Regular", Consolas, monospace; font-size: 0.85rem; }
            .diagram-container {
                text-align: center;
                margin: 1rem 0;
                overflow-x: auto;
            }
            .diagram-container svg {
                max-width: 100%;
                height: auto;
            }
            @media print {
                body { padding: 0; font-size: 10pt; }
                .container { max-width: 100%; }
                h1 { font-size: 16pt; }
                h2 { font-size: 12pt; page-break-after: avoid; }
                table { page-break-inside: avoid; }
                .element-table tbody tr:hover { background: none; }
            }
            </style>
            """;
}
