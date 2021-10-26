package org.eln2.libelectric.sim.electrical.generic

import org.eln2.libelectric.sim.electrical.mna.component.Component

interface GenericCircuit {
    fun addComponent(c: GenericComponent)
    fun removeComponent(c: GenericComponent)
    fun simulate(timeDelta: Double)
    fun connectComponents(
        localComponent: GenericComponent,
        localConnection: GenericConnection,
        remoteComponent: GenericComponent,
        remoteConnection: GenericConnection
    )
    fun disconnectComponents(localComponent: GenericComponent, remoteComponent: GenericComponent)
    val backingImplementation: String
}

open class GenericComponent {
    // This feels like a subpar solution as opposed to having some way for the system to call back into the backing class.
    fun getVoltageAcross(point1: GenericConnection, point2: GenericConnection): Double {
        return when (point1) {
            is Component -> {
                return point1.node(0).potential - point2.node(1).potential
            } else -> {
                0.0
            }
        }
    }
    fun getCurrentAcross(point1: GenericConnection, point2: GenericConnection): Double {
        return 0.0
    }
    var internal: Any? = null
}

open class GenericConnection {
    fun getPointVoltage(point: GenericConnection): Double {
        return 0.0
    }
    var internal: Any? = null
}

abstract class GenericResistor(var resistanceOhms: Double): GenericComponent()
abstract class GenericCapacitor(var capacityFarads: Double): GenericComponent()
abstract class GenericInductor(var inductanceHenrie: Double): GenericComponent()
abstract class GenericVoltageSource(var voltage: Double): GenericComponent()
abstract class GenericCurrentSource(var current: Double): GenericComponent()

data class ComponentConnectivityMapping(
    var localComponent: GenericComponent,
    var localConnection: GenericConnection,
    var remoteComponent: GenericComponent,
    var remoteConnection: GenericConnection
)
