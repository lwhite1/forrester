## Units are your model's type system

Just as a compiler catches type errors — you can't add a string to an integer — unit checking catches structural errors in equations. A population stock should hold "People", not "Dollars". A flow of water should be "Gallons/Day", not "Widgets/Month".

**Dimensional analysis** is the practice of verifying that every equation's left side and right side have the same units. It catches bugs before you run the simulation:

- Adding People to Dollars? Error.
- A stock gaining People/Day over a Day time step produces People? Correct.
- A flow measured in Gallons feeding a stock measured in Widgets? Error.

These mistakes are easy to make and hard to spot by reading equations alone. Courant can check them automatically.

Assigning units takes seconds. Skipping units costs hours of debugging when a model produces nonsensical results and you can't figure out why.
