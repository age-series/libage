# libage

Age-Series Simulation Library

A Kotlin-based set of simulator functions. Depends on apache math libraries

## Contents

This repository roughly contains:

* MNA-based Electrical Solver (`org.libage.sim.electrical.mna`)
* Falstad netlist loader for MNA Solver (`org.libage.parsers.falstad`)
* Thermal system solver (`org.libage.sim.thermal`)
* Data structures (`org.libage.data`)
  * MultiMap, Disjoint Set, and Component Graph
* Function tables (`org.libage.math`)
* Spatial functions for connection graphs (`org.libage.space`)

## Planned Features

* Generic Circuit API
* Advanced pluggable circuit solvers (eg, non-smooth dynamic systems solvers)

## License

Source code is MIT licensed unless otherwise noted.
