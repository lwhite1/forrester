## Tracking attributes with co-flows

A **co-flow** tracks an attribute of the population as it moves through the chain. The attribute could be education level, health status, wealth, or skill.

A co-flow parallels the main aging chain. For each cohort stock, there's a corresponding co-flow stock:

- **Children** -> **Total_Education_Children**
- **Adults** -> **Total_Education_Adults**
- **Elderly** -> **Total_Education_Elderly**

The co-flow stock tracks the *total* attribute in that cohort, not the average. To get the average attribute per person:

`Average_Education = Total_Education / Population`

## Why co-flows matter

Co-flows capture how attributes accumulate and transfer between cohorts. Children gain education during childhood. When they mature into adults, they carry their education with them. The co-flow's aging rate mirrors the main chain's aging rate, preserving the attribute as people move between stages.

This is essential for questions like: *If we invest in childhood education today, when does the workforce feel the effect?* The answer depends on the aging chain's delay structure.
