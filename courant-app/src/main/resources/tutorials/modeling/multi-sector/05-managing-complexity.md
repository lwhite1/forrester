## Keeping large models understandable

Multi-sector models can grow to hundreds of variables. Without discipline, they become unreadable and untestable. These practices keep complexity manageable.

## Build incrementally

1. **Build and test each sector in isolation first** -- verify it produces reasonable behavior on its own with placeholder inputs
2. **Add sectors one at a time** -- connect a new sector, test the combined model, then add the next
3. **Test after each addition** -- don't wait until the full model is assembled to discover problems

## Use modules

Encapsulate each sector as a **module**. The top-level diagram should show only the sector boxes and their connections -- a readable map of the system architecture.

## Naming and documentation

- **Prefix variables** with sector names when they appear at the top level (e.g., Supply_Inventory, Demand_Orders)
- **Document interfaces** -- for each module, list its inputs and outputs
- **Keep sectors small** -- aim for 10-30 elements per sector. If a sector grows beyond 30, consider splitting it

## Dimensional consistency

Run **Ctrl+B** after connecting sectors. Cross-sector links are the most common source of units errors -- a flow in "widgets/week" connecting to a stock in "dollars" is easy to miss.
