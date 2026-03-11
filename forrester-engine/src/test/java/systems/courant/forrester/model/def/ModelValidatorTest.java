package systems.courant.forrester.model.def;

import systems.courant.forrester.model.def.CausalLinkDef.Polarity;
import systems.courant.forrester.model.def.ValidationIssue.Severity;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ModelValidator")
class ModelValidatorTest {

    @Nested
    @DisplayName("Clean model")
    class CleanModel {

        @Test
        void shouldReturnNoIssuesForValidModel() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("SIR")
                    .stock("S", 1000, "Person")
                    .stock("I", 10, "Person")
                    .stock("R", 0, "Person")
                    .flow("Infection", "S * I * 0.001", "Day", "S", "I")
                    .flow("Recovery", "I * 0.1", "Day", "I", "R")
                    .build();

            ValidationResult result = ModelValidator.validate(def);

            assertThat(result.isClean()).isTrue();
            assertThat(result.errorCount()).isZero();
            assertThat(result.warningCount()).isZero();
        }
    }

    @Nested
    @DisplayName("DefinitionValidator errors wrapped as ERROR")
    class DefinitionValidatorErrors {

        @Test
        void shouldWrapDuplicateNameAsError() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Dup")
                    .stock("S", 100, "Person")
                    .constant("S", 5.0, "Person")
                    .build();

            ValidationResult result = ModelValidator.validate(def);

            assertThat(result.errorCount()).isGreaterThan(0);
            assertThat(result.issues()).anyMatch(i ->
                    i.severity() == Severity.ERROR && i.message().contains("Duplicate"));
        }

        @Test
        void shouldExtractElementNameFromError() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Bad")
                    .stock("A", 100, "Person")
                    .flow("F", "A +", "Day", "A", null)
                    .build();

            ValidationResult result = ModelValidator.validate(def);

            assertThat(result.issues()).anyMatch(i ->
                    i.severity() == Severity.ERROR
                            && "F".equals(i.elementName())
                            && i.message().contains("invalid equation"));
        }
    }

    @Nested
    @DisplayName("Disconnected flows")
    class DisconnectedFlows {

        @Test
        void shouldWarnForFlowWithNoSourceOrSink() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Open")
                    .flow("Leak", "10", "Day", null, null)
                    .build();

            ValidationResult result = ModelValidator.validate(def);

            assertThat(result.issues()).anyMatch(i ->
                    i.severity() == Severity.WARNING
                            && "Leak".equals(i.elementName())
                            && i.message().contains("disconnected"));
        }

        @Test
        void shouldNotWarnForFlowWithSource() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("OK")
                    .stock("S", 100, "Person")
                    .flow("Drain", "S * 0.1", "Day", "S", null)
                    .build();

            ValidationResult result = ModelValidator.validate(def);

            assertThat(result.issues()).noneMatch(i ->
                    i.message().contains("disconnected"));
        }
    }

    @Nested
    @DisplayName("Missing units")
    class MissingUnits {

        @Test
        void shouldWarnForStockWithNoUnit() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("NoUnit")
                    .stock("S", 100, null)
                    .build();

            ValidationResult result = ModelValidator.validate(def);

            assertThat(result.issues()).anyMatch(i ->
                    i.severity() == Severity.WARNING
                            && "S".equals(i.elementName())
                            && i.message().contains("no unit"));
        }

        @Test
        void shouldWarnForAuxWithBlankUnit() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("NoUnit")
                    .stock("S", 100, "Person")
                    .aux("A", "S * 2", "  ")
                    .build();

            ValidationResult result = ModelValidator.validate(def);

            assertThat(result.issues()).anyMatch(i ->
                    i.severity() == Severity.WARNING
                            && "A".equals(i.elementName())
                            && i.message().contains("no unit"));
        }

        @Test
        void shouldWarnForConstantWithNoUnit() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("NoUnit")
                    .constant("K", 5.0, null)
                    .build();

            ValidationResult result = ModelValidator.validate(def);

            assertThat(result.issues()).anyMatch(i ->
                    i.severity() == Severity.WARNING
                            && "K".equals(i.elementName())
                            && i.message().contains("no unit"));
        }
    }

    @Nested
    @DisplayName("Algebraic loops")
    class AlgebraicLoops {

        @Test
        void shouldWarnForCircularAuxDependencyWithoutStock() {
            // A = B * 2, B = A + 1 — circular without a stock
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Loop")
                    .aux("A", "B * 2", "X")
                    .aux("B", "A + 1", "X")
                    .build();

            ValidationResult result = ModelValidator.validate(def);

            assertThat(result.issues()).anyMatch(i ->
                    i.severity() == Severity.WARNING
                            && i.message().contains("Algebraic loop"));
        }

        @Test
        void shouldNotWarnForStockFlowLoop() {
            // Stock → Flow → Stock is a normal feedback loop, not algebraic
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Normal")
                    .stock("S", 100, "Person")
                    .flow("F", "S * 0.1", "Day", "S", null)
                    .build();

            ValidationResult result = ModelValidator.validate(def);

            assertThat(result.issues()).noneMatch(i ->
                    i.message().contains("Algebraic loop"));
        }
    }

    @Nested
    @DisplayName("Unused elements")
    class UnusedElements {

        @Test
        void shouldWarnForUnusedConstant() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Unused")
                    .stock("S", 100, "Person")
                    .flow("F", "S * 0.1", "Day", "S", null)
                    .constant("K", 5.0, "X")
                    .build();

            ValidationResult result = ModelValidator.validate(def);

            assertThat(result.issues()).anyMatch(i ->
                    i.severity() == Severity.WARNING
                            && "K".equals(i.elementName())
                            && i.message().contains("not referenced"));
        }

        @Test
        void shouldWarnForUnusedLookupTable() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Unused")
                    .stock("S", 100, "Person")
                    .flow("F", "S * 0.1", "Day", "S", null)
                    .lookupTable("Table1",
                            new double[]{0, 1, 2}, new double[]{0, 5, 10}, "LINEAR")
                    .build();

            ValidationResult result = ModelValidator.validate(def);

            assertThat(result.issues()).anyMatch(i ->
                    i.severity() == Severity.WARNING
                            && "Table1".equals(i.elementName())
                            && i.message().contains("not referenced"));
        }

        @Test
        void shouldNotWarnForUsedConstant() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Used")
                    .stock("S", 100, "Person")
                    .flow("F", "S * K", "Day", "S", null)
                    .constant("K", 0.1, "1/Day")
                    .build();

            ValidationResult result = ModelValidator.validate(def);

            assertThat(result.issues()).noneMatch(i ->
                    i.message().contains("not referenced"));
        }

        @Test
        void shouldNotWarnForConstantReferencedByAux() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Used")
                    .stock("S", 100, "Person")
                    .aux("Rate", "K * 2", "1/Day")
                    .flow("F", "S * Rate", "Day", "S", null)
                    .constant("K", 0.05, "1/Day")
                    .build();

            ValidationResult result = ModelValidator.validate(def);

            assertThat(result.issues()).noneMatch(i ->
                    i.message().contains("not referenced"));
        }
    }

    @Nested
    @DisplayName("Isolated stocks")
    class IsolatedStocks {

        @Test
        void shouldWarnForStockWithNoFlows() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Isolated")
                    .stock("Inventory", 100, "Widget")
                    .build();

            ValidationResult result = ModelValidator.validate(def);

            assertThat(result.issues()).anyMatch(i ->
                    i.severity() == Severity.WARNING
                            && "Inventory".equals(i.elementName())
                            && i.message().contains("no inflows or outflows"));
        }

        @Test
        void shouldNotWarnForStockWithInflow() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("HasInflow")
                    .stock("S", 100, "Person")
                    .flow("births", "10", "Day", null, "S")
                    .build();

            ValidationResult result = ModelValidator.validate(def);

            assertThat(result.issues()).noneMatch(i ->
                    i.message().contains("no inflows or outflows"));
        }

        @Test
        void shouldNotWarnForStockWithOutflow() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("HasOutflow")
                    .stock("S", 100, "Person")
                    .flow("deaths", "S * 0.1", "Day", "S", null)
                    .build();

            ValidationResult result = ModelValidator.validate(def);

            assertThat(result.issues()).noneMatch(i ->
                    i.message().contains("no inflows or outflows"));
        }
    }

    @Nested
    @DisplayName("Dangling connectors")
    class DanglingConnectors {

        @Test
        void shouldWarnForConnectorNotMatchingEquation() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Dangling")
                    .stock("Population", 1000, "Person")
                    .aux("effect", "42", "Dimensionless")
                    .view(new ViewDef("main", List.of(),
                            List.of(new ConnectorRoute("Population", "effect")),
                            List.of()))
                    .build();

            ValidationResult result = ModelValidator.validate(def);

            assertThat(result.issues()).anyMatch(i ->
                    i.severity() == Severity.WARNING
                            && "effect".equals(i.elementName())
                            && i.message().contains("does not reference")
                            && i.message().contains("Population"));
        }

        @Test
        void shouldNotWarnWhenEquationReferencesSource() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Valid")
                    .stock("Population", 1000, "Person")
                    .aux("effect", "Population * 0.5", "Person")
                    .view(new ViewDef("main", List.of(),
                            List.of(new ConnectorRoute("Population", "effect")),
                            List.of()))
                    .build();

            ValidationResult result = ModelValidator.validate(def);

            assertThat(result.issues()).noneMatch(i ->
                    i.message().contains("does not reference"));
        }

        @Test
        void shouldNotWarnForConnectorToStock() {
            // Stocks don't have equations — connectors to stocks are layout-only
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("ToStock")
                    .stock("S", 100, "Person")
                    .aux("A", "10", "Person")
                    .view(new ViewDef("main", List.of(),
                            List.of(new ConnectorRoute("A", "S")),
                            List.of()))
                    .build();

            ValidationResult result = ModelValidator.validate(def);

            assertThat(result.issues()).noneMatch(i ->
                    i.message().contains("does not reference"));
        }

        @Test
        void shouldHandleUnderscoreSpaceResolution() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Underscore")
                    .stock("birth rate", 0.03, "1/Year")
                    .aux("effect", "birth_rate * 2", "1/Year")
                    .view(new ViewDef("main", List.of(),
                            List.of(new ConnectorRoute("birth rate", "effect")),
                            List.of()))
                    .build();

            ValidationResult result = ModelValidator.validate(def);

            assertThat(result.issues()).noneMatch(i ->
                    i.message().contains("does not reference"));
        }
    }

    @Nested
    @DisplayName("CLD validation")
    class CldValidation {

        @Test
        void shouldWarnForOrphanedCldVariable() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Orphan")
                    .cldVariable("Lonely")
                    .build();

            ValidationResult result = ModelValidator.validate(def);

            assertThat(result.issues()).anyMatch(i ->
                    i.severity() == Severity.WARNING
                            && "Lonely".equals(i.elementName())
                            && i.message().contains("not connected"));
        }

        @Test
        void shouldNotWarnForConnectedCldVariable() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Connected")
                    .cldVariable("A")
                    .cldVariable("B")
                    .causalLink("A", "B", Polarity.POSITIVE)
                    .build();

            ValidationResult result = ModelValidator.validate(def);

            assertThat(result.issues()).noneMatch(i ->
                    i.message().contains("not connected"));
        }

        @Test
        void shouldErrorForCausalLinkToNonExistentTarget() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("BadLink")
                    .cldVariable("A")
                    .causalLink("A", "Missing", Polarity.POSITIVE)
                    .build();

            ValidationResult result = ModelValidator.validate(def);

            assertThat(result.issues()).anyMatch(i ->
                    i.severity() == Severity.ERROR
                            && i.message().contains("non-existent target")
                            && i.message().contains("Missing"));
        }

        @Test
        void shouldErrorForCausalLinkFromNonExistentSource() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("BadLink")
                    .cldVariable("B")
                    .causalLink("Missing", "B", Polarity.NEGATIVE)
                    .build();

            ValidationResult result = ModelValidator.validate(def);

            assertThat(result.issues()).anyMatch(i ->
                    i.severity() == Severity.ERROR
                            && i.message().contains("non-existent source")
                            && i.message().contains("Missing"));
        }

        @Test
        void shouldWarnForUnknownPolarity() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Unknown")
                    .cldVariable("X")
                    .cldVariable("Y")
                    .causalLink("X", "Y", Polarity.UNKNOWN)
                    .build();

            ValidationResult result = ModelValidator.validate(def);

            assertThat(result.issues()).anyMatch(i ->
                    i.severity() == Severity.WARNING
                            && i.message().contains("unknown polarity"));
        }

        @Test
        void shouldNotWarnForKnownPolarity() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Known")
                    .cldVariable("X")
                    .cldVariable("Y")
                    .causalLink("X", "Y", Polarity.POSITIVE)
                    .build();

            ValidationResult result = ModelValidator.validate(def);

            assertThat(result.issues()).noneMatch(i ->
                    i.message().contains("unknown polarity"));
        }

        @Test
        void shouldAcceptCausalLinkBetweenSfElements() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("SF Link")
                    .stock("S", 100, "Person")
                    .aux("A", "S * 2", "Person")
                    .causalLink("S", "A", Polarity.POSITIVE)
                    .build();

            ValidationResult result = ModelValidator.validate(def);

            assertThat(result.issues()).noneMatch(i ->
                    i.message().contains("non-existent"));
        }
    }

    @Nested
    @DisplayName("Unconnected modules")
    class UnconnectedModules {

        @Test
        void shouldWarnWhenModuleHasNoPortsAndNoBindings() {
            ModelDefinition innerDef = new ModelDefinitionBuilder()
                    .name("Inner")
                    .stock("X", 10, "Unit")
                    .build();

            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Parent")
                    .module("m1", innerDef, Map.of(), Map.of())
                    .build();

            ValidationResult result = ModelValidator.validate(def);

            assertThat(result.issues()).anyMatch(i ->
                    i.severity() == Severity.WARNING
                            && i.elementName().equals("m1")
                            && i.message().contains("no ports defined"));
        }

        @Test
        void shouldNotWarnWhenModuleHasPorts() {
            ModuleInterface iface = new ModuleInterface(
                    List.of(new PortDef("rate", "1/Day")),
                    List.of());
            ModelDefinition innerDef = new ModelDefinitionBuilder()
                    .name("Inner")
                    .moduleInterface(iface)
                    .stock("X", 10, "Unit")
                    .build();

            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Parent")
                    .module("m1", innerDef, Map.of(), Map.of())
                    .build();

            ValidationResult result = ModelValidator.validate(def);

            assertThat(result.issues()).noneMatch(i ->
                    i.message().contains("no ports defined"));
        }

        @Test
        void shouldNotWarnWhenModuleHasBindings() {
            ModelDefinition innerDef = new ModelDefinitionBuilder()
                    .name("Inner")
                    .stock("X", 10, "Unit")
                    .build();

            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Parent")
                    .constant("Rate", 0.05, "1/Day")
                    .module("m1", innerDef,
                            Map.of("rate", "Rate"), Map.of())
                    .build();

            ValidationResult result = ModelValidator.validate(def);

            assertThat(result.issues()).noneMatch(i ->
                    i.message().contains("no ports defined"));
        }
    }
}
