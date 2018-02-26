package com.deathrayresearch.forrester.measure;

import com.deathrayresearch.forrester.measure.units.length.Meter;
import com.deathrayresearch.forrester.measure.units.length.Mile;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for Unit conversion
 */
public class UnitTest {

    private Quantity miles = new Quantity("Distance 1", 100, Mile.getInstance());
    private Quantity meters = new Quantity("Distance 2", 160934, Meter.getInstance());

    @Test
    public void ratioToBaseUnit() throws Exception {
        assertEquals(1609.34, miles.getUnit().ratioToBaseUnit(), 0.0);
    }

    @Test
    public void getBaseUnit() throws Exception {
        assertEquals(Meter.getInstance(), miles.getUnit().getBaseUnit());
    }

    @Test
    public void fromBaseUnits() throws Exception {
        assertEquals(100, Mile.getInstance().fromBaseUnits(meters).getValue(), 0.0);
    }

    @Test
    public void toBaseUnits() throws Exception {
        assertEquals(160934.0, miles.inBaseUnits().getValue(), 0.0);
        assertEquals(Meter.getInstance(), miles.inBaseUnits().getUnit());
    }

}