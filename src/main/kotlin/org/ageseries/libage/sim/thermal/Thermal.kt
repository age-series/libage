package org.ageseries.libage.sim.thermal

import org.ageseries.libage.data.mutableMultiMapOf
import org.ageseries.libage.sim.Material
import org.ageseries.libage.sim.Scale
import java.util.*
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * A temperature.
 *
 * The inner unit is always Kelvin; conversions are available as properties and methods.
 */
@JvmInline
value class Temperature(val kelvin: Double) {
    /**
     * Return this temperature on the given [Scale].
     */
    fun to(scale: Scale): Double = scale.map(kelvin)

    operator fun plus(rhs: Temperature) = Temperature(kelvin + rhs.kelvin)
    operator fun minus(rhs: Temperature) = Temperature(kelvin - rhs.kelvin)
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
 *
 * It is equal to 0 degrees Celsius.
 *
 * (A little history: since the founding of IUPAC in 1919, 0 centigrade, or 273.15 Kelvin, has been the "standard
 * temperature" according to that organization--even though the "standard pressure" changed from 101.3kPa to 100kPa in
 * 1982 . Fortunately, IUPAC has been somewhat of a _de facto_ standard as of late, though it certainly isn't the only
 * organization to have made a standard, with, e.g. NIST at 20 centigrade, ICAO at 15 centigrade, the US EPA at 25
 * centigrade, and so forth. Current best practice is to document which "stnadard" is actually adhered to, so consider
 * this comment to declare our adherence to IUPAC 1982 :)
 */
val STANDARD_TEMPERATURE: Temperature = Temperature(273.15)

class ThermalMass(
    /** The material of this mass, used for its thermal properties. */
    val material: Material,
    /** Thermal energy, in J. Leave null to set [STANDARD_TEMPERATURE]. */
    energy: Double? = null,
    /** Mass, in kg. */
    val mass: Double = 1.0,
) {
    var energy: Double = energy ?: (STANDARD_TEMPERATURE.kelvin * mass * material.specificHeat)

    fun temperatureAt(e: Double): Temperature = Temperature(e / mass / material.specificHeat)

    /**
     * Temperature of this mass, in K.
     *
     * Setting this changes the [energy].
     */
    var temperature: Temperature
        get() = temperatureAt(energy)
        set(value) {
            energy = value.kelvin * mass * material.specificHeat
        }

    override fun toString() = "<Thermal Mass $material ${mass}kg ${energy}J $temperature>"
}

data class ConnectionParameters(
    /** Conductance of the contact point. There is almost always a little loss due to mechanical effects, unless the materials are welded. This is W/K, with no distance effect. */
    val conductance: Double = 1.0,
    /** How long the connection is between the two masses--which affects the transfer rate. Measured in m. */
    val distance: Double = 1.0,
    /** How far along the line segment from [Connection.a] to [Connection.b] the contact point is. Affects how much [Connection.a]'s conductance dominates over [Connection.b]'s. Keep this between 0.0 and 1.0 inclusive. */
    val contactPoint: Double = 0.5,
    /**
     * Energy lost "to the environment" from this connnection. Implements the Second Law, and has a direct impact on the stability of the simulation.
     */
    val efficiency: Double = 0.99,
) {
    companion object {
        val DEFAULT = ConnectionParameters()
    }
}

class Connection(
    /** One of the connected masses. */
    val a: ThermalMass,
    /** The other connected mass. */
    val b: ThermalMass,
    /** Thermal parameters of this connection. */
    params: ConnectionParameters = ConnectionParameters.DEFAULT,
) {
    val contactPoint = params.contactPoint
    val distance = params.distance
    val conductance = params.conductance
    val efficiency = params.efficiency

    var prevFlux: Double = 0.0

    override fun toString() = "<Conn $a $b ${distance}m($contactPoint) ${conductance}W/K>"

    /**
     * Returns the energies to add to [a] and [b] (in that order), which would attempt to equilibriate the connected thermal masses over the given period of time [dt] (in s).
     *
     * For this to be stable, [dt] must be held relatively small, since this is a linear approximation to an exponential curve--otherwise, overshoot may be observed.
     */
    fun transfer(dt: Double): Pair<Double, Double> {
        // Kelvin
        val deltaT = b.temperature.kelvin - a.temperature.kelvin
        // W/K
        val distCondA = a.material.thermalConductivity * contactPoint * distance
        val distCondB = b.material.thermalConductivity * (1.0 - contactPoint) * distance
        val overallCond = (distCondA * distCondB * conductance).pow(1.0 / 3.0)
        // W
        val power = deltaT * overallCond
        // J
        val energy = power * dt
        // TODO: find the right way to calculate this
        // val critDamp = sqrt((a.mass + b.mass) * overallCond) * 2.0
        val critDamp = 1.0
        val dTerm = prevFlux * critDamp
        prevFlux = energy
        var toA = energy - dTerm
        var toB = -energy + dTerm
        if(toA > 0.0) { toA *= efficiency }
        if(toB > 0.0) { toB *= efficiency }
        /*
        // Detect an inflection point--if the energies would collide, assume they equilibriate.
        val newTempA = a.temperatureAt(a.energy + energy).kelvin
        val newTempB = b.temperatureAt(b.energy - energy).kelvin
        if(deltaT.sign != (newTempB - newTempA).sign) {
            // Distribute the energies at equilibrium.
            val totalE = a.energy + b.energy
            val eqAEnergy = totalE * (a.mass / b.mass) * (a.material.specificHeat / b.material.specificHeat)
            return eqAEnergy - a.energy
        }
         */
        return toA to toB
    }
}

class Simulator<Locator>(val environment: Environment<Locator>) {
    interface Body<Locator> {
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
    fun connect(
        a: Body<Locator>,
        b: Body<Locator>,
        params: ConnectionParameters = ConnectionParameters.DEFAULT
    ): Connection<Locator> =
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
            val (toA, toB) = connection.connection.transfer(dt)
            deltaE[connection.a] = deltaE.getOrDefault(connection.a, 0.0) + toA
            deltaE[connection.b] = deltaE.getOrDefault(connection.b, 0.0) + toB
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
