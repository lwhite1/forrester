## Fixed-time delays with conveyors

The **conveyor** pattern is a variation where items spend a fixed time in each stage. Unlike aging chains where residence time is an *average* (some people die young, some live long), conveyors guarantee an exact delay.

Use `DELAY FIXED` for conveyor behavior:

`Output = DELAY FIXED(Input, delay_time, initial_value)`

## When to use conveyors

Conveyors suit processes with deterministic timing:

- **Manufacturing** — each assembly stage takes exactly 3 days
- **Shipping** — transit time is a fixed 5 days
- **Regulatory** — an approval process takes exactly 90 days

## Aging chains vs. conveyors

| Property | Aging chain | Conveyor |
|---|---|---|
| Delay type | Average (exponential) | Fixed (pipeline) |
| Output pattern | Smooth, distributed | Sharp, exact timing |
| Material conservation | Yes | Yes |
| Best for | Populations, cohorts | Pipelines, queues |

In practice, real systems often fall between these extremes. Aging chains are more common in social and biological systems; conveyors are more common in engineering and logistics.
