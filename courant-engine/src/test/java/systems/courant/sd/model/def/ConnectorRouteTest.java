package systems.courant.sd.model.def;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ConnectorRoute")
class ConnectorRouteTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create with from, to, and control points")
        void shouldCreateWithControlPoints() {
            ConnectorRoute route = new ConnectorRoute("A", "B",
                    List.of(new double[]{1.0, 2.0}, new double[]{3.0, 4.0}));
            assertThat(route.from()).isEqualTo("A");
            assertThat(route.to()).isEqualTo("B");
            assertThat(route.controlPoints()).hasSize(2);
        }

        @Test
        @DisplayName("should create straight connector without control points")
        void shouldCreateStraightConnector() {
            ConnectorRoute route = new ConnectorRoute("A", "B");
            assertThat(route.controlPoints()).isEmpty();
        }

        @Test
        @DisplayName("should reject blank from")
        void shouldRejectBlankFrom() {
            assertThatThrownBy(() -> new ConnectorRoute("", "B"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject blank to")
        void shouldRejectBlankTo() {
            assertThatThrownBy(() -> new ConnectorRoute("A", ""))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should treat null control points as empty list")
        void shouldTreatNullControlPointsAsEmpty() {
            ConnectorRoute route = new ConnectorRoute("A", "B", null);
            assertThat(route.controlPoints()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Defensive copy (#445)")
    class DefensiveCopy {

        @Test
        @DisplayName("constructor should clone control point arrays")
        void shouldCloneOnConstruction() {
            double[] point = {1.0, 2.0};
            ConnectorRoute route = new ConnectorRoute("A", "B", List.of(point));

            point[0] = 999.0;

            assertThat(route.controlPoints().getFirst()[0])
                    .as("Mutating input array must not affect internal state")
                    .isEqualTo(1.0);
        }

        @Test
        @DisplayName("controlPoints() accessor should return defensive copy of arrays")
        void shouldReturnDefensiveCopyFromAccessor() {
            ConnectorRoute route = new ConnectorRoute("A", "B",
                    List.of(new double[]{1.0, 2.0}));

            double[] returned = route.controlPoints().getFirst();
            returned[0] = 999.0;

            assertThat(route.controlPoints().getFirst()[0])
                    .as("Mutating returned array must not affect internal state")
                    .isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal for same from, to, and control points")
        void shouldBeEqualForSameData() {
            ConnectorRoute a = new ConnectorRoute("X", "Y",
                    List.of(new double[]{1.0, 2.0}));
            ConnectorRoute b = new ConnectorRoute("X", "Y",
                    List.of(new double[]{1.0, 2.0}));
            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        @DisplayName("should not be equal for different control points")
        void shouldNotBeEqualForDifferentPoints() {
            ConnectorRoute a = new ConnectorRoute("X", "Y",
                    List.of(new double[]{1.0, 2.0}));
            ConnectorRoute b = new ConnectorRoute("X", "Y",
                    List.of(new double[]{3.0, 4.0}));
            assertThat(a).isNotEqualTo(b);
        }
    }
}
