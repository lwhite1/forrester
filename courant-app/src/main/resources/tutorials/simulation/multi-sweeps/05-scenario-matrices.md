## Structure exploration with scenarios

A **scenario matrix** is a multi-parameter sweep with deliberately chosen, named combinations rather than a uniform grid. Common scenarios include:

- **Best case** -- favorable values for all key parameters
- **Worst case** -- unfavorable values for all key parameters
- **Most likely** -- best estimates for each parameter
- **Mixed cases** -- favorable on one dimension, unfavorable on another

## Building a scenario matrix

Define 2-3 meaningful values per parameter (not 10). Run all combinations. This gives decision-makers a structured, interpretable view of possible outcomes.

For the coffee model, you might define:

- `Cooling_Rate`: slow (0.05), medium (0.10), fast (0.20)
- `Room_Temperature`: cold room (10), warm room (25)

Six combinations, each with a clear physical interpretation. This is easier to communicate than a 100-run grid.
