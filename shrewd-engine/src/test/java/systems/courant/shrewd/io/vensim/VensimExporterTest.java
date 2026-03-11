package systems.courant.shrewd.io.vensim;

import systems.courant.shrewd.io.ImportResult;
import systems.courant.shrewd.model.def.AuxDef;
import systems.courant.shrewd.model.def.CausalLinkDef;
import systems.courant.shrewd.model.def.CldVariableDef;
import systems.courant.shrewd.model.def.FlowDef;
import systems.courant.shrewd.model.def.LookupTableDef;
import systems.courant.shrewd.model.def.ModelDefinition;
import systems.courant.shrewd.model.def.ModelDefinitionBuilder;
import systems.courant.shrewd.model.def.StockDef;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

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
            assertThat(mdl).contains("\\---///");

            // Re-import the exported content and verify structures preserved
            ImportResult reImported = importer.importModel(mdl, "sir");
            ModelDefinition roundTripped = reImported.definition();

            assertThat(roundTripped.stocks()).hasSameSizeAs(imported.stocks());
            assertThat(roundTripped.auxiliaries()).hasSameSizeAs(imported.auxiliaries());
            assertThat(roundTripped.parameters()).hasSameSizeAs(imported.parameters());
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

            assertThat(mdl).contains("Tank= INTEG");
            assertThat(mdl).doesNotContain("Tank=\n\tINTEG");
            // Should contain both inflows and outflow in the rate expression
            assertThat(mdl).contains("fill rate");
            assertThat(mdl).contains("refill rate");
            assertThat(mdl).contains("drain rate");
            assertThat(mdl).contains("50");
        }

        @Test
        void shouldExportStockWithInitialExpression() {
            StockDef stock = new StockDef("Population", null, 0.0,
                    "Capacity * 0.5", "People", null, List.of());
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 100, "Day")
                    .stock(stock)
                    .constant("Capacity", 1000, "People")
                    .build();

            String mdl = VensimExporter.toVensim(def);

            assertThat(mdl).contains("Capacity");
            assertThat(mdl).contains("0.5");
            // Should NOT fall back to "0" (the numeric initialValue)
            assertThat(mdl).doesNotContain("INTEG (\n\t0,\n\t\t0)");
        }

        @Test
        void shouldFallBackToNumericInitialValueWhenNoExpression() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 100, "Day")
                    .stock("Tank", 42.0, "Gallons")
                    .build();

            String mdl = VensimExporter.toVensim(def);

            assertThat(mdl).contains("42");
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

            assertThat(mdl).contains("growth rate=\n\t0.05");
            assertThat(mdl).contains("1/Day");
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
            assertThat(VensimExporter.toVensimExpr("a and b"))
                    .contains(":AND:");
            assertThat(VensimExporter.toVensimExpr("a or b"))
                    .contains(":OR:");
        }

        @Test
        void shouldTranslateNotOperator() {
            assertThat(VensimExporter.toVensimExpr("not(x > 0)"))
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

        @Test
        void shouldDenormalizeDigitPrefixedNamesInExpressions() {
            assertThat(VensimExporter.toVensimExpr("_2nd_Batch * 2"))
                    .isEqualTo("2nd Batch * 2");
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

        @Test
        void shouldStripLeadingUnderscoreFromDigitPrefixedNames() {
            assertThat(VensimExporter.denormalizeName("_2nd_Batch"))
                    .isEqualTo("2nd Batch");
        }

        @Test
        void shouldPreserveLeadingUnderscoreForNonDigitNames() {
            assertThat(VensimExporter.denormalizeName("_internal"))
                    .isEqualTo(" internal");
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
            assertThat(mdl).contains("\\---///");
        }
    }

    @Nested
    @DisplayName("Vensim formatting conventions")
    class FormattingConventions {

        @Test
        void shouldPutIntegOnSameLineAsEquals() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 100, "Day")
                    .stock("Population", 1000.0, "People")
                    .build();

            String mdl = VensimExporter.toVensim(def);

            assertThat(mdl).contains("Population= INTEG (");
        }

        @Test
        void shouldFormatControlVariablesOnSingleLine() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 200, "Day")
                    .build();

            String mdl = VensimExporter.toVensim(def);

            assertThat(mdl).contains("INITIAL TIME  = 0");
            assertThat(mdl).contains("FINAL TIME  = 200");
            assertThat(mdl).contains("TIME STEP  = 1");
        }

        @Test
        void shouldFormatSaveperWithoutExtraBlankLine() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 100, "Day")
                    .build();

            String mdl = VensimExporter.toVensim(def);

            assertThat(mdl).contains("SAVEPER  =\n        TIME STEP\n");
            // Should NOT have a double newline between = and TIME STEP
            assertThat(mdl).doesNotContain("SAVEPER  =\n\t\n");
        }
    }

    @Nested
    @DisplayName("CLD variable export")
    class CldVariables {

        @Test
        void shouldExportCldVariablesWithPlaceholderEquation() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Year", 100, "Year")
                    .cldVariable("Population", "The total population")
                    .cldVariable("Birth_Rate", "Rate of births")
                    .build();

            String mdl = VensimExporter.toVensim(def);

            assertThat(mdl).contains("Population=\n\t0");
            assertThat(mdl).contains("Birth Rate=\n\t0");
            assertThat(mdl).contains("The total population");
            assertThat(mdl).contains("Rate of births");
        }

        @Test
        void shouldRoundTripCldModel() {
            // Build a CLD with a view so the sketch section has element placements,
            // allowing the importer to detect CLD mode on re-import
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("CLD")
                    .defaultSimulation("Year", 100, "Year")
                    .cldVariable("Population", "The total population")
                    .cldVariable("Birth_Rate", "Rate of births")
                    .cldVariable("Death_Rate", "Rate of deaths")
                    .causalLink("Birth_Rate", "Population", CausalLinkDef.Polarity.POSITIVE)
                    .view(new systems.courant.shrewd.model.def.ViewDef("CLD",
                            List.of(
                                    new systems.courant.shrewd.model.def.ElementPlacement(
                                            "Population", systems.courant.shrewd.model.def.ElementType.CLD_VARIABLE, 200, 200),
                                    new systems.courant.shrewd.model.def.ElementPlacement(
                                            "Birth_Rate", systems.courant.shrewd.model.def.ElementType.CLD_VARIABLE, 100, 100),
                                    new systems.courant.shrewd.model.def.ElementPlacement(
                                            "Death_Rate", systems.courant.shrewd.model.def.ElementType.CLD_VARIABLE, 300, 100)),
                            List.of(new systems.courant.shrewd.model.def.ConnectorRoute("Birth_Rate", "Population")),
                            List.of()))
                    .build();

            String mdl = VensimExporter.toVensim(def);

            // Verify sketch section has element placements and connector
            assertThat(mdl).contains("*CLD");
            assertThat(mdl).contains("10,1,Population,200,200");
            assertThat(mdl).contains("10,2,Birth Rate,100,100");
            assertThat(mdl).contains("1,4,2,1");

            // Re-import — sketch with no flow valves + no stocks → CLD mode
            VensimImporter importer = new VensimImporter();
            ImportResult result = importer.importModel(mdl, "CLD");

            assertThat(result.definition().cldVariables()).hasSize(3);
            assertThat(result.definition().causalLinks()).hasSize(1);
            assertThat(result.definition().parameters()).hasSize(3); // only built-in constants
        }
    }

    @Nested
    @DisplayName("Sketch export")
    class SketchExport {

        @Test
        void shouldExportViewElementsAndConnectors() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 100, "Day")
                    .stock("Tank", 50.0, "Gallons")
                    .constant("rate", 10.0, "Gallons/Day")
                    .view(new systems.courant.shrewd.model.def.ViewDef("Main",
                            List.of(
                                    new systems.courant.shrewd.model.def.ElementPlacement(
                                            "Tank", systems.courant.shrewd.model.def.ElementType.STOCK, 200, 150),
                                    new systems.courant.shrewd.model.def.ElementPlacement(
                                            "rate", systems.courant.shrewd.model.def.ElementType.AUX, 100, 300)),
                            List.of(new systems.courant.shrewd.model.def.ConnectorRoute("rate", "Tank")),
                            List.of()))
                    .build();

            String mdl = VensimExporter.toVensim(def);

            assertThat(mdl).contains("*Main");
            assertThat(mdl).contains("10,1,Tank,200,150");
            assertThat(mdl).contains("10,2,rate,100,300");
            assertThat(mdl).contains("1,3,2,1");
        }

        @Test
        void shouldRoundTripSketchPositions() {
            Path sirPath = Path.of("src/test/resources/vensim/sir.mdl");
            VensimImporter importer = new VensimImporter();
            ImportResult importResult;
            try {
                importResult = importer.importModel(sirPath);
            } catch (Exception e) {
                throw new RuntimeException("Failed to import sir.mdl", e);
            }

            String mdl = VensimExporter.toVensim(importResult.definition());

            // Re-import and verify sketch data survives
            ImportResult reImported = importer.importModel(mdl, "sir");
            assertThat(reImported.definition().views()).isNotEmpty();
            assertThat(reImported.definition().views().getFirst().elements()).isNotEmpty();
            assertThat(reImported.definition().views().getFirst().connectors()).isNotEmpty();
        }

        @Test
        void shouldWriteFlowValvesAsType11() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 100, "Day")
                    .stock("Tank", 50.0, "Gallons")
                    .flow(new FlowDef("fill", "10", "Day", null, "Tank"))
                    .view(new systems.courant.shrewd.model.def.ViewDef("Main",
                            List.of(
                                    new systems.courant.shrewd.model.def.ElementPlacement(
                                            "Tank", systems.courant.shrewd.model.def.ElementType.STOCK, 200, 150),
                                    new systems.courant.shrewd.model.def.ElementPlacement(
                                            "fill", systems.courant.shrewd.model.def.ElementType.FLOW, 100, 150)),
                            List.of(),
                            List.of()))
                    .build();

            String mdl = VensimExporter.toVensim(def);

            assertThat(mdl).contains("10,1,Tank,200,150");
            assertThat(mdl).contains("11,2,fill,100,150");
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
                    .aux(new AuxDef("rate", "The infection rate per day", 0.3, "1/Day"))
                    .build();

            String mdl = VensimExporter.toVensim(def);

            assertThat(mdl).contains("The infection rate per day");
        }
    }
}
