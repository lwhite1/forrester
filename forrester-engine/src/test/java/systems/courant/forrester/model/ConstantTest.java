package systems.courant.forrester.model;

import org.junit.jupiter.api.Test;

import static systems.courant.forrester.measure.Units.THING;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConstantTest {

    @Test
    public void shouldReturnValue() {
        Constant c = new Constant("Pi", THING, 3.14);
        assertEquals(3.14, c.getValue(), 0.0);
    }

    @Test
    public void shouldReturnIntValue() {
        Constant c = new Constant("Count", THING, 7.0);
        assertEquals(7, c.getIntValue());
    }

    @Test
    public void shouldRoundIntValue() {
        Constant c = new Constant("Count", THING, 7.6);
        assertEquals(8, c.getIntValue());
    }

    @Test
    public void shouldReturnUnit() {
        Constant c = new Constant("C1", THING, 1);
        assertEquals(THING, c.getUnit());
    }

    @Test
    public void shouldReturnName() {
        Constant c = new Constant("Gravity", THING, 9.81);
        assertEquals("Gravity", c.getName());
    }
}
