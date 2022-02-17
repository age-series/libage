package org.ageseries.libage.parsers.falstad.components.sources

import org.ageseries.libage.parsers.falstad.CCData
import org.ageseries.libage.parsers.falstad.PoleConstructor
import org.ageseries.libage.sim.electrical.mna.component.Component
import org.ageseries.libage.sim.electrical.mna.component.VoltageSource

/**
 * Voltage Source Constructor
 *
 * Basic Falstad voltage source. Two pin?
 */
class VoltageSourceConstructor : PoleConstructor() {
    override fun component(ccd: CCData) = VoltageSource()
    override fun configure(ccd: CCData, cmp: Component) {
        (cmp as VoltageSource).potential = ccd.data[2].toDouble()
    }
}
