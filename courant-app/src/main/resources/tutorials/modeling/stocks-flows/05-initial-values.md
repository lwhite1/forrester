## A stock's starting point

Every stock needs an **initial value** -- its state at time zero, before any flows have acted. In the bathtub model, Water in Tub starts at 50 gallons.

The initial value determines the simulation's starting point. It answers: *How much is in the stock when the clock starts?*

## Experiment: change the initial value

1. Click **Water in Tub** on the canvas
2. In the **Properties panel**, change the initial value from `50` to `100`
3. Press `Ctrl+R` to run

The trajectory changes -- the stock starts higher and takes longer to drain before the inflow kicks in. But notice: **the equilibrium stays the same**. Once inflow equals outflow, the stock stabilizes regardless of where it started.

This is an important insight: **equilibrium depends on flow rates, not initial conditions**. The initial value determines *where* the trajectory starts, but the structure of the flows determines *where it ends up*.

## Try other values

Set the initial value to `10`, then `200`. Run each time. The curves look different early on, but all converge to the same behavior once the inflow activates at minute 5.

## Initial values as expressions

Initial values can also be expressions that reference other elements. For example, a stock's initial value could be `Inflow_Rate * 10` -- setting it to whatever 10 minutes of inflow would produce. This keeps the model internally consistent when you change parameters.
