## More than one dimension

Elements can carry multiple subscripts. A stock `Population[Region, Age]` creates one instance for every combination:

- `Population[North, Child]`
- `Population[North, Adult]`
- `Population[South, Child]`
- `Population[South, Adult]`
- ... and so on

With 3 regions and 3 age groups, you get 9 instances from a single stock definition.

## When to use multiple dimensions

Use multiple subscripts when behavior varies along more than one axis. A demographic model might need Region × Age to capture that birth rates differ by both location and age group.

## Keeping it manageable

Multi-dimensional subscripts multiply quickly. 5 regions × 4 age groups × 2 genders = 40 instances per stock. This is powerful but can make results harder to interpret.

**Guidelines:**
- Start with one subscript, add dimensions only when needed
- Use aggregation (`SUM`) to create summary variables for dashboards
- Name dimensions clearly — `Region`, `Age_Group`, not `R`, `A`
- Test with a small number of elements first, expand later
