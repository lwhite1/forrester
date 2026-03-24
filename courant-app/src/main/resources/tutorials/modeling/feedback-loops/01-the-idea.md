## Model an epidemic with reinforcing and balancing feedback

The SIR model divides a population into three groups:

- **Susceptible** — people who can catch the disease
- **Infectious** — people who have it and can spread it
- **Recovered** — people who have recovered and are immune

Two processes drive the epidemic:

1. **Infection** — a **reinforcing loop**. More infectious people infect more susceptible people, which creates even more infectious people. This drives exponential growth early on.

2. **Recovery** — a **balancing loop**. Infectious people recover over time, depleting the infectious pool. As the susceptible population shrinks, the infection rate slows.

The interplay between these two loops produces **S-shaped growth** — the epidemic curve rises exponentially, peaks, then declines as susceptible people are depleted.

By the end, you'll answer: *How does the infection peak depend on the contact rate?*
