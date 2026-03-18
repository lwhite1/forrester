package systems.courant.sd.io.vensim;

import systems.courant.sd.io.ImportResult;
import systems.courant.sd.model.def.VariableDef;
import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.def.CldVariableDef;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.LookupTableDef;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModelDefinitionBuilder;
import systems.courant.sd.model.def.StockDef;
import systems.courant.sd.model.def.SubscriptDef;

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
            assertThat(mdl).contains("\\\\\\---///");

            // Re-import the exported content and verify structures preserved
            ImportResult reImported = importer.importModel(mdl, "sir");
            ModelDefinition roundTripped = reImported.definition();

            assertThat(roundTripped.stocks()).hasSameSizeAs(imported.stocks());
            // Variables may differ in count because decomposed flows (e.g. inflow/outflow)
            // become separate equation blocks on export, which re-import as additional auxs.
            assertThat(roundTripped.variables().size())
                    .isGreaterThanOrEqualTo(imported.variables().size());
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
        void shouldExportCorrectYRangeForLookupTable() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 100, "Day")
                    .lookupTable("effect curve",
                            new double[]{0, 1, 2, 3},
                            new double[]{5.0, 10.0, 3.0, 8.0},
                            "LINEAR")
                    .build();

            String mdl = VensimExporter.toVensim(def);
            // Range annotation should use ymin=3 and ymax=10
            assertThat(mdl).contains("(0,3)-(3,10)");
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
                    .variable(new VariableDef("effect", "LOOKUP(my_lookup, TIME)", null))
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
        @DisplayName("Issue #648 — mixed-case AND/OR/NOT should be converted")
        void shouldTranslateMixedCaseLogicalOperators() {
            assertThat(VensimExporter.toVensimExpr("x AND y"))
                    .contains(":AND:");
            assertThat(VensimExporter.toVensimExpr("a Or b"))
                    .contains(":OR:");
            assertThat(VensimExporter.toVensimExpr("NOT(flag)"))
                    .contains(":NOT:");
            assertThat(VensimExporter.toVensimExpr("x And y Or z"))
                    .contains(":AND:")
                    .contains(":OR:");
        }

        @Test
        void shouldTranslateComparisonOperators() {
            assertThat(VensimExporter.toVensimExpr("x == 0"))
                    .contains("x = 0");
            assertThat(VensimExporter.toVensimExpr("x != 0"))
                    .contains("x <> 0");
        }

        @Test
        void shouldNotMangleAdjacentOperators() {
            // Malformed !== should not produce invalid <>=
            assertThat(VensimExporter.toVensimExpr("x !== 0"))
                    .doesNotContain("<>=");
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

        @Test
        void shouldDenormalizeQuotedVariableNamesAsAtomicUnit() {
            assertThat(VensimExporter.toVensimExpr("\"my_var\" + 5"))
                    .isEqualTo("\"my var\" + 5");
        }

        @Test
        void shouldDenormalizeQuotedDigitPrefixedName() {
            assertThat(VensimExporter.toVensimExpr("\"_2nd_Batch\" * x"))
                    .isEqualTo("\"2nd Batch\" * x");
        }

        @Test
        void shouldHandleUnclosedQuoteGracefully() {
            assertThat(VensimExporter.toVensimExpr("\"unclosed + x"))
                    .isEqualTo("\"unclosed + x");
        }
    }

    @Nested
    @DisplayName("Name denormalization")
    class NameDenormalization {

        @Test
        void shouldReplaceUnderscoresWithSpacesForNonVensimNames() {
            // Names without spaces (XMILE/native): underscores are word separators
            assertThat(VensimExporter.denormalizeName("Population_Growth"))
                    .isEqualTo("Population Growth");
        }

        @Test
        void shouldPreserveUnderscoresWhenNameHasSpaces() {
            // Names with spaces (Vensim display-name format): underscores are literal
            assertThat(VensimExporter.denormalizeName("Net_Flow Rate"))
                    .isEqualTo("Net_Flow Rate");
            assertThat(VensimExporter.denormalizeName("Population Growth"))
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
            // Vensim display-name format preserves spaces
            assertThat(VensimExporter.denormalizeName("_2nd Batch"))
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
            assertThat(mdl).contains("\\\\\\---///");
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
                    .view(new systems.courant.sd.model.def.ViewDef("CLD",
                            List.of(
                                    new systems.courant.sd.model.def.ElementPlacement(
                                            "Population", systems.courant.sd.model.def.ElementType.CLD_VARIABLE, 200, 200),
                                    new systems.courant.sd.model.def.ElementPlacement(
                                            "Birth_Rate", systems.courant.sd.model.def.ElementType.CLD_VARIABLE, 100, 100),
                                    new systems.courant.sd.model.def.ElementPlacement(
                                            "Death_Rate", systems.courant.sd.model.def.ElementType.CLD_VARIABLE, 300, 100)),
                            List.of(new systems.courant.sd.model.def.ConnectorRoute("Birth_Rate", "Population")),
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
                    .view(new systems.courant.sd.model.def.ViewDef("Main",
                            List.of(
                                    new systems.courant.sd.model.def.ElementPlacement(
                                            "Tank", systems.courant.sd.model.def.ElementType.STOCK, 200, 150),
                                    new systems.courant.sd.model.def.ElementPlacement(
                                            "rate", systems.courant.sd.model.def.ElementType.AUX, 100, 300)),
                            List.of(new systems.courant.sd.model.def.ConnectorRoute("rate", "Tank")),
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
                    .view(new systems.courant.sd.model.def.ViewDef("Main",
                            List.of(
                                    new systems.courant.sd.model.def.ElementPlacement(
                                            "Tank", systems.courant.sd.model.def.ElementType.STOCK, 200, 150),
                                    new systems.courant.sd.model.def.ElementPlacement(
                                            "fill", systems.courant.sd.model.def.ElementType.FLOW, 100, 150)),
                            List.of(),
                            List.of()))
                    .build();

            String mdl = VensimExporter.toVensim(def);

            assertThat(mdl).contains("10,1,Tank,200,150");
            assertThat(mdl).contains("11,2,fill,100,150");
        }

        @Test
        void shouldSkipConnectorsWithUnknownEndpoints() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 100, "Day")
                    .stock("Tank", 50.0, "Gallons")
                    .view(new systems.courant.sd.model.def.ViewDef("Main",
                            List.of(
                                    new systems.courant.sd.model.def.ElementPlacement(
                                            "Tank", systems.courant.sd.model.def.ElementType.STOCK, 200, 150)),
                            List.of(new systems.courant.sd.model.def.ConnectorRoute("missing_source", "Tank"),
                                    new systems.courant.sd.model.def.ConnectorRoute("Tank", "missing_target")),
                            List.of()))
                    .build();

            String mdl = VensimExporter.toVensim(def);

            // Connectors with unknown endpoints should be omitted entirely — no ",0," IDs
            assertThat(mdl).doesNotContain(",0,");
            // The valid element should still be present
            assertThat(mdl).contains("10,1,Tank,200,150");
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
                    .variable(new VariableDef("rate", "The infection rate per day", 0.3, "1/Day"))
                    .build();

            String mdl = VensimExporter.toVensim(def);

            assertThat(mdl).contains("The infection rate per day");
        }
    }

    @Nested
    @DisplayName("Subscript export (#484)")
    class SubscriptExport {

        @Test
        void shouldExportSubscriptDefinition() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 100, "Day")
                    .subscript("Region", List.of("North", "South", "East"))
                    .build();

            String mdl = VensimExporter.toVensim(def);

            assertThat(mdl).contains("Region:\n\tNorth, South, East");
        }

        @Test
        void shouldExportSubscriptedStock() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 100, "Day")
                    .subscript("Region", List.of("North", "South"))
                    .stock("Population", 100.0, "People", List.of("Region"))
                    .build();

            String mdl = VensimExporter.toVensim(def);

            assertThat(mdl).contains("Population[Region]= INTEG");
        }

        @Test
        void shouldExportSubscriptedFlow() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 100, "Day")
                    .subscript("Region", List.of("North", "South"))
                    .flow("migration", "10", "Day", null, "Population",
                            List.of("Region"))
                    .stock("Population", 100.0, "People", List.of("Region"))
                    .build();

            String mdl = VensimExporter.toVensim(def);

            assertThat(mdl).contains("migration[Region]=");
        }

        @Test
        void shouldExportSubscriptedAux() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 100, "Day")
                    .subscript("Region", List.of("North", "South"))
                    .variable("growth_rate", "0.05", "1/Day", List.of("Region"))
                    .build();

            String mdl = VensimExporter.toVensim(def);

            assertThat(mdl).contains("growth rate[Region]=");
        }

        @Test
        void shouldExportMultiDimensionalSubscript() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 100, "Day")
                    .subscript("Region", List.of("North", "South"))
                    .subscript("Age_Group", List.of("Young", "Old"))
                    .stock("Population", 100.0, "People", List.of("Region", "Age_Group"))
                    .build();

            String mdl = VensimExporter.toVensim(def);

            assertThat(mdl).contains("Population[Region,Age Group]= INTEG");
        }
    }

    @Nested
    @DisplayName("XIDZ/ZIDZ reverse mapping (#485)")
    class XidzZidzReverse {

        @Test
        void shouldReverseZidzPattern() {
            // ZIDZ(a, b) was imported as: IF((b) == 0, 0, (a) / (b))
            String expr = "IF((denominator) == 0, 0, (numerator) / (denominator))";
            String result = VensimExporter.reverseXidzZidz(expr);
            assertThat(result).isEqualTo("ZIDZ(numerator, denominator)");
        }

        @Test
        void shouldReverseXidzPattern() {
            // XIDZ(a, b, x) was imported as: IF((b) == 0, x, (a) / (b))
            String expr = "IF((b) == 0, fallback, (a) / (b))";
            String result = VensimExporter.reverseXidzZidz(expr);
            assertThat(result).isEqualTo("XIDZ(a, b, fallback)");
        }

        @Test
        void shouldNotReverseUnrelatedIfExpression() {
            String expr = "IF(x > 0, 1, 0)";
            String result = VensimExporter.reverseXidzZidz(expr);
            assertThat(result).isEqualTo("IF(x > 0, 1, 0)");
        }

        @Test
        void shouldReverseXidzInFullExpressionPipeline() {
            String result = VensimExporter.toVensimExpr(
                    "IF((b) == 0, 0, (a) / (b))");
            assertThat(result).contains("ZIDZ");
            assertThat(result).doesNotContain("IF THEN ELSE");
        }
    }

    @Nested
    @DisplayName("SMOOTH/DELAY reverse mapping (#486)")
    class SmoothDelayReverse {

        @Test
        void shouldReverseDelayFixed() {
            assertThat(VensimExporter.toVensimExpr("DELAY_FIXED(input, 5, 0)"))
                    .isEqualTo("DELAY FIXED(input, 5, 0)");
        }

        @Test
        void shouldReverseRandomUniform() {
            assertThat(VensimExporter.toVensimExpr("RANDOM_UNIFORM(0, 1, seed)"))
                    .isEqualTo("RANDOM UNIFORM(0, 1, seed)");
        }

        @Test
        void shouldReversePulseTrain() {
            assertThat(VensimExporter.toVensimExpr("PULSE_TRAIN(1, 2, 3, 10)"))
                    .isEqualTo("PULSE TRAIN(1, 2, 3, 10)");
        }

        @Test
        void shouldPreserveSmoothFunctions() {
            // SMOOTH, SMOOTH3, SMOOTHI, SMOOTH3I use same name in both systems
            assertThat(VensimExporter.toVensimExpr("SMOOTH(x, 3)"))
                    .isEqualTo("SMOOTH(x, 3)");
            assertThat(VensimExporter.toVensimExpr("SMOOTH3(x, 3)"))
                    .isEqualTo("SMOOTH3(x, 3)");
            assertThat(VensimExporter.toVensimExpr("DELAY1(x, 3)"))
                    .isEqualTo("DELAY1(x, 3)");
            assertThat(VensimExporter.toVensimExpr("DELAY3I(x, 3, init)"))
                    .isEqualTo("DELAY3I(x, 3, init)");
        }
    }

    @Nested
    @DisplayName("SAMPLE_IF_TRUE / FIND_ZERO / LOOKUP_AREA reverse mapping (#866)")
    class MissingFunctionReverse {

        @Test
        void shouldReverseSampleIfTrue() {
            assertThat(VensimExporter.toVensimExpr("SAMPLE_IF_TRUE(cond, input, initial)"))
                    .isEqualTo("SAMPLE IF TRUE(cond, input, initial)");
        }

        @Test
        void shouldReverseFindZero() {
            assertThat(VensimExporter.toVensimExpr("FIND_ZERO(x, lo, hi)"))
                    .isEqualTo("FIND ZERO(x, lo, hi)");
        }

        @Test
        void shouldReverseLookupArea() {
            assertThat(VensimExporter.toVensimExpr("LOOKUP_AREA(table, lo, hi)"))
                    .isEqualTo("LOOKUP AREA(table, lo, hi)");
        }

        @Test
        void shouldPreserveFunctionNamesInMixedExpressions() {
            // Ensure underscores in function names are not replaced with spaces
            // by the denormalization step
            assertThat(VensimExporter.toVensimExpr("SAMPLE_IF_TRUE(flag, my_var, 0)"))
                    .isEqualTo("SAMPLE IF TRUE(flag, my var, 0)");
        }

        @Test
        void shouldHandleCaseInsensitiveReversal() {
            assertThat(VensimExporter.toVensimExpr("sample_if_true(cond, x, 0)"))
                    .isEqualTo("SAMPLE IF TRUE(cond, x, 0)");
            assertThat(VensimExporter.toVensimExpr("find_zero(x, 0, 10)"))
                    .isEqualTo("FIND ZERO(x, 0, 10)");
            assertThat(VensimExporter.toVensimExpr("lookup_area(tbl, 0, 5)"))
                    .isEqualTo("LOOKUP AREA(tbl, 0, 5)");
        }
    }

    @Nested
    @DisplayName("WITH LOOKUP complex expressions (#487)")
    class WithLookupComplex {

        @Test
        void shouldInlineLookupInComplexExpression() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 100, "Day")
                    .lookupTable("effect_table",
                            new double[]{0, 1, 2},
                            new double[]{0, 0.5, 1.0},
                            "LINEAR")
                    .variable(new VariableDef("scaled_effect",
                            "LOOKUP(effect_table, TIME) * factor", "Dmnl"))
                    .constant("factor", 2.0, "Dmnl")
                    .build();

            String mdl = VensimExporter.toVensim(def);

            // The LOOKUP call should be inlined as table-call syntax
            assertThat(mdl).contains("effect table(Time) * factor");
            // The lookup table should still be emitted as a standalone block
            assertThat(mdl).contains("effect table(");
            assertThat(mdl).contains("(0,0)");
        }

        @Test
        void shouldStillUseWithLookupForSimpleCases() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 100, "Day")
                    .lookupTable("my_lookup",
                            new double[]{0, 1, 2},
                            new double[]{0, 0.5, 1.0},
                            "LINEAR")
                    .variable(new VariableDef("effect", "LOOKUP(my_lookup, TIME)", null))
                    .build();

            String mdl = VensimExporter.toVensim(def);

            assertThat(mdl).contains("WITH LOOKUP");
        }
    }

    @Nested
    @DisplayName("Coordinate precision (#488)")
    class CoordinatePrecision {

        @Test
        void shouldPreserveFractionalCoordinates() {
            assertThat(VensimExporter.formatCoord(200.5)).isEqualTo("200.5");
            assertThat(VensimExporter.formatCoord(150.75)).isEqualTo("150.75");
        }

        @Test
        void shouldFormatIntegerCoordinatesWithoutDecimal() {
            assertThat(VensimExporter.formatCoord(200.0)).isEqualTo("200");
            assertThat(VensimExporter.formatCoord(0.0)).isEqualTo("0");
        }

        @Test
        void shouldPreserveFractionalCoordsInSketchExport() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .defaultSimulation("Day", 100, "Day")
                    .stock("Tank", 50.0, "Gallons")
                    .view(new systems.courant.sd.model.def.ViewDef("Main",
                            List.of(new systems.courant.sd.model.def.ElementPlacement(
                                    "Tank", systems.courant.sd.model.def.ElementType.STOCK,
                                    200.5, 150.75)),
                            List.of(), List.of()))
                    .build();

            String mdl = VensimExporter.toVensim(def);

            assertThat(mdl).contains("200.5,150.75");
        }
    }

    @Nested
    @DisplayName("extractEqZeroOperand balanced parens (#633)")
    class ExtractEqZeroOperand {

        @Test
        void shouldMatchBalancedParensAroundOperand() {
            // (a + b) == 0 should extract "a + b"
            String result = VensimExporter.reverseXidzZidz(
                    "IF((a + b) == 0, 0, (x) / (a + b))");
            assertThat(result).isEqualTo("ZIDZ(x, a + b)");
        }

        @Test
        void shouldNotMatchUnbalancedParens() {
            // (a) + (b == 0 has unbalanced parens — should NOT match XIDZ/ZIDZ
            String expr = "IF((a) + (b == 0, fallback, something)";
            String result = VensimExporter.reverseXidzZidz(expr);
            // Should pass through unchanged (no XIDZ/ZIDZ match)
            assertThat(result).isEqualTo(expr);
        }
    }

    @Nested
    @DisplayName("reverseXidzZidz skip non-matching IF (#629)")
    class ReverseXidzZidzSkip {

        @Test
        void shouldNotInfiniteLoopOnNonMatchingIf() {
            // Two IF calls, first doesn't match XIDZ pattern, second does
            String expr = "IF(x > 0, 1, 0) + IF((b) == 0, 0, (a) / (b))";
            String result = VensimExporter.reverseXidzZidz(expr);
            assertThat(result).contains("ZIDZ(a, b)");
            // First IF should remain unchanged
            assertThat(result).startsWith("IF(x > 0, 1, 0)");
        }

        @Test
        void shouldHandleConsecutiveNonMatchingIfs() {
            String expr = "IF(a, b, c) + IF(d, e, f)";
            String result = VensimExporter.reverseXidzZidz(expr);
            // Neither matches — should pass through unchanged without looping
            assertThat(result).isEqualTo(expr);
        }
    }
}
