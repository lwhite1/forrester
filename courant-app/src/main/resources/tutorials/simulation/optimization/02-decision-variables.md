## What the optimizer can change

**Decision variables** are the parameters the optimizer is allowed to adjust. They represent real decisions -- things you can actually control in the system being modeled.

## Setting up decision variables

Select decision variables in **Simulate --> Optimization**. For each variable, set upper and lower bounds:

- **Cooling_Rate**: 0.01 to 0.50
- **Room_Temperature**: 15 to 30

The optimizer searches only within these bounds. Tight bounds focus the search but may exclude the true optimum. Wide bounds give more freedom but may slow convergence.

## Choosing well

Good decision variables are:

- **Controllable** -- you can actually set their value in the real system
- **Influential** -- changing them has a meaningful effect on the outcome
- **Bounded** -- you know a reasonable range for each

Avoid using internal model constants (like unit conversions) as decision variables. The optimizer will happily change them, but the results won't mean anything useful.
