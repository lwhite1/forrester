## Try it yourself

Open the POP model. This is a population model that can be extended with subscripts.

### Exercise 1: Add regional disaggregation

1. Define a subscript `Region` with elements `Urban` and `Rural`
2. Assign it to the population stock and its flows
3. Set different initial populations: Urban = 800, Rural = 200
4. Set different birth rates: Urban = 0.01, Rural = 0.03
5. Run and compare the trajectories

### Exercise 2: Observe divergence

With different birth rates, the regions diverge over time. Urban starts larger but grows slowly. Rural starts small but grows faster. At what point does Rural overtake Urban?

### Exercise 3: Add migration

Create a flow from Rural to Urban population:

  `Migration = Rural_Population * Migration_Rate`

Set `Migration_Rate = 0.02`. Now rural-to-urban migration counteracts the higher rural birth rate. Does Rural still overtake Urban?

### Exercise 4: Check the total

Add a variable: `Total = SUM(Population[Region!])`. Verify that the total population equals Urban + Rural at every step.
