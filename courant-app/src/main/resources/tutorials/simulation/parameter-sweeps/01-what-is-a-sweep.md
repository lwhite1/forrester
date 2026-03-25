## Automate parameter exploration

A **parameter sweep** runs the model multiple times, changing one parameter across a range of values. Instead of manually adjusting a parameter, running, recording the result, and repeating, the sweep automates the entire process.

The engine takes a parameter, a minimum value, a maximum value, and a number of steps. It runs the model once for each step, producing a set of time series -- one per parameter value.

The result is a **fan chart**: multiple time series overlaid on a single plot, showing how the system behaves across the parameter range. Fan charts make it immediately visible whether a parameter matters.

By the end of this tutorial, you'll answer: *Which parameters in the coffee cooling model have the most influence on the outcome?*
