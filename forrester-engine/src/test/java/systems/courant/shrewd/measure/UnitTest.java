package systems.courant.shrewd.measure;

import systems.courant.shrewd.measure.units.length.LengthUnits;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for Unit conversion
 */
public class UnitTest {

    private Quantity miles = new Quantity(100, LengthUnits.MILE);
    private Quantity meters = new Quantity(160934.4, LengthUnits.METER);

    @Test
    public void ratioToBaseUnit() throws Exception {
        assertEquals(1609.344, miles.getUnit().ratioToBaseUnit(), 0.0);
    }

    @Test
    public void getBaseUnit() throws Exception {
        assertEquals(LengthUnits.METER, miles.getUnit().getBaseUnit());
    }

    @Test
    public void fromBaseUnits() throws Exception {
        assertEquals(100, LengthUnits.MILE.fromBaseUnits(meters).getValue(), 0.0001);
    }

    @Test
    public void converter() throws Exception {
        assertEquals(160934.4, miles.convertUnits(LengthUnits.METER).getValue(), 0.0001);
    }

    @Test
    public void toBaseUnits() throws Exception {
        assertEquals(160934.4, miles.inBaseUnits().getValue(), 0.0001);
        assertEquals(LengthUnits.METER, miles.inBaseUnits().getUnit());
    }

}
