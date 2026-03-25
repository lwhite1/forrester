## Launching an optimization

Open **Simulate --> Optimization**. You'll see three sections:

- **Decision variables** -- select parameters and set bounds for each
- **Objective** -- choose the target variable and whether to minimize or maximize
- **Constraints** -- add any behavioral limits (optional)

Click **Run** to start the optimizer.

## What happens during a run

The optimizer runs many simulations, adjusting decision variables between each run to improve the objective. The progress panel shows:

- **Current best value** of the objective function
- **Number of runs** completed so far
- **Current parameter values** being tested

The search may take dozens to hundreds of runs depending on the number of decision variables and the complexity of the model.

## Stopping early

If the objective value has plateaued and you're satisfied with the result, you can stop the optimizer early. The best solution found so far is preserved.
