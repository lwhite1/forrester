## The fundamental unit relationship

A stock and its flows are connected by integration over time. This creates a strict unit relationship:

- A **stock** holds a **material unit** — People, Gallons, Widgets, Dollars
- A **flow** moves material per unit time — People/Day, Gallons/Minute, Dollars/Year

The flow's time unit must match the simulation's time step. If your simulation runs in Days, all flows must be expressed per Day.

## Example

| Element          | Units        |
|------------------|--------------|
| Population       | People       |
| Birth_Rate       | People/Day   |
| Death_Rate       | People/Day   |
| Simulation step  | Day          |

Each time step, Courant computes:

    Population = Population + (Birth_Rate - Death_Rate) * DT

For this to work dimensionally: People + (People/Day) * Day = People + People = People. The units balance.

## What goes wrong

If Birth_Rate were accidentally in People/Year but the simulation runs in Days, the stock would accumulate 365 times too slowly. The model runs without crashing, but the results are quietly wrong.
