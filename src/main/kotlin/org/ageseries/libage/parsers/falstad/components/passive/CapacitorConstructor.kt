package org.ageseries.libage.parsers.falstad.components.passive

import org.ageseries.libage.parsers.falstad.CCData
import org.ageseries.libage.parsers.falstad.PoleConstructor
import org.ageseries.libage.sim.electrical.mna.component.Capacitor
import org.ageseries.libage.sim.electrical.mna.component.Component

/**
 * Capacitor Constructor
 *
 * Basic Falstad Capacitor
 */
class CapacitorConstructor: PoleConstructor() {
	override fun component(ccd: CCData) = Capacitor()
	override fun configure(ccd: CCData, cmp: Component) {
		val c = (cmp as Capacitor)
		c.timeStep = ccd.falstad.nominalTimestep
		c.capacitance = ccd.data[0].toDouble()
	}
}
