## Explore the bathtub model

The tutorial has opened the **Bathtub** model. It has three elements:

- **Water in Tub** -- a stock, starting at 50 gallons
- **Inflow** -- a flow that adds water (controlled by Inflow Rate, currently 5 gal/min, starting at minute 5)
- **Outflow** -- a flow that drains water (controlled by Outflow Rate, currently 5 gal/min)

Click each element on the canvas and examine its equation in the **Properties panel** on the right.

## Run the simulation

Press `Ctrl+R` to run. The simulation runs for 10 minutes with a 1-minute time step.

## What you should see

For the first 5 minutes, only the drain is active. Water in Tub falls steadily. At minute 5, the inflow kicks in (via a STEP function). Since Inflow Rate equals Outflow Rate (both 5 gal/min), the stock stabilizes.

This is **dynamic equilibrium** -- the stock isn't changing, but both flows are active. Water is still pouring in and draining out; the net flow is zero.

## Try it

Click **Inflow Rate** and change it to `8`. Re-run with `Ctrl+R`. Now inflow exceeds outflow after minute 5, so the tub fills. What happens if you set Inflow Rate to `3`?
