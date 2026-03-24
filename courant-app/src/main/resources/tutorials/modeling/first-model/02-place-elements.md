## Name your model

Before adding elements, give the model a name and description. Go to **File → Model Properties** and enter:

  Name:        `Coffee Cooling`
  Description: `Newton's law of cooling — coffee approaching room temperature`

This helps identify your model later in file listings and reports.

## Place a Stock

Press `2` (or click the Stock button in the toolbar), then click on the canvas.
Double-click the stock to select it. In the **Properties panel** on the right, name it: `Coffee Temperature`
Set the initial value to `100` (degrees Celsius).

## Place two Variables as parameters

Press `4` to switch to the Variable tool.

Click the canvas to the right of the stock. Name it `Room Temperature`, value `18`.
Click again below. Name it `Cooling Rate`, value `0.10`.

The cooling rate means the coffee loses 10% of the temperature difference each minute.

## Place a Variable

Press `4`. Click between the stock and the constants. Name it `Discrepancy`.
When prompted for the equation, type:

  `Coffee_Temperature - Room_Temperature`

The autocomplete dropdown suggests names as you type — press `Tab` to accept.

This variable continuously recalculates: when the coffee is 100°C, the discrepancy is 82°C. As the coffee cools, it shrinks toward zero.
