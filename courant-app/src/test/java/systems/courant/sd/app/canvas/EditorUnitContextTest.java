package systems.courant.sd.app.canvas;

import systems.courant.sd.measure.CompositeUnit;
import systems.courant.sd.measure.UnitRegistry;
import systems.courant.sd.model.def.VariableDef;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.LookupTableDef;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModelDefinitionBuilder;
import systems.courant.sd.model.def.StockDef;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EditorUnitContext")
class EditorUnitContextTest {

    private ModelEditor editor;
    private UnitRegistry registry;
    private EditorUnitContext context;

    @BeforeEach
    void setUp() {
        editor = new ModelEditor();
        registry = new UnitRegistry();
        context = new EditorUnitContext(editor, registry);
    }

    @Nested
    @DisplayName("resolveUnit for stocks")
    class StockResolution {

        @Test
        void shouldResolveStockWithUnit() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Population", 100, "Person")
                    .build();
            editor.loadFrom(def);

            Optional<CompositeUnit> result = context.resolveUnit("Population");

            assertThat(result).isPresent();
            assertThat(result.get().isDimensionless()).isFalse();
        }

        @Test
        void shouldReturnDimensionless_whenStockHasBlankUnit() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Population", 100, "")
                    .build();
            editor.loadFrom(def);

            Optional<CompositeUnit> result = context.resolveUnit("Population");

            assertThat(result).isPresent();
            assertThat(result.get().isDimensionless()).isTrue();
        }

        @Test
        void shouldResolveStockByUnderscoreName() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Total Population", 100, "Person")
                    .build();
            editor.loadFrom(def);

            Optional<CompositeUnit> result = context.resolveUnit("Total_Population");

            assertThat(result).isPresent();
            assertThat(result.get().isDimensionless()).isFalse();
        }
    }

    @Nested
    @DisplayName("resolveUnit for flows")
    class FlowResolution {

        @Test
        void shouldResolveFlowAsRateUnit() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .stock("Population", 100, "Person")
                    .flow("births", "Population * 0.1", "Day", null, "Population")
                    .build();
            editor.loadFrom(def);

            Optional<CompositeUnit> result = context.resolveUnit("births");

            assertThat(result).isPresent();
            assertThat(result.get().isDimensionless()).isFalse();
        }

        @Test
        void shouldResolveFlowWithExplicitMaterialUnit() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .flow(new FlowDef("rate", null, "10", "Day",
                            "Person", null, null, List.of()))
                    .build();
            editor.loadFrom(def);

            Optional<CompositeUnit> result = context.resolveUnit("rate");

            assertThat(result).isPresent();
            assertThat(result.get().isDimensionless()).isFalse();
        }

        @Test
        void shouldResolveFlowByUnderscoreName() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .flow("birth rate", "10", "Day", null, null)
                    .build();
            editor.loadFrom(def);

            Optional<CompositeUnit> result = context.resolveUnit("birth_rate");

            assertThat(result).isPresent();
        }
    }

    @Nested
    @DisplayName("resolveUnit for variables")
    class AuxResolution {

        @Test
        void shouldResolveAuxWithUnit() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .variable("growth rate", "0.1", "Person")
                    .build();
            editor.loadFrom(def);

            Optional<CompositeUnit> result = context.resolveUnit("growth rate");

            assertThat(result).isPresent();
            assertThat(result.get().isDimensionless()).isFalse();
        }

        @Test
        void shouldReturnDimensionless_whenAuxHasBlankUnit() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .variable("ratio", "0.5", "")
                    .build();
            editor.loadFrom(def);

            Optional<CompositeUnit> result = context.resolveUnit("ratio");

            assertThat(result).isPresent();
            assertThat(result.get().isDimensionless()).isTrue();
        }

        @Test
        void shouldResolveAuxByUnderscoreName() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .variable("contact rate", "5", "Person")
                    .build();
            editor.loadFrom(def);

            Optional<CompositeUnit> result = context.resolveUnit("contact_rate");

            assertThat(result).isPresent();
        }
    }

    @Nested
    @DisplayName("resolveUnit for lookup tables")
    class LookupResolution {

        @Test
        void shouldResolveLookupAsDimensionless() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .lookupTable("Effect of Pressure",
                            new double[]{0, 1}, new double[]{0, 1}, "LINEAR")
                    .build();
            editor.loadFrom(def);

            Optional<CompositeUnit> result = context.resolveUnit("Effect of Pressure");

            assertThat(result).isPresent();
            assertThat(result.get().isDimensionless()).isTrue();
        }

        @Test
        void shouldResolveLookupByUnderscoreName() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .lookupTable("Effect of Pressure",
                            new double[]{0, 1}, new double[]{0, 1}, "LINEAR")
                    .build();
            editor.loadFrom(def);

            Optional<CompositeUnit> result = context.resolveUnit("Effect_of_Pressure");

            assertThat(result).isPresent();
            assertThat(result.get().isDimensionless()).isTrue();
        }
    }

    @Nested
    @DisplayName("resolveUnit for unknown elements")
    class UnknownResolution {

        @Test
        void shouldReturnEmpty_whenElementNotFound() {
            Optional<CompositeUnit> result = context.resolveUnit("nonexistent");

            assertThat(result).isEmpty();
        }

        @Test
        void shouldReturnEmpty_whenEditorIsEmpty() {
            Optional<CompositeUnit> result = context.resolveUnit("anything");

            assertThat(result).isEmpty();
        }
    }
}
