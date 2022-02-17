package org.ageseries.libage.parsers.falstad.components.sources

import org.ageseries.libage.parsers.falstad.CCData
import org.ageseries.libage.parsers.falstad.PoleConstructor
import org.ageseries.libage.sim.electrical.mna.component.Component
import org.ageseries.libage.sim.electrical.mna.component.CurrentSource

/**
 * Current Source Constructor
 *
 * Falstad basic Current Source
 */
class CurrentSourceConstructor : PoleConstructor() {
    override fun component(ccd: CCData) = CurrentSource()
    override fun configure(ccd: CCData, cmp: Component) {
        (cmp as CurrentSource).current = ccd.data[0].toDouble()
    }
}
