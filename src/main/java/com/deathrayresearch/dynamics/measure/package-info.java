/**
 *
 *
 * The handling of measures is based on the java standard (JSR 363). The key concepts are:
 *
 * Dimensions:
 *
 * Some important dimensions are: Length, Mass, Time, Temperature, and Amount of Substance.
 * Two measures are compatible when they have the same dimension. In other words, you can for example,
 * compare two quantities in the same dimension regardless of their measure: (3 mi > 3 km), or perform mathematical
 * operations on them: (3 mi + 3 km), or convert one to another (miles to kilometers, for example).
 *
 * Units:
 * Units are a standard way to discuss and compare measurements in some dimension. For example, in 3 km,
 * kilometers is the Unit. Units are based on a standard or base unit. For kilometers the base unit
 * is meters as a kilometer is defined as 1000 meters. Other base measure include grams, and seconds.
 *
 * Derived measure are based on other measure, for example, Speed is defined as a length divided by a quantity of time.
 *
 * Quantities:
 * Quantities are measurable attributes of a thing that have a unit.
 *
 */
package com.deathrayresearch.dynamics.measure;
