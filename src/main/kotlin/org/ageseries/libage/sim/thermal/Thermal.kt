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
    /** The "scale" of power conducted along this connection. If constant density is assumed, such that mass and volume are related, this is linear in contact area, thus the name. */
    val area: Double = 1.0,
    /** How far along the line segment from [MassConnection.a] to [MassConnection.b] the contact point is. Affects how much [MassConnection.a]'s conductance dominates over [MassConnection.b]'s. Keep this between 0.0 and 1.0 inclusive. Meaningless in [EnvironmentConnection]s. */
    val contactPoint: Double = 0.5,
    /**
     * Energy lost "to the environment" from this connection. Implements the Second Law, and has a direct impact on the stability of the simulation.
     */
    val efficiency: Double = 0.99,
) {
    override fun toString(): String =
        "<ConnParam (${contactPoint})${distance}m ${area}m^2 ${conductance}W/mK ~$efficiency>"

    companion object {
        val DEFAULT = ConnectionParameters()
    }
}

/**
 * Represents a connection between a [ThermalMass] and something else.
 *
 * The current implementations involve another ThermalMass and the environment.
 */
interface Connection {
    /**
     * Compute the heat transfer, in Joules, to do to each side of the connection over [dt] seconds.
     *
     * By convention, the returned energy transfers are in argument order; whereas the first is usually the
     * [ThermalMass], the second may or may not be relevant depending on whether there is a heat sink.
     */
    fun transfer(dt: Double): Pair<Double, Double>

    /**
     * [ThermalMass]es owned by this connection, so the [Simulator] can manage them.
     */
    val masses: List<ThermalMass>
}

class MassConnection(
    /** One of the connected masses. */
    val a: ThermalMass,
    /** The other connected mass. */
    val b: ThermalMass,
    /** Thermal parameters of this connection. */
    val params: ConnectionParameters = ConnectionParameters.DEFAULT,
): Connection {
    private var prevFlux: Double = 0.0

    override fun toString() = "<MassConn $a $b $params>"

    /**
     * Returns the energies to add to [a] and [b] (in that order), which would attempt to equilibriate the connected thermal masses over the given period of time [dt] (in s).
     *
     * For this to be stable, [dt] must be held relatively small, since this is a linear approximation to an exponential curve--otherwise, overshoot may be observed.
     */
    override fun transfer(dt: Double): Pair<Double, Double> {
        // Kelvin
        val deltaT = b.temperature.kelvin - a.temperature.kelvin
        // W/K
        val distCondA = a.material.thermalConductivity * params.contactPoint * params.distance
        val distCondB = b.material.thermalConductivity * (1.0 - params.contactPoint) * params.distance
        val overallCond = params.area * (distCondA * distCondB * params.conductance).pow(1.0 / 3.0)
        // W
        val power = deltaT * overallCond
        // J
        val energy = power * dt
        val critDamp = sqrt((a.mass + b.mass) / overallCond) * dt
        val dTerm = prevFlux * critDamp * dt
        prevFlux = power
        var toA = energy - dTerm
        var toB = -energy + dTerm
        if(toA > 0.0) { toA *= params.efficiency }
        if(toB > 0.0) { toB *= params.efficiency }
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

    override val masses: List<ThermalMass>
        get() = listOf(a, b)
}

class EnvironmentConnection(
    /** The [ThermalMass] to affect. */
    val a: ThermalMass,
    /** The [Temperature] of the environment--assumed to have infinite energy. */
    var temperature: Temperature,
    /** The parameters of this contact. Not all fields are meaningful in this application. */
    val params: ConnectionParameters,
): Connection {
    private var prevFlux: Double = 0.0

    override fun toString() = "<EnvConn $a $temperature $params>"

    override fun transfer(dt: Double): Pair<Double, Double> {
        // See above for comments explaining more of this
        val deltaT = temperature.kelvin - a.temperature.kelvin
        val overallCond = sqrt(params.area * a.material.thermalConductivity * params.conductance)
        val power = deltaT * overallCond
        val critDamp = sqrt(a.mass / overallCond) * dt
        var energy = (power - prevFlux * critDamp) * dt
        prevFlux = power
        if(energy > 0.0) { energy *= params.efficiency }
        return energy to 0.0
    }

    override val masses: List<ThermalMass>
        get() = listOf(a)
}

class Simulator {
    val connections = mutableSetOf<Connection>()
    val masses = mutableSetOf<ThermalMass>()
    val connectionMap = mutableMultiMapOf<ThermalMass, Connection>()

    /**
     * Add a body to the simulation.
     *
     * Without connection, this body will still conduct from/to the [environment].
     */
    fun add(mass: ThermalMass) {
        masses.add(mass)
    }

    /**
     * Add a connection to the simulation.
     *
     * Generally, you would use [connect] instead.
     */
    fun add(connection: Connection) {
        connections.add(connection)
        connection.masses.forEach {
            add(it)
            connectionMap[it] = connection
        }
    }

    /**
     * Connect two [ThermalMass]es, with the given connection parameters for the underlying [Connection].
     *
     * The connection is automatically [add]ed to the simulation, as well as returned. The object can be used to [remove] it later.
     */
    fun connect(
        a: ThermalMass,
        b: ThermalMass,
        params: ConnectionParameters = ConnectionParameters.DEFAULT
    ): Connection =
        MassConnection(a, b, params).also {
            add(it)
        }

    /**
     * Connect a [ThermalMass] to the environment at a given temperature.
     *
     * The resulting connection is [add]ed and returned, so that it can be [remove]d later.
     */
    fun connect(
        a: ThermalMass,
        temperature: Temperature,
        params: ConnectionParameters = ConnectionParameters.DEFAULT,
    ): Connection =
        EnvironmentConnection(a, temperature, params).also {
            add(it)
        }

    /**
     * Remove the mass from the simulation.
     *
     * If this mass is involved in any [Connection]s, those connections will be removed as well. This can affect the flux on other masses.
     */
    fun remove(mass: ThermalMass) {
        masses.remove(mass)
        connectionMap[mass].toList().forEach {
            remove(it)
        }
    }

    /**
     * Remove the [Connection] from the simulation.
     *
     * This does not affect the membership of the underlying [ThermalBodies](ThermalBody).
     */
    fun remove(connection: Connection) {
        connections.remove(connection)
        connection.masses.forEach {
            connectionMap[it].remove(connection)
        }
    }

    /**
     * The "delta E" cache, a "double buffer" for the thermal step.
     *
     * This is held because the simulation can be run repeatedly in a given game loop, and it is best to keep it warm for that reason. Furthermore, while this could be stored on the [Body] itself, that would constitute an unnecessary implementation detail, subject to change.
     *
     * Keep this weak, so its persistence does not hold alive any ThermalMasses that have been removed.
     */
    private val deltaE = WeakHashMap<ThermalMass, Double>()

    /**
     * Run the simulation for a step.
     *
     * This does a discrete step of [dt] seconds on all bodies and connections in the simulation. Due to the approximations involved in the model (and the primitive integration used here), smaller step sizes are generally more stable.
     *
     * Flux between bodies (via [Connection]s) and with the environment are considered "simultaneously"--energy updates are deferred until the end.
     */
    fun step(dt: Double) {
        connections.forEach { connection ->
            val (toA, toB) = connection.transfer(dt)
            val masses = connection.masses
            masses.getOrNull(0)?.let { a ->
                deltaE[a] = deltaE.getOrDefault(a, 0.0) + toA
            }
            masses.getOrNull(1)?.let { b ->
                deltaE[b] = deltaE.getOrDefault(b, 0.0) + toB
            }
        }
        deltaE.entries.forEach { (mass, delta) ->
            mass.energy += delta
        }
        deltaE.clear()
    }
}
