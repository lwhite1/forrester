## Parameters don't act alone

Single-parameter sweeps hold everything else fixed. This is useful but incomplete -- in real systems, parameters often **interact**. The effect of one parameter depends on the value of another.

For example, in the coffee model: does the cooling rate matter equally whether the room is cold or warm? A single sweep of `Cooling_Rate` can't answer that because it tests only one room temperature.

**Multi-parameter sweeps** vary two or more parameters simultaneously, revealing **interaction effects** that single sweeps miss. If parameters interact, analyzing them one at a time can be misleading.
