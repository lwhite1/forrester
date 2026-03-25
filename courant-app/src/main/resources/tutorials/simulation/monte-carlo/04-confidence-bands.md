## Read the results

Monte Carlo results are shown as **confidence bands** -- shaded regions around the median trajectory.

- The **50% band** shows where half the runs fell. There is a 50% chance the true outcome lies within this region.
- The **90% band** shows the range for 90% of runs. Only 5% of runs fell above the band and 5% below.
- The **median line** shows the central tendency across all runs.

## What the bands tell you

- A **wide band** means high uncertainty -- the outcome depends heavily on parameters you don't know precisely
- A **narrow band** means the outcome is **robust** -- it doesn't change much despite parameter uncertainty
- If the band is wide early but narrows later, uncertainty resolves over time (common in systems that converge to equilibrium)
- If the band widens over time, small uncertainties amplify -- the system is chaotically sensitive
