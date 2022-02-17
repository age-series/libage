package org.ageseries.libage.parsers.falstad.components.passive

import org.ageseries.libage.parsers.falstad.CCData
import org.ageseries.libage.parsers.falstad.PoleConstructor
import org.ageseries.libage.sim.electrical.mna.component.Component
import org.ageseries.libage.sim.electrical.mna.component.Resistor

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
