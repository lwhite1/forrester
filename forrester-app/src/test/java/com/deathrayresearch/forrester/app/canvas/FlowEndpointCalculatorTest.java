package com.deathrayresearch.forrester.app.canvas;

import com.deathrayresearch.forrester.model.def.ElementType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("FlowEndpointCalculator")
class FlowEndpointCalculatorTest {

    private CanvasState canvasState;
    private ModelEditor editor;

    @BeforeEach
    void setUp() {
        canvasState = new CanvasState();
        editor = new ModelEditor();
    }

    @Nested
    @DisplayName("cloudPosition")
    class CloudPosition {

        @Test
        void shouldReturnNullForConnectedSource() {
            editor.addStock(); // Stock 1
            editor.addStock(); // Stock 2
            String flow = editor.addFlow("Stock 1", "Stock 2");
            canvasState.addElement("Stock 1", ElementType.STOCK, 100, 200);
            canvasState.addElement("Stock 2", ElementType.STOCK, 400, 200);
            canvasState.addElement(flow, ElementType.FLOW, 250, 200);

            FlowGeometry.Point2D pos = FlowEndpointCalculator.cloudPosition(
                    FlowEndpointCalculator.FlowEnd.SOURCE,
                    editor.getFlows().get(0), canvasState);

            assertThat(pos).isNull();
        }

        @Test
        void shouldReturnNullForConnectedSink() {
            editor.addStock(); // Stock 1
            editor.addStock(); // Stock 2
            String flow = editor.addFlow("Stock 1", "Stock 2");
            canvasState.addElement("Stock 1", ElementType.STOCK, 100, 200);
            canvasState.addElement("Stock 2", ElementType.STOCK, 400, 200);
            canvasState.addElement(flow, ElementType.FLOW, 250, 200);

            FlowGeometry.Point2D pos = FlowEndpointCalculator.cloudPosition(
                    FlowEndpointCalculator.FlowEnd.SINK,
                    editor.getFlows().get(0), canvasState);

            assertThat(pos).isNull();
        }

        @Test
        void shouldReturnPositionForDisconnectedSourceWithSink() {
            editor.addStock(); // Stock 1
            String flow = editor.addFlow(null, "Stock 1");
            canvasState.addElement("Stock 1", ElementType.STOCK, 400, 200);
            canvasState.addElement(flow, ElementType.FLOW, 250, 200);

            FlowGeometry.Point2D pos = FlowEndpointCalculator.cloudPosition(
                    FlowEndpointCalculator.FlowEnd.SOURCE,
                    editor.getFlows().get(0), canvasState);

            assertThat(pos).isNotNull();
            // Should be offset from diamond center away from sink direction
            assertThat(pos.x()).isLessThan(250); // left of diamond (away from sink at 400)
        }

        @Test
        void shouldReturnPositionForDisconnectedSinkWithSource() {
            editor.addStock(); // Stock 1
            String flow = editor.addFlow("Stock 1", null);
            canvasState.addElement("Stock 1", ElementType.STOCK, 400, 200);
            canvasState.addElement(flow, ElementType.FLOW, 550, 200);

            FlowGeometry.Point2D pos = FlowEndpointCalculator.cloudPosition(
                    FlowEndpointCalculator.FlowEnd.SINK,
                    editor.getFlows().get(0), canvasState);

            assertThat(pos).isNotNull();
            // Should be offset from diamond center away from source direction
            assertThat(pos.x()).isGreaterThan(550); // right of diamond (away from source at 400)
        }

        @Test
        void shouldReturnPositionForDisconnectedSourceNoSink() {
            String flow = editor.addFlow(null, null);
            canvasState.addElement(flow, ElementType.FLOW, 250, 200);

            FlowGeometry.Point2D pos = FlowEndpointCalculator.cloudPosition(
                    FlowEndpointCalculator.FlowEnd.SOURCE,
                    editor.getFlows().get(0), canvasState);

            assertThat(pos).isNotNull();
            // Default: left of diamond
            assertThat(pos.x()).isCloseTo(250 - 80, within(1.0));
            assertThat(pos.y()).isCloseTo(200, within(1.0));
        }

        @Test
        void shouldReturnPositionForDisconnectedSinkNoSource() {
            String flow = editor.addFlow(null, null);
            canvasState.addElement(flow, ElementType.FLOW, 250, 200);

            FlowGeometry.Point2D pos = FlowEndpointCalculator.cloudPosition(
                    FlowEndpointCalculator.FlowEnd.SINK,
                    editor.getFlows().get(0), canvasState);

            assertThat(pos).isNotNull();
            // Default: right of diamond
            assertThat(pos.x()).isCloseTo(250 + 80, within(1.0));
            assertThat(pos.y()).isCloseTo(200, within(1.0));
        }
    }

    @Nested
    @DisplayName("hitTestClouds")
    class HitTestClouds {

        @Test
        void shouldReturnHitWhenClickingNearCloud() {
            editor.addStock(); // Stock 1
            String flow = editor.addFlow(null, "Stock 1");
            canvasState.addElement("Stock 1", ElementType.STOCK, 400, 200);
            canvasState.addElement(flow, ElementType.FLOW, 250, 200);

            // Get cloud position to know where to click
            FlowGeometry.Point2D cloudPos = FlowEndpointCalculator.cloudPosition(
                    FlowEndpointCalculator.FlowEnd.SOURCE,
                    editor.getFlows().get(0), canvasState);
            assertThat(cloudPos).isNotNull();

            FlowEndpointCalculator.CloudHit hit = FlowEndpointCalculator.hitTestClouds(
                    cloudPos.x(), cloudPos.y(), canvasState, editor);

            assertThat(hit).isNotNull();
            assertThat(hit.flowName()).isEqualTo(flow);
            assertThat(hit.end()).isEqualTo(FlowEndpointCalculator.FlowEnd.SOURCE);
        }

        @Test
        void shouldReturnNullWhenClickingAwayFromClouds() {
            editor.addStock();
            String flow = editor.addFlow(null, "Stock 1");
            canvasState.addElement("Stock 1", ElementType.STOCK, 400, 200);
            canvasState.addElement(flow, ElementType.FLOW, 250, 200);

            FlowEndpointCalculator.CloudHit hit = FlowEndpointCalculator.hitTestClouds(
                    0, 0, canvasState, editor);

            assertThat(hit).isNull();
        }
    }

    @Nested
    @DisplayName("hitTestConnectedEndpoints")
    class HitTestConnectedEndpoints {

        @Test
        void shouldReturnHitNearStockBorder() {
            editor.addStock(); // Stock 1
            editor.addStock(); // Stock 2
            String flow = editor.addFlow("Stock 1", "Stock 2");
            canvasState.addElement("Stock 1", ElementType.STOCK, 100, 200);
            canvasState.addElement("Stock 2", ElementType.STOCK, 400, 200);
            canvasState.addElement(flow, ElementType.FLOW, 250, 200);

            // The source endpoint is at the right edge of Stock 1
            // Stock 1 is at (100, 200), halfW=70, so right edge = 170
            FlowEndpointCalculator.CloudHit hit = FlowEndpointCalculator.hitTestConnectedEndpoints(
                    170, 200, canvasState, editor);

            assertThat(hit).isNotNull();
            assertThat(hit.flowName()).isEqualTo(flow);
            assertThat(hit.end()).isEqualTo(FlowEndpointCalculator.FlowEnd.SOURCE);
        }

        @Test
        void shouldReturnNullForHalfConnectedFlow() {
            // When one end is already a cloud, connected endpoint detachment
            // should be suppressed to prevent accidentally ripping off the
            // remaining connection.
            editor.addStock(); // Stock 1
            String flow = editor.addFlow("Stock 1", null); // sink is cloud
            canvasState.addElement("Stock 1", ElementType.STOCK, 100, 200);
            canvasState.addElement(flow, ElementType.FLOW, 250, 200);

            // Click right at the source endpoint (Stock 1 right edge = 170)
            FlowEndpointCalculator.CloudHit hit = FlowEndpointCalculator.hitTestConnectedEndpoints(
                    170, 200, canvasState, editor);

            assertThat(hit).isNull();
        }

        @Test
        void shouldReturnNullWhenClickingFarFromEndpoints() {
            editor.addStock(); // Stock 1
            editor.addStock(); // Stock 2
            String flow = editor.addFlow("Stock 1", "Stock 2");
            canvasState.addElement("Stock 1", ElementType.STOCK, 100, 200);
            canvasState.addElement("Stock 2", ElementType.STOCK, 400, 200);
            canvasState.addElement(flow, ElementType.FLOW, 250, 200);

            FlowEndpointCalculator.CloudHit hit = FlowEndpointCalculator.hitTestConnectedEndpoints(
                    0, 0, canvasState, editor);

            assertThat(hit).isNull();
        }
    }
}
