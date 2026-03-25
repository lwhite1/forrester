## How aging chains work

An aging chain is a series of stocks connected by flows. Each stock represents a cohort — a group defined by its stage in the progression.

For a population model with three cohorts:

- **Children** (Age 0-14) — 15-year residence time
- **Adults** (Age 15-64) — 50-year residence time
- **Elderly** (Age 65+) — no outflow to a next cohort, only death

The outflow from one cohort is the inflow to the next. The **residence time** in each stock equals the duration of that life stage. The flow rate = Stock / Residence_Time.

The chain **conserves material**. People aren't created or destroyed by aging — they move from one cohort to the next. Births enter the first cohort from outside. Deaths exit each cohort at age-specific mortality rates.

This conservation property is what makes aging chains different from independent stocks. The stages are coupled: a baby boom in the first cohort will eventually propagate through every stage.
