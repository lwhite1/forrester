## Open the predator-prey model

The tutorial has loaded a classic **Lotka-Volterra** model. Examine its structure:

- **Prey** stock with two flows:
  - *Prey Births* (inflow) -- proportional to prey population
  - *Predation* (outflow) -- proportional to both prey and predator populations

- **Predator** stock with two flows:
  - *Predator Births* (inflow) -- depends on prey caught (predation feeds reproduction)
  - *Predator Deaths* (outflow) -- proportional to predator population

## Run the simulation

Press `Ctrl+R` to simulate. Watch both stocks on the chart.

You should see oscillating populations: prey rises, predators follow after a lag, prey crashes as predation peaks, predators decline from starvation, and the cycle repeats. The populations chase each other in a loop that never settles.

This is the **Lotka-Volterra dynamic** -- one of the earliest formal models of ecological interaction, published independently by Alfred Lotka (1925) and Vito Volterra (1926).
