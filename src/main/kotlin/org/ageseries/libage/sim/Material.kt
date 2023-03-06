package org.ageseries.libage.sim

/**
 * Material
 *
 * Contains data about materials that we need for various calculations such as electrical and thermal simulations
 */
class Material(
    /** Intrinsic linear electrical resistance, in Ohm/m. */
    val electricalResisitivity: Double,
    /** Intrinsic linear thermal conductivity (on one axis), in W/mK. */
    val thermalConductivity: Double,
    /** Intrinsic specific heat capacity, in J/Kkg. */
    val specificHeat: Double,
    /** Density at STP, in g/cm^3 (or kd/dm^3). */
    val density: Double,
) {
    override fun toString() =
        "<Mat ${electricalResisitivity}Ohm/m ${thermalConductivity}W/mK ${specificHeat}J/Kkg ${density}g/cm^3>"

    companion object {
        // Good sources for this information:
        // Electrical resistivity: https://en.wikipedia.org/wiki/Electrical_resistivity_and_conductivity
        // Thermal conductivity: https://en.wikipedia.org/wiki/List_of_thermal_conductivities
        // Specific heat: https://en.wikipedia.org/wiki/Table_of_specific_heat_capacities
        // 		(use STANDARD_TEMPERATURE; note that the table has J/Kg, not J/Kkg!)
        // 		alternatively: https://www.engineeringtoolbox.com/specific-heat-solids-d_154.html
        //		(also uses kJ/Kkg = J/Kg!)
        // Density: https://en.wikipedia.org/wiki/Density
        val COPPER = Material(1.68e-8, 401.0, 385.0, 8.94)
        val LATEX_RUBBER = Material(1e13, 0.134, 1130.0, 0.92)
        val IRON = Material(9.7e-8, 83.5, 412.0, 7.87)
        val GLASS = Material(1e13, 1.11, 840.0, 2.5)
        val ALUMINUM = Material(2.65e-8, 100.0, 897.0, 2.7)
    }
}