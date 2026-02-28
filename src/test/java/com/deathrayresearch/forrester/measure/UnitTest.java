package com.deathrayresearch.forrester.measure;

import com.deathrayresearch.forrester.measure.units.length.LengthUnits;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for Unit conversion
 */
public class UnitTest {

    private Quantity miles = new Quantity(100, LengthUnits.MILE);
    private Quantity meters = new Quantity(160934, LengthUnits.METER);

    @Test
    public void ratioToBaseUnit() throws Exception {
        assertEquals(1609.34, miles.getUnit().ratioToBaseUnit(), 0.0);
    }

    @Test
    public void getBaseUnit() throws Exception {
        assertEquals(LengthUnits.METER, miles.getUnit().getBaseUnit());
    }

    @Test
    public void fromBaseUnits() throws Exception {
        assertEquals(100, LengthUnits.MILE.fromBaseUnits(meters).getValue(), 0.0);
    }

    @Test
    public void converter() throws Exception {
        assertEquals(160934.0, miles.convertUnits(LengthUnits.METER).getValue(), 0.0);
    }

    @Test
    public void toBaseUnits() throws Exception {
        assertEquals(160934.0, miles.inBaseUnits().getValue(), 0.0);
        assertEquals(LengthUnits.METER, miles.inBaseUnits().getUnit());
    }

}
