package org.eln2.libelectric.sim.electrical.generic

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
    var internal: Any? = null
    // TODO: Suggestions on how to make this more useful?
}

open class GenericConnection {
    var internal: Any? = null
    // TODO: Suggestions on how to make this more useful?
}

abstract class GenericResistor(var resistanceOhms: Double): GenericComponent()
abstract class GenericCapacitor(var capacityFarads: Double): GenericComponent()
abstract class GenericInductor(var inductanceHenries: Double): GenericComponent()
abstract class GenericVoltageSource(var voltage: Double): GenericComponent()
abstract class GenericCurrentSource(var current: Double): GenericComponent()

data class ComponentConnectivityMapping(
    var localComponent: GenericComponent,
    var localConnection: GenericConnection,
    var remoteComponent: GenericComponent,
    var remoteConnection: GenericConnection
)
