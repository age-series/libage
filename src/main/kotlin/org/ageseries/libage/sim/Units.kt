package org.ageseries.libage.sim

/**
 * A linear scale.
 *
 * This scale suffices for most linear natural units; a natural example is temperature.
 *
 * In general, don't construct these unless you really are making a new scale; the associated
 * constant members should suffice for most use cases.
 *
 * This package usually uses a specific "base unit" in each of the simulation domains, chosen to be
 * an easy-to-compute value (e.g., unit in the kms system). Those domains have further documentation
 * on their choice of base unit, which is usually part of the documentation in or around wherever
 * these scales are defined.
 */
class Scale(
    val factor: Double,
    val base: Double,
    /**
     * The unit suffix string accepted for use with this linear scale, to be displayed after the value.
     *
     * This should be the "physical symbol" of this unit, which is inherently international--avoid units
     * which lack these, and set this to an empty string otherwise.
     */
    val displayUnit: String = "",
) {
    /** Given a value [u] in base unit, return its value in this unit. */
    fun map(u: Double): Double = factor * u + base

    /** Given a value [u] in this unit, return its value in base units. */
    fun unmap(u: Double): Double = (u - base) / factor

    /**
     * Give a human-readable string of a value in this scale.
     */
    fun display(u: Double): String = "${u}${displayUnit}"
}