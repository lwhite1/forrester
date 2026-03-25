## Push the model to its limits

**Extreme condition tests** set parameters to extreme values -- zero, very large, negative -- and check whether the model produces physically plausible behavior.

## How to run extreme tests

1. Pick a parameter and set it to an extreme value
2. Press **Ctrl+R** to run the simulation
3. Check whether the output makes physical sense

## Examples

- Set **birth rate** to 0 -- population should decline monotonically. If it doesn't, something is creating people from nothing.
- Set **birth rate** to a very large value -- population should grow rapidly, but never go negative.
- Set **initial inventory** to 0 -- production should still work, just with backlog. If the model crashes or produces negative shipments, there's a structural flaw.
- Set **delivery delay** to 0 -- orders should arrive instantly, with no oscillation.

## What failures reveal

When stocks go negative, flows reverse implausibly, or the simulation produces NaN, the model has a structural flaw -- not just a bad parameter value. These are the most valuable tests because they expose hidden assumptions.

Common fixes include adding MIN/MAX constraints to flows and ensuring denominators can't reach zero.
