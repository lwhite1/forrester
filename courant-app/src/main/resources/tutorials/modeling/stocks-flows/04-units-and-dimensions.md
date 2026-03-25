## Every element needs units

Good models are dimensionally consistent. In the bathtub model:

- **Water in Tub** has units of `Gallon` -- a material unit
- **Inflow** and **Outflow** have units of `Gallon per Minute` -- a rate unit
- **Inflow Rate** and **Outflow Rate** have units of `Gallon per Minute`

The key rule: a flow's time unit must match the simulation time step. Since the bathtub runs with a 1-minute time step, flows are in gallons **per minute**.

## Check dimensional consistency

Press `Ctrl+B` to run a dimensional analysis check. Courant verifies that every equation's units balance. If they don't, you'll see warnings highlighting the mismatched elements.

## Common unit errors

- **Missing time dimension on a flow** -- a flow should be a rate (e.g., gallons/minute), not just a quantity (gallons)
- **Mixing units** -- adding gallons to liters, or days to minutes, produces nonsensical results
- **Forgetting rate conversions** -- if your stock is in People and your time step is Days, a flow of 10 means 10 people per day, not per year

## Set units on your elements

Click any element and look at the **Units** field in the **Properties panel**. Always fill this in. It catches errors early and makes your model self-documenting.
