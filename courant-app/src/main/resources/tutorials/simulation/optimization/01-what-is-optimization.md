## Finding the best answer

**Optimization** finds the parameter values that best achieve a goal. Instead of manually trying different values and eyeballing results, the optimizer searches systematically.

## A concrete example

Imagine you want the coffee to reach exactly 60 degrees at minute 10. You could guess a cooling rate, run the simulation, check the result, and try again. That works for one parameter, but becomes impractical for two or more.

The optimizer automates this process:

1. **Pick a value** for Cooling_Rate
2. **Run the simulation** with that value
3. **Check the result** -- how close is the temperature to 60 at minute 10?
4. **Adjust** the value based on what it learned
5. **Repeat** until it finds the best value

## When to use optimization

Optimization is the right tool when you have a clear goal and controllable parameters. It answers the question: *what's the best I can do?*
