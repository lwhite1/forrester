## One model, many dimensions

So far, every stock and flow has been a single number. But real systems often have the same structure repeated across categories: population by age group, inventory by region, production by product line.

You *could* create separate stocks for each category — `Population_North`, `Population_South` — but this quickly becomes unmanageable. If you add a third region, you must duplicate every stock, flow, and equation by hand.

**Subscripts** solve this. A single subscripted stock `Population[Region]` automatically expands into one instance per region. Equations are written once and applied to all instances. Add a region and everything updates.

This is **disaggregation** — breaking an aggregate variable into its component parts without duplicating model structure.

By the end, you'll answer: *How do subscripts let you model heterogeneity without duplicating structure?*
