package systems.courant.sd.model;

import systems.courant.sd.measure.Quantity;

import org.junit.jupiter.api.Test;

import static systems.courant.sd.measure.Units.DAY;
import static systems.courant.sd.measure.Units.HOUR;
import static systems.courant.sd.measure.Units.MINUTE;
import static systems.courant.sd.measure.Units.SECOND;
import static systems.courant.sd.measure.Units.THING;
import static systems.courant.sd.measure.Units.WEEK;
import static systems.courant.sd.measure.Units.YEAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FlowTest {

    private Flow createTestFlow(String name, double valuePerMinute) {
        return Flow.create(name, MINUTE, () -> new Quantity(valuePerMinute, THING));
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

    // Flow.create() static factory tests

    @Test
    public void createShouldReturnFlowWithCorrectTimeUnit() {
        Flow flow = Flow.create("test", DAY, () -> new Quantity(10, THING));
        assertEquals(DAY, flow.getTimeUnit());
    }

    @Test
    public void createShouldReturnFlowWithCorrectRate() {
        Flow flow = Flow.create("test", DAY, () -> new Quantity(10, THING));
        Quantity rate = flow.flowPerTimeUnit(DAY);
        assertEquals(10, rate.getValue(), 0.001);
    }

    @Test
    public void createShouldConvertRateToOtherTimeUnit() {
        Flow flow = Flow.create("test", DAY, () -> new Quantity(7, THING));
        Quantity perWeek = flow.flowPerTimeUnit(WEEK);
        assertEquals(49, perWeek.getValue(), 0.001);
    }

    // Time unit tests via Flow.create()

    @Test
    public void flowWithDayTimeUnitShouldReturnDay() {
        Flow flow = Flow.create("daily", DAY, () -> new Quantity(1, THING));
        assertEquals(DAY, flow.getTimeUnit());
    }

    @Test
    public void flowWithHourTimeUnitShouldReturnHour() {
        Flow flow = Flow.create("hourly", HOUR, () -> new Quantity(1, THING));
        assertEquals(HOUR, flow.getTimeUnit());
    }

    @Test
    public void flowWithSecondTimeUnitShouldReturnSecond() {
        Flow flow = Flow.create("perSecond", SECOND, () -> new Quantity(1, THING));
        assertEquals(SECOND, flow.getTimeUnit());
    }

    @Test
    public void flowWithWeekTimeUnitShouldReturnWeek() {
        Flow flow = Flow.create("weekly", WEEK, () -> new Quantity(1, THING));
        assertEquals(WEEK, flow.getTimeUnit());
    }

    @Test
    public void flowWithYearTimeUnitShouldReturnYear() {
        Flow flow = Flow.create("yearly", YEAR, () -> new Quantity(1, THING));
        assertEquals(YEAR, flow.getTimeUnit());
    }
}
