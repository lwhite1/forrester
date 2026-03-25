## Beyond variable bounds

**Constraints** limit the search space in ways that variable bounds alone cannot express. They prevent the optimizer from finding solutions that are mathematically optimal but practically impossible.

## Examples of constraints

- Inventory must never drop below zero during the simulation
- Total cost must stay under $1M
- Temperature must remain above freezing at all times

These are conditions on the **behavior** of the model, not just the values of individual parameters.

## Why constraints matter

Without constraints, the optimizer is free to exploit any loophole. It might find a "solution" that drains a stock negative, exceeds a physical limit, or violates a policy rule. Constraints keep the search grounded in reality.

## Setting constraints in Courant

Add constraints in **Simulate --> Optimization** alongside your decision variables and objective. Each constraint specifies a model variable, a condition (greater than, less than, or equal to), and a threshold value.
