## The conservation equation

Stocks obey a fundamental rule: the change in a stock equals inflows minus outflows.

`Stock(t) = Stock(t-1) + (Inflow - Outflow) * dt`

This is **Euler integration**. At each time step, the simulation engine computes the net flow, multiplies by the time step, and adds the result to the stock's previous value. Courant does this automatically -- you just define the flows.

Conservation means material is never created or destroyed. Every gallon that leaves the tub through Outflow must come from the stock. Every gallon that enters through Inflow adds to it.

## Experiment: drain the tub

1. Click **Inflow Rate** and set it to `0`
2. Press `Ctrl+R` to run

Watch **Water in Tub** drain steadily. The stock decreases by 5 gallons each minute (Outflow Rate = 5 gal/min). After 10 minutes, the tub is empty.

## Experiment: fill the tub

1. Set **Inflow Rate** back to `5`
2. Set **Outflow Rate** to `0`
3. Press `Ctrl+R`

Now the stock only increases. With no drain, water accumulates at 5 gallons per minute after the step at minute 5.

Notice that the Outflow equation uses `MIN(Outflow_Rate, Water_in_Tub)` -- this prevents the stock from going negative. Conservation enforced.
