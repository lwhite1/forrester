## The S-curve: slow start, rapid middle, saturation

An **S-shaped response** (sigmoid) combines three phases:

1. **Slow start** — at low input, the effect barely registers
2. **Rapid growth** — once a threshold is crossed, the effect accelerates
3. **Saturation** — at high input, the effect levels off

This pattern appears in technology adoption, epidemics, learning curves, and market penetration.

## Create an S-shaped table

Add a new lookup table called `Adoption_Effect`:

| Input | Output |
|-------|--------|
| 0     | 0      |
| 20    | 0.05   |
| 40    | 0.3    |
| 60    | 0.7    |
| 80    | 0.95   |
| 100   | 1.0    |

The inflection point is around 50 — below it, progress is slow; above it, adoption accelerates before leveling off.

## Why S-curves matter in system dynamics

S-curves often produce surprising behavior. Early in the curve, linear thinkers underestimate growth ("it's barely moving"). In the steep middle, they overestimate it ("it'll grow forever"). At saturation, they're surprised it stopped. The table function captures all three phases in one structure.
