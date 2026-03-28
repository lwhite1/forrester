## Create a new model

Start by creating a blank model: **File → New**.

## Name your model

Before adding elements, give the model a name and description. Go to **File → Model Info** and enter:

  Name: `Coffee Cooling`

  Description: `Newton's law of cooling — coffee approaching room temperature`

Good documentation is essential. When you revisit a model weeks later, or share it with a colleague, clear names and descriptions make the difference between a model that communicates its purpose and one that requires detective work to understand.

## Place a Stock

A stock (also called a **level**) represents a quantity whose value accumulates over time through inflows and outflows. Temperature may not seem like a typical stock — we usually think of stocks as inventories or populations — but it fits: coffee temperature is a state that changes continuously as heat flows out into the room. You set its starting value; the simulation computes the rest.

Press `4` (or click the Stock button in the toolbar), then click on the canvas.
Double-click the stock to select it. In the **Properties panel** on the right, name it: `Coffee Temperature`
Set the initial value to `100` (degrees Celsius).

## Place two Variables as parameters

Press `6` to switch to the Variable tool.

Click the canvas to the right of the stock. Name it `Room Temperature`, value `18`.
Click again below. Name it `Cooling Rate`, value `0.10`.

The cooling rate means the coffee loses 10% of the temperature difference each minute.

## Place a Variable

Press `6`. Click between the stock and the constants. Name it `Discrepancy`.
When prompted for the equation, type:

  `Coffee_Temperature - Room_Temperature`

The autocomplete dropdown suggests names as you type — press `Tab` to accept.

This variable continuously recalculates: when the coffee is 100°C, the discrepancy is 82°C. As the coffee cools, it shrinks toward zero.
