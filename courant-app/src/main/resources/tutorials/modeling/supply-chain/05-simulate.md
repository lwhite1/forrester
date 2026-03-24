## Configure simulation settings

Go to **Simulate -> Simulation Settings** and enter:

  Time step:      `Day`
  Duration:       `100`
  Duration unit:  `Day`

## Introduce a demand shock

To see oscillation, we need to disturb the equilibrium. Change Customer_Demand from a constant to a step function:

In the Properties panel, change Customer_Demand's equation to:

  `20 + STEP(5, 10)`

This means demand is 20 for the first 10 days, then jumps to 25 permanently. The STEP function adds 5 units starting at day 10.

## Run

Press `Ctrl+R`. Watch the Inventory curve in the Chart tab.

## What you should see

After the demand shock at day 10:

- Inventory drops as demand exceeds deliveries
- The ordering rule increases orders to compensate
- But deliveries are delayed, so inventory continues to fall
- When the delayed orders finally arrive, inventory overshoots the target
- The cycle repeats with decreasing amplitude

This is **damped oscillation** — the hallmark of delayed balancing feedback.
