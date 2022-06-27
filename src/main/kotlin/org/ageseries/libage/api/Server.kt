package org.ageseries.libage.api

import com.google.gson.Gson
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.ageseries.libage.sim.electrical.mna.Circuit
import org.ageseries.libage.sim.electrical.mna.component.Component
import org.ageseries.libage.sim.electrical.mna.component.Resistor
import org.ageseries.libage.sim.electrical.mna.component.VoltageSource
import java.util.*
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.set

data class CircuitBundle(var circuit: Circuit, var components: MutableList<Component>)

val circuits = mutableMapOf<UUID, CircuitBundle>()

fun loadTestCircuits() {
    val c = Circuit()
    val r = Resistor()
    val vs = VoltageSource()

    c.add(vs, r)

    vs.connect(0, r, 0)
    vs.connect(1, r, 1)
    vs.ground(0)

    vs.potential = 10.0
    r.resistance = 10.0

    circuits[UUID.randomUUID()] = CircuitBundle(c, mutableListOf(r, vs))
}

fun main() {
    loadTestCircuits()

    embeddedServer(Netty, port = 8080) {
        routing {
            get("/") {
                call.respondText("libage api endpoint")
            }
            get("/circuits.json") {

                call.libageApiResponse {
                    gson.toJson(circuits)
                }
            }
        }
    }.start(wait = true)
}
