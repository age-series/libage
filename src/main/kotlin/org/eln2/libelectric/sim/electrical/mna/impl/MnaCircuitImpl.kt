package org.eln2.libelectric.sim.electrical.mna.impl

import org.eln2.libelectric.sim.electrical.generic.*
import org.eln2.libelectric.sim.electrical.mna.Circuit
import org.eln2.libelectric.sim.electrical.mna.component.*

class MnaCircuitImpl() : GenericCircuit {
    var circuit = Circuit()
    var components = mutableListOf<GenericComponent>()
    var connectivity = mutableListOf<ComponentConnectivityMapping>()
    override val backingImplementation = "ModifiedNodalAnalysis"

    override fun addComponent(c: GenericComponent) {
        components.add(c)
        internalAddComponent(c)
    }

    private fun internalAddComponent(c: GenericComponent) {
        return when (c) {
            is GenericResistor -> {
                val resistor = Resistor()
                resistor.resistance = c.resistanceOhms
                circuit.add(resistor)
                c.internal = resistor
            }
            is GenericCapacitor -> {
                val capacitor = Capacitor()
                capacitor.capacitance = c.capacityFarads
                capacitor.charge = 0.0
                circuit.add(capacitor)
                c.internal = capacitor
            }
            is GenericInductor -> {
                val inductor = Inductor()
                inductor.inductance = c.inductanceHenrie
                inductor.flux = 0.0
                circuit.add(inductor)
                c.internal = inductor
            }
            is GenericVoltageSource -> {
                val voltageSource = VoltageSource()
                voltageSource.potential = c.voltage
                circuit.add(voltageSource)
                c.internal = voltageSource
            }
            is GenericCurrentSource -> {
                val currentSource = CurrentSource()
                currentSource.current = c.current
                circuit.add(currentSource)
                c.internal = currentSource
            }
            else -> {
            }
        }
    }

    override fun removeComponent(c: GenericComponent) {
        components.remove(c)
        // Due to the underlying implementation, nuke the circuit and replay everything.
        circuit = Circuit()
        connectivity.filter { it.localComponent == c || it.remoteComponent == c}.forEach { connectivity.remove(it) }
        internalReplay()
    }

    override fun simulate(timeDelta: Double) {
        circuit.step(timeDelta)
    }

    override fun connectComponents(
        localComponent: GenericComponent,
        localConnection: GenericConnection,
        remoteComponent: GenericComponent,
        remoteConnection: GenericConnection
    ) {
        connectivity.add(ComponentConnectivityMapping(localComponent, localConnection, remoteComponent, remoteConnection))
        internalConnectComponents(localComponent, localConnection, remoteComponent, remoteConnection)
    }

    private fun internalConnectComponents(
        localComponent: GenericComponent,
        localConnection: GenericConnection,
        remoteComponent: GenericComponent,
        remoteConnection: GenericConnection
    ) {
        (localComponent.internal as Component).connect(
            localConnection.internal as Int,
            remoteComponent.internal as Component,
            remoteConnection.internal as Int
        )
    }

    override fun disconnectComponents(localComponent: GenericComponent, remoteComponent: GenericComponent) {
        connectivity.filter { it.localComponent == localComponent && it.remoteComponent == remoteComponent}.forEach { connectivity.remove(it) }
        // Due to the underlying implementation, nuke the circuit and replay everything.
        circuit = Circuit()
        return internalReplay()
    }

    private fun internalReplay() {
        components.forEach { this.addComponent(it) }
        connectivity.forEach { this.connectComponents(it.localComponent, it.localConnection, it.remoteComponent, it.remoteConnection) }
    }
}
