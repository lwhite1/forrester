package com.deathrayresearch.forrester.io.vensim;

import com.deathrayresearch.forrester.io.ImportResult;
import com.deathrayresearch.forrester.model.def.AuxDef;
import com.deathrayresearch.forrester.model.def.FlowDef;
import com.deathrayresearch.forrester.model.def.LookupTableDef;
import com.deathrayresearch.forrester.model.def.ModelDefinition;
import com.deathrayresearch.forrester.model.def.ModelDefinitionBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("VensimExporter")
class VensimExporterTest {

    @Nested
    @DisplayName("Round-trip export → import")
    class RoundTrip {

        @Test
        void shouldRoundTripSirModel() {
            Path sirPath = Path.of("src/test/resources/vensim/sir.mdl");
            VensimImporter importer = new VensimImporter();
            ImportResult importResult;
            try {
                importResult = importer.importModel(sirPath);
            } catch (Exception e) {
                throw new RuntimeException("Failed to import sir.mdl", e);
            }

            ModelDefinition imported = importResult.definition();
            String mdl = VensimExporter.toVensim(imported);

            // Verify key structures in the exported output
            assertThat(mdl).startsWith("{UTF-8}");
            assertThat(mdl).contains("INTEG");
            assertThat(mdl).contains(".Control");
            assertThat(mdl).contains("FINAL TIME");
            assertThat(mdl).contains("INITIAL TIME");
            assertThat(mdl).contains("TIME STEP");
            assertThat(mdl).contains("SAVEPER");
            assertThat(mdl).contains("\\---/// Sketch");

            // Re-import the exported content and verify structures preserved
            ImportResult reImported = importer.importModel(mdl, "sir");
            ModelDefinition roundTripped = reImported.definition();

            assertThat(roundTripped.stocks()).hasSameSizeAs(imported.stocks());
            assertThat(roundTripped.constants()).hasSameSizeAs(imported.constants());

            // Auxiliaries may grow on round-trip because the importer creates
            // synthetic _net_flow flows that are re-classified as auxiliaries on
            // re-import. Verify that the original auxiliaries are still present.
            Set<String> roundTrippedAuxNames = roundTripped.auxiliaries().stream()
                    .map(AuxDef::name).collect(Collectors.toSet());
            for (AuxDef orig : imported.auxiliaries()) {
                assertThat(roundTrippedAuxNames).contains(orig.name());
            }
        }
    }

    @Nested
    @DisplayName("Stock INTEG reconstruction")
    class StockInteg {

        @Test
        void shouldReconstructIntegWithInflowsAndOutflows() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 100, "Day")
                    .stock("Tank", 50.0, "Gallons")
                    .flow(new FlowDef("fill rate", "10", "Day", null, "Tank"))
                    .flow(new FlowDef("refill rate", "5", "Day", null, "Tank"))
                    .flow(new FlowDef("drain rate", "3", "Day", "Tank", null))
                    .build();

            String mdl = VensimExporter.toVensim(def);

            assertThat(mdl).contains("Tank=");
            assertThat(mdl).contains("INTEG");
            // Should contain both inflows and outflow in the rate expression
            assertThat(mdl).contains("fill rate");
            assertThat(mdl).contains("refill rate");
            assertThat(mdl).contains("drain rate");
            assertThat(mdl).contains("50");
        }

        @Test
        void shouldProduceZeroRateForDisconnectedStock() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 100, "Day")
                    .stock("Inventory", 100.0, "Units")
                    .build();

            String mdl = VensimExporter.toVensim(def);

            assertThat(mdl).contains("INTEG");
            assertThat(mdl).contains("0,");
            assertThat(mdl).contains("100");
        }
    }

    @Nested
    @DisplayName("Constant export")
    class Constants {

        @Test
        void shouldExportConstantWithEqualsOperator() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 100, "Day")
                    .constant("growth rate", 0.05, "1/Day")
                    .build();

            String mdl = VensimExporter.toVensim(def);

            assertThat(mdl).contains("growth rate=");
            assertThat(mdl).contains("0.05");
            assertThat(mdl).contains("1/Day");
            // Should not use := operator
            assertThat(mdl).doesNotContain("growth rate:=");
        }
    }

    @Nested
    @DisplayName("Lookup table export")
    class LookupTables {

        @Test
        void shouldExportStandaloneLookupTable() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 100, "Day")
                    .lookupTable("effect curve",
                            new double[]{0, 1, 2, 3, 4},
                            new double[]{0, 0.5, 1.0, 0.5, 0},
                            "LINEAR")
                    .build();

            String mdl = VensimExporter.toVensim(def);

            assertThat(mdl).contains("effect curve(");
            assertThat(mdl).contains("(0,0)");
            assertThat(mdl).contains("(1,0.5)");
            assertThat(mdl).contains("(2,1)");
            assertThat(mdl).contains("(3,0.5)");
            assertThat(mdl).contains("(4,0)");
        }

        @Test
        void shouldExportAuxWithLookupAsWithLookup() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 100, "Day")
                    .lookupTable("my_lookup",
                            new double[]{0, 1, 2},
                            new double[]{0, 0.5, 1.0},
                            "LINEAR")
                    .aux(new AuxDef("effect", "LOOKUP(my_lookup, TIME)", null))
                    .build();

            String mdl = VensimExporter.toVensim(def);

            assertThat(mdl).contains("WITH LOOKUP");
            assertThat(mdl).contains("Time");
            assertThat(mdl).contains("(0,0)");
            assertThat(mdl).contains("(1,0.5)");
            assertThat(mdl).contains("(2,1)");
        }
    }

    @Nested
    @DisplayName("Expression translation")
    class ExprTranslation {

        @Test
        void shouldTranslateIfToIfThenElse() {
            assertThat(VensimExporter.toVensimExpr("IF(x > 0, 1, 0)"))
                    .contains("IF THEN ELSE(");
        }

        @Test
        void shouldTranslateLogicalOperators() {
            assertThat(VensimExporter.toVensimExpr("a && b"))
                    .contains(":AND:");
            assertThat(VensimExporter.toVensimExpr("a || b"))
                    .contains(":OR:");
        }

        @Test
        void shouldTranslateNotOperator() {
            assertThat(VensimExporter.toVensimExpr("!(x > 0)"))
                    .contains(":NOT:");
        }

        @Test
        void shouldTranslateComparisonOperators() {
            assertThat(VensimExporter.toVensimExpr("x == 0"))
                    .contains("x = 0");
            assertThat(VensimExporter.toVensimExpr("x != 0"))
                    .contains("x <> 0");
        }

        @Test
        void shouldTranslateTimeVariable() {
            assertThat(VensimExporter.toVensimExpr("TIME + 1"))
                    .isEqualTo("Time + 1");
        }
    }

    @Nested
    @DisplayName("Name denormalization")
    class NameDenormalization {

        @Test
        void shouldReplaceUnderscoresWithSpaces() {
            assertThat(VensimExporter.denormalizeName("Population_Growth"))
                    .isEqualTo("Population Growth");
        }

        @Test
        void shouldHandleSimpleNames() {
            assertThat(VensimExporter.denormalizeName("rate"))
                    .isEqualTo("rate");
        }

        @Test
        void shouldHandleNullAndBlank() {
            assertThat(VensimExporter.denormalizeName(null)).isEmpty();
            assertThat(VensimExporter.denormalizeName("  ")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Empty model")
    class EmptyModel {

        @Test
        void shouldExportEmptyModelWithHeaderAndControl() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Empty")
                    .defaultSimulation("Day", 100, "Day")
                    .build();

            String mdl = VensimExporter.toVensim(def);

            assertThat(mdl).startsWith("{UTF-8}");
            assertThat(mdl).contains(".Control");
            assertThat(mdl).contains("FINAL TIME");
            assertThat(mdl).contains("INITIAL TIME");
            assertThat(mdl).contains("TIME STEP");
            assertThat(mdl).contains("SAVEPER");
            assertThat(mdl).contains("\\---/// Sketch");
        }
    }

    @Nested
    @DisplayName("Comment preservation")
    class Comments {

        @Test
        void shouldPreserveCommentsInVariableBlocks() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 100, "Day")
                    .constant(new com.deathrayresearch.forrester.model.def.ConstantDef(
                            "rate", "The infection rate per day", 0.3, "1/Day"))
                    .build();

            String mdl = VensimExporter.toVensim(def);

            assertThat(mdl).contains("The infection rate per day");
        }
    }
}
