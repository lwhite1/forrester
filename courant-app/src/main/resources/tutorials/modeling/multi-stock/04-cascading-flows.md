## Stocks connected in series

Not all multi-stock systems have mutual feedback. In a **cascading flow** structure, material moves through a chain of stocks in one direction. The outflow from one stock is the inflow to the next.

The classic example is the **SIR model** of disease:

- **Susceptible** -> Infectious -> Recovered
- People flow from S to I (infection) and from I to R (recovery)
- Total population is conserved: S + I + R = constant

This is a **chain** structure. Material is never created or destroyed -- it moves through the pipeline.

## Where chains appear

- **Manufacturing**: raw materials -> work in progress -> finished goods
- **Disease progression**: susceptible -> exposed -> infectious -> recovered
- **Project management**: backlog -> in progress -> completed
- **Customer lifecycle**: prospects -> trials -> subscribers -> churned

## Chain vs. coupled

In a chain, flow is one-directional. In a coupled system, stocks influence each other bidirectionally. Many real models combine both: the SIR model becomes coupled when recovered individuals lose immunity and flow back to susceptible.
