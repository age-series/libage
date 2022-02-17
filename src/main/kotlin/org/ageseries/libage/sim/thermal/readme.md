I deleted the thermal simulator because it did not work. (It's in version control if you want to look at it and revive it).

What I do know is that between blocks we probably want to represent thermal motion as W/m^2 across a surface; although I'm not sure how much sense that makes.

There was some documentation linked in the code that I will share here:

https://en.wikipedia.org/wiki/Thermal_contact_conductance

`q = (T2 - T1) / ( dx_a/(k_a*A) + 1/(h_c*A) + dx_b/(k_b*A) )`

Optimally, our simulator takes mass and thermal conductivity of materials into account as well.

There's a bunch of useful material properties in [Material](../Material.kt).

My guess is that [IProcess](../IProcess.kt) would also be useful.

Good luck!
