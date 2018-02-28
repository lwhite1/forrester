package com.deathrayresearch.forrester.model;

/*
 This package holds the primary elements that make up a system dynamics model:
 - Stocks and Flows
 - Constants and Variables

 Modules are grouping constructs that allow you to build complex models from a set of sub-components in a hierarchical
 fashion.

 As you would expect, all of the attributes of a Constant are final, and the value returned is always the same.
 All the fields of a Variable are also constant, but one is a Formula, that may return different values at different
 points of time.



 */