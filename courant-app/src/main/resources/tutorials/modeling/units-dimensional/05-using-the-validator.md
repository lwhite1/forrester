## Check your model's units

Press **Ctrl+B** to run the validator. Courant checks unit consistency across all equations in the model.

## Reading the results

Errors appear in the **Problems** panel at the bottom of the screen. Each error shows:

- The element with the inconsistency
- The expected unit (from the left side of the equation)
- The actual unit (from the right side)

Click an error to jump to the offending element on the canvas. The element is highlighted so you can find it quickly.

## Fixing errors

1. Select the element on the canvas
2. Open the **Properties panel**
3. Check the **Units** field — is it correct?
4. If the unit is right, the equation is wrong. Check for missing division by a time constant, mixed time scales, or added unlike units

## Tips

- **Assign units early.** It's easier to fix one equation at a time than to assign all units at the end and face a wall of errors
- **Validate often.** Press **Ctrl+B** after adding each new equation. Catching errors one at a time is far easier than debugging ten at once
- **Don't leave units blank.** A missing unit isn't "dimensionless" — it's unknown, and the validator can't help you
