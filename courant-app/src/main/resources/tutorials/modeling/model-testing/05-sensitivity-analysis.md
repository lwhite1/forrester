## Which parameters matter?

**Sensitivity analysis** varies parameters one at a time and observes which ones significantly change the model's behavior. This reveals what the model depends on -- and what it doesn't.

## Running a parameter sweep

1. Go to **Simulate -> Parameter Sweep**
2. Select the parameter to vary
3. Set the range (start, end, step)
4. Run the sweep and compare the resulting trajectories

## Interpreting results

- **High sensitivity** -- small changes in the parameter produce large changes in behavior. These parameters need careful estimation from data or expert judgment.
- **Low sensitivity** -- the behavior barely changes. These parameters can be set to reasonable defaults without much concern.
- **Threshold sensitivity** -- behavior is stable across a range, then changes abruptly. These are the most important to understand because they indicate nonlinear regime shifts.

## What sensitivity tells you

Sensitivity analysis serves two purposes:

- **Practical** -- it tells you where to invest effort in data collection and parameter estimation
- **Theoretical** -- it reveals which structures dominate the model's behavior and which are secondary

A model that is wildly sensitive to a poorly known parameter is not ready for policy analysis. Either estimate that parameter better or acknowledge the uncertainty.
