## One-time impulses

`PULSE(volume, time)` produces a one-time impulse at the specified time. The total volume delivered equals `volume`, concentrated into a single time step.

`PULSE(100, 30)` delivers 100 units at time 30 and nothing at any other time.

Use PULSE to test **impulse response** — how does the system absorb and recover from a one-time shock? A resilient system returns to equilibrium quickly. A fragile system may be permanently displaced.

## Steady ramps

`RAMP(slope, start)` produces a steadily increasing signal beginning at the start time.

`RAMP(5, 10)` returns:
- `0` for TIME < 10
- `5 * (TIME - 10)` for TIME >= 10

At time 20, the value is 50. At time 30, it's 100. The signal grows without bound.

Use RAMP to test how a system handles **gradually increasing pressure**. Can the balancing feedback keep up, or does the system eventually break down?

## Choosing between them

- **STEP** — permanent level change. *"What if demand is suddenly higher?"*
- **PULSE** — temporary shock. *"What if we get a one-time surge of orders?"*
- **RAMP** — steady trend. *"What if demand grows by 5 units per year?"*
