## Create the Shipments flow (outflow from Inventory)

Press `3` for the Flow tool.

1. Click **Inventory** (source)
2. Click an empty area to the left (cloud — products go to customers)

Name it `Shipments`. Set the equation to:

  `Customer_Demand`

We assume demand is met immediately from inventory.

## Create the Order Placement flow (inflow to Supply Line)

1. Click an empty area to the right of Supply Line (cloud — supplier)
2. Click **Supply Line** (sink)

Name it `Order Placement`. Set the equation to:

  `MAX(0, Customer_Demand + (Target_Inventory - Inventory) / Adjustment_Time - Supply_Line / Delivery_Delay)`

This order rule has three parts: replace what was sold, correct the inventory gap, and account for what's already on order.

## Create the Delivery flow (from Supply Line to Inventory)

1. Click **Supply Line** (source)
2. Click **Inventory** (sink)

Name it `Delivery`. Set the equation to:

  `Supply_Line / Delivery_Delay`

Orders in the supply line are delivered after the delay period.
