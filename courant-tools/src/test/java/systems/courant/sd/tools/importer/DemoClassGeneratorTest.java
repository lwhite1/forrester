package systems.courant.sd.tools.importer;

import systems.courant.sd.model.ModelMetadata;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.LookupTableDef;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModelDefinitionBuilder;
import systems.courant.sd.model.def.StockDef;
import systems.courant.sd.model.def.VariableDef;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DemoClassGenerator")
class DemoClassGeneratorTest {

    private final DemoClassGenerator generator = new DemoClassGenerator(2025);

    @Test
    void shouldGenerateMinimalModel() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .stock("Population", 100.0, "people")
                .flow("births", "Population * 0.03", "Year", null, "Population")
                .defaultSimulation("Year", 50.0, "Year")
                .build();

        ModelMetadata metadata = ModelMetadata.builder()
                .license("CC-BY-SA-4.0")
                .build();

        String source = generator.generate(def, metadata, "TestDemo",
                "systems.courant.sd.demo", "test.xmile",
                List.of(), List.of());

        assertThat(source).contains("package systems.courant.sd.demo;");
        assertThat(source).contains("public class TestDemo {");
        assertThat(source).contains(".name(\"Test\")");
        assertThat(source).contains(".defaultSimulation(\"Year\", 50.0, \"Year\")");
        assertThat(source).contains("\"Population\"");
        assertThat(source).contains("\"births\"");
        assertThat(source).contains(".license(\"CC-BY-SA-4.0\")");
        assertThat(source).contains("compiled.createSimulation()");
    }

    @Test
    void shouldIncludeAllMetadataFields() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("SIR")
                .stock("S", 990.0, "people")
                .defaultSimulation("Day", 100.0, "Day")
                .build();

        ModelMetadata metadata = ModelMetadata.builder()
                .author("Kermack & McKendrick")
                .source("SIR model (1927)")
                .license("CC-BY-SA-4.0")
                .url("https://doi.org/10.1098/rspa.1927.0118")
                .build();

        String source = generator.generate(def, metadata, "SirDemo",
                "systems.courant.sd.demo.epidemiology", "sir.xmile",
                List.of(), List.of());

        assertThat(source).contains(".author(\"Kermack & McKendrick\")");
        assertThat(source).contains(".source(\"SIR model (1927)\")");
        assertThat(source).contains(".license(\"CC-BY-SA-4.0\")");
        assertThat(source).contains(".url(\"https://doi.org/10.1098/rspa.1927.0118\")");
        assertThat(source).contains("* Author: Kermack &amp; McKendrick");
    }

    @Test
    void shouldGenerateConstants() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .constant("rate", 0.05, "1/Year")
                .defaultSimulation("Year", 10.0, "Year")
                .build();

        ModelMetadata metadata = ModelMetadata.builder().license("CC-BY-SA-4.0").build();

        String source = generator.generate(def, metadata, "TestDemo",
                "systems.courant.sd.demo", "test.mdl",
                List.of(), List.of());

        assertThat(source).contains("builder.constant(");
        assertThat(source).contains("\"rate\"");
        assertThat(source).contains("0.05");
    }

    @Test
    void shouldGenerateLookupTables() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .lookupTable("effect", new double[]{0.0, 1.0}, new double[]{0.0, 1.0}, "LINEAR")
                .defaultSimulation("Year", 10.0, "Year")
                .build();

        ModelMetadata metadata = ModelMetadata.builder().license("CC-BY-SA-4.0").build();

        String source = generator.generate(def, metadata, "TestDemo",
                "systems.courant.sd.demo", "test.xmile",
                List.of(), List.of());

        assertThat(source).contains("new LookupTableDef(");
        assertThat(source).contains("new double[]{0.0, 1.0}");
        assertThat(source).contains("\"LINEAR\"");
    }

    @Test
    void shouldGenerateVariables() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .stock("Pop", 100.0, "people")
                .variable("growth_rate", "Pop * 0.03", "people/Year")
                .defaultSimulation("Year", 10.0, "Year")
                .build();

        ModelMetadata metadata = ModelMetadata.builder().license("CC-BY-SA-4.0").build();

        String source = generator.generate(def, metadata, "TestDemo",
                "systems.courant.sd.demo", "test.xmile",
                List.of(), List.of());

        assertThat(source).contains("new VariableDef(");
        assertThat(source).contains("\"growth_rate\"");
        assertThat(source).contains("\"Pop * 0.03\"");
    }

    @Test
    void shouldUseMapOfEntriesForMoreThan10Bindings() {
        // Build a module with 12 input bindings
        Map<String, String> inputs = new LinkedHashMap<>();
        for (int i = 1; i <= 12; i++) {
            inputs.put("port" + i, "value" + i);
        }

        ModelDefinition innerDef = new ModelDefinitionBuilder()
                .name("Inner")
                .stock("X", 0, "unit")
                .defaultSimulation("Day", 10.0, "Day")
                .build();

        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .module("myModule", innerDef, inputs, Map.of())
                .defaultSimulation("Day", 10.0, "Day")
                .build();

        ModelMetadata metadata = ModelMetadata.builder().license("CC-BY-SA-4.0").build();

        String source = generator.generate(def, metadata, "TestDemo",
                "systems.courant.sd.demo", "test.xmile",
                List.of(), List.of());

        // Should use Map.ofEntries instead of Map.of for >10 entries
        assertThat(source).contains("Map.ofEntries(");
        assertThat(source).contains("Map.entry(");
        assertThat(source).doesNotContain("Map.of(\"port");
    }

    @Test
    void shouldEscapeHtmlInWarningsAndErrors() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .stock("S", 100.0, "people")
                .defaultSimulation("Day", 10.0, "Day")
                .build();

        ModelMetadata metadata = ModelMetadata.builder().license("CC-BY-SA-4.0").build();

        String source = generator.generate(def, metadata, "TestDemo",
                "systems.courant.sd.demo", "test.xmile",
                List.of("Value <0 found in A&B"),
                List.of("Ref to */ block"));

        // HTML special chars should be escaped
        assertThat(source).contains("Value &lt;0 found in A&amp;B");
        assertThat(source).doesNotContain("Value <0 found");
        assertThat(source).contains("Ref to &#42;/ block");
    }

    @Test
    void shouldEscapeMetadataFieldsInJavadoc() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .stock("S", 100.0, "people")
                .defaultSimulation("Day", 10.0, "Day")
                .build();

        ModelMetadata metadata = ModelMetadata.builder()
                .author("Evil */ Author")
                .source("Source with */closing")
                .license("License */ break")
                .build();

        String source = generator.generate(def, metadata, "TestDemo",
                "systems.courant.sd.demo", "file*/.xmile",
                List.of(), List.of());

        // Escaped versions should be present in the Javadoc
        assertThat(source).contains("Evil &#42;/ Author");
        assertThat(source).contains("Source with &#42;/closing");
        assertThat(source).contains("License &#42;/ break");
        assertThat(source).contains("file&#42;/.xmile");

        // Raw values must not appear unescaped in the Javadoc header
        assertThat(source).doesNotContain("* Author: Evil */ Author");
        assertThat(source).doesNotContain("* Source: Source with */closing");
        assertThat(source).doesNotContain("* License: License */ break");
        assertThat(source).doesNotContain("* Imported from: file*/.xmile");
    }

    @Test
    void shouldEscapeHtmlMethodHandlesAllSpecialChars() {
        assertThat(DemoClassGenerator.escapeHtml("<tag>")).isEqualTo("&lt;tag&gt;");
        assertThat(DemoClassGenerator.escapeHtml("a & b")).isEqualTo("a &amp; b");
        assertThat(DemoClassGenerator.escapeHtml("end */")).isEqualTo("end &#42;/");
        assertThat(DemoClassGenerator.escapeHtml("normal text")).isEqualTo("normal text");
    }

    @Test
    void shouldEmitNCLicenseHeaderForNCLicense() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .stock("S", 100.0, "people")
                .defaultSimulation("Day", 10.0, "Day")
                .build();

        ModelMetadata metadata = ModelMetadata.builder()
                .license("CC-BY-NC-SA-4.0")
                .build();

        String source = generator.generate(def, metadata, "TestDemo",
                "systems.courant.sd.demo", "test.xmile",
                List.of(), List.of());

        assertThat(source).contains("CC-BY-NC-SA-4.0");
        assertThat(source).contains("THIRD-PARTY-LICENSES");
        assertThat(source).doesNotContain("Copyright (c) 2025 Courant Systems");
    }

    @Test
    void shouldEmitCourantLicenseHeaderForNonNCLicense() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .stock("S", 100.0, "people")
                .defaultSimulation("Day", 10.0, "Day")
                .build();

        ModelMetadata metadata = ModelMetadata.builder()
                .license("CC-BY-SA-4.0")
                .build();

        String source = generator.generate(def, metadata, "TestDemo",
                "systems.courant.sd.demo", "test.xmile",
                List.of(), List.of());

        assertThat(source).contains("Copyright (c) 2025 Courant Systems");
        assertThat(source).doesNotContain("THIRD-PARTY-LICENSES");
    }

    @Test
    void shouldHandleNullMetadataFields() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .stock("S", 100.0, "people")
                .defaultSimulation("Day", 10.0, "Day")
                .build();

        ModelMetadata metadata = ModelMetadata.builder().build();

        String source = generator.generate(def, metadata, "TestDemo",
                "systems.courant.sd.demo", "test.xmile",
                List.of(), List.of());

        assertThat(source).contains("public class TestDemo");
        // null fields should not appear in Javadoc
        assertThat(source).doesNotContain("Author:");
        assertThat(source).doesNotContain("Source:");
        assertThat(source).doesNotContain("License:");
        // null fields should not appear in metadata builder
        assertThat(source).doesNotContain(".author(");
        assertThat(source).doesNotContain(".source(");
        assertThat(source).doesNotContain(".license(");
        assertThat(source).doesNotContain(".url(");
    }

    @Test
    void shouldEmitCustomDtWhenNotOne() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .stock("S", 100.0, "people")
                .defaultSimulation("Day", 100.0, "Day", 0.25)
                .build();

        ModelMetadata metadata = ModelMetadata.builder().license("CC-BY-SA-4.0").build();

        String source = generator.generate(def, metadata, "TestDemo",
                "systems.courant.sd.demo", "test.xmile",
                List.of(), List.of());

        assertThat(source).contains(".defaultSimulation(\"Day\", 100.0, \"Day\", 0.25)");
    }

    @Test
    void shouldGenerateModuleWithEmptyBindings() {
        ModelDefinition innerDef = new ModelDefinitionBuilder()
                .name("Inner")
                .stock("X", 0, "unit")
                .defaultSimulation("Day", 10.0, "Day")
                .build();

        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .module("myModule", innerDef, Map.of(), Map.of())
                .defaultSimulation("Day", 10.0, "Day")
                .build();

        ModelMetadata metadata = ModelMetadata.builder().license("CC-BY-SA-4.0").build();

        String source = generator.generate(def, metadata, "TestDemo",
                "systems.courant.sd.demo", "test.xmile",
                List.of(), List.of());

        assertThat(source).contains("Map.of()");
        assertThat(source).contains("ModuleInstanceDef");
    }

    @Test
    void shouldOmitImportsForEmptyElementLists() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Empty")
                .defaultSimulation("Day", 10.0, "Day")
                .build();

        ModelMetadata metadata = ModelMetadata.builder().license("CC-BY-SA-4.0").build();

        String source = generator.generate(def, metadata, "EmptyDemo",
                "systems.courant.sd.demo", "test.xmile",
                List.of(), List.of());

        assertThat(source).doesNotContain("import systems.courant.sd.model.def.StockDef;");
        assertThat(source).doesNotContain("import systems.courant.sd.model.def.FlowDef;");
        assertThat(source).doesNotContain("import systems.courant.sd.model.def.VariableDef;");
        assertThat(source).doesNotContain("import systems.courant.sd.model.def.LookupTableDef;");
    }

    @Test
    void shouldIncludeWarningsAndErrorsInJavadoc() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .stock("S", 100.0, "people")
                .defaultSimulation("Day", 10.0, "Day")
                .build();

        ModelMetadata metadata = ModelMetadata.builder().license("CC-BY-SA-4.0").build();

        String source = generator.generate(def, metadata, "TestDemo",
                "systems.courant.sd.demo", "test.xmile",
                List.of("Unsupported PULSE function"),
                List.of("Unresolved reference: missing_var"));

        assertThat(source).contains("Unsupported PULSE function");
        assertThat(source).contains("Unresolved reference: missing_var");
    }

    @Nested
    @DisplayName("Issue #718 — copyrightYear reproducibility")
    class CopyrightYearReproducibility {

        @Test
        void shouldProduceIdenticalOutputWithSameYear() {
            DemoClassGenerator gen = new DemoClassGenerator(2025);
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("S", 100.0, "people")
                    .defaultSimulation("Day", 10.0, "Day")
                    .build();
            ModelMetadata metadata = ModelMetadata.builder().license("CC-BY-SA-4.0").build();

            String first = gen.generate(def, metadata, "TestDemo",
                    "systems.courant.sd.demo", "test.xmile", List.of(), List.of());
            String second = gen.generate(def, metadata, "TestDemo",
                    "systems.courant.sd.demo", "test.xmile", List.of(), List.of());

            assertThat(first).isEqualTo(second);
        }

        @Test
        void shouldUseProvidedCopyrightYear() {
            DemoClassGenerator gen = new DemoClassGenerator(2024);
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("S", 100.0, "people")
                    .defaultSimulation("Day", 10.0, "Day")
                    .build();
            ModelMetadata metadata = ModelMetadata.builder().license("CC-BY-SA-4.0").build();

            String source = gen.generate(def, metadata, "TestDemo",
                    "systems.courant.sd.demo", "test.xmile", List.of(), List.of());

            assertThat(source).contains("Copyright (c) 2024 Courant Systems");
        }
    }

    @Nested
    @DisplayName("Issue #271 — initialExpression preservation")
    class InitialExpressionPreservation {

        @Test
        void shouldEmitInitialExpressionForStocks() {
            StockDef stock = new StockDef("Water", null, Double.NaN,
                    "Capacity * 0.5", "liters", null, List.of());
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock(stock)
                    .constant("Capacity", 100.0, "liters")
                    .defaultSimulation("Day", 10.0, "Day")
                    .build();

            ModelMetadata metadata = ModelMetadata.builder().license("CC-BY-SA-4.0").build();
            String source = generator.generate(def, metadata, "TestDemo",
                    "systems.courant.sd.demo", "test.mdl", List.of(), List.of());

            assertThat(source).contains("\"Capacity * 0.5\"");
            assertThat(source).contains("Double.NaN");
            // Bare NaN (without "Double." prefix) would be invalid Java
            assertThat(source).doesNotContain(", NaN,");
        }

        @Test
        void shouldEmitDoubleNanForNonFiniteInitialValue() {
            StockDef stock = new StockDef("X", null, Double.NaN,
                    "Y + 1", "unit", null, List.of());
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock(stock)
                    .defaultSimulation("Day", 10.0, "Day")
                    .build();

            ModelMetadata metadata = ModelMetadata.builder().license("CC-BY-SA-4.0").build();
            String source = generator.generate(def, metadata, "TestDemo",
                    "systems.courant.sd.demo", "test.mdl", List.of(), List.of());

            // Must use Double.NaN, not bare NaN which isn't valid Java
            assertThat(source).contains("Double.NaN");
        }

        @Test
        void shouldEmitFiniteInitialValueWithoutInitialExpression() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Water", 50.0, "liters")
                    .defaultSimulation("Day", 10.0, "Day")
                    .build();

            ModelMetadata metadata = ModelMetadata.builder().license("CC-BY-SA-4.0").build();
            String source = generator.generate(def, metadata, "TestDemo",
                    "systems.courant.sd.demo", "test.mdl", List.of(), List.of());

            assertThat(source).contains("50.0");
            assertThat(source).doesNotContain("Double.NaN");
        }
    }

    @Nested
    @DisplayName("Issue #272 — subscript preservation")
    class SubscriptPreservation {

        @Test
        void shouldEmitStockSubscripts() {
            StockDef stock = new StockDef("Population", null, 100.0, null,
                    "people", null, List.of("Region"));
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock(stock)
                    .defaultSimulation("Year", 10.0, "Year")
                    .build();

            ModelMetadata metadata = ModelMetadata.builder().license("CC-BY-SA-4.0").build();
            String source = generator.generate(def, metadata, "TestDemo",
                    "systems.courant.sd.demo", "test.mdl", List.of(), List.of());

            assertThat(source).contains("List.of(\"Region\")");
            assertThat(source).contains("import java.util.List;");
        }

        @Test
        void shouldEmitVariableSubscripts() {
            VariableDef v = new VariableDef("growth_rate", null, "Pop * 0.03",
                    "1/Year", List.of("Region", "AgeGroup"));
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .variable(v)
                    .defaultSimulation("Year", 10.0, "Year")
                    .build();

            ModelMetadata metadata = ModelMetadata.builder().license("CC-BY-SA-4.0").build();
            String source = generator.generate(def, metadata, "TestDemo",
                    "systems.courant.sd.demo", "test.mdl", List.of(), List.of());

            assertThat(source).contains("List.of(\"Region\", \"AgeGroup\")");
        }

        @Test
        void shouldEmitFlowSubscripts() {
            FlowDef flow = new FlowDef("births", null, "Pop * rate", "Year",
                    null, "Population", List.of("Region"));
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Population", 100.0, "people")
                    .flow(flow)
                    .defaultSimulation("Year", 10.0, "Year")
                    .build();

            ModelMetadata metadata = ModelMetadata.builder().license("CC-BY-SA-4.0").build();
            String source = generator.generate(def, metadata, "TestDemo",
                    "systems.courant.sd.demo", "test.mdl", List.of(), List.of());

            assertThat(source).contains("List.of(\"Region\")");
        }

        @Test
        void shouldEmitSubscriptedConstantAsVariableDef() {
            VariableDef constant = new VariableDef("rate", null, "0.05",
                    "1/Year", List.of("Region"));
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .variable(constant)
                    .defaultSimulation("Year", 10.0, "Year")
                    .build();

            ModelMetadata metadata = ModelMetadata.builder().license("CC-BY-SA-4.0").build();
            String source = generator.generate(def, metadata, "TestDemo",
                    "systems.courant.sd.demo", "test.mdl", List.of(), List.of());

            // Subscripted constant should use VariableDef constructor, not builder.constant()
            assertThat(source).contains("new VariableDef(");
            assertThat(source).contains("List.of(\"Region\")");
        }
    }

    @Nested
    @DisplayName("Issue #273 — nested module preservation")
    class NestedModulePreservation {

        @Test
        void shouldEmitNestedModulesWithinModuleDefinitions() {
            ModelDefinition innerInnerDef = new ModelDefinitionBuilder()
                    .name("InnerInner")
                    .stock("Z", 0, "unit")
                    .defaultSimulation("Day", 10.0, "Day")
                    .build();

            ModelDefinition innerDef = new ModelDefinitionBuilder()
                    .name("Inner")
                    .stock("Y", 0, "unit")
                    .module("nested", innerInnerDef, Map.of(), Map.of())
                    .defaultSimulation("Day", 10.0, "Day")
                    .build();

            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Outer")
                    .module("myModule", innerDef, Map.of("port1", "value1"), Map.of())
                    .defaultSimulation("Day", 10.0, "Day")
                    .build();

            ModelMetadata metadata = ModelMetadata.builder().license("CC-BY-SA-4.0").build();
            String source = generator.generate(def, metadata, "TestDemo",
                    "systems.courant.sd.demo", "test.mdl", List.of(), List.of());

            // The nested module should appear in the generated source
            assertThat(source).contains("\"InnerInner\"");
            assertThat(source).contains("\"nested\"");
            // Two ModuleInstanceDef calls: one for myModule, one for nested
            int count = countOccurrences(source, "new ModuleInstanceDef(");
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("nested module code should be indented deeper than parent module (#568)")
        void shouldIndentNestedModuleDeeperThanParent() {
            ModelDefinition innerInnerDef = new ModelDefinitionBuilder()
                    .name("InnerInner")
                    .stock("Z", 0, "unit")
                    .defaultSimulation("Day", 10.0, "Day")
                    .build();

            ModelDefinition innerDef = new ModelDefinitionBuilder()
                    .name("Inner")
                    .stock("Y", 0, "unit")
                    .module("nested", innerInnerDef, Map.of(), Map.of())
                    .defaultSimulation("Day", 10.0, "Day")
                    .build();

            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Outer")
                    .module("myModule", innerDef, Map.of(), Map.of())
                    .defaultSimulation("Day", 10.0, "Day")
                    .build();

            ModelMetadata metadata = ModelMetadata.builder().license("CC-BY-SA-4.0").build();
            String source = generator.generate(def, metadata, "TestDemo",
                    "systems.courant.sd.demo", "test.mdl", List.of(), List.of());

            // Outer module block opens at 8-space indent (INDENT)
            // Inner (nested) module block should open at 12-space indent (INDENT + 4)
            String[] lines = source.split("\n");
            int outerBlockIndent = -1;
            int nestedBlockIndent = -1;
            boolean foundOuter = false;
            for (String line : lines) {
                if (line.contains("\"Inner\"") && !line.contains("\"InnerInner\"")) {
                    outerBlockIndent = countLeadingSpaces(line);
                    foundOuter = true;
                }
                if (foundOuter && line.contains("\"InnerInner\"")) {
                    nestedBlockIndent = countLeadingSpaces(line);
                    break;
                }
            }
            assertThat(outerBlockIndent).as("outer block indent found").isGreaterThan(0);
            assertThat(nestedBlockIndent).as("nested block indent found").isGreaterThan(0);
            assertThat(nestedBlockIndent)
                    .as("nested module should be indented deeper than parent module")
                    .isGreaterThan(outerBlockIndent);
        }

        private int countLeadingSpaces(String line) {
            int count = 0;
            for (char c : line.toCharArray()) {
                if (c == ' ') {
                    count++;
                } else {
                    break;
                }
            }
            return count;
        }

        private int countOccurrences(String text, String search) {
            int count = 0;
            int idx = 0;
            while ((idx = text.indexOf(search, idx)) != -1) {
                count++;
                idx += search.length();
            }
            return count;
        }
    }
}
