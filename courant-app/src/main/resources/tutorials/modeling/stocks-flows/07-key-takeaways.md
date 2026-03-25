## What you learned

- **Stocks accumulate** -- they are the memory of a system, changed only through flows
- **Flows are rates** -- they add to or subtract from stocks over time
- **Conservation is guaranteed** -- `Stock(t) = Stock(t-1) + (Inflow - Outflow) * dt`
- **Units matter** -- stocks have material units, flows have rate units, and dimensional consistency prevents errors
- **Initial values set the starting point** -- but equilibrium depends on flow structure, not starting conditions
- **Reinforcing loops drive exponential growth** -- when a flow depends on its own stock, compounding takes over

## Behavior modes seen

- Linear change (constant flow rates in the bathtub)
- Dynamic equilibrium (inflow equals outflow)
- Exponential growth (reinforcing loop with no balancing feedback)

## Try next

- Open the **SIR Epidemic Model** tutorial to see reinforcing and balancing loops interact
- Add a **Carrying Capacity** variable to the exponential growth model: change Births to `Population * Birth_Rate * (1 - Population / Carrying_Capacity)` and watch S-shaped growth emerge
- Experiment with different time steps in **Simulate → Simulation Settings** to see how step size affects accuracy
