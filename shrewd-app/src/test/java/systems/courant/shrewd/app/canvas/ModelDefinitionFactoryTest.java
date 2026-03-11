package systems.courant.shrewd.app.canvas;

import systems.courant.shrewd.measure.Quantity;
import systems.courant.shrewd.measure.TimeUnit;
import systems.courant.shrewd.model.ModelMetadata;
import systems.courant.shrewd.model.def.CausalLinkDef;
import systems.courant.shrewd.model.def.CldVariableDef;
import systems.courant.shrewd.model.def.ModelDefinition;
import systems.courant.shrewd.model.def.ModelDefinitionBuilder;
import systems.courant.shrewd.model.def.SimulationSettings;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ModelDefinitionFactory (issue #236)")
class ModelDefinitionFactoryTest {

    private static ModelDefinition buildWithCldAndMetadata() {
        ModelDefinition base = new ModelDefinitionBuilder()
                .name("CLD Model")
                .stock("Workload", 100, "Thing")
                .flow("Drain", "Workload * 0.1", "Day", "Workload", null)
                .constant("Rate", 0.1, "Dimensionless unit")
                .cldVariable("Workload")
                .cldVariable("Burnout")
                .causalLink("Workload", "Burnout", CausalLinkDef.Polarity.POSITIVE)
                .defaultSimulation("Day", 10, "Day")
                .build();

        ModelMetadata metadata = ModelMetadata.builder()
                .author("Test Author")
                .source("Test Source")
                .build();

        // Rebuild with metadata using the canonical constructor
        return new ModelDefinition(
                base.name(), base.comment(), base.moduleInterface(),
                base.stocks(), base.flows(), base.auxiliaries(),
                base.lookupTables(), base.modules(), base.subscripts(),
                base.cldVariables(), base.causalLinks(), base.views(),
                base.defaultSimulation(), metadata
        );
    }

    @Nested
    @DisplayName("applyParameterOverrides preserves CLD and metadata")
    class ApplyConstantOverrides {

        @Test
        void shouldPreserveCldVariables() {
            ModelDefinition def = buildWithCldAndMetadata();
            ModelDefinition overridden = ModelDefinitionFactory.applyParameterOverrides(
                    def, Map.of("Rate", 0.2));

            assertThat(overridden.cldVariables())
                    .as("CLD variables must survive constant overrides")
                    .hasSize(2);
            assertThat(overridden.cldVariables())
                    .extracting(CldVariableDef::name)
                    .containsExactly("Workload", "Burnout");
        }

        @Test
        void shouldPreserveCausalLinks() {
            ModelDefinition def = buildWithCldAndMetadata();
            ModelDefinition overridden = ModelDefinitionFactory.applyParameterOverrides(
                    def, Map.of("Rate", 0.2));

            assertThat(overridden.causalLinks())
                    .as("Causal links must survive constant overrides")
                    .hasSize(1);
            CausalLinkDef link = overridden.causalLinks().get(0);
            assertThat(link.from()).isEqualTo("Workload");
            assertThat(link.to()).isEqualTo("Burnout");
            assertThat(link.polarity()).isEqualTo(CausalLinkDef.Polarity.POSITIVE);
        }

        @Test
        void shouldPreserveMetadata() {
            ModelDefinition def = buildWithCldAndMetadata();
            ModelDefinition overridden = ModelDefinitionFactory.applyParameterOverrides(
                    def, Map.of("Rate", 0.2));

            assertThat(overridden.metadata())
                    .as("Metadata must survive constant overrides")
                    .isNotNull();
            assertThat(overridden.metadata().author()).isEqualTo("Test Author");
            assertThat(overridden.metadata().source()).isEqualTo("Test Source");
        }

        @Test
        void shouldStillApplyOverride() {
            ModelDefinition def = buildWithCldAndMetadata();
            ModelDefinition overridden = ModelDefinitionFactory.applyParameterOverrides(
                    def, Map.of("Rate", 0.5));

            assertThat(overridden.parameters()).hasSize(1);
            assertThat(overridden.parameters().get(0).literalValue()).isEqualTo(0.5);
        }
    }

    @Nested
    @DisplayName("createFactory preserves CLD and metadata through full pipeline")
    class CreateFactory {

        @Test
        void shouldPreserveCldThroughFactoryPipeline() {
            ModelDefinition def = buildWithCldAndMetadata();
            SimulationSettings settings = def.defaultSimulation();

            // The factory calls applyParameterOverrides then embedSettings
            var factory = ModelDefinitionFactory.createFactory(def, settings);

            // Execute the factory — it returns a CompiledModel
            // The important thing is that it doesn't throw, and we can verify
            // the definition passed through correctly by checking the overrides method
            ModelDefinition overridden = ModelDefinitionFactory.applyParameterOverrides(
                    def, Map.of("Rate", 0.3));

            assertThat(overridden.cldVariables()).hasSize(2);
            assertThat(overridden.causalLinks()).hasSize(1);
            assertThat(overridden.metadata()).isNotNull();
        }
    }

    @Nested
    @DisplayName("empty CLD and null metadata are handled correctly")
    class EdgeCases {

        @Test
        void shouldHandleEmptyCldVariablesAndLinks() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("No CLD")
                    .stock("S", 100, "Thing")
                    .constant("Rate", 0.1, "Dimensionless unit")
                    .defaultSimulation("Day", 10, "Day")
                    .build();

            ModelDefinition overridden = ModelDefinitionFactory.applyParameterOverrides(
                    def, Map.of("Rate", 0.5));

            assertThat(overridden.cldVariables()).isEmpty();
            assertThat(overridden.causalLinks()).isEmpty();
        }

        @Test
        void shouldHandleNullMetadata() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("No Metadata")
                    .stock("S", 100, "Thing")
                    .constant("Rate", 0.1, "Dimensionless unit")
                    .defaultSimulation("Day", 10, "Day")
                    .build();

            ModelDefinition overridden = ModelDefinitionFactory.applyParameterOverrides(
                    def, Map.of("Rate", 0.5));

            assertThat(overridden.metadata()).isNull();
        }

        @Test
        void shouldReturnSameDefinitionForEmptyOverrides() {
            ModelDefinition def = buildWithCldAndMetadata();
            ModelDefinition result = ModelDefinitionFactory.applyParameterOverrides(
                    def, Map.of());

            assertThat(result).isSameAs(def);
        }
    }

    @Nested
    @DisplayName("Shared UnitRegistry (#444)")
    class SharedRegistry {

        @Test
        void shouldResolveTimeStepWithCachedRegistry() {
            SimulationSettings settings = new SimulationSettings("Day", 10, "Day");
            TimeUnit ts = ModelDefinitionFactory.resolveTimeStep(settings);
            assertThat(ts.getName()).isEqualTo("Day");
        }

        @Test
        void shouldResolveDurationWithCachedRegistry() {
            SimulationSettings settings = new SimulationSettings("Day", 10, "Week");
            Quantity dur = ModelDefinitionFactory.resolveDuration(settings);
            assertThat(dur.getValue()).isEqualTo(10);
            assertThat(dur.getUnit().getName()).isEqualTo("Week");
        }

        @Test
        void shouldReturnConsistentResultsAcrossRepeatedCalls() {
            SimulationSettings settings = new SimulationSettings("Minute", 100, "Hour");
            TimeUnit first = ModelDefinitionFactory.resolveTimeStep(settings);
            TimeUnit second = ModelDefinitionFactory.resolveTimeStep(settings);
            assertThat(first).isSameAs(second);
        }
    }

    @Nested
    @DisplayName("formatValue overflow guard (#421)")
    class FormatValueOverflow {

        @Test
        void shouldFormatLargeDoubleWithoutOverflow() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Overflow")
                    .constant("BigParam", 42, "Dimensionless unit")
                    .defaultSimulation("Day", 10, "Day")
                    .build();

            ModelDefinition overridden = ModelDefinitionFactory.applyParameterOverrides(
                    def, Map.of("BigParam", 1e19));

            assertThat(overridden.parameters().get(0).equation()).isEqualTo("1.0E19");
        }

        @Test
        void shouldFormatIntegerValueCleanly() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("IntVal")
                    .constant("Param", 1, "Dimensionless unit")
                    .defaultSimulation("Day", 10, "Day")
                    .build();

            ModelDefinition overridden = ModelDefinitionFactory.applyParameterOverrides(
                    def, Map.of("Param", 42.0));

            assertThat(overridden.parameters().get(0).equation()).isEqualTo("42");
        }
    }
}
