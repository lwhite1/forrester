## Drawing the boundary

Every model needs a **system boundary** -- the line between what's inside the model and what's outside.

- **Sources** (clouds on the inflow side) represent unlimited external supply. Prey births draw from a source because we aren't modeling the food chain below prey.
- **Sinks** (clouds on the outflow side) represent where material leaves. Predator deaths flow to a sink because we aren't tracking decomposition.

The boundary determines what is **endogenous** (explained by the model) and what is **exogenous** (assumed as given).

## Boundary choices matter

- **Too narrow**: you miss important feedback. If you model prey alone and treat predation as a constant death rate, you'll never see oscillation.
- **Too broad**: you add complexity without insight. Modeling the prey's food supply, weather, soil chemistry, and plate tectonics won't help you understand predator-prey cycles.

## A practical rule

Include a variable inside the boundary when its feedback to the system is essential to the behavior you're studying. Leave it outside when it changes slowly relative to your time horizon, or when its effect is secondary.
