package org.ageseries.libage.sim.thermal

import org.ageseries.libage.data.mutableMultiMapOf
import org.ageseries.libage.sim.Material
import org.ageseries.libage.sim.Scale
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

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

    override fun toString() = Scale.Thermal.KELVIN.display(kelvin)

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

class Mass(
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

data class ConnectionParameters(
    /** Conductance of the contact point. There is almost always a little loss due to mechanical effects, unless the materials are welded. This is W/K, with no distance effect. */
    val conductance: Double = 1.0,
    /** How long the connection is between the two masses--which affects the transfer rate. Measured in m. */
    val distance: Double = 1.0,
    /** How far along the line segment from [Connection.a] to [Connection.b] the contact point is. Affects how much [Connection.a]'s conductance dominates over [Connection.b]'s. Keep this between 0.0 and 1.0 inclusive. */
    val contactPoint: Double = 0.5,
) {
    companion object {
        val DEFAULT = ConnectionParameters()
    }
}

class Connection(
    /** One of the connected masses. */
    val a: Mass,
    /** The other connected mass. */
    val b: Mass,
    /** Thermal parameters of this connection. */
    params: ConnectionParameters = ConnectionParameters.DEFAULT,
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
        val distCondA = a.material.thermalConductivity * contactPoint * distance
        val distCondB = b.material.thermalConductivity * (1.0 - contactPoint) * distance
        val overallCond = (distCondA * distCondB * conductance).pow(1.0/3.0)
        // W
        val power = deltaT * overallCond
        // J
        return power * dt
    }
}

class Simulator<Locator>(val environment: Environment<Locator>) {
    interface Body<Locator> {
        val mass: Mass
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

    class Connection<Locator>(
        val a: Body<Locator>,
        val b: Body<Locator>,
        params: ConnectionParameters,
    ) {
        val connection = Connection(a.mass, b.mass, params)
    }

    val connections = mutableSetOf<Connection<Locator>>()
    val bodies = mutableSetOf<Body<Locator>>()
    val connectionMap = mutableMultiMapOf<Body<Locator>, Connection<Locator>>()

    /**
     * Add a body to the simulation.
     *
     * Without connection, this body will still conduct from/to the [environment].
     */
    fun add(body: Body<Locator>) {
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
     * Connect two [ThermalBodies](ThermalBody), with the given connection parameters for the underlying [Connection].
     *
     * The connection is automatically [add]ed to the simulation, as well as returned. The object can be used to [remove] it later.
     */
    fun connect(a: Body<Locator>, b: Body<Locator>, params: ConnectionParameters = ConnectionParameters.DEFAULT): Connection<Locator> =
        Connection(a, b, params).also {
            add(it)
        }

    /**
     * Remove the body from the simulation.
     *
     * If this body is involved in any [Connection]s, those connections will be removed as well. This can affect the flux on other bodies.
     */
    fun remove(body: Body<Locator>) {
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
     * This is held because the simulation can be run repeatedly in a given game loop, and it is best to keep it warm for that reason. Furthermore, while this could be stored on the [Body] itself, that would constitute an unnecessary implementation detail, subject to change.
     *
     * Keep this weak, so its persistence does not hold alive any ThermalBodies that have been removed.
     */
    protected val deltaE = WeakHashMap<Body<Locator>, Double>()

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
            val overallCond = sqrt(cond * body.surfaceArea * body.mass.material.thermalConductivity)
            val power = overallCond * deltaT
            deltaE[body] = deltaE.getOrDefault(body, 0.0) + power * dt
        }
        deltaE.entries.forEach { (body, delta) ->
            body.mass.energy += delta
        }
    }
}
