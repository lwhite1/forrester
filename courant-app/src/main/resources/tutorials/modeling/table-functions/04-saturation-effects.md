## When more input stops producing more output

**Saturation** means the response flattens as the input grows large. Your `Effect_of_Gap` table already shows this: the output rises quickly from 0 to 0.5 in the first quarter, but only from 0.95 to 1.0 in the last quarter.

Real-world examples:

- **Hiring** — adding workers helps until the office is crowded, then productivity per person drops
- **Advertising** — the first campaign reaches new customers, the tenth reaches people who've already seen your ads
- **Fertilizer** — a little boosts crop yield; too much burns the roots

## Experiment with the curve shape

Try reshaping `Effect_of_Gap` to be more aggressive:

| Input | Output |
|-------|--------|
| 0     | 0      |
| 10    | 0.7    |
| 25    | 0.9    |
| 50    | 0.98   |
| 100   | 1.0    |

Re-run with **Ctrl+R**. The system corrects faster early on but barely reacts to small remaining gaps.

Now try a more gradual curve — (0, 0), (50, 0.3), (100, 1.0). Notice how the system responds sluggishly to moderate gaps. The shape of the table controls the personality of the response.
