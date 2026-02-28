package com.deathrayresearch.forrester.model;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.TimeUnit;
import com.deathrayresearch.forrester.model.flows.FlowPerDay;
import com.deathrayresearch.forrester.model.flows.FlowPerHour;
import com.deathrayresearch.forrester.model.flows.FlowPerMinute;
import com.deathrayresearch.forrester.model.flows.FlowPerSecond;
import com.deathrayresearch.forrester.model.flows.FlowPerWeek;
import com.deathrayresearch.forrester.model.flows.FlowPerYear;

import org.junit.jupiter.api.Test;

import static com.deathrayresearch.forrester.measure.Units.DAY;
import static com.deathrayresearch.forrester.measure.Units.HOUR;
import static com.deathrayresearch.forrester.measure.Units.MINUTE;
import static com.deathrayresearch.forrester.measure.Units.SECOND;
import static com.deathrayresearch.forrester.measure.Units.THING;
import static com.deathrayresearch.forrester.measure.Units.WEEK;
import static com.deathrayresearch.forrester.measure.Units.YEAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FlowTest {

    private FlowPerMinute createTestFlow(String name, double valuePerMinute) {
        return new FlowPerMinute(name) {
            @Override
            protected Quantity quantityPerTimeUnit() {
                return new Quantity(valuePerMinute, THING);
            }
        };
    }

    @Test
    public void shouldReturnTimeUnit() {
        Flow flow = createTestFlow("test", 10);
        assertEquals(MINUTE, flow.getTimeUnit());
    }

    @Test
    public void shouldConvertFlowRateToOtherTimeUnit() {
        Flow flow = createTestFlow("test", 10);
        Quantity perHour = flow.flowPerTimeUnit(HOUR);
        assertEquals(600, perHour.getValue(), 0.001);
    }

    @Test
    public void shouldRecordAndRetrieveHistory() {
        Flow flow = createTestFlow("test", 10);
        flow.recordValue(new Quantity(5.0, THING));
        flow.recordValue(new Quantity(15.0, THING));
        assertEquals(5.0, flow.getHistoryAtTimeStep(0), 0.001);
        assertEquals(15.0, flow.getHistoryAtTimeStep(1), 0.001);
    }

    @Test
    public void shouldReturnZeroForNegativeHistoryIndex() {
        Flow flow = createTestFlow("test", 10);
        assertEquals(0, flow.getHistoryAtTimeStep(-1), 0.001);
    }

    @Test
    public void shouldReturnZeroForOutOfBoundsHistory() {
        Flow flow = createTestFlow("test", 10);
        assertEquals(0, flow.getHistoryAtTimeStep(0), 0.001);
        assertEquals(0, flow.getHistoryAtTimeStep(100), 0.001);
    }

    @Test
    public void shouldHaveNullSourceByDefault() {
        Flow flow = createTestFlow("test", 10);
        assertNull(flow.getSource());
    }

    @Test
    public void shouldHaveNullSinkByDefault() {
        Flow flow = createTestFlow("test", 10);
        assertNull(flow.getSink());
    }

    @Test
    public void shouldSetAndGetSource() {
        Flow flow = createTestFlow("test", 10);
        Stock stock = new Stock("sourceStock", 100, THING);
        flow.setSource(stock);
        assertEquals(stock, flow.getSource());
    }

    @Test
    public void shouldSetAndGetSink() {
        Flow flow = createTestFlow("test", 10);
        Stock stock = new Stock("sinkStock", 100, THING);
        flow.setSink(stock);
        assertEquals(stock, flow.getSink());
    }

    @Test
    public void shouldIncludeNameInToString() {
        Flow flow = createTestFlow("myFlow", 10);
        assertTrue(flow.toString().contains("myFlow"));
    }

    // FlowPer* subclass time unit tests

    @Test
    public void flowPerDayShouldReturnDay() {
        FlowPerDay flow = new FlowPerDay("daily") {
            @Override
            protected Quantity quantityPerTimeUnit() {
                return new Quantity(1, THING);
            }
        };
        assertEquals(DAY, flow.getTimeUnit());
    }

    @Test
    public void flowPerHourShouldReturnHour() {
        FlowPerHour flow = new FlowPerHour("hourly") {
            @Override
            protected Quantity quantityPerTimeUnit() {
                return new Quantity(1, THING);
            }
        };
        assertEquals(HOUR, flow.getTimeUnit());
    }

    @Test
    public void flowPerSecondShouldReturnSecond() {
        FlowPerSecond flow = new FlowPerSecond("perSecond") {
            @Override
            protected Quantity quantityPerTimeUnit() {
                return new Quantity(1, THING);
            }
        };
        assertEquals(SECOND, flow.getTimeUnit());
    }

    @Test
    public void flowPerWeekShouldReturnWeek() {
        FlowPerWeek flow = new FlowPerWeek("weekly") {
            @Override
            protected Quantity quantityPerTimeUnit() {
                return new Quantity(1, THING);
            }
        };
        assertEquals(WEEK, flow.getTimeUnit());
    }

    @Test
    public void flowPerYearShouldReturnYear() {
        FlowPerYear flow = new FlowPerYear("yearly") {
            @Override
            protected Quantity quantityPerTimeUnit() {
                return new Quantity(1, THING);
            }
        };
        assertEquals(YEAR, flow.getTimeUnit());
    }
}
