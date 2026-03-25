## Create a Flow

Press `5` to switch to the Flow tool.

1. Click the **Coffee Temperature** stock — this is the source
2. Click an empty area nearby — a cloud appears (heat dissipates)

A flow arrow connects the stock to the cloud. Double-click the diamond flow indicator and name it `Cooling`.

In the **Properties panel** on the right, set the equation to:

  `Discrepancy * Cooling_Rate`

In the Properties panel, set the **Time Unit** for this flow to `Minute`. This tells the simulation that the cooling rate is per minute — matching the simulation time step you will set in the next tab. Without this, the model will produce incorrect results.

This is the key feedback equation. When the coffee is hot, the discrepancy is large, so cooling is fast. As the coffee approaches room temperature, the discrepancy shrinks, and cooling slows. The system regulates itself.

## Check your work

Press `Ctrl+B` to validate. If everything is connected correctly, you'll see no errors. If something is missing, click the error to jump to the problem element.

Your model:
- Stock: Coffee Temperature (100)
- Flow: Cooling = Discrepancy × Cooling_Rate
- Variable: Discrepancy = Coffee_Temperature − Room_Temperature
- Parameters: Room Temperature (18), Cooling Rate (0.10)
