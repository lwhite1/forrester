package systems.courant.forrester.app.canvas;

import systems.courant.forrester.model.def.AuxDef;
import systems.courant.forrester.model.def.FlowDef;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EquationReferenceManager")
class EquationReferenceManagerTest {

    private List<FlowDef> flows;
    private List<AuxDef> auxiliaries;
    private EquationReferenceManager manager;

    @BeforeEach
    void setUp() {
        flows = new ArrayList<>();
        auxiliaries = new ArrayList<>();
        manager = new EquationReferenceManager(flows, auxiliaries);
    }

    @Nested
    @DisplayName("replaceToken (static)")
    class ReplaceToken {

        @Test
        void shouldReplaceWholeTokenOnly() {
            String result = EquationReferenceManager.replaceToken("A + AB + A_B", "A", "X");
            assertThat(result).isEqualTo("X + AB + A_B");
        }

        @Test
        void shouldReplaceMultipleOccurrences() {
            String result = EquationReferenceManager.replaceToken("A + A * A", "A", "B");
            assertThat(result).isEqualTo("B + B * B");
        }

        @Test
        void shouldHandleUnderscoreTokens() {
            String result = EquationReferenceManager.replaceToken(
                    "Contact_Rate * Pop", "Contact_Rate", "New_Rate");
            assertThat(result).isEqualTo("New_Rate * Pop");
        }

        @Test
        void shouldNotReplacePartialMatch() {
            String result = EquationReferenceManager.replaceToken("Population", "Pop", "People");
            assertThat(result).isEqualTo("Population");
        }

        @Test
        void shouldHandleEmptyEquation() {
            String result = EquationReferenceManager.replaceToken("", "A", "B");
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("isTokenChar (static)")
    class IsTokenChar {

        @Test
        void shouldRecognizeLetters() {
            assertThat(EquationReferenceManager.isTokenChar('a')).isTrue();
            assertThat(EquationReferenceManager.isTokenChar('Z')).isTrue();
        }

        @Test
        void shouldRecognizeDigits() {
            assertThat(EquationReferenceManager.isTokenChar('0')).isTrue();
            assertThat(EquationReferenceManager.isTokenChar('9')).isTrue();
        }

        @Test
        void shouldRecognizeUnderscore() {
            assertThat(EquationReferenceManager.isTokenChar('_')).isTrue();
        }

        @Test
        void shouldRejectOperators() {
            assertThat(EquationReferenceManager.isTokenChar('+')).isFalse();
            assertThat(EquationReferenceManager.isTokenChar('*')).isFalse();
            assertThat(EquationReferenceManager.isTokenChar(' ')).isFalse();
        }
    }

    @Nested
    @DisplayName("updateEquationReferences")
    class UpdateEquationReferences {

        @Test
        void shouldUpdateFlowEquations() {
            flows.add(new FlowDef("Drain", "Pop * Rate", "Day", null, null));
            manager.updateEquationReferences("Rate", "New_Rate");
            assertThat(flows.getFirst().equation()).isEqualTo("Pop * New_Rate");
        }

        @Test
        void shouldUpdateAuxEquations() {
            auxiliaries.add(new AuxDef("Ratio", "Pop / Total", "units"));
            manager.updateEquationReferences("Total", "Grand_Total");
            assertThat(auxiliaries.getFirst().equation()).isEqualTo("Pop / Grand_Total");
        }

        @Test
        void shouldNotUpdateWhenTokensEqual() {
            flows.add(new FlowDef("Drain", "Rate * Pop", "Day", null, null));
            manager.updateEquationReferences("Rate", "Rate");
            assertThat(flows.getFirst().equation()).isEqualTo("Rate * Pop");
        }

        @Test
        void shouldLeaveUnrelatedEquationsAlone() {
            flows.add(new FlowDef("Drain", "Pop * 5", "Day", null, null));
            manager.updateEquationReferences("Rate", "New_Rate");
            assertThat(flows.getFirst().equation()).isEqualTo("Pop * 5");
        }
    }

    @Nested
    @DisplayName("updateEquationByName")
    class UpdateEquationByName {

        @Test
        void shouldTransformFlowEquation() {
            flows.add(new FlowDef("Drain", "Rate * Pop", "Day", null, null));
            boolean changed = manager.updateEquationByName("Drain", eq -> eq.replace("Rate", "0"));
            assertThat(changed).isTrue();
            assertThat(flows.getFirst().equation()).isEqualTo("0 * Pop");
        }

        @Test
        void shouldTransformAuxEquation() {
            auxiliaries.add(new AuxDef("Ratio", "Pop / Total", "units"));
            boolean changed = manager.updateEquationByName("Ratio", eq -> eq.replace("Total", "100"));
            assertThat(changed).isTrue();
            assertThat(auxiliaries.getFirst().equation()).isEqualTo("Pop / 100");
        }

        @Test
        void shouldReturnFalseWhenNameNotFound() {
            boolean changed = manager.updateEquationByName("Missing", eq -> "0");
            assertThat(changed).isFalse();
        }

        @Test
        void shouldReturnFalseWhenTransformNoOp() {
            flows.add(new FlowDef("Drain", "Pop", "Day", null, null));
            boolean changed = manager.updateEquationByName("Drain", eq -> eq);
            assertThat(changed).isFalse();
        }

        @Test
        void shouldPreserveMaterialUnitWhenTransformingFlow() {
            flows.add(new FlowDef("Drain", "comment", "Rate * Pop", "Day",
                    "Person", "Source", "Sink", List.of()));
            manager.updateEquationByName("Drain", eq -> eq.replace("Rate", "0"));
            assertThat(flows.getFirst().materialUnit()).isEqualTo("Person");
        }

        @Test
        void shouldPreserveSubscriptsWhenTransformingFlow() {
            flows.add(new FlowDef("Drain", "comment", "Rate * Pop", "Day",
                    "Person", "Source", "Sink", List.of("Region", "Age")));
            manager.updateEquationByName("Drain", eq -> eq.replace("Rate", "0"));
            assertThat(flows.getFirst().subscripts()).containsExactly("Region", "Age");
        }
    }

    @Nested
    @DisplayName("addConnectionReference")
    class AddConnectionReference {

        @Test
        void shouldReplaceZeroEquationWithToken() {
            flows.add(new FlowDef("Drain", "0", "Day", null, null));
            boolean added = manager.addConnectionReference("Drain", "Pop");
            assertThat(added).isTrue();
            assertThat(flows.getFirst().equation()).isEqualTo("Pop");
        }

        @Test
        void shouldAppendToExistingEquation() {
            flows.add(new FlowDef("Drain", "Rate", "Day", null, null));
            boolean added = manager.addConnectionReference("Drain", "Pop");
            assertThat(added).isTrue();
            assertThat(flows.getFirst().equation()).isEqualTo("Rate * Pop");
        }

        @Test
        void shouldNotDuplicateExistingReference() {
            flows.add(new FlowDef("Drain", "Rate * Pop", "Day", null, null));
            boolean added = manager.addConnectionReference("Drain", "Pop");
            assertThat(added).isTrue();
            assertThat(flows.getFirst().equation()).isEqualTo("Rate * Pop");
        }

        @Test
        void shouldAddToAuxiliary() {
            auxiliaries.add(new AuxDef("Ratio", "0", "units"));
            boolean added = manager.addConnectionReference("Ratio", "Pop");
            assertThat(added).isTrue();
            assertThat(auxiliaries.getFirst().equation()).isEqualTo("Pop");
        }

        @Test
        void shouldReturnFalseForUnknownElement() {
            boolean added = manager.addConnectionReference("Missing", "Pop");
            assertThat(added).isFalse();
        }
    }
}
