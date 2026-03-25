## Does the model reproduce the right pattern?

**Behavior reproduction** tests whether the model generates the known historical or expected pattern of behavior. Compare the model's output to the **reference mode** -- the behavior you're trying to explain.

## Pattern, not precision

The goal isn't a perfect numerical match. It's capturing the right **pattern**:

- **Exponential growth** -- does the output accelerate over time?
- **Oscillation** -- does it cycle with roughly the right period?
- **S-shaped growth** -- does it accelerate then decelerate?
- **Overshoot-and-collapse** -- does it peak then decline?

A model that produces the right pattern for the right structural reasons is more valuable than one that matches historical data through curve-fitting. Curve-fitting can match any dataset but tells you nothing about why the behavior occurs.

## How to evaluate

- Run the model with baseline parameters
- Compare the output visually to your reference mode
- Check timing -- does the transition happen in the right era?
- Check magnitude -- is the output in the right ballpark?

If the model produces a qualitatively different pattern (growth instead of oscillation), the feedback structure is wrong. Go back to your dynamic hypothesis.
