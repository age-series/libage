package org.ageseries.libage.sim.thermal

import org.ageseries.libage.sim.constant.Material
import org.ageseries.libage.sim.measurement.Scale
import org.ageseries.libage.sim.measurement.ThermalUnits
import kotlin.math.pow

/**
 * A temperature.
 *
 * The inner unit type is always Kelvin; conversions are available as functions and properties.
 */
@JvmInline
value class Temperature(val kelvin: Double) {
    /**
     * Return this temperature on the given [Scale].
     */
    fun to(scale: Scale): Double = scale.map(kelvin)

    val absoluteGrade get() = to(ThermalUnits.ABSOLUTE_GRADE)
    val rankine get() = to(ThermalUnits.RANKINE)
    val grade get() = to(ThermalUnits.GRADE)
    val centigrade get() = to(ThermalUnits.CENTIGRADE)
    val celsius get() = to(ThermalUnits.CELSIUS)
    val milligrade get() = to(ThermalUnits.MILLIGRADE)
    val fahrenheit get() = to(ThermalUnits.FAHRENHEIT)

    operator fun plus(rhs: Temperature) = Temperature(kelvin + rhs.kelvin)
    operator fun minus(rhs: Temperature) = Temperature(kelvin - rhs.kelvin)
    // Would it make sense to multiply 2 temps?
    operator fun times(rhs: Double) = Temperature(kelvin * rhs)
    operator fun div(rhs: Double) = Temperature(kelvin / rhs)
    operator fun compareTo(rhs: Temperature) = kelvin.compareTo(rhs.kelvin)
    override fun toString() = ThermalUnits.KELVIN.display(kelvin)

    companion object {
        /**
         * Return a Temperature given a measurement on a different [Scale].
         */
        fun from(temp: Double, scale: Scale) = Temperature(scale.unmap(temp))
    }
}

/**
 * "Standard" temperature, in Kelvin.
 *
 * This is the "ST" of STP, as reported in laboratory conditions.
 *
 * This is used as the default temperature for thermal masses.
 */
val STANDARD_TEMPERATURE: Temperature = Temperature(273.15)

/**
 * Represents a mass with thermal energy.
 * */
class ThermalMass(
    /** The material of this mass, used for its thermal properties. */
    val material: Material,
    /** Thermal energy, in J. Leave null to set [STANDARD_TEMPERATURE]. */
    energy: Double? = null,
    /** Mass, in kg. */
    val mass: Double = 1.0) {

    var energy: Double = energy ?: (STANDARD_TEMPERATURE.kelvin * mass * material.specificHeat)

    /**
     * Temperature of this mass, in K.
     *
     * Setting this changes the [energy].
     */
    var temperature: Temperature
        get() = Temperature(energy / mass / material.specificHeat)
        set(value) {
            energy = value.kelvin * mass * material.specificHeat
        }

    override fun toString() = "<Thermal Mass $material ${mass}kg ${energy}J $temperature>"
}

data class ThermalConnectionParameters(
    /** Conductance of the contact point. There is almost always a little loss due to mechanical effects, unless the materials are welded. This is W/K, with no distance effect. */
    val conductance: Double = 1.0,
    /** How long the connection is between the two masses--which affects the transfer rate. Measured in m. */
    val distance: Double = 1.0,
    /** How far along the line segment from [ThermalConnection.a] to [ThermalConnection.b] the contact point is. Affects how much [ThermalConnection.a]'s conductance dominates over [ThermalConnection.b]'s. Keep this between 0.0 and 1.0 inclusive. */
    val contactPoint: Double = 0.5,
) {
    companion object {
        val DEFAULT = ThermalConnectionParameters()
    }
}

class ThermalConnection(
    val a: ThermalMass,
    val b: ThermalMass,
    /** Thermal parameters of this connection. */
    params: ThermalConnectionParameters = ThermalConnectionParameters.DEFAULT,
) {
    val contactPoint = params.contactPoint
    val distance = params.distance
    val conductance = params.conductance

    override fun toString() = "<Thermal Conn $a $b ${distance}m($contactPoint) ${conductance}W/K>"

    /**
     * Returns the change in energy, added to [a] and subtracted from [b], which would equilibrate the connected thermal masses over the given period of time [dt] (in s).
     *
     * For this to be stable, [dt] must be held relatively small, since this is a linear approximation to an exponential curve--otherwise, overshoot may be observed.
     */
    fun transfer(dt: Double): Double {
        // Kelvin
        val deltaT = b.temperature.kelvin - a.temperature.kelvin
        // W/K
        val distCondA = a.material.thermalConductivity * contactPoint * distance
        val distCondB = b.material.thermalConductivity * (1.0 - contactPoint) * distance
        val overallCond = (distCondA * distCondB * conductance).pow(1.0/3.0)
        // W
        val power = deltaT * overallCond
        // J
        return power * dt
    }
}