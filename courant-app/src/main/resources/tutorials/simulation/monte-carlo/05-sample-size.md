## How many runs are enough?

More runs produce smoother bands and more precise percentile estimates.

- **200 runs** -- gives a rough picture; good for initial exploration
- **500-1000 runs** -- gives stable confidence intervals; suitable for most analyses
- **Beyond 1000 runs** -- returns diminish; the bands won't change noticeably

## Signs you need more runs

If the confidence bands still look **jagged or unstable** at 500 runs, one of two things is happening:

- You need more runs to smooth out sampling noise
- The model has **chaotic sensitivity** -- small parameter changes cause disproportionately large outcome changes

To distinguish: double the run count. If the bands smooth out, you just needed more samples. If they remain jagged, investigate which parameters cause the instability using single-parameter sweeps.

## Balancing speed and precision

Each run takes the same time as a single simulation. Plan accordingly for complex models -- 1000 runs of a 30-second model takes 8+ hours. Start small and scale up.
