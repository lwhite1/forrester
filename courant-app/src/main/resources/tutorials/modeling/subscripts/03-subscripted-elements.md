## Assign subscripts to elements

Once a subscript is defined, assign it to model elements:

1. Select a stock (e.g., `Population`)
2. In the **Properties panel**, find the **Subscripts** field
3. Add `Region`

The stock now represents three values: `Population[North]`, `Population[South]`, `Population[East]`. Each has its own initial value and its own trajectory during simulation.

## Subscripted flows

Assign the same subscript to flows that connect to subscripted stocks. A flow `Births[Region]` creates one birth rate per region, each feeding its corresponding population stock.

## Subscripted variables

Variables can also carry subscripts. A variable `Birth_Rate[Region]` can hold different rates for each region. If the equation is `Population * 0.02`, it automatically evaluates per region — `Population[North] * 0.02`, `Population[South] * 0.02`, etc.

## Broadcasting

A non-subscripted constant (e.g., `Growth_Rate = 0.02`) used in a subscripted equation is **broadcast** — the same value applies to every element. You don't need to subscript everything.
