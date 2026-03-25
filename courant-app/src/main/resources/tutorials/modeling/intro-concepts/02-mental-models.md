## Everyone already has a model

Every time you make a decision, you rely on a **mental model** — an implicit set of assumptions about how the world works. "If I lower the price, more people will buy." "If we hire more staff, the backlog will shrink." These are models, even if they're never written down.

The problem is that mental models are:

- **Incomplete** — they leave out variables and relationships we forget to consider
- **Inconsistent** — two people on the same team often hold contradictory assumptions
- **Untestable** — you can't simulate a mental model to see where it leads over time
- **Invisible** — because they live in our heads, they can't be inspected or challenged

## From informal to formal

System Dynamics takes mental models and makes them **explicit**. You draw the feedback structure, write equations for each relationship, and let the computer trace the consequences over time.

| Mental Model | Formal SD Model |
|---|---|
| "Hiring more staff will fix the backlog" | Staff increases completion rate, but training delays reduce productivity for 3 months, temporarily *worsening* the backlog |
| "The epidemic will fade on its own" | Susceptible population depletes, slowing transmission — but only after a peak determined by the contact rate |

The formal model doesn't replace your thinking — it **disciplines** it. When the simulation produces behavior you didn't expect, that gap between expectation and result is where learning happens.
