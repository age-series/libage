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

    /**
     * Linear scales for temperatures.
     *
     * All scales are defined relative to Kelvin, which is the package-wide base unit.
     */
    object Thermal {
        /**
         * Kelvin; absolute zero, with steps of a hundredth of a grade.
         *
         * Useless directly, but maybe useful as an argument where a Scale is expected.
         */
        val KELVIN = Scale(1.0, 0.0, "K")
        /**
         * Absolute Grade; absolute zero, with steps equal to the grade.
         */
        val ABSOLUTE_GRADE = Scale(0.01, 0.0, "AG")
        /**
         * Rankine; absolute zero, with steps one-hundred-eightieth of a grade.
         */
        val RANKINE = Scale(9.0 / 5.0, 0.0, "°R")
        /**
         * Grade; zero is pure water freezing, one is pure water boiling, all at standard pressure.
         */
        val GRADE = Scale(0.01, -2.7315, "G")
        /**
         * Centigrade; zero as Grade, but with steps one-hundredth of a grade.
         */
        val CENTIGRADE = Scale(1.0, -273.15, "°C")
        /**
         * Celsius, an alias for Centigrade.
         */
        val CELSIUS = CENTIGRADE
        /**
         * Milligrade; zero as Grade, but with steps one-thousandth of a grade.
         */
        val MILLIGRADE = Scale(10.0, -2731.5, "mG")
        /**
         * Fahrenheit; 32 is zero grade, 212 is one grade.
         */
        val FAHRENHEIT = Scale(9.0 / 5.0, -491.67, "°F")
    }
}