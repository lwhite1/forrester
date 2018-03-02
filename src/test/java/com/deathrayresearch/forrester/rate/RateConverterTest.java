package com.deathrayresearch.forrester.rate;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.measure.units.length.Mile;
import com.deathrayresearch.forrester.measure.units.time.Day;
import com.deathrayresearch.forrester.measure.units.time.Hour;
import com.deathrayresearch.forrester.measure.units.time.Minute;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class RateConverterTest {


    @Test
    public void convert() throws Exception {
        Quantity original = new Quantity(100, Mile.getInstance());
        Quantity newQuantity = RateConverter.convert(original, Hour.getInstance(), Day.getInstance());
        assertEquals(2400, newQuantity.getValue(), 0.0);
    }

    @Test
    public void convert2() throws Exception {
        Quantity original = new Quantity(10, Mile.getInstance());
        Quantity newQuantity = RateConverter.convert(original, Hour.getInstance(), Day.getInstance());
        assertEquals(240, newQuantity.getValue(), 0.0);
    }

    @Test
    public void convert3() throws Exception {
        Quantity original = new Quantity(2400, Mile.getInstance());
        Quantity newQuantity = RateConverter.convert(original, Day.getInstance(), Hour.getInstance());
        assertEquals(100, newQuantity.getValue(), 0.0);
    }

    @Test
    public void convert4() throws Exception {
        Quantity original = new Quantity(60, Mile.getInstance());
        Quantity newQuantity = RateConverter.convert(original, Hour.getInstance(), Minute.getInstance());
        assertEquals(1, newQuantity.getValue(), 0.0);
    }

}