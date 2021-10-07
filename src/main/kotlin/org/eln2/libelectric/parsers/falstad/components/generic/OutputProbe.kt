package org.eln2.libelectric.parsers.falstad.components.generic

import org.eln2.libelectric.parsers.falstad.CCData
import org.eln2.libelectric.parsers.falstad.IComponentConstructor
import org.eln2.libelectric.parsers.falstad.PinRef
import org.eln2.libelectric.parsers.falstad.PosSet
import org.eln2.libelectric.sim.electrical.mna.component.Resistor

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

        val pp = (ccd.falstad.getPin(ccd.pos).representative as PosSet)
        val pr = PinRef(r, 0)
        ccd.falstad.addPinRef(pp, pr)
        ccd.falstad.addOutput(pr)

        ccd.pins = 1
    }
}
