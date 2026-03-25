## Mistakes that unit checking catches

**Forgetting the time unit on a flow.** If a flow's equation produces Widgets instead of Widgets/Day, integration gives Widgets * Day — a meaningless unit. Fix: divide by a time constant.

    Outflow = Stock                     -- wrong: units are Widgets
    Outflow = Stock / Residence_Time    -- right: Widgets / Day

**Mixing time scales.** A rate computed in weeks used in a daily simulation is off by a factor of 7. The numbers may look plausible, making this hard to catch by inspection.

    Rate = 0.1    -- is this per day, per week, or per year?

Always label constants with their time unit. Use the **Properties panel** to assign units to every parameter.

**Adding unlike units.** This often happens when combining terms from different parts of the model:

    Total = Revenue + Headcount    -- Dollars + People = error

**Using a dimensionless parameter where a rate is needed.** A "fraction per day" is not the same as a plain fraction. If Fractional_Growth_Rate is 0.05/Day, it must carry the /Day unit; otherwise the flow equation won't balance.
