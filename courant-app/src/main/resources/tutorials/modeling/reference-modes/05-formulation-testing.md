## From hypothesis to running model

**Formulation** translates the dynamic hypothesis into a running simulation. This means defining:

- **Stocks** -- what accumulates?
- **Flows** -- what changes the stocks?
- **Auxiliaries** -- what intermediate calculations are needed?
- **Parameters** -- what constants control behavior?
- **Equations** -- what mathematical relationships link variables?
- **Initial values** -- where does the system start?

## Testing the formulation

Apply the tests from the previous tutorial:

1. Run **Ctrl+B** to check units
2. Test extreme conditions on key parameters
3. Compare output to the reference mode
4. Run sensitivity analysis on uncertain parameters

## Iteration is normal

This stage is rarely one-pass. Test results frequently reveal:

- Missing feedback loops that need to be added
- Equations that produce implausible extreme behavior
- Parameters the model is too sensitive to
- Behavior patterns that don't match the reference mode

Each finding sends you back to revise. Don't stop at the first working version -- refine until the model passes all tests and you understand why it produces the behavior it does.
