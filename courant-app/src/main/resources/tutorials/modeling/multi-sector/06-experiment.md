## Experiment with the MARKET model

Try these modifications and observe how the system responds.

## Increase production capacity

Double the Production_Capacity parameter and run. What happens to price and inventory? With more supply available, price should fall and inventory should rise -- but does the system overshoot?

## Add a demand shock

Add a STEP function to Customer_Orders to simulate a sudden surge in demand. How quickly does the supply side respond? The delay between the demand shock and the production response reveals how tightly coupled the sectors are.

## Change price sensitivity

Increase the Price_Sensitivity parameter. Does the system become more or less stable?

- **Higher sensitivity** -- customers react strongly to price changes, which should dampen oscillation (stronger balancing feedback)
- **Lower sensitivity** -- customers barely respond to price, weakening the price mechanism and potentially causing larger inventory swings

## Combine changes

Try a demand shock with high price sensitivity, then with low. The interaction between the shock and the feedback strength determines whether the system recovers smoothly or oscillates.
