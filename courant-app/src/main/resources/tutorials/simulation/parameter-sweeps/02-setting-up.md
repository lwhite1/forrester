## Configure a single-parameter sweep

Go to **Simulate -> Parameter Sweep**. The sweep dialog opens with the model's parameters listed.

- Select the parameter to sweep -- choose `Cooling_Rate`
- Set the **minimum** value: `0.01`
- Set the **maximum** value: `0.30`
- Set the **number of steps**: `10`

Click **Run**. The engine runs the model 10 times, once for each value of `Cooling_Rate` evenly spaced between 0.01 and 0.30.

## What happens behind the scenes

Each run uses a different value of `Cooling_Rate` while holding all other parameters at their default values. The results are collected and displayed together as a fan chart in the Chart tab.
