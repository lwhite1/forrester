## The golden rule

Both sides of every equation must have the same dimensions.

## Valid operations

- **Addition and subtraction** — both operands must have the same units. People + People = People. People + Dollars = error.
- **Multiplication** — units multiply. People * (1/Day) = People/Day.
- **Division** — units divide. Dollars / People = Dollars/Person.

## Dimensionless quantities

Some values have no units:

- **Fractions** — inventory / target_inventory = dimensionless (e.g., 0.85)
- **Percentages** — a fraction scaled by 100
- **Ratios** — any same-unit division cancels units
- **Multipliers** — table function outputs normalized to 0-1

Dimensionless values can multiply anything without changing its unit. This is why normalizing table functions to 0-1 works: the lookup output is dimensionless, so `Max_Rate * LOOKUP(Table, Input)` has the same units as Max_Rate.

## A worked example

    Adjustment = (Target - Stock) / Adjustment_Time

- Target - Stock: Widgets - Widgets = Widgets
- Adjustment_Time: Days
- Result: Widgets / Days = Widgets/Day

This is a valid flow unit. The equation is dimensionally consistent.
