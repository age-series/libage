package org.ageseries.libage.sim.thermal

import org.ageseries.libage.data.mutableMultiMapOf
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * A temperature scale.
 *
 * In general, don't construct these unless you really are making a new temperature scale; the associated
 * constant members should suffice for most use cases.
 *
 * All scales are defined relative to Kelvin, which is the package-wide base unit.
 */
class Scale(
    val factor: Double,
    val base: Double,
    /** The unit suffix string accepted for use with this temperature scale, to be displayed after the value. */
    val displayUnit: String = "",
) {
    /** Given a temperature [t] in Kelvin, return its value in this unit. */
    fun map(t: Double): Double = factor * t + base
    /** Given a temperature [t] in this unit, return its value in Kelvin. */
    fun unmap(t: Double): Double = (t - base) / factor

    /**
     * Give a human-readable string of a temperature in this scale.
     */
    fun display(t: Double): String = "${t}${displayUnit}"

    companion object {
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
        val RANKINE = Scale(9.0/5.0, 0.0, "°R")
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
        val FAHRENHEIT = Scale(9.0/5.0, -491.67, "°F")
    }
}
/**
 * A temperature.
 *
 * The inner type is always Kelvin; conversions are available as properties.
 */
@JvmInline
value class Temperature(val kelvin: Double) {
    fun in_(scale: Scale): Double = scale.map(kelvin)

    companion object {
        fun from(temp: Double, scale: Scale) = Temperature(scale.unmap(temp))
    }
}
/**
 * "Standard" temperature, in Kelvin.
 *
 * This is used as the default temperature for thermal masses.
 */
val STANDARD_TEMPERATURE: Temperature = Temperature(273.15)

/**
 * A description of a material's instrinsic thermal properties.
 *
 * You shouldn't need to make one of these unless you're really describing a new material; otherwise,
 * the members here cover sane defaults.
 */
class Material(
    /** Specific heat capacity, in J/Kkg. */
    val specificHeat: Double,
    /** Intrinsic conductance, in W/mK. */
    val conductance: Double,
) {
    companion object {
        // NB: If you're populating these from the Wikipedia article,
        // note that they record J/gK, not J/kgK, so make sure to
        // multiply by 1e3
        val IRON = Material(412.0, 83.5)
    }
}

class ThermalMass(
    /** The material of this mass, used for its thermal properties. */
    val material: Material,
    /** Thermal energy, in J. Leave null to set [STANDARD_TEMPERATURE]. */
    energy: Double?,
    /** Mass, in kg. */
    val mass: Double = 1.0,
) {
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
    /** One of the connected masses. */
    val a: ThermalMass,
    /** The other connected mass. */
    val b: ThermalMass,
    /** Thermal parameters of this connection. */
    params: ThermalConnectionParameters = ThermalConnectionParameters.DEFAULT,
) {
    val contactPoint = params.contactPoint
    val distance = params.distance
    val conductance = params.conductance
    /**
     * Returns the change in energy, added to [a] and subtracted from [b], which would equilibriate the connected thermal masses over the given period of time [dt] (in s).
     *
     * For this to be stable, [dt] must be held relatively small, since this is a linear approximation to an exponential curve--otherwise, overshoot may be observed.
     */
    fun transfer(dt: Double): Double {
        // Kelvin
        val delta_T = b.temperature.kelvin - a.temperature.kelvin
        // W/K
        val dist_cond_a = a.material.conductance * contactPoint * distance
        val dist_cond_b = b.material.conductance * (1.0 - contactPoint) * distance
        val overall_cond = (dist_cond_a * dist_cond_b * conductance).pow(1.0/3.0)
        // W
        val power = delta_T * overall_cond
        // J
        return power * dt
    }
}

interface ThermalBody<Locator> {
    val mass: ThermalMass
    val locator: Locator
    val surfaceArea: Double
}

interface Environment<Locator> {
    fun temperature(locator: Locator): Temperature
    fun conductance(locator: Locator): Double
}

class Simulator<Locator>(val environment: Environment<Locator>) {
    class Connection<Locator>(
        val a: ThermalBody<Locator>,
        val b: ThermalBody<Locator>,
        params: ThermalConnectionParameters,
    ) {
        val connection = ThermalConnection(a.mass, b.mass, params)
    }

    val connections = mutableSetOf<Connection<Locator>>()
    val bodies = mutableSetOf<ThermalBody<Locator>>()
    val connectionMap = mutableMultiMapOf<ThermalBody<Locator>, Connection<Locator>>()

    fun add(vararg bodies_added: ThermalBody<Locator>) {
        bodies.addAll(bodies_added)
    }

    fun add(vararg connections_added: Connection<Locator>) {
        connections.addAll(connections_added)
        connections_added.forEach {
            add(it.a, it.b)
            connectionMap[it.a] = it
            connectionMap[it.b] = it
        }
    }

    fun connect(a: ThermalBody<Locator>, b: ThermalBody<Locator>, params: ThermalConnectionParameters = ThermalConnectionParameters.DEFAULT) {
        add(Connection(a, b, params))
    }

    fun remove(vararg bodies_removed: ThermalBody<Locator>) {
        bodies.removeAll(bodies_removed)
        bodies_removed.forEach { body ->
            // toList() here is just used to make a cheap copy
            connectionMap[body].toList().forEach {
                remove(it)
            }
        }
    }

    fun remove(vararg connections_removed: Connection<Locator>) {
        connections_removed.forEach {
            connectionMap[it.a].remove(it)
            connectionMap[it.b].remove(it)
        }
    }

    fun step(dt: Double) {
        val delta_E = mutableMapOf<ThermalBody<Locator>, Double>()
        for (connection in connections) {
            val transfer = connection.connection.transfer(dt)
            delta_E[connection.a] = delta_E.getOrDefault(connection.a, 0.0) + transfer
            delta_E[connection.b] = delta_E.getOrDefault(connection.b, 0.0) - transfer
        }
        for(body in bodies) {
            val temp = environment.temperature(body.locator)
            val cond = environment.conductance(body.locator)
            // FIXME: integrate this better with ThermalConnection above
            val delta_T = temp.kelvin - body.mass.temperature.kelvin
            val overall_cond = sqrt(cond * body.surfaceArea * body.mass.material.conductance)
            val power = overall_cond * delta_T
            delta_E[body] = delta_E.getOrDefault(body, 0.0) + power * dt
        }
        for((body, delta) in delta_E.entries) {
            body.mass.energy += delta
        }
    }
}