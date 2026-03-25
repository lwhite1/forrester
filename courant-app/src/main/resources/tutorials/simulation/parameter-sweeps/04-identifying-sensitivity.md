## Which parameters matter most?

**Sensitivity analysis** asks a practical question: where should you invest effort in getting accurate data?

The approach is straightforward:

- Sweep each parameter individually across a plausible range
- Compare the resulting fan widths
- Parameters that produce wide fans need careful estimation -- errors in these values will significantly affect your conclusions
- Parameters that produce narrow fans can tolerate rough estimates

## Practical guidance

Not all sensitivity is equal. A parameter the model is sensitive to only matters if you're uncertain about its real-world value. If you know the cooling rate precisely, its sensitivity is irrelevant. Focus on parameters that are **both** sensitive **and** uncertain.

This combination -- high sensitivity plus high uncertainty -- identifies where additional data collection, measurement, or expert judgment will most improve your model's usefulness.
