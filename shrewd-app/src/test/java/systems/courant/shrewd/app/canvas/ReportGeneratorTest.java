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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReportGenerator")
class ReportGeneratorTest {

    @Nested
    @DisplayName("full report generation")
    class FullReport {

        @Test
        @DisplayName("should produce valid HTML with all sections for a complete model")
        void shouldProduceCompleteHtml() {
            ModelDefinition def = buildFullModel();
            String html = ReportGenerator.generate(def, "<svg>diagram</svg>");

            assertThat(html).startsWith("<!DOCTYPE html>");
            assertThat(html).contains("<html lang=\"en\">");
            assertThat(html).contains("</html>");
            assertThat(html).contains("<title>SIR Epidemic — Model Report</title>");

            // Model info
            assertThat(html).contains("Model Information");
            assertThat(html).contains("SIR Epidemic");
            assertThat(html).contains("A simple epidemic model");
            assertThat(html).contains("Test Author");

            // Stocks
            assertThat(html).contains(">Stocks</h2>");
            assertThat(html).contains("Susceptible");
            assertThat(html).contains("1000");
            assertThat(html).contains("People");

            // Flows
            assertThat(html).contains(">Flows</h2>");
            assertThat(html).contains("Infection");
            assertThat(html).contains("Susceptible * infection_rate");

            // Constants
            assertThat(html).contains(">Constants</h2>");
            assertThat(html).contains("infection_rate");
            assertThat(html).contains("0.3");

            // Variables
            assertThat(html).contains(">Variables</h2>");
            assertThat(html).contains("total_population");
            assertThat(html).contains("Susceptible + Infectious + Recovered");

            // Diagram
            assertThat(html).contains("Model Diagram");
            assertThat(html).contains("<svg>diagram</svg>");

            // Simulation settings
            assertThat(html).contains("Simulation Settings");
            assertThat(html).contains("Day");
            assertThat(html).contains("100");
        }

        @Test
        @DisplayName("should produce valid HTML for a minimal model with no optional fields")
        void shouldHandleMinimalModel() {
            ModelDefinition def = new ModelDefinition(
                    "Empty", null, null,
                    List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                    List.of(), List.of(), List.of(), List.of(), null, null, List.of());

            String html = ReportGenerator.generate(def, null);

            assertThat(html).startsWith("<!DOCTYPE html>");
            assertThat(html).contains("Empty");
            // Should not have element tables when lists are empty
            assertThat(html).doesNotContain(">Stocks</h2>");
            assertThat(html).doesNotContain(">Flows</h2>");
            assertThat(html).doesNotContain("Model Diagram");
        }
    }

    @Nested
    @DisplayName("section selection")
    class SectionSelection {

        @Test
        @DisplayName("should only include selected sections")
        void shouldRespectSectionSelection() {
            ModelDefinition def = buildFullModel();
            String html = ReportGenerator.generate(def, "<svg>diagram</svg>",
                    EnumSet.of(ReportGenerator.Section.STOCKS,
                            ReportGenerator.Section.SIMULATION_SETTINGS));

            assertThat(html).contains(">Stocks</h2>");
            assertThat(html).contains("Simulation Settings");
            // Excluded sections
            assertThat(html).doesNotContain("Model Information");
            assertThat(html).doesNotContain(">Flows</h2>");
            assertThat(html).doesNotContain("Model Diagram");
            assertThat(html).doesNotContain(">Constants</h2>");
            assertThat(html).doesNotContain(">Variables</h2>");
        }

        @Test
        @DisplayName("should omit diagram when DIAGRAM section excluded even if SVG provided")
        void shouldOmitDiagramWhenExcluded() {
            ModelDefinition def = buildFullModel();
            String html = ReportGenerator.generate(def, "<svg>diagram</svg>",
                    EnumSet.of(ReportGenerator.Section.MODEL_INFO));

            assertThat(html).doesNotContain("Model Diagram");
            assertThat(html).doesNotContain("<svg>");
        }
    }

    @Nested
    @DisplayName("HTML escaping")
    class HtmlEscaping {

        @Test
        @DisplayName("should escape special HTML characters in model name")
        void shouldEscapeModelName() {
            ModelDefinition def = new ModelDefinition(
                    "Model <A> & \"B\"", null, null,
                    List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                    List.of(), List.of(), List.of(), List.of(), null, null, List.of());

            String html = ReportGenerator.generate(def, null);

            assertThat(html).contains("Model &lt;A&gt; &amp; &quot;B&quot;");
            assertThat(html).doesNotContain("Model <A>");
        }

        @Test
        @DisplayName("should escape special characters in equations")
        void shouldEscapeEquations() {
            VariableDef v = new VariableDef("test", null, "a < b && c > d", null, List.of());
            ModelDefinition def = new ModelDefinition(
                    "Test", null, null,
                    List.of(), List.of(), List.of(v), List.of(), List.of(), List.of(),
                    List.of(), List.of(), List.of(), List.of(), null, null, List.of());

            String html = ReportGenerator.generate(def, null);

            assertThat(html).contains("a &lt; b &amp;&amp; c &gt; d");
        }
    }

    @Nested
    @DisplayName("stocks section")
    class StocksSection {

        @Test
        @DisplayName("should show expression-based initial values")
        void shouldShowExpressionInitialValues() {
            StockDef stock = new StockDef("Population", null, 0,
                    "base_population * 1.5", "People", null, List.of());
            ModelDefinition def = modelWith(List.of(stock), List.of(), List.of());

            String html = ReportGenerator.generate(def, null);

            assertThat(html).contains("base_population * 1.5");
        }

        @Test
        @DisplayName("should show numeric initial value when no expression")
        void shouldShowNumericInitialValue() {
            StockDef stock = new StockDef("Tank", null, 42.5, null, "Liters", null, List.of());
            ModelDefinition def = modelWith(List.of(stock), List.of(), List.of());

            String html = ReportGenerator.generate(def, null);

            assertThat(html).contains("42.5");
        }
    }

    @Nested
    @DisplayName("flows section")
    class FlowsSection {

        @Test
        @DisplayName("should show source and sink for internal flows")
        void shouldShowSourceAndSink() {
            FlowDef flow = new FlowDef("Transfer", null, "rate * Source",
                    "Day", null, "Source", "Sink", List.of());
            ModelDefinition def = modelWith(List.of(), List.of(flow), List.of());

            String html = ReportGenerator.generate(def, null);

            assertThat(html).contains("Source");
            assertThat(html).contains("Sink");
        }

        @Test
        @DisplayName("should show (external) for flows with no source or sink")
        void shouldShowExternalForNullSourceSink() {
            FlowDef flow = new FlowDef("Inflow", null, "10", "Day", null, null, "Tank", List.of());
            ModelDefinition def = modelWith(List.of(), List.of(flow), List.of());

            String html = ReportGenerator.generate(def, null);

            assertThat(html).contains("(external)");
        }
    }

    @Nested
    @DisplayName("variables section")
    class VariablesSection {

        @Test
        @DisplayName("should split variables into constants and variables")
        void shouldSplitConstantsAndVariables() {
            VariableDef constant = new VariableDef("rate", null, "0.5", "1/Day", List.of());
            VariableDef variable = new VariableDef("effect", null, "rate * Population", null, List.of());
            ModelDefinition def = modelWith(List.of(), List.of(), List.of(constant, variable));

            String html = ReportGenerator.generate(def, null);

            assertThat(html).contains(">Constants</h2>");
            assertThat(html).contains(">Variables</h2>");
            // Both elements present
            assertThat(html).contains("rate");
            assertThat(html).contains("effect");
        }

        @Test
        @DisplayName("should omit constants heading when all are computed")
        void shouldOmitConstantsWhenNone() {
            VariableDef variable = new VariableDef("effect", null, "A + B", null, List.of());
            ModelDefinition def = modelWith(List.of(), List.of(), List.of(variable));

            String html = ReportGenerator.generate(def, null);

            assertThat(html).doesNotContain(">Constants</h2>");
            assertThat(html).contains(">Variables</h2>");
        }
    }

    @Nested
    @DisplayName("lookup tables section")
    class LookupTablesSection {

        @Test
        @DisplayName("should show point count and ranges")
        void shouldShowLookupDetails() {
            LookupTableDef lt = new LookupTableDef("effect_curve", null,
                    new double[]{0, 1, 2, 3}, new double[]{0, 0.5, 0.8, 1.0}, "LINEAR");
            ModelDefinition def = new ModelDefinition(
                    "Test", null, null,
                    List.of(), List.of(), List.of(), List.of(lt), List.of(), List.of(),
                    List.of(), List.of(), List.of(), List.of(), null, null, List.of());

            String html = ReportGenerator.generate(def, null);

            assertThat(html).contains("effect_curve");
            assertThat(html).contains("4"); // 4 points
            assertThat(html).contains("0 – 3"); // x range
            assertThat(html).contains("0 – 1"); // y range
        }
    }

    @Nested
    @DisplayName("subscripts section")
    class SubscriptsSection {

        @Test
        @DisplayName("should list subscript labels")
        void shouldShowSubscriptLabels() {
            SubscriptDef sub = new SubscriptDef("Region", List.of("North", "South", "East"));
            ModelDefinition def = new ModelDefinition(
                    "Test", null, null,
                    List.of(), List.of(), List.of(), List.of(), List.of(),
                    List.of(sub),
                    List.of(), List.of(), List.of(), List.of(), null, null, List.of());

            String html = ReportGenerator.generate(def, null);

            assertThat(html).contains("Region");
            assertThat(html).contains("North, South, East");
        }
    }

    @Nested
    @DisplayName("simulation settings section")
    class SimulationSettingsSection {

        @Test
        @DisplayName("should show DT when fractional")
        void shouldShowFractionalDt() {
            SimulationSettings sim = new SimulationSettings("Day", 365, "Day", 0.25);
            ModelDefinition def = new ModelDefinition(
                    "Test", null, null,
                    List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                    List.of(), List.of(), List.of(), List.of(), sim, null, List.of());

            String html = ReportGenerator.generate(def, null);

            assertThat(html).contains("0.25");
        }

        @Test
        @DisplayName("should omit DT when it equals 1.0")
        void shouldOmitDefaultDt() {
            SimulationSettings sim = new SimulationSettings("Week", 52, "Week");
            ModelDefinition def = new ModelDefinition(
                    "Test", null, null,
                    List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                    List.of(), List.of(), List.of(), List.of(), sim, null, List.of());

            String html = ReportGenerator.generate(def, null);

            assertThat(html).doesNotContain("DT");
        }
    }

    @Nested
    @DisplayName("diagram embedding")
    class DiagramEmbedding {

        @Test
        @DisplayName("should strip XML declaration from embedded SVG")
        void shouldStripXmlDeclaration() {
            String svg = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<svg><rect/></svg>";
            ModelDefinition def = modelWith(List.of(), List.of(), List.of());

            String html = ReportGenerator.generate(def, svg);

            assertThat(html).contains("<svg><rect/></svg>");
            assertThat(html).doesNotContain("<?xml");
        }

        @Test
        @DisplayName("should handle SVG without XML declaration")
        void shouldHandleSvgWithoutDeclaration() {
            String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><circle/></svg>";
            ModelDefinition def = modelWith(List.of(), List.of(), List.of());

            String html = ReportGenerator.generate(def, svg);

            assertThat(html).contains("<svg xmlns=\"http://www.w3.org/2000/svg\"><circle/></svg>");
        }

        @Test
        @DisplayName("should omit diagram section when SVG is null")
        void shouldOmitDiagramWhenNull() {
            ModelDefinition def = modelWith(List.of(), List.of(), List.of());

            String html = ReportGenerator.generate(def, null);

            assertThat(html).doesNotContain("Model Diagram");
        }

        @Test
        @DisplayName("should omit diagram section when SVG is blank")
        void shouldOmitDiagramWhenBlank() {
            ModelDefinition def = modelWith(List.of(), List.of(), List.of());

            String html = ReportGenerator.generate(def, "   ");

            assertThat(html).doesNotContain("Model Diagram");
        }
    }

    @Nested
    @DisplayName("esc helper")
    class EscHelper {

        @Test
        @DisplayName("should escape all HTML special characters")
        void shouldEscapeSpecialChars() {
            assertThat(ReportGenerator.esc("a & b")).isEqualTo("a &amp; b");
            assertThat(ReportGenerator.esc("<tag>")).isEqualTo("&lt;tag&gt;");
            assertThat(ReportGenerator.esc("\"quoted\"")).isEqualTo("&quot;quoted&quot;");
        }

        @Test
        @DisplayName("should return empty string for null")
        void shouldHandleNull() {
            assertThat(ReportGenerator.esc(null)).isEmpty();
        }

        @Test
        @DisplayName("should return empty string for empty input")
        void shouldHandleEmpty() {
            assertThat(ReportGenerator.esc("")).isEmpty();
        }

        @Test
        @DisplayName("should pass through plain text unchanged")
        void shouldPassThroughPlainText() {
            assertThat(ReportGenerator.esc("hello world")).isEqualTo("hello world");
        }
    }

    @Nested
    @DisplayName("print CSS")
    class PrintCss {

        @Test
        @DisplayName("should include print media query")
        void shouldIncludePrintStyles() {
            ModelDefinition def = modelWith(List.of(), List.of(), List.of());
            String html = ReportGenerator.generate(def, null);

            assertThat(html).contains("@media print");
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private static ModelDefinition buildFullModel() {
        StockDef susceptible = new StockDef("Susceptible", null, 1000, null, "People", null, List.of());
        StockDef infectious = new StockDef("Infectious", null, 10, null, "People", null, List.of());
        StockDef recovered = new StockDef("Recovered", null, 0, null, "People", null, List.of());

        FlowDef infection = new FlowDef("Infection", null, "Susceptible * infection_rate",
                "Day", "People", "Susceptible", "Infectious", List.of());
        FlowDef recovery = new FlowDef("Recovery", null, "Infectious * recovery_rate",
                "Day", "People", "Infectious", "Recovered", List.of());

        VariableDef infectionRate = new VariableDef("infection_rate", null, "0.3", "1/Day", List.of());
        VariableDef recoveryRate = new VariableDef("recovery_rate", null, "0.1", "1/Day", List.of());
        VariableDef totalPop = new VariableDef("total_population", null,
                "Susceptible + Infectious + Recovered", "People", List.of());

        SimulationSettings sim = new SimulationSettings("Day", 100, "Day");
        ModelMetadata meta = ModelMetadata.builder()
                .author("Test Author")
                .source("Test Source")
                .build();

        return new ModelDefinition(
                "SIR Epidemic", "A simple epidemic model", null,
                List.of(susceptible, infectious, recovered),
                List.of(infection, recovery),
                List.of(infectionRate, recoveryRate, totalPop),
                List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(),
                sim, meta, List.of());
    }

    private static ModelDefinition modelWith(List<StockDef> stocks, List<FlowDef> flows,
                                             List<VariableDef> variables) {
        return new ModelDefinition(
                "Test", null, null,
                stocks, flows, variables, List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of(), null, null, List.of());
    }
}
