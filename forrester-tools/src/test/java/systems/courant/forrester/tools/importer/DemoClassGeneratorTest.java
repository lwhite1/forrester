package systems.courant.forrester.tools.importer;

import systems.courant.forrester.model.ModelMetadata;
import systems.courant.forrester.model.def.AuxDef;
import systems.courant.forrester.model.def.ConstantDef;
import systems.courant.forrester.model.def.FlowDef;
import systems.courant.forrester.model.def.LookupTableDef;
import systems.courant.forrester.model.def.ModelDefinitionBuilder;
import systems.courant.forrester.model.def.ModelDefinition;
import systems.courant.forrester.model.def.StockDef;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DemoClassGeneratorTest {

    private final DemoClassGenerator generator = new DemoClassGenerator();

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
                "systems.courant.forrester.demo", "test.xmile",
                List.of(), List.of());

        assertThat(source).contains("package systems.courant.forrester.demo;");
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
                "systems.courant.forrester.demo.epidemiology", "sir.xmile",
                List.of(), List.of());

        assertThat(source).contains(".author(\"Kermack & McKendrick\")");
        assertThat(source).contains(".source(\"SIR model (1927)\")");
        assertThat(source).contains(".license(\"CC-BY-SA-4.0\")");
        assertThat(source).contains(".url(\"https://doi.org/10.1098/rspa.1927.0118\")");
        assertThat(source).contains("* Author: Kermack & McKendrick");
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
                "systems.courant.forrester.demo", "test.mdl",
                List.of(), List.of());

        assertThat(source).contains("new ConstantDef(");
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
                "systems.courant.forrester.demo", "test.xmile",
                List.of(), List.of());

        assertThat(source).contains("new LookupTableDef(");
        assertThat(source).contains("new double[]{0.0, 1.0}");
        assertThat(source).contains("\"LINEAR\"");
    }

    @Test
    void shouldGenerateAuxiliaries() {
        ModelDefinition def = new ModelDefinitionBuilder()
                .name("Test")
                .stock("Pop", 100.0, "people")
                .aux("growth_rate", "Pop * 0.03", "people/Year")
                .defaultSimulation("Year", 10.0, "Year")
                .build();

        ModelMetadata metadata = ModelMetadata.builder().license("CC-BY-SA-4.0").build();

        String source = generator.generate(def, metadata, "TestDemo",
                "systems.courant.forrester.demo", "test.xmile",
                List.of(), List.of());

        assertThat(source).contains("new AuxDef(");
        assertThat(source).contains("\"growth_rate\"");
        assertThat(source).contains("\"Pop * 0.03\"");
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
                "systems.courant.forrester.demo", "test.xmile",
                List.of("Unsupported PULSE function"),
                List.of("Unresolved reference: missing_var"));

        assertThat(source).contains("Unsupported PULSE function");
        assertThat(source).contains("Unresolved reference: missing_var");
    }
}
