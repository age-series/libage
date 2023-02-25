package org.ageseries.libage.sim.thermal

import org.ageseries.libage.data.mutableMultiMapOf
import java.util.*
import kotlin.math.sqrt

interface ThermalBody<Locator> {
    val mass: ThermalMass
    val locator: Locator

    /** Surface area of this body w.r.t. the [ThermalEnvironment], nominally in m^2. */
    val surfaceArea: Double
}

interface ThermalEnvironment<Locator> {
    /** The [Temperature] of the environment at this locator. */
    fun temperature(locator: Locator): Temperature

    /** How conductive the substance in the environment is at this locator. */
    fun conductance(locator: Locator): Double
}

class ThermalSimulator<Locator>(val environment: ThermalEnvironment<Locator>) {
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
    fun connect(
        a: ThermalBody<Locator>,
        b: ThermalBody<Locator>,
        params: ThermalConnectionParameters = ThermalConnectionParameters.DEFAULT
    ): Connection<Locator> =
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
    private val deltaE = WeakHashMap<ThermalBody<Locator>, Double>()

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