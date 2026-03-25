## Explore the AGECHAIN model

The tutorial has loaded the AGECHAIN model. Examine its structure:

- **Three cohort stocks** — Children, Adults, Elderly
- **Aging flows** between cohorts — Maturing, Aging
- **Births** entering Children
- **Deaths** exiting each cohort at age-specific rates

Notice the flow equations. Each aging flow divides the stock by its residence time:

- `Maturing = Children / Childhood_Duration`
- `Aging = Adults / Adulthood_Duration`

## Run the simulation

Press **Ctrl+R** to run. Watch how the age distribution evolves over time. The initial conditions set up a young population with high birth rates.

Observe:

- Children grow first as births accumulate
- After 15 years, the adult cohort begins to swell
- After 65 years, the elderly cohort starts growing
- The "wave" of a baby boom takes decades to pass through the full chain
