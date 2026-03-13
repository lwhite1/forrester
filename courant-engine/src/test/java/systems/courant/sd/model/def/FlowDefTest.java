package systems.courant.sd.model.def;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FlowDef")
class FlowDefTest {

    @Nested
    @DisplayName("Canonical constructor")
    class CanonicalConstructor {

        @Test
        void shouldAcceptMaterialUnit() {
            FlowDef flow = new FlowDef("F", null, "10", "Day", "Person",
                    "Stock A", "Stock B", List.of());
            assertThat(flow.materialUnit()).isEqualTo("Person");
            assertThat(flow.source()).isEqualTo("Stock A");
            assertThat(flow.sink()).isEqualTo("Stock B");
        }

        @Test
        void shouldAcceptNullMaterialUnit() {
            FlowDef flow = new FlowDef("F", null, "10", "Day", null,
                    "A", "B", List.of());
            assertThat(flow.materialUnit()).isNull();
        }

        @Test
        void shouldRejectBlankName() {
            assertThatThrownBy(() -> new FlowDef("", null, "10", "Day", null,
                    null, null, List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        void shouldRejectBlankEquation() {
            assertThatThrownBy(() -> new FlowDef("F", null, "", "Day", null,
                    null, null, List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("equation");
        }

        @Test
        void shouldRejectBlankTimeUnit() {
            assertThatThrownBy(() -> new FlowDef("F", null, "10", "", null,
                    null, null, List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("timeUnit");
        }
    }

    @Nested
    @DisplayName("Backward-compatible constructors")
    class BackwardCompatible {

        @Test
        void shouldSetNullMaterialUnitWith6ArgConstructor() {
            FlowDef flow = new FlowDef("F", "comment", "10", "Day", "A", "B");
            assertThat(flow.materialUnit()).isNull();
            assertThat(flow.subscripts()).isEmpty();
        }

        @Test
        void shouldSetNullMaterialUnitWith5ArgConstructor() {
            FlowDef flow = new FlowDef("F", "10", "Day", "A", "B");
            assertThat(flow.materialUnit()).isNull();
            assertThat(flow.comment()).isNull();
            assertThat(flow.subscripts()).isEmpty();
        }

        @Test
        void shouldSetNullMaterialUnitWith7ArgSubscriptsConstructor() {
            FlowDef flow = new FlowDef("F", "comment", "10", "Day",
                    "A", "B", List.of("Region"));
            assertThat(flow.materialUnit()).isNull();
            assertThat(flow.subscripts()).containsExactly("Region");
        }
    }

    @Nested
    @DisplayName("Record equality")
    class Equality {

        @Test
        void shouldBeEqualWithSameMaterialUnit() {
            FlowDef a = new FlowDef("F", null, "10", "Day", "Person",
                    null, null, List.of());
            FlowDef b = new FlowDef("F", null, "10", "Day", "Person",
                    null, null, List.of());
            assertThat(a).isEqualTo(b);
        }

        @Test
        void shouldNotBeEqualWithDifferentMaterialUnit() {
            FlowDef a = new FlowDef("F", null, "10", "Day", "Person",
                    null, null, List.of());
            FlowDef b = new FlowDef("F", null, "10", "Day", "USD",
                    null, null, List.of());
            assertThat(a).isNotEqualTo(b);
        }

        @Test
        void shouldNotBeEqualWhenOneMaterialUnitIsNull() {
            FlowDef a = new FlowDef("F", null, "10", "Day", "Person",
                    null, null, List.of());
            FlowDef b = new FlowDef("F", "10", "Day", null, null);
            assertThat(a).isNotEqualTo(b);
        }
    }
}
