## Change predator efficiency

Increase the predator birth efficiency parameter (the fraction of prey caught that converts to new predators). Re-run with `Ctrl+R`.

Higher efficiency means predators reproduce faster when prey is abundant. The oscillations should grow in **amplitude** -- bigger booms and deeper busts. The system becomes more volatile.

Now decrease the efficiency. Oscillations should shrink. At very low efficiency, predators can barely sustain themselves.

## Add a carrying capacity

In the real world, prey don't grow without limit even without predators. Add a **carrying capacity** to the prey birth rate:

  `Prey_Birth_Rate * Prey * (1 - Prey / Carrying_Capacity)`

Create a new variable `Carrying_Capacity` = `1000`. Re-run.

The system should **stabilize** -- oscillations dampen and both populations settle to an equilibrium. The carrying capacity adds a second balancing loop that limits prey growth independently of predation.

## Explore initial conditions

Try doubling the initial prey population. Then try doubling the initial predator population. Which produces larger oscillations? The ratio of prey to predators at time zero sets the initial displacement from equilibrium.
