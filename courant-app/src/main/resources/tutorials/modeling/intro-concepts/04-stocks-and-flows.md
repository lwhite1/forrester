## The bathtub analogy

Imagine a bathtub with a faucet and a drain. Water accumulates in the tub. The faucet adds water; the drain removes it. The water level at any moment depends on the entire history of inflow and outflow — not just what's happening right now.

This is the fundamental building block of System Dynamics:

- **Stock** — a quantity that accumulates over time (the water in the tub)
- **Flow** — a rate that changes a stock (the faucet, the drain)

## Why stocks matter

Stocks create **inertia** and **memory** in a system. You can't empty a bathtub instantly, no matter how fast the drain. You can't train a workforce overnight. You can't undo decades of CO2 emissions in a year.

Key properties of stocks:

- **They accumulate** — their value at time *t* depends on all past flows
- **They create delays** — even after a flow stops, the stock retains its level
- **They decouple flows** — inflow and outflow can differ, which is why bathtubs fill and drain
- **They provide information** — decisions are often based on the current level of a stock

## Flows are rates

A flow is measured as a quantity **per unit of time** — gallons per minute, people per day, dollars per month. Flows are the only way to change a stock. No other mechanism can alter a stock's value.

Everything in System Dynamics builds on this distinction. Every model, no matter how complex, is ultimately a network of stocks connected by flows.
