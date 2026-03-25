## Supply meets demand

Open the **MARKET** model (**File -> Open Model** and navigate to economics/MARKET.json). This model couples two sectors: Supply and Demand.

## The Supply sector

- **Inventory** (stock) -- goods available for sale
- **Production Rate** (flow) -- how fast new goods are made
- **Production Capacity** -- the maximum production rate

## The Demand sector

- **Customer Orders** (flow) -- how much customers want to buy
- **Price Sensitivity** -- how strongly customers react to price changes
- **Backlog** (stock) -- unfilled orders

## The coupling variable: Price

**Price** connects the two sectors. When demand exceeds supply, price rises. Higher price reduces demand (balancing loop) and stimulates investment in production capacity (balancing loop with delay).

## Run and observe

Press **Ctrl+R** to run. Watch how supply and demand co-evolve. Price mediates the interaction -- it's the signal that coordinates two independent decision-making sectors without central planning.
