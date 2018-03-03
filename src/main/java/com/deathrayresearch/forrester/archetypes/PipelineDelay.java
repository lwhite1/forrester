package com.deathrayresearch.forrester.archetypes;

import com.deathrayresearch.forrester.measure.Quantity;
import com.deathrayresearch.forrester.model.Flow;
import com.deathrayresearch.forrester.model.Stock;

/**
 * Encapsulates a formula for a pipeline delay: the kind of delay you see in an assembly line where units pass through
 * in fifo order, and the delay is a constant value of timeSteps. This formula is for the outflow from the stock,
 * which represent departures from that assembly line
 *
 * The delay is calculated as an offset from the inflow to the same stock
 */
public class PipelineDelay {

    private final int currentStep;
    private final int delay;
    private final Flow inflow;

    public static Quantity from(Flow inflow, int currentStep, int delay) {
        return new PipelineDelay(inflow, currentStep, delay).getCurrentQuantity();
    }

    private PipelineDelay(Flow inflow, int currentStep, int delay) {
        this.delay = delay;
        this.inflow = inflow;
        this.currentStep = currentStep;
    }

    public Quantity getCurrentQuantity() {
        int referenceStep = currentStep - delay;
        double value = inflow.getHistoryAtTimeStep(referenceStep);
        return new Quantity(value, inflow.getSink().getUnit());
    }
}
