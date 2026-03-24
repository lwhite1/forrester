## What you learned

- **Delays** — orders take time to arrive, creating a gap between decisions and their effects
- **Oscillation** — delayed balancing feedback causes overshoot and undershoot, producing cycles
- **Bullwhip effect** — small demand changes are amplified through the supply chain
- **Supply line** — tracking orders in transit prevents over-ordering
- **Adjustment time** — aggressive correction under delay makes things worse, not better

## Behavior modes seen

- Damped oscillation (inventory cycles)
- Overshoot (inventory exceeding target)
- Goal-seeking with delay (inventory eventually settling)

## Key insight

People systematically underestimate the effect of delays (Sterman, 1989). The instinct to "do more" when things aren't improving is counterproductive when the feedback is delayed — your earlier actions haven't arrived yet.

## Try next

- Replace the constant Delivery_Delay with `DELAY FIXED` for a pure pipeline delay
- Add a second warehouse (retailer -> distributor) to see the bullwhip amplify
- Explore the SIR Tutorial (Help menu) if you haven't already — it covers reinforcing feedback
