package org.eln2.libelectric.sim.electrical

import org.ageseries.libage.debug.mnaPrintln
import org.ageseries.libage.sim.electrical.component.Switch
import org.ageseries.libage.sim.electrical.mna.Circuit
import org.ageseries.libage.sim.electrical.mna.component.VoltageSource
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class SwitchTest {
    @Test
    fun switchTest() {
        val c = Circuit()
        val sw = Switch()
        val vs = VoltageSource()

        c.add(vs, sw)

        vs.connect(0, sw, 0)
        vs.connect(1, sw, 1)
        vs.ground(0)

        vs.potential = 10.0
        sw.closedResistance = 10.0
        sw.openResistance = 100_000_000.0

        sw.closed = true
        Assertions.assertEquals(true, sw.closedResistance == sw.resistance)

        assert(c.step(0.05))
        mnaPrintln(c)
        Assertions.assertEquals(true, (sw.current > 0.99) and (sw.current < 1.01))

        sw.open = true
        Assertions.assertEquals(true, sw.openResistance == sw.resistance)

        assert(c.step(0.05))
        mnaPrintln(c)
        Assertions.assertEquals(true, (sw.current > 0.000000099) and (sw.current < 0.000000101))
    }

    @Test
    fun switchToggleTest() {
        val c = Circuit()
        val sw = Switch()
        val vs = VoltageSource()

        c.add(vs, sw)

        vs.connect(0, sw, 0)
        vs.connect(1, sw, 1)
        vs.ground(0)

        vs.potential = 10.0
        sw.closedResistance = 10.0
        sw.openResistance = 100_000_000.0

        sw.closed = true
        Assertions.assertEquals(true, sw.closedResistance == sw.resistance) { "Resistance: ${sw.resistance} != ${sw.closedResistance}" }

        assert(c.step(0.05))
        mnaPrintln(c)
        Assertions.assertEquals(true, (sw.current > 0.99) and (sw.current < 1.01)) { "Current: ${sw.current}" }

        sw.toggle()
        Assertions.assertEquals(true, sw.openResistance == sw.resistance) { "Resistance: ${sw.resistance} != ${sw.openResistance}" }

        assert(c.step(0.05))
        mnaPrintln(c)
        Assertions.assertEquals(true, (sw.current > 0.000000099) and (sw.current < 0.000000101)) { "Current: ${sw.current}" }
    }
}
