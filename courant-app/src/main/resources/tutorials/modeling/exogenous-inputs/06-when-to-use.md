## Good uses for exogenous inputs

- **Testing model response** — apply a STEP or PULSE to see how feedback loops respond. This is the most common and most valuable use.
- **Scenario analysis** — *"What if demand jumps by 20%?"* Exogenous inputs let you pose precise what-if questions.
- **Historical data fitting** — drive the model with known historical inputs to see if it reproduces observed behavior.
- **Truly external forces** — weather, government regulations, natural disasters. These are genuinely outside the system boundary.

## Warning signs

If your model has many exogenous inputs, ask yourself:

- *Should this variable respond to system state?* If demand depends on price, and price depends on inventory, then demand is endogenous. Making it exogenous hides important feedback.
- *Am I avoiding modeling something hard?* Exogenous inputs can be a shortcut that masks missing structure.
- *Is the model boundary too narrow?* If you're feeding in many external time series, consider expanding the model to include those processes.

## The endogenous principle

A model with strong endogenous structure generates behavior from feedback. A model driven mostly by exogenous inputs is closer to a spreadsheet forecast — it can't explain *why* things happen, only replay what you put in.

Aim for endogenous structure first. Add exogenous inputs only where they're truly needed.
