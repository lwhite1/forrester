## Configure and run

Go to **Simulate -> Monte Carlo**. The dialog lists the model's parameters.

For each uncertain parameter:

- Click **Add Parameter**
- Select the parameter name
- Choose a **distribution type** (uniform, normal, or triangular)
- Enter the distribution parameters (min/max, mean/std dev, or min/mode/max)

Set the **number of runs**. Start with **200** for a quick look, then increase for precision.

Click **Run**. The engine samples each parameter from its distribution, runs the model, and collects results. Progress is shown in the status bar.

## Example setup for the coffee model

- `Cooling_Rate`: uniform, min `0.05`, max `0.20`
- `Room_Temperature`: normal, mean `20`, std dev `3`
- Runs: `200`
