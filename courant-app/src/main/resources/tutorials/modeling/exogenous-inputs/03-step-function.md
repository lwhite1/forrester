## Sudden permanent changes

`STEP(height, time)` produces a step change: zero before the specified time, `height` after.

`STEP(10, 20)` returns:
- `0` for TIME < 20
- `10` for TIME >= 20

## What it's for

Use STEP to test how a system responds to a sudden, permanent change:

- **Demand shock** — customers suddenly want 10 more units per day
- **New regulation** — an emissions cap takes effect on a specific date
- **Resource change** — a factory adds a new production line

## Try it

Create a simple model with a constant parameter:

`External_Demand = 100 + STEP(20, 50)`

This gives demand of 100 until time 50, then 120 forever after. Run the simulation and watch how the system adjusts.

The key question: *How long does it take the system to reach a new equilibrium after the step?* Fast-responding systems settle quickly. Systems with delays and long adjustment times take much longer — and may oscillate along the way.
