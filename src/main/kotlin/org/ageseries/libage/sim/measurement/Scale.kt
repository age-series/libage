package org.ageseries.libage.sim.measurement

class Scale(
    val factor: Double,
    val base: Double,
    /** The unit suffix string accepted for use with this temperature scale, to be displayed after the value. */
    val displayUnit: String = "",
) {
    /** Given a value [T] in standard units, return its value in this unit. */
    fun map(t: Double): Double = factor * t + base

    /** Given a temperature [t] in this unit, return its value in standard units. */
    fun unmap(t: Double): Double = (t - base) / factor

    /**
     * Give a human-readable string of a value in this scale.
     */
    fun display(t: Double): String = "${t}${displayUnit}"
}