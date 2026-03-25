## How a simulation works

A simulation is a loop that repeats four steps:

1. **Evaluate flows** — compute all rates from current stock values
2. **Update stocks** — add or subtract the net flow for each stock
3. **Advance time** — move the clock forward by one time step
4. **Repeat** — go back to step 1 until time runs out

Each pass through the loop is one **time step**. The simulation starts at **INITIAL TIME** and ends at **FINAL TIME**.

## The integration rule

The engine uses **Euler integration**, the simplest numerical method:

    Stock(t + dt) = Stock(t) + NetFlow * dt

At every step, each stock's new value equals its old value plus the net flow multiplied by the time increment. This is the discrete approximation of continuous change that makes simulation possible.

## Why this matters

Every setting you configure in the next few steps controls some part of this loop — how big each time step is, how many steps to take, and how finely to subdivide each step for accuracy.
