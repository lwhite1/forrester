## Defining "best"

The **objective function** tells the optimizer what "best" means. Without it, the optimizer has no direction -- it doesn't know whether to make values larger, smaller, or closer to a target.

## Common objectives

- **Minimize** the difference between a stock and a target at a specific time
- **Minimize** total cost over the simulation horizon
- **Maximize** throughput or efficiency
- **Minimize** the peak value of an undesirable outcome

## Setting the objective in Courant

Define the objective as a variable in your model (or use an existing one), then select it as the optimization target in **Simulate --> Optimization**. Choose whether to **minimize** or **maximize**.

For the coffee model, a useful objective might be:

    Error = ABS(Coffee_Temperature - 60)

evaluated at time 10. The optimizer minimizes this error by adjusting the decision variables.

## One objective at a time

Each optimization run uses a single objective. If you have multiple goals, combine them into a weighted sum or run separate optimizations and compare the trade-offs.
