## Create the Infection flow

Press `3` to switch to the Flow tool.

1. Click the **Susceptible** stock (source)
2. Click the **Infectious** stock (sink)

Double-click the flow indicator and name it `Infection`.
Set the equation to:

`Contact_Rate * Infectivity * Infectious * Susceptible / (Susceptible + Infectious + Recovered)`

This is the standard mass-action infection rate. The fraction at the end is the probability a contact is with a susceptible person.

## Create the Recovery flow

Press `3` again.

1. Click the **Infectious** stock (source)
2. Click the **Recovered** stock (sink)

Name it `Recovery`. Set the equation to:

`Infectious * Recovery_Rate`

People recover at a constant fractional rate.
