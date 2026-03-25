## What is DT?

**DT** (delta time) subdivides each time step into smaller calculation intervals. It controls the precision of the Euler integration without changing the time step reported in your results.

- **DT = 1.0** (default) — one calculation per time step
- **DT = 0.5** — two calculations per time step
- **DT = 0.25** — four calculations per time step

Smaller DT values improve accuracy because the Euler approximation is better over shorter intervals.

## When to reduce DT

Use a smaller DT when:

- **Oscillations appear jagged** — the chart shows sharp zigzags instead of smooth waves
- **Stocks overshoot impossible values** — a population goes negative, or a percentage exceeds 100%
- **Results change significantly when you halve DT** — this means the current DT is too coarse

## A practical test

Run the coffee cooling model with DT = 1.0, then again with DT = 0.25. If the curves are nearly identical, DT = 1.0 is fine. If they differ noticeably, use the smaller value.

## Trade-off

Each halving of DT doubles the computation. For most tutorial models, DT = 1.0 is sufficient. Reserve smaller values for stiff systems or fast-changing dynamics.
