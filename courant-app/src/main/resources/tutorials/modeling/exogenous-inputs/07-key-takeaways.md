## What you learned

- **TIME** gives the current simulation time for time-dependent equations
- **STEP(height, time)** produces a sudden permanent change — useful for demand shocks and policy switches
- **PULSE(volume, time)** delivers a one-time impulse — useful for testing system resilience
- **RAMP(slope, start)** creates a steadily increasing signal — useful for testing trend response
- **Combined inputs** build complex test scenarios from simple building blocks

## Behavior modes revealed

- Step response (how fast the system reaches new equilibrium)
- Impulse response (how the system absorbs and recovers from shocks)
- Ramp response (whether feedback can keep pace with a growing trend)

## Key insight

Exogenous inputs are a **testing tool**, not a modeling philosophy. The best system dynamics models generate behavior from internal feedback structure. Use STEP, PULSE, and RAMP to probe that structure — to find where the model is robust and where it breaks down. But resist the temptation to replace endogenous structure with exogenous time series.

## Try next

- Apply STEP and PULSE inputs to the Delays tutorial model — how does delivery delay affect the system's response to demand shocks?
- Use RAMP to stress-test the feedback loops tutorial model — at what growth rate does the balancing loop fail to maintain equilibrium?
