## Check the results critically

Finding an optimal value is only the first step. You need to verify that the solution makes sense.

## Physical plausibility

Run the model with the optimal parameter values and inspect the behavior. Does the trajectory look reasonable? A mathematically optimal solution can still be physically nonsensical if the model or constraints are incomplete.

## Boundary effects

Are any optimal values sitting right at their bounds? That often means the true optimum lies outside the range you specified. Consider widening the bounds and re-running.

## Local optima

Optimization can get trapped in **local optima** -- solutions that are better than their neighbors but not the best overall. Try running the optimizer from different starting points. If you get different answers, the best one is more likely to be near the global optimum.

## Sensitivity near the optimum

Small changes near the optimum should produce small changes in the objective. If the objective is very sensitive to tiny parameter shifts, the solution may be fragile and hard to implement reliably in practice.
