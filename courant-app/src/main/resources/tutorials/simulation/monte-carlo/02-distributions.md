## Describe what you know (and don't know)

Each uncertain parameter gets a **probability distribution** that encodes your knowledge about its plausible values.

- **Uniform** -- equally likely anywhere in a range. Use when you only know the bounds (e.g., cooling rate is somewhere between 0.05 and 0.20).
- **Normal** -- bell curve centered on a mean with a standard deviation. Use when you have a best estimate and a sense of how much it could vary (e.g., room temperature is about 20 with a standard deviation of 3).
- **Triangular** -- defined by a minimum, most likely value, and maximum. Use when you have an expert estimate with asymmetric bounds (e.g., cooling rate is most likely 0.10, but could be as low as 0.05 or as high as 0.25).

Choose the distribution that best matches what you actually know. When in doubt, uniform is the most conservative -- it assumes the least.
