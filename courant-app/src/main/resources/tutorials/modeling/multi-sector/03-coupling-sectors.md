## Connecting subsystems

Sectors are **coupled** through variables that cross sector boundaries. These cross-sector links create system-wide feedback loops that no single sector can explain.

## Examples of coupling

- **Production** depends on Labor (from HR sector) -- you can't produce without workers
- **Revenue** (Finance) depends on Sales (Marketing) -- income follows demand
- **Congestion** (Transportation) depends on Population (Housing) -- more people means more traffic
- **Price** connects Supply and Demand -- high demand raises price, which stimulates supply and dampens demand

## Implementation with modules

Use **modules** to implement sectors and **bindings** to implement cross-sector links:

1. Build each sector as a separate module
2. Define **input bindings** for variables the sector receives from other sectors
3. Define **output bindings** for variables the sector provides to other sectors
4. At the top level, wire the modules together by connecting outputs to inputs

This keeps each sector's internal structure encapsulated while making the inter-sector dependencies explicit and visible in the top-level diagram.
