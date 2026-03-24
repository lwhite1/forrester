## Change the delivery delay

Set Delivery_Delay to `10` and re-run. The oscillations should be larger and slower. Longer delays mean more over-correction.

Now try `2`. The oscillations nearly vanish. Short delays allow the system to correct quickly.

## Run a parameter sweep

1. Go to **Simulate -> Parameter Sweep**
2. Select `Delivery_Delay` as the parameter
3. Set Start = `1`, End = `10`, Step = `1`
4. Click OK

The chart reveals how delay length controls oscillation. At short delays (1-2 days), inventory barely wobbles. At long delays (8-10 days), inventory swings wildly.

## Change the Adjustment Time

Reset Delivery_Delay to 5. Now sweep Adjustment_Time from 1 to 10.

Short adjustment times (aggressive correction) make oscillations worse. Longer adjustment times (patient correction) are more stable. This is counterintuitive: *reacting more cautiously produces better results* when feedback is delayed.
