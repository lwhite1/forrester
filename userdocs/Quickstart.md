# Quickstart: Build Your First Model in 10 Minutes

You're going to build a simulation of a coffee cup cooling down to room temperature. Along the way you'll learn the core concepts of System Dynamics — stocks, flows, constants, and feedback — by watching them play out in real time.

By the end, you'll have a working model that answers: *How long does it take for coffee to cool from 100°C to drinkable temperature?*

---

## 1. Launch Shrewd and create a new model

```bash
java -jar shrewd-app/target/shrewd-app-*.jar
```

A blank canvas opens. This is your workspace — you'll drag and drop elements here to build a visual model.

> **Tip:** If you'd rather explore a finished model first, go to **File > Open Example** and pick any of the 8 bundled models. You can run them immediately with **Ctrl+R**.

---

## 2. Place a Stock

A **stock** is a container — something that accumulates. Think of it as a bathtub, a bank account, or in our case, the heat in a coffee cup.

Press **2** (or click the Stock button in the toolbar), then click on the canvas. A rounded rectangle appears. Double-click it and type:

```
Coffee Temperature
```

Press **Enter**. A second prompt asks for the initial value. Type:

```
100
```

This means the coffee starts at 100°C. Press **Enter** to confirm.

Your canvas now has a single stock labeled "Coffee Temperature = 100".

---

## 3. Place two Constants

A **constant** is a fixed value that doesn't change during the simulation. We need two:

Press **5** to switch to the Constant tool.

Click the canvas to the right of the stock. Name it:

```
Room Temperature
```

Set the value to `18` (°C).

Click the canvas again, below Room Temperature. Name this one:

```
Cooling Rate
```

Set the value to `0.10`. This means the coffee loses 10% of the temperature difference each minute — Newton's law of cooling in one number.

---

## 4. Add a Variable

A **variable** (called an auxiliary or converter in the SD literature) computes a value from other elements. We'll use it to calculate the temperature gap between the coffee and the room.

Press **4** to switch to the Variable tool. Click between the stock and the constants. Name it:

```
Discrepancy
```

When prompted for the equation, type:

```
Coffee_Temperature - Room_Temperature
```

> **Note:** In equations, spaces in element names become underscores. The autocomplete dropdown will suggest matching names as you type — press **Tab** or click to accept a suggestion.

This variable will continuously recalculate as the coffee temperature changes. When the coffee is at 100°C, the discrepancy is 82°C. As the coffee cools toward 18°C, the discrepancy shrinks toward zero.

---

## 5. Create a Flow

A **flow** is the rate of change — it drains from or fills a stock. Cooling is an outflow that drains heat from the coffee.

Press **3** to switch to the Flow tool. Now:

1. **Click the Coffee Temperature stock** — this is the flow's source (where material comes from)
2. **Click an empty area** to the right or below — this is the flow's sink (where it goes; a "cloud" appears, meaning the heat dissipates)

A flow arrow appears connecting the stock to a cloud. Double-click the diamond-shaped flow indicator and name it:

```
Cooling
```

When prompted for the equation, type:

```
Discrepancy * Cooling_Rate
```

This is the key feedback equation. When the coffee is hot, the discrepancy is large, so cooling is fast. As the coffee approaches room temperature, the discrepancy shrinks, and cooling slows down. This is **negative feedback** — the system self-corrects toward equilibrium.

---

## 6. Check your diagram

Your model should now have:

- **Stock:** Coffee Temperature (initial value 100)
- **Flow:** Cooling (equation: `Discrepancy * Cooling_Rate`), draining from Coffee Temperature
- **Variable:** Discrepancy (equation: `Coffee_Temperature - Room_Temperature`)
- **Constants:** Room Temperature (18), Cooling Rate (0.10)

You can drag elements around to arrange them neatly. Use **1** to switch back to the Select tool, then click and drag.

> **Keyboard shortcut:** Press **Ctrl+B** to validate your model. Any errors (undefined equations, disconnected flows, etc.) will appear in a list. Click any error to jump to the problem element.

---

## 7. Configure simulation settings

Go to **Simulate > Simulation Settings** and enter:

| Field | Value |
|---|---|
| Time step | Minute |
| Duration | 60 |
| Duration unit | Minute |

This will simulate one hour of cooling, one minute at a time.

Click **OK**.

---

## 8. Run the simulation

Press **Ctrl+R** (or **Simulate > Run Simulation**).

The dashboard panel opens at the bottom with two tabs:

- **Table** — a sortable grid showing Coffee Temperature at each time step
- **Chart** — a line chart plotting the temperature curve over time

You should see an exponential decay curve: the coffee drops quickly at first (losing ~8°C in the first minute), then slows down as it approaches room temperature. After 60 minutes, the coffee is around 20°C — barely above room temperature.

> **Try it:** Right-click the chart and select **Export CSV** to save the data for further analysis.

---

## 9. Experiment with parameters

This is where modeling gets interesting. What if you want to find the optimal cooling rate to reach 60°C (drinkable temperature) in exactly 10 minutes?

### Change a constant

Click the **Cooling Rate** constant on the canvas. In the Properties panel on the right, change its value from `0.10` to `0.05`. Press **Ctrl+R** again. The curve is flatter — the coffee cools more slowly.

### Run a parameter sweep

Instead of changing values one at a time, sweep the entire range:

1. Go to **Simulate > Parameter Sweep**
2. Select **Cooling Rate** as the parameter
3. Set Start = `0.02`, End = `0.20`, Step = `0.02`
4. Click **OK**

The dashboard shows a family of curves — one for each cooling rate. You can see exactly which rate produces the desired behavior. Toggle individual series on and off using the checkboxes on the right.

---

## 10. Save your model

Press **Ctrl+S**. Choose a location and filename. The model is saved as JSON — a human-readable text format. You can reopen it later, share it, or even edit the JSON directly.

---

## What you just learned

| Concept | What it does | In this model |
|---|---|---|
| **Stock** | Accumulates a quantity over time | Coffee Temperature |
| **Flow** | Changes a stock's value each time step | Cooling |
| **Variable** | Computes a derived value | Discrepancy |
| **Constant** | Holds a fixed parameter | Room Temperature, Cooling Rate |
| **Negative feedback** | The system self-corrects toward a goal | Cooling slows as temp approaches room temp |

These five building blocks can model surprisingly complex systems — from disease epidemics to supply chains to climate dynamics.

---

## Next steps

- **Open the example models** — File > Open Example has 8 models covering population growth, epidemiology, ecology, and supply chains
- **Try a positive feedback loop** — Build an exponential growth model (stock = Population, inflow = Births = Population * Birth_Rate). Compare it to the negative feedback you just built
- **Explore causal loop diagrams** — Press **8** to place CLD variables and **9** to draw causal links. Sketch your system's causal structure before formalizing it into stocks and flows
- **Read the [Expression Language Reference](Expression_Language.md)** — covers all built-in functions (SMOOTH, DELAY3, STEP, RAMP, IF, LOOKUP, etc.)
- **Import an existing model** — File > Open supports Vensim `.mdl` files and XMILE format (Stella/iThink)
