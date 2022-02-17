package org.ageseries.libage.parsers.falstad.components.sources

import org.ageseries.libage.parsers.falstad.CCData
import org.ageseries.libage.parsers.falstad.IComponentConstructor

/**
 * Ground Constructor
 *
 * Falstad's basic ground pin. Sets a node as being a ground connected node.
 */
class GroundConstructor : IComponentConstructor {
    override fun construct(ccd: CCData) {
        ccd.pins = 1
        ccd.falstad.addGround(
            ccd.falstad.getPin(ccd.pinPositions[0])
        )
        ccd.falstad.floating = false
    }
}
