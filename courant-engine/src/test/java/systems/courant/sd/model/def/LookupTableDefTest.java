package systems.courant.sd.model.def;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LookupTableDef (#743)")
class LookupTableDefTest {

    private static final double[] XS = {0, 1, 2};
    private static final double[] YS = {0, 0.5, 1.0};

    @Nested
    @DisplayName("unit field")
    class UnitField {

        @Test
        void shouldStoreAndRetrieveUnit() {
            LookupTableDef def = new LookupTableDef("tbl", null, XS, YS, "LINEAR", "Widget");
            assertThat(def.unit()).isEqualTo("Widget");
        }

        @Test
        void shouldAllowNullUnit() {
            LookupTableDef def = new LookupTableDef("tbl", null, XS, YS, "LINEAR", null);
            assertThat(def.unit()).isNull();
        }

        @Test
        void shouldDefaultToNullUnitFromFiveParamConstructor() {
            LookupTableDef def = new LookupTableDef("tbl", null, XS, YS, "LINEAR");
            assertThat(def.unit()).isNull();
        }

        @Test
        void shouldDefaultToNullUnitFromFourParamConstructor() {
            LookupTableDef def = new LookupTableDef("tbl", XS, YS, "LINEAR");
            assertThat(def.unit()).isNull();
        }
    }

    @Nested
    @DisplayName("equals and hashCode with unit")
    class EqualsHashCode {

        @Test
        void shouldBeEqualWhenUnitsMatch() {
            LookupTableDef a = new LookupTableDef("tbl", null, XS, YS, "LINEAR", "kg");
            LookupTableDef b = new LookupTableDef("tbl", null, XS, YS, "LINEAR", "kg");
            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        void shouldNotBeEqualWhenUnitsDiffer() {
            LookupTableDef a = new LookupTableDef("tbl", null, XS, YS, "LINEAR", "kg");
            LookupTableDef b = new LookupTableDef("tbl", null, XS, YS, "LINEAR", "lb");
            assertThat(a).isNotEqualTo(b);
        }

        @Test
        void shouldNotBeEqualWhenOneUnitIsNull() {
            LookupTableDef a = new LookupTableDef("tbl", null, XS, YS, "LINEAR", "kg");
            LookupTableDef b = new LookupTableDef("tbl", null, XS, YS, "LINEAR", null);
            assertThat(a).isNotEqualTo(b);
        }

        @Test
        void shouldBeEqualWhenBothUnitsAreNull() {
            LookupTableDef a = new LookupTableDef("tbl", null, XS, YS, "LINEAR", null);
            LookupTableDef b = new LookupTableDef("tbl", null, XS, YS, "LINEAR");
            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }
    }
}
