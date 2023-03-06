package org.ageseries.libage.sim.thermal

import org.ageseries.libage.sim.Scale

/**
 * Linear scales for temperatures.
 *
 * All scales are defined relative to Kelvin, which is the package-wide base unit.
 */
object ThermalUnits {
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