package org.ageseries.libage.parsers.falstad.components.generic

import org.ageseries.libage.parsers.falstad.CCData
import org.ageseries.libage.parsers.falstad.IComponentConstructor
import org.ageseries.libage.parsers.falstad.PinRef
import org.ageseries.libage.sim.electrical.mna.component.Resistor

/**
 * Output Probe
 *
 * Falstad's Output Probe - shows the voltage of a particular node in the circuit.
 */
class OutputProbe : IComponentConstructor {
    companion object {
        val HIGH_IMPEDANCE = Double.POSITIVE_INFINITY
    }

    override fun construct(ccd: CCData) {
        val r = Resistor()
        r.resistance = HIGH_IMPEDANCE
        ccd.circuit.add(r)
        r.ground(1)

        val pp = ccd.falstad.getPin(ccd.pos).representative
        val pr = PinRef(r, 0)
        ccd.falstad.addPinRef(pp, pr)
        ccd.falstad.addOutput(pr)

        ccd.pins = 1
    }
}
