package systems.courant.sd.app.canvas;

import systems.courant.sd.model.def.CausalLinkDef;
import systems.courant.sd.model.def.ElementType;
import systems.courant.sd.model.def.FlowDef;
import systems.courant.sd.model.def.ModelDefinition;
import systems.courant.sd.model.def.ModelDefinitionBuilder;
import systems.courant.sd.model.def.StockDef;
import systems.courant.sd.model.def.VariableDef;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for CLD (causal loop diagram) features of {@link ModelEditor}:
 * CLD variable CRUD, causal link management, and classification of
 * CLD variables into stock-flow element types.
 */
@DisplayName("ModelEditor — CLD")
class ModelEditorCldTest {

    private ModelEditor editor;

    @BeforeEach
    void setUp() {
        editor = new ModelEditor();
    }

    @Nested
    @DisplayName("CLD Variables")
    class CldVariables {

        @Test
        void shouldAddCldVariable() {
            String name = editor.addCldVariable();
            assertThat(name).isEqualTo("Variable 1");
            assertThat(editor.getCldVariables()).hasSize(1);
            assertThat(editor.hasElement("Variable 1")).isTrue();
        }

        @Test
        void shouldAutoIncrementCldVariableNames() {
            editor.addCldVariable();
            String second = editor.addCldVariable();
            assertThat(second).isEqualTo("Variable 2");
        }

        @Test
        void shouldRemoveCldVariable() {
            editor.addCldVariable();
            editor.removeElement("Variable 1");
            assertThat(editor.getCldVariables()).isEmpty();
            assertThat(editor.hasElement("Variable 1")).isFalse();
        }

        @Test
        void shouldRenameCldVariable() {
            editor.addCldVariable();
            boolean result = editor.renameElement("Variable 1", "Workload");
            assertThat(result).isTrue();
            assertThat(editor.getCldVariables().get(0).name()).isEqualTo("Workload");
            assertThat(editor.hasElement("Workload")).isTrue();
            assertThat(editor.hasElement("Variable 1")).isFalse();
        }

        @Test
        void shouldSetCldVariableComment() {
            editor.addCldVariable();
            boolean result = editor.setCldVariableComment("Variable 1", "A description");
            assertThat(result).isTrue();
            assertThat(editor.getCldVariables().get(0).comment()).isEqualTo("A description");
        }

        @Test
        void shouldLoadCldVariablesFromDefinition() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("CLD")
                    .cldVariable("Stress", "Work stress")
                    .cldVariable("Performance")
                    .causalLink("Stress", "Performance", CausalLinkDef.Polarity.NEGATIVE)
                    .build();

            editor.loadFrom(def);

            assertThat(editor.getCldVariables()).hasSize(2);
            assertThat(editor.getCausalLinks()).hasSize(1);
            assertThat(editor.hasElement("Stress")).isTrue();
            assertThat(editor.hasElement("Performance")).isTrue();
        }

        @Test
        void shouldIncludeCldDataInSnapshot() {
            editor.addCldVariable();
            editor.addCldVariable();
            editor.addCausalLink("Variable 1", "Variable 2", CausalLinkDef.Polarity.POSITIVE);

            ModelDefinition snapshot = editor.toModelDefinition();
            assertThat(snapshot.cldVariables()).hasSize(2);
            assertThat(snapshot.causalLinks()).hasSize(1);
            assertThat(snapshot.causalLinks().get(0).polarity())
                    .isEqualTo(CausalLinkDef.Polarity.POSITIVE);
        }

        @Test
        void shouldClearCldDataOnReload() {
            editor.addCldVariable();
            editor.addCausalLink("Variable 1", "Variable 1", CausalLinkDef.Polarity.POSITIVE);

            editor.loadFrom(new ModelDefinitionBuilder().name("Empty").build());
            assertThat(editor.getCldVariables()).isEmpty();
            assertThat(editor.getCausalLinks()).isEmpty();
        }

        @Test
        void shouldContinueCounterAfterLoad() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("CLD")
                    .cldVariable("Variable 5")
                    .build();
            editor.loadFrom(def);

            String next = editor.addCldVariable();
            assertThat(next).isEqualTo("Variable 6");
        }
    }

    @Nested
    @DisplayName("Causal Links")
    class CausalLinks {

        @Test
        void shouldAddCausalLink() {
            editor.addCldVariable();
            editor.addCldVariable();

            boolean result = editor.addCausalLink("Variable 1", "Variable 2",
                    CausalLinkDef.Polarity.POSITIVE);

            assertThat(result).isTrue();
            assertThat(editor.getCausalLinks()).hasSize(1);
        }

        @Test
        void shouldRejectCausalLinkWithMissingEndpoint() {
            editor.addCldVariable();

            boolean result = editor.addCausalLink("Variable 1", "Nonexistent",
                    CausalLinkDef.Polarity.POSITIVE);

            assertThat(result).isFalse();
            assertThat(editor.getCausalLinks()).isEmpty();
        }

        @Test
        void shouldRemoveCausalLink() {
            editor.addCldVariable();
            editor.addCldVariable();
            editor.addCausalLink("Variable 1", "Variable 2", CausalLinkDef.Polarity.POSITIVE);

            boolean result = editor.removeCausalLink("Variable 1", "Variable 2");

            assertThat(result).isTrue();
            assertThat(editor.getCausalLinks()).isEmpty();
        }

        @Test
        void shouldSetCausalLinkPolarity() {
            editor.addCldVariable();
            editor.addCldVariable();
            editor.addCausalLink("Variable 1", "Variable 2", CausalLinkDef.Polarity.UNKNOWN);

            boolean result = editor.setCausalLinkPolarity("Variable 1", "Variable 2",
                    CausalLinkDef.Polarity.NEGATIVE);

            assertThat(result).isTrue();
            assertThat(editor.getCausalLinks().get(0).polarity())
                    .isEqualTo(CausalLinkDef.Polarity.NEGATIVE);
        }

        @Test
        void shouldRemoveCausalLinksWhenVariableDeleted() {
            editor.addCldVariable();
            editor.addCldVariable();
            editor.addCldVariable();
            editor.addCausalLink("Variable 1", "Variable 2", CausalLinkDef.Polarity.POSITIVE);
            editor.addCausalLink("Variable 2", "Variable 3", CausalLinkDef.Polarity.NEGATIVE);
            editor.addCausalLink("Variable 3", "Variable 1", CausalLinkDef.Polarity.POSITIVE);

            editor.removeElement("Variable 2");

            assertThat(editor.getCausalLinks()).hasSize(1);
            assertThat(editor.getCausalLinks().get(0).from()).isEqualTo("Variable 3");
            assertThat(editor.getCausalLinks().get(0).to()).isEqualTo("Variable 1");
        }

        @Test
        void shouldUpdateCausalLinksWhenVariableRenamed() {
            editor.addCldVariable();
            editor.addCldVariable();
            editor.addCausalLink("Variable 1", "Variable 2", CausalLinkDef.Polarity.POSITIVE);

            editor.renameElement("Variable 1", "Workload");

            CausalLinkDef link = editor.getCausalLinks().get(0);
            assertThat(link.from()).isEqualTo("Workload");
            assertThat(link.to()).isEqualTo("Variable 2");
        }

        @Test
        void shouldAllowCausalLinkBetweenCldAndSfElements() {
            editor.addCldVariable();
            editor.addStock();

            boolean result = editor.addCausalLink("Variable 1", "Stock 1",
                    CausalLinkDef.Polarity.POSITIVE);

            assertThat(result).isTrue();
            assertThat(editor.getCausalLinks()).hasSize(1);
        }

        @Test
        void shouldRejectDuplicateCausalLink() {
            editor.addCldVariable();
            editor.addCldVariable();

            boolean first = editor.addCausalLink("Variable 1", "Variable 2",
                    CausalLinkDef.Polarity.POSITIVE);
            boolean second = editor.addCausalLink("Variable 1", "Variable 2",
                    CausalLinkDef.Polarity.NEGATIVE);

            assertThat(first).isTrue();
            assertThat(second).isFalse();
            assertThat(editor.getCausalLinks()).hasSize(1);
            assertThat(editor.getCausalLinks().getFirst().polarity())
                    .isEqualTo(CausalLinkDef.Polarity.POSITIVE);
        }

        @Test
        void shouldAllowReverseLinkBetweenSameVariables() {
            editor.addCldVariable();
            editor.addCldVariable();

            boolean forward = editor.addCausalLink("Variable 1", "Variable 2",
                    CausalLinkDef.Polarity.POSITIVE);
            boolean reverse = editor.addCausalLink("Variable 2", "Variable 1",
                    CausalLinkDef.Polarity.NEGATIVE);

            assertThat(forward).isTrue();
            assertThat(reverse).isTrue();
            assertThat(editor.getCausalLinks()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("classifyCldVariable")
    class ClassifyCldVariable {

        @Test
        void shouldClassifyAsStock() {
            editor.addCldVariable();
            boolean result = editor.classifyCldVariable("Variable 1", ElementType.STOCK);
            assertThat(result).isTrue();
            assertThat(editor.getCldVariables()).isEmpty();
            assertThat(editor.getStocks()).hasSize(1);
            StockDef stock = editor.getStocks().getFirst();
            assertThat(stock.name()).isEqualTo("Variable 1");
            assertThat(stock.initialValue()).isEqualTo(0);
        }

        @Test
        void shouldClassifyAsFlow() {
            editor.addCldVariable();
            boolean result = editor.classifyCldVariable("Variable 1", ElementType.FLOW);
            assertThat(result).isTrue();
            assertThat(editor.getCldVariables()).isEmpty();
            assertThat(editor.getFlows()).hasSize(1);
            FlowDef flow = editor.getFlows().getFirst();
            assertThat(flow.name()).isEqualTo("Variable 1");
        }

        @Test
        void shouldClassifyAsAux() {
            editor.addCldVariable();
            boolean result = editor.classifyCldVariable("Variable 1", ElementType.AUX);
            assertThat(result).isTrue();
            assertThat(editor.getCldVariables()).isEmpty();
            assertThat(editor.getVariables()).hasSize(1);
            VariableDef v = editor.getVariables().getFirst();
            assertThat(v.name()).isEqualTo("Variable 1");
        }

        @Test
        void shouldClassifyAsLiteralAux() {
            editor.addCldVariable();
            boolean result = editor.classifyCldVariable("Variable 1", ElementType.AUX);
            assertThat(result).isTrue();
            assertThat(editor.getCldVariables()).isEmpty();
            assertThat(editor.getVariables()).hasSize(1);
            VariableDef v = editor.getVariables().getFirst();
            assertThat(v.name()).isEqualTo("Variable 1");
        }

        @Test
        void shouldPreserveComment() {
            editor.addCldVariable();
            editor.setCldVariableComment("Variable 1", "Important variable");
            editor.classifyCldVariable("Variable 1", ElementType.STOCK);
            StockDef stock = editor.getStocks().getFirst();
            assertThat(stock.comment()).isEqualTo("Important variable");
        }

        @Test
        void shouldRemoveCausalLinksInvolvingClassifiedVariable() {
            editor.addCldVariable();
            editor.addCldVariable();
            editor.addCldVariable();
            editor.addCausalLink("Variable 1", "Variable 2", CausalLinkDef.Polarity.POSITIVE);
            editor.addCausalLink("Variable 3", "Variable 1", CausalLinkDef.Polarity.NEGATIVE);
            editor.addCausalLink("Variable 2", "Variable 3", CausalLinkDef.Polarity.POSITIVE);

            editor.classifyCldVariable("Variable 1", ElementType.STOCK);

            assertThat(editor.getCausalLinks()).hasSize(1);
            assertThat(editor.getCausalLinks().getFirst().from()).isEqualTo("Variable 2");
            assertThat(editor.getCausalLinks().getFirst().to()).isEqualTo("Variable 3");
        }

        @Test
        void shouldReturnFalseForNonExistentVariable() {
            boolean result = editor.classifyCldVariable("NoSuchVar", ElementType.STOCK);
            assertThat(result).isFalse();
        }

        @Test
        void shouldReturnFalseForUnsupportedTargetType() {
            editor.addCldVariable();
            boolean result = editor.classifyCldVariable("Variable 1", ElementType.MODULE);
            assertThat(result).isFalse();
            assertThat(editor.getCldVariables()).hasSize(1);
        }

        @Test
        void shouldPreserveNameInNameIndex() {
            editor.addCldVariable();
            editor.classifyCldVariable("Variable 1", ElementType.STOCK);
            assertThat(editor.hasElement("Variable 1")).isTrue();
        }

        @Test
        void shouldPreserveOtherCldVariables() {
            editor.addCldVariable();
            editor.addCldVariable();
            editor.classifyCldVariable("Variable 1", ElementType.AUX);
            assertThat(editor.getCldVariables()).hasSize(1);
            assertThat(editor.getCldVariables().getFirst().name()).isEqualTo("Variable 2");
        }

        @Test
        void shouldAvoidNameCollisionWithExistingElements() {
            ModelDefinition def = new ModelDefinitionBuilder()
                    .name("Test")
                    .constant("Variable 1", 0, "u")
                    .build();
            editor.loadFrom(def);
            String name = editor.addCldVariable();
            assertThat(name).isNotEqualTo("Variable 1");
            assertThat(editor.hasElement(name)).isTrue();
        }
    }
}
