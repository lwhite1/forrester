## Building complex test scenarios

You can combine input functions in a single equation:

`Test_Input = Base_Value + STEP(5, 10) + PULSE(20, 30) + RAMP(2, 50)`

This produces:

- **Time 0-9**: Base_Value (steady state)
- **Time 10**: Base_Value + 5 (permanent step up)
- **Time 30**: A one-time spike of 20 on top of the step
- **Time 50+**: A ramp begins adding 2 per time unit, on top of everything else

## Why combine?

Combined inputs create realistic test scenarios. Real-world systems face multiple disruptions simultaneously — a trend, a shock, a policy change. Testing your model against combined inputs reveals whether the feedback structure can handle complexity.

## Negative inputs

Input functions can be negative:

- `STEP(-10, 40)` — a permanent decrease at time 40
- `RAMP(-3, 20)` — a declining trend starting at time 20

You can also cancel a step: `STEP(10, 20) + STEP(-10, 40)` produces a "pulse" that's on from time 20 to 40. This is a useful pattern for modeling temporary policies or seasonal effects.
