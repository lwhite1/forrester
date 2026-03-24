## Discover how delays cause oscillation

You manage a warehouse. Customers order products, you ship them from inventory, and you place orders with a supplier to replenish stock. But the supplier takes time to deliver.

This model has two key features:

1. **Balancing feedback** — when inventory drops below target, you order more. When it rises above, you order less.

2. **Delay** — orders take time to arrive. You're making decisions based on current inventory, but the results won't arrive for several days.

The delay creates a trap: you keep ordering because inventory hasn't recovered yet, then all the orders arrive at once, overshooting your target. This produces **oscillation** — inventory swings above and below the target.

This is a simplified version of the **bullwhip effect**, one of the most studied phenomena in supply chain management.

By the end, you'll answer: *How does delivery delay affect inventory stability?*
