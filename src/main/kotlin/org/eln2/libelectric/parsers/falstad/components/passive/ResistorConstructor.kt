package org.eln2.libelectric.parsers.falstad.components.passive

import org.eln2.libelectric.parsers.falstad.CCData
import org.eln2.libelectric.parsers.falstad.PoleConstructor
import org.eln2.libelectric.sim.electrical.mna.component.Component
import org.eln2.libelectric.sim.electrical.mna.component.Resistor

/**
 * Resistor Constructor
 *
 * Basic Falstad Resistor
 */
class ResistorConstructor : PoleConstructor() {
    override fun component(ccd: CCData) = Resistor()
    override fun configure(ccd: CCData, cmp: Component) {
        (cmp as Resistor).resistance = ccd.data[0].toDouble()
    }
}
