package org.ageseries.libage.sim.thermal

import org.ageseries.libage.data.mutableMultiMapOf
import java.util.*
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
    /**
     * Return this temperature on the given [Scale].
     */
    fun in_(scale: Scale): Double = scale.map(kelvin)

    operator fun plus(rhs: Temperature) = Temperature(kelvin + rhs.kelvin)
    operator fun minus(rhs: Temperature) = Temperature(kelvin - rhs.kelvin)

    operator fun compareTo(rhs: Temperature) = kelvin.compareTo(rhs.kelvin)

    override fun toString() = Scale.KELVIN.display(kelvin)

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
    override fun toString() = "<Mat ${specificHeat}J/(K kg) ${conductance}W/(m K)>"

    companion object {
        // NB: If you're populating these from the Wikipedia article,
        // note that they record J/gK, not J/kgK, so make sure to
        // multiply by 1e3
        // Source: https://en.wikipedia.org/wiki/Table_of_specific_heat_capacities
        // Source: https://en.wikipedia.org/wiki/List_of_thermal_conductivities
        // I've chosen the conductivities at ST, noting that they vary
        val IRON = Material(412.0, 83.5)
    }
}

class ThermalMass(
    /** The material of this mass, used for its thermal properties. */
    val material: Material,
    /** Thermal energy, in J. Leave null to set [STANDARD_TEMPERATURE]. */
    energy: Double? = null,
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

    override fun toString() = "<Mass $material ${mass}kg ${energy}J $temperature>"
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

    override fun toString() = "<Conn $a $b ${distance}m($contactPoint) ${conductance}W/K>"
    /**
     * Returns the change in energy, added to [a] and subtracted from [b], which would equilibriate the connected thermal masses over the given period of time [dt] (in s).
     *
     * For this to be stable, [dt] must be held relatively small, since this is a linear approximation to an exponential curve--otherwise, overshoot may be observed.
     */
    fun transfer(dt: Double): Double {
        // Kelvin
        val deltaT = b.temperature.kelvin - a.temperature.kelvin
        // W/K
        val distCondA = a.material.conductance * contactPoint * distance
        val distCondB = b.material.conductance * (1.0 - contactPoint) * distance
        val overallCond = (distCondA * distCondB * conductance).pow(1.0/3.0)
        // W
        val power = deltaT * overallCond
        // J
        return power * dt
    }
}

interface ThermalBody<Locator> {
    val mass: ThermalMass
    val locator: Locator
    /** Surface area of this body w.r.t. the [Environment], nominally in m^2. */
    val surfaceArea: Double
}

interface Environment<Locator> {
    /** The [Temperature] of the environment at this locator. */
    fun temperature(locator: Locator): Temperature
    /** How conductive the substance in the environment is at this locator. */
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

    /**
     * Add a body to the simulation.
     *
     * Without connection, this body will still conduct from/to the [environment].
     */
    fun add(body: ThermalBody<Locator>) {
        bodies.add(body)
    }

    /**
     * Add a connection to the simulation.
     *
     * Generally, you would use [connect] instead.
     */
    fun add(connection: Connection<Locator>) {
        connections.add(connection)
        add(connection.a)
        add(connection.b)
        connectionMap[connection.a] = connection
        connectionMap[connection.b] = connection
    }

    /**
     * Connect two [ThermalBodies](ThermalBody), with the given connection parameters for the underlying [ThermalConnection].
     *
     * The connection is automatically [add]ed to the simulation, as well as returned. The object can be used to [remove] it later.
     */
    fun connect(a: ThermalBody<Locator>, b: ThermalBody<Locator>, params: ThermalConnectionParameters = ThermalConnectionParameters.DEFAULT): Connection<Locator> =
        Connection(a, b, params).also {
            add(it)
        }

    /**
     * Remove the body from the simulation.
     *
     * If this body is involved in any [Connection]s, those connections will be removed as well. This can affect the flux on other bodies.
     */
    fun remove(body: ThermalBody<Locator>) {
        bodies.remove(body)
        connectionMap[body].toList().forEach {
            remove(it)
        }
    }

    /**
     * Remove the [Connection] from the simulation.
     *
     * This does not affect the membership of the underlying [ThermalBodies](ThermalBody).
     */
    fun remove(connection: Connection<Locator>) {
        connections.remove(connection)
        connectionMap[connection.a].remove(connection)
        connectionMap[connection.b].remove(connection)
    }

    /**
     * The "delta E" cache, a "double buffer" for the thermal step.
     *
     * This is held because the simulation can be run repeatedly in a given game loop, and it is best to keep it warm for that reason. Furthermore, while this could be stored on the [ThermalBody] itself, that would constitute an unnecessary implementation detail, subject to change.
     *
     * Keep this weak, so its persistence does not hold alive any ThermalBodies that have been removed.
     */
    protected val deltaE = WeakHashMap<ThermalBody<Locator>, Double>()

    /**
     * Run the simulation for a step.
     *
     * This does a discrete step of [dt] seconds on all bodies and connections in the simulation. Due to the approximations involved in the model (and the primitive integration used here), smaller step sizes are generally more stable.
     *
     * Flux between bodies (via [Connection]s) and with the [environment] are considered "simultaneously"--energy updates are deferred until the end.
     */
    fun step(dt: Double) {
        connections.forEach { connection ->
            val transfer = connection.connection.transfer(dt)
            deltaE[connection.a] = deltaE.getOrDefault(connection.a, 0.0) + transfer
            deltaE[connection.b] = deltaE.getOrDefault(connection.b, 0.0) - transfer
        }
        bodies.forEach { body ->
            val temp = environment.temperature(body.locator)
            val cond = environment.conductance(body.locator)
            // FIXME: integrate this better with ThermalConnection above
            val deltaT = temp.kelvin - body.mass.temperature.kelvin
            val overallCond = sqrt(cond * body.surfaceArea * body.mass.material.conductance)
            val power = overallCond * deltaT
            deltaE[body] = deltaE.getOrDefault(body, 0.0) + power * dt
        }
        deltaE.entries.forEach { (body, delta) ->
            body.mass.energy += delta
        }
    }
}
