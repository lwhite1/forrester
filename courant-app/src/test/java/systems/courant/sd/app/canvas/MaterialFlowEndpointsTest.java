package systems.courant.sd.app.canvas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import systems.courant.sd.model.def.FlowDef;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MaterialFlowEndpoints")
class MaterialFlowEndpointsTest {

    @Nested
    @DisplayName("resolve")
    class Resolve {

        @Test
        void shouldReturnNullWhenFlowNotOnCanvas() {
            CanvasState state = new CanvasState();
            // Flow "myflow" not added to canvas
            FlowDef flow = new FlowDef("myflow", "10", "Day", "stock1", "stock2");
            assertThat(MaterialFlowEndpoints.resolve(flow, state)).isNull();
        }

        @Test
        void shouldResolveBothCloudsWhenDisconnected() {
            CanvasState state = new CanvasState();
            state.addElement("myflow", systems.courant.sd.model.def.ElementType.FLOW, 100, 200);
            FlowDef flow = new FlowDef("myflow", "10", "Day", null, null);

            MaterialFlowEndpoints ep = MaterialFlowEndpoints.resolve(flow, state);
            assertThat(ep).isNotNull();
            assertThat(ep.midX()).isEqualTo(100);
            assertThat(ep.midY()).isEqualTo(200);
            assertThat(ep.sourceIsCloud()).isTrue();
            assertThat(ep.sinkIsCloud()).isTrue();
        }
    }
}
