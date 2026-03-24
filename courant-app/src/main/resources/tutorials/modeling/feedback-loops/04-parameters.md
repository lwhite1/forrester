## Place three Parameters

Press `4` to switch to the Variable tool. Create three constants:

1. `Contact_Rate` = `8` (contacts per person per day)
2. `Infectivity` = `0.10` (probability of transmission per contact)
3. `Recovery_Rate` = `0.20` (20% of infected people recover each day)

A recovery rate of 0.20 means the average duration of infection is 5 days (1/0.20).

The basic reproduction number R₀ = Contact_Rate × Infectivity / Recovery_Rate = 8 × 0.10 / 0.20 = **4**. Each infected person infects 4 others on average at the start of the epidemic, so the outbreak will grow rapidly.

## Validate

Press `Ctrl+B` to check for errors. Every variable should be resolved and every flow should have a valid equation.
