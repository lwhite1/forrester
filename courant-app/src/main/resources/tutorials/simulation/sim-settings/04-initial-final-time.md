## Built-in time constants

The engine provides two constants you can use in any equation:

- **INITIAL_TIME** — the time value at the start of the simulation (defaults to 0)
- **FINAL_TIME** — the time value at the end, calculated as `INITIAL_TIME + Duration`

## Changing INITIAL TIME

Some models set INITIAL_TIME to a calendar year for readability. A model of product adoption from 1990 to 2020 is easier to understand with `INITIAL_TIME = 1990` than `INITIAL_TIME = 0`.

Set this in **Simulate --> Simulation Settings** under **Initial Time**.

## Using time constants in equations

These constants are available anywhere you write an equation. Common patterns:

- **Policy switch at the midpoint:**

      IF THEN ELSE(Time > (INITIAL_TIME + FINAL_TIME) / 2, new_policy, old_policy)

- **Fraction of time elapsed:**

      (Time - INITIAL_TIME) / (FINAL_TIME - INITIAL_TIME)

- **Phase-in over the first quarter:**

      MIN(1, (Time - INITIAL_TIME) / ((FINAL_TIME - INITIAL_TIME) * 0.25))

These let you write time-aware equations without hard-coding specific numbers.
