package org.eln2.libelectric.parsers.falstad.components.sources

import org.eln2.libelectric.parsers.falstad.CCData
import org.eln2.libelectric.parsers.falstad.PoleConstructor
import org.eln2.libelectric.sim.electrical.mna.component.Component
import org.eln2.libelectric.sim.electrical.mna.component.CurrentSource

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
