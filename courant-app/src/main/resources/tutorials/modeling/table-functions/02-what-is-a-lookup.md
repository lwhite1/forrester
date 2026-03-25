## How lookup tables work

A **lookup table** maps input values to output values through a series of (x, y) pairs. You define the points; Courant linearly interpolates between them.

The syntax in an equation is:

    LOOKUP(TableName, InputVariable)

- **TableName** — the name of the table you've defined
- **InputVariable** — the variable whose current value selects the point on the curve

For example, if your table `Effect_of_Gap` contains the points (0, 0) and (100, 1.0), and the input is 50, the output is 0.5.

## Key properties

- **Interpolation** — between defined points, Courant draws a straight line. More points = smoother curve
- **Extrapolation** — outside the defined range, the output is clamped to the nearest endpoint. An input of 150 with a max x of 100 returns the y-value at 100
- **Independence** — the table defines the *shape*. The input variable selects *where you are* on that shape. Changing the input doesn't change the table itself
