package systems.courant.sd.model.def;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FlowRoute")
class FlowRouteTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("should create with flow name and points")
        void shouldCreateWithPoints() {
            FlowRoute route = new FlowRoute("Flow1",
                    List.of(new double[]{1.0, 2.0}, new double[]{3.0, 4.0}));
            assertThat(route.flowName()).isEqualTo("Flow1");
            assertThat(route.points()).hasSize(2);
        }

        @Test
        @DisplayName("should reject blank flow name")
        void shouldRejectBlankFlowName() {
            assertThatThrownBy(() -> new FlowRoute("", List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should treat null points as empty list")
        void shouldTreatNullPointsAsEmpty() {
            FlowRoute route = new FlowRoute("Flow1", null);
            assertThat(route.points()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Defensive copy (#445)")
    class DefensiveCopy {

        @Test
        @DisplayName("constructor should clone point arrays")
        void shouldCloneOnConstruction() {
            double[] point = {1.0, 2.0};
            FlowRoute route = new FlowRoute("Flow1", List.of(point));

            point[0] = 999.0;

            assertThat(route.points().getFirst()[0])
                    .as("Mutating input array must not affect internal state")
                    .isEqualTo(1.0);
        }

        @Test
        @DisplayName("points() accessor should return defensive copy of arrays")
        void shouldReturnDefensiveCopyFromAccessor() {
            FlowRoute route = new FlowRoute("Flow1",
                    List.of(new double[]{1.0, 2.0}));

            double[] returned = route.points().getFirst();
            returned[0] = 999.0;

            assertThat(route.points().getFirst()[0])
                    .as("Mutating returned array must not affect internal state")
                    .isEqualTo(1.0);
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal for same flow name and points")
        void shouldBeEqualForSameData() {
            FlowRoute a = new FlowRoute("Flow1",
                    List.of(new double[]{1.0, 2.0}));
            FlowRoute b = new FlowRoute("Flow1",
                    List.of(new double[]{1.0, 2.0}));
            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        @DisplayName("should not be equal for different points")
        void shouldNotBeEqualForDifferentPoints() {
            FlowRoute a = new FlowRoute("Flow1",
                    List.of(new double[]{1.0, 2.0}));
            FlowRoute b = new FlowRoute("Flow1",
                    List.of(new double[]{3.0, 4.0}));
            assertThat(a).isNotEqualTo(b);
        }
    }
}
