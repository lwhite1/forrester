package systems.courant.sd.app.canvas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("PortGeometry")
class PortGeometryTest {

    @Nested
    @DisplayName("portY")
    class PortYTest {

        @Test
        void shouldCenterSinglePort() {
            double y = PortGeometry.portY(100, 60, 0, 1);
            assertThat(y).isCloseTo(130.0, within(0.001));
        }

        @Test
        void shouldEvenlySpaceTwoPorts() {
            double y0 = PortGeometry.portY(100, 60, 0, 2);
            double y1 = PortGeometry.portY(100, 60, 1, 2);
            assertThat(y0).isCloseTo(120.0, within(0.001));
            assertThat(y1).isCloseTo(140.0, within(0.001));
        }

        @Test
        void shouldEvenlySpaceThreePorts() {
            double y0 = PortGeometry.portY(0, 80, 0, 3);
            double y1 = PortGeometry.portY(0, 80, 1, 3);
            double y2 = PortGeometry.portY(0, 80, 2, 3);
            assertThat(y0).isCloseTo(20.0, within(0.001));
            assertThat(y1).isCloseTo(40.0, within(0.001));
            assertThat(y2).isCloseTo(60.0, within(0.001));
        }
    }

    @Nested
    @DisplayName("port X coordinates")
    class PortXTest {

        @Test
        void shouldReturnLeftEdgeForInputPorts() {
            double x = PortGeometry.inputPortX(200, 60);
            assertThat(x).isCloseTo(140.0, within(0.001));
        }

        @Test
        void shouldReturnRightEdgeForOutputPorts() {
            double x = PortGeometry.outputPortX(200, 60);
            assertThat(x).isCloseTo(260.0, within(0.001));
        }
    }

    @Nested
    @DisplayName("constants")
    class Constants {

        @Test
        void shouldHaveExpectedPortRadius() {
            assertThat(PortGeometry.PORT_RADIUS).isEqualTo(3.0);
        }

        @Test
        void shouldHaveGenerousHitRadius() {
            assertThat(PortGeometry.PORT_HIT_RADIUS).isEqualTo(8.0);
            assertThat(PortGeometry.PORT_HIT_RADIUS).isGreaterThan(PortGeometry.PORT_RADIUS);
        }
    }
}
