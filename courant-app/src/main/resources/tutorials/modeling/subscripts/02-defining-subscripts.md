## Create a subscript range

A subscript is a named dimension with a list of elements. To define one:

1. Go to **Model → Subscripts**
2. Click **Add Subscript**
3. Name it `Region` with elements: `North`, `South`, `East`

This creates a dimension. By itself it does nothing — you must assign it to stocks, flows, or variables.

## Common subscript patterns

- **Region**: North, South, East, West
- **Age Group**: Child, Adult, Elderly
- **Product**: Widget, Gadget, Doohickey
- **Gender**: Male, Female

Subscripts represent any dimension where the same structure applies to each element but values differ.

## Rules

- Element names must be unique within a subscript
- A model can have multiple subscripts (Region, Age, Gender)
- Subscript names follow the same rules as variable names — no spaces, use underscores
