## Build a coffee cooling model in 10 minutes

You're going to simulate a cup of coffee cooling down to room temperature. Along the way you'll learn the four building blocks of System Dynamics:

- **Stock** — a container that accumulates (the coffee's heat)
- **Flow** — a rate that changes a stock (cooling)
- **Variable** — a computed value (the temperature gap)
- **Parameter** — a variable with a fixed value (room temperature, cooling rate)

The model uses Newton's law of cooling: the hotter the coffee relative to the room, the faster it cools. As the coffee approaches room temperature, cooling slows down. This is **negative feedback** — the system self-corrects toward equilibrium.

By the end, you'll answer: *How long does it take to reach drinkable temperature?*
