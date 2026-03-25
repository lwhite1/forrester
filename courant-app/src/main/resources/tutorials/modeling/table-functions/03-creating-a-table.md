## Add a lookup table to the goal-seeking model

The goal-seeking model adjusts linearly toward a target. Let's replace that linear correction with a non-linear one using a lookup table.

## Create the table

1. Go to **Model -> Add Lookup Table**
2. Name it `Effect_of_Gap`
3. Enter these (x, y) points:

| Input (Gap %) | Output (Effect) |
|---------------|-----------------|
| 0             | 0               |
| 25            | 0.5             |
| 50            | 0.8             |
| 75            | 0.95            |
| 100           | 1.0             |

This defines a **diminishing-returns curve** — small gaps produce proportionally large effects, but large gaps produce diminishing additional response.

## Wire it into the model

In the equation for the adjustment flow, replace the linear term with:

    LOOKUP(Effect_of_Gap, Gap_Percentage)

Press **Ctrl+B** to validate. The model should pass with no errors.

## Run and compare

Press **Ctrl+R**. Notice how the system approaches the target more aggressively at first, then slows as the gap shrinks. This is the signature of diminishing returns.
