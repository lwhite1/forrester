## Flatten the curve

Reduce the Contact_Rate from 8 to 4 and re-run. The peak should be lower and later. This is the mechanism behind social distancing — reducing contacts slows the reinforcing loop.

## Run a parameter sweep

1. Go to **Simulate → Parameter Sweep**
2. Select `Contact_Rate` as the parameter
3. Set Start = `2`, End = `12`, Step = `2`
4. Click OK

The chart shows a family of epidemic curves. Notice how:

- Lower contact rates produce smaller, later peaks
- Higher contact rates produce taller, earlier peaks
- The total number of recovered converges toward the full population regardless of rate (almost everyone eventually gets infected), but the peak height varies dramatically

## Try it

What value of Contact_Rate keeps the infectious peak below 200 (20% of the population)?
