## Check structure before behavior

**Structure tests** verify that the model is consistent with known physical and institutional reality. These tests don't require running the simulation -- they examine the model's equations and diagram.

Ask these questions:

- **Conservation** -- Does every stock have appropriate inflows and outflows? Can inventory go negative when it physically can't?
- **Dimensional consistency** -- Are the units correct throughout? Use **Ctrl+B** to run a units check. A flow in "widgets/week" feeding a stock in "dollars" is a structural error.
- **Causal relationships** -- Is each relationship supported by evidence or theory? Does a higher price actually reduce demand in this market?
- **Boundary adequacy** -- Are all important feedback loops inside the model boundary? A model that treats price as exogenous can't explain market dynamics.

Structure tests are the first line of defense. A model with dimensional errors or missing feedbacks will produce misleading behavior no matter how carefully you calibrate it.

Fix structural problems before moving on to behavioral tests.
