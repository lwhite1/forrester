## What you learned

- **Units are a type system** — they catch structural errors in equations before you run the simulation
- **Stocks hold material** — People, Gallons, Dollars
- **Flows move material per time** — People/Day, Gallons/Minute
- **Dimensional consistency** — both sides of every equation must have the same units
- **Dimensionless values** — fractions, ratios, and normalized table outputs carry no units
- **Common errors** — missing time units on flows, mixed time scales, adding unlike units

## The rule of thumb

If you can't state the units of every variable in your model, you don't fully understand the model yet.

## Key insight

Dimensional analysis is the cheapest debugging tool in system dynamics. It costs seconds to assign units and catches entire categories of bugs automatically. Models without units are untestable in a fundamental way — you can't verify that the equations make physical sense.

## Try next

The next tutorial covers building models with **multiple interacting stocks**, where unit discipline becomes even more important as material flows between subsystems.
