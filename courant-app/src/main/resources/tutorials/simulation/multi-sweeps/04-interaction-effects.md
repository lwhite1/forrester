## Detecting interactions

An **interaction effect** means the influence of one parameter changes depending on the value of another.

To check for interactions, compare the fan chart shape at different slices:

- Filter the results to show only runs where `Room_Temperature` is low (10). Look at how `Cooling_Rate` affects the outcome.
- Filter again for high `Room_Temperature` (30). Does `Cooling_Rate` have the same effect?

If the fan shape changes -- wider in one case, narrower in the other, or shifted in a different direction -- the parameters **interact**. You cannot analyze them independently.

## Why interactions matter

If parameters interact, single-parameter sensitivity results can be misleading. A parameter might appear insensitive when tested at the default value of another, but highly sensitive at a different value. Multi-parameter sweeps catch this.
