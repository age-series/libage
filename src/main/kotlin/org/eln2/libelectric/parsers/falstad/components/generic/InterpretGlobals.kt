package org.eln2.libelectric.parsers.falstad.components.generic

import org.eln2.libelectric.parsers.falstad.CCData
import org.eln2.libelectric.parsers.falstad.IComponentConstructor

/**
 * InterpretGlobals
 *
 * Takes the information from Falstad's circuit settings operator ($)
 */
class InterpretGlobals : IComponentConstructor {
    override fun construct(ccd: CCData) {
        ccd.falstad.nominalTimestep = ccd.line.getDouble(2)
        ccd.pins = 0
    }
}
