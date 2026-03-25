## The TIME variable

`TIME` gives the current simulation time at each step. Use it in equations that need to depend on time directly.

Related variables:

- **INITIAL TIME** — the simulation start time
- **FINAL TIME** — the simulation end time
- **TIME STEP** — the integration step size

## Using TIME in equations

You can write time-dependent equations directly:

`Policy = IF THEN ELSE(TIME > 50, new_value, old_value)`

This switches from `old_value` to `new_value` at time 50. It works, but it's not the cleanest approach. The built-in input functions (STEP, PULSE, RAMP) are more readable and less error-prone.

## When to use TIME directly

Use `TIME` directly when you need custom time-dependent behavior that the built-in functions don't cover — for example, a sinusoidal input for seasonal patterns:

`Seasonal_Factor = 1 + 0.3 * SIN(2 * 3.14159 * TIME / 12)`

This creates a 12-month cycle with 30% amplitude.
