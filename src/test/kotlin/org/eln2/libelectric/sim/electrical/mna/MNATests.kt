package org.eln2.libelectric.sim.electrical.mna

import org.ageseries.libage.debug.dprintln
import org.ageseries.libage.debug.mnaPrintln
import org.ageseries.libage.sim.electrical.mna.Circuit
import org.ageseries.libage.sim.electrical.mna.NEGATIVE
import org.ageseries.libage.sim.electrical.mna.POSITIVE
import org.ageseries.libage.sim.electrical.mna.component.*
import org.ageseries.libage.sim.electrical.mna.component.Port.Companion.NEG_INDEX
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.abs

internal class MNATests {

    /**
     *  A percent error function that returns true if the given simulated and actual values are within the given
     *  tolerance percentage.
     **/
    fun within_tolerable_error(simulated: Double, actual: Double, tolerance: Double) : Boolean {
        val percentError = abs((simulated - actual) / actual)
        dprintln("sim = $simulated, act = $actual, %err = $percentError")
        return percentError < tolerance
    }

    @Test
    fun resistorVoltageSourceTest() {
        val c = Circuit()
        val r = Resistor()
        val vs = VoltageSource()

        c.add(vs, r)

        vs.connect(0, r, 0)
        vs.connect(1, r, 1)
        vs.ground(0)

        vs.potential = 10.0
        r.resistance = 10.0

        assert(c.step(0.05))
        mnaPrintln(c)
        assertEquals(true, (r.current > 0.99) and (r.current < 1.01))
    }

    @Test
    fun resistorCurrentSourceTest() {
        val c = Circuit()
        val r = Resistor()
        val cs = CurrentSource()

        c.add(cs, r)

        cs.connect(POSITIVE, r, POSITIVE)
        cs.connect(NEGATIVE, r, NEGATIVE)
        cs.ground(NEGATIVE)

        cs.current = -1.0
        r.resistance = 10.0

        assert(c.step(0.05))
        mnaPrintln(c)
        assertEquals(true, (r.current > 0.99) and (r.current < 1.01))
        assertEquals(true, (r.potential > 9.99) and (r.current < 10.01))
        assertEquals(true, (cs.potential > 9.99) and (cs.current < 10.01))
    }

    @Test
    fun resistorsInSeriesTest() {
        val circuit = Circuit()
        val vs = VoltageSource()
        val r1 = Resistor()
        val r2 = Resistor()

        vs.potential = 10.0
        r1.resistance = 5.0
        r2.resistance = 5.0

        circuit.add(vs, r1, r2)
        vs.connect(1, r1, 1)
        r1.connect(0, r2, 1)
        r2.connect(0, vs, 0)
        vs.ground(1)

        assert(circuit.step(0.5))
        assert(within_tolerable_error(r1.current, r2.current, 0.001))
        assert(within_tolerable_error(r1.current, 1.0, 0.01))
    }

    @Test
    fun resistorVoltageSourceModificationTest() {
        val c = Circuit()
        val r = Resistor()
        val vs = VoltageSource()

        c.add(vs, r)

        vs.connect(0, r, 0)
        vs.connect(1, r, 1)
        vs.ground(0)

        vs.potential = 10.0
        r.resistance = 10.0

        assert(c.step(0.05))
        assertEquals(true, (r.current > 0.99) and (r.current < 1.01))

        r.resistance = 50.0

        assert(c.step(0.05))
        assertEquals(true, (r.current > 0.19) and (r.current < 0.21))
    }

    @Test
    fun twoResistorVoltageSourceModificationTest() {
        val c = Circuit()
        val r1 = Resistor()
        val r2 = Resistor()
        val vs = VoltageSource()

        c.add(vs, r1, r2)

        vs.connect(0, r1, 0)
        r1.connect(1, r2, 0)
        vs.connect(1, r2, 1)
        vs.ground(0)

        vs.potential = 10.0
        r1.resistance = 10.0
        r2.resistance = 20.0

        assert(c.step(0.05))
        assert((r1.current > 0.333) and (r1.current < 0.334))
        assert((r2.current > 0.333) and (r2.current < 0.334))
        assert(((r1.node(1)?.potential ?: 0.0) > 3.333) and ((r1.node(1)?.potential ?: 0.0) < 3.334))

        r2.resistance = 50.0

        assert(c.step(0.05))
        assert((r1.current > 0.1666) and (r1.current < 0.1667))
        assert((r2.current > 0.1666) and (r2.current < 0.1667))
        assert(((r1.node(1)?.potential ?: 0.0)> 1.666) and ((r1.node(1)?.potential ?: 0.0) < 1.667))
    }

    @Test
    fun resistorsInParallelAndConsistencyTest() {
        val c = Circuit()
        val vs = VoltageSource()
        val r1 = Resistor()
        val r2 = Resistor()

        c.add(vs, r1)

        vs.connect(POSITIVE, r1, POSITIVE)
        vs.connect(NEGATIVE, r1, NEGATIVE)
        vs.ground(NEGATIVE)

        vs.potential = 10.0
        r1.resistance = 5.0

        assert(c.step(0.5))
        val current = r1.current

        c.add(r2)
        r2.resistance = r1.resistance
        r2.connect(POSITIVE, r1, POSITIVE)
        r2.connect(NEGATIVE, r1, NEGATIVE)

        assert(c.step(0.5))
        assertEquals(r2.current, r1.current, 1e-9)
        assertEquals(r1.current, current, 1e-9)
        assertEquals(vs.current, current * 2.0, 1e-9)

        c.remove(r2)
        assert(c.step(0.5))
        assertEquals(r1.current, current, 1e-9)
        assertEquals(r2.circuit, null)
    }

    @Test
    fun resistorCapacitorCircuitTest() {
        val c = Circuit()
        val r1 = Resistor()
        val c1 = Capacitor()
        val vs = VoltageSource()

        c.add(r1, c1, vs)

        vs.connect(POSITIVE, c1, POSITIVE)
        c1.connect(NEGATIVE, r1, POSITIVE)
        r1.connect(NEGATIVE, vs, NEGATIVE)
        vs.ground(NEGATIVE)

        vs.potential = 5.0
        r1.resistance = 267.0 + 22.0
        c1.capacitance = 0.000932

        /*
            The following uses data from measuring real circuits rather than comparing against Falstad.
            See /testdata/discharging_data.ods
         */

        // Setting the tolerance of inaccuracy to 15% error.
        val tolerance = 0.15

        // Charging

        assert(c.step(0.05)) // 0.050 s
        assert(within_tolerable_error(r1.potential, 4.0909, tolerance))
        assert(within_tolerable_error(c1.potential, 5 - 4.0909, tolerance))
        assert(within_tolerable_error(c1.current, 0.0142, tolerance))

        assert(c.step(0.05)) // 0.100 s
        assert(within_tolerable_error(r1.potential, 3.4017, tolerance))
        assert(within_tolerable_error(c1.potential, 5 - 3.4017, tolerance))
        assert(within_tolerable_error(c1.current, 0.0118, tolerance))

        assert(c.step(0.05)) // 0.150 s
        assert(within_tolerable_error(r1.potential, 2.8250, tolerance))
        assert(within_tolerable_error(c1.potential, 5 - 2.8250, tolerance))
        assert(within_tolerable_error(c1.current, 0.0098, tolerance))

        assert(c.step(0.05)) // 0.200 s
        assert(within_tolerable_error(r1.potential, 2.3412, tolerance))
        assert(within_tolerable_error(c1.potential, 5 - 2.3412, tolerance))
        assert(within_tolerable_error(c1.current, 0.0081, tolerance))

        // Should finish charging within 2.00 seconds.
        for (it in 4..40) assert(c.step(0.05))

        // Discharging

        vs.potential = 0.0

        assert(c.step(0.05)) // 0.050 s
        assert(within_tolerable_error(c1.potential, 4.0909, tolerance))
        assert(within_tolerable_error(r1.potential, -4.0909, tolerance))
        assert(within_tolerable_error(c1.current, -0.0142, tolerance))

        assert(c.step(0.05)) // 0.100 s
        assert(within_tolerable_error(c1.potential, 3.4017, tolerance))
        assert(within_tolerable_error(r1.potential, -3.4017, tolerance))
        assert(within_tolerable_error(c1.current, -0.0118, tolerance))

        assert(c.step(0.05)) // 0.150 s
        assert(within_tolerable_error(c1.potential, 2.8250, tolerance))
        assert(within_tolerable_error(r1.potential, -2.8250, tolerance))
        assert(within_tolerable_error(c1.current, -0.0098, tolerance))

        assert(c.step(0.05)) // 0.200 s
        assert(within_tolerable_error(c1.potential, 2.3412, tolerance))
        assert(within_tolerable_error(r1.potential, -2.3412, tolerance))
        assert(within_tolerable_error(c1.current, -0.0081, tolerance))
    }

    @Test
    fun resistorInductorCircuitTest() {
        val c = Circuit()
        val r1 = Resistor()
        val i1 = Inductor()
        val vs = VoltageSource()

        c.add(r1, i1, vs)

        vs.connect(POSITIVE, r1, POSITIVE)
        r1.connect(NEGATIVE, i1, POSITIVE)
        i1.connect(NEGATIVE, vs, NEGATIVE)
        vs.ground(NEGATIVE)

        vs.potential = 5.0
        r1.resistance = 100.0
        i1.inductance = 1.0

        // Setting the tolerance of inaccuracy to 15% error.
        val tolerance = 0.15

        assert(c.step(0.001))
        assert(within_tolerable_error(i1.potential, 4.525, tolerance))
        assert(within_tolerable_error(i1.current, 4.247e-3, tolerance))
        assert(c.step(0.001))
        assert(within_tolerable_error(i1.potential, 4.095, tolerance))
        assert(within_tolerable_error(i1.current, 9.053e-3, tolerance))
        assert(c.step(0.001))
        assert(within_tolerable_error(i1.potential, 3.703, tolerance))
        assert(within_tolerable_error(i1.current, 12.968e-3, tolerance))
    }

    @Test
    fun testPinRefAPI() {
        val c = Circuit()
        val r = Resistor()
        val vs = VoltageSource()

        val vp = vs.posRef
        val vn = vs.negRef
        val rp = r.posRef
        val rn = r.negRef

        c.add(vs, r)

        vs.potential = 10.0
        r.resistance = 10.0

        vp.connect(rp)
        vn.connect(rn)
        vn.ground()

        assert(c.step(0.05))
        mnaPrintln(c)
        assert(r.current > 0.99 && r.current < 1.01)
        assert(vs.current > 0.99 && vs.current < 1.01)
    }

    @Test
    fun testCircuitDot() {
        val c = Circuit()
        val r = Resistor()
        val vs = VoltageSource()

        c.add(vs, r)

        vs.connect(0, r, 0)
        vs.connect(1, r, 1)
        vs.ground(0)

        vs.potential = 10.0
        r.resistance = 10.0

        c.buildMatrix() // Required to populate nodes, otherwise toDot will crash
        val output = c.toDot()

        assert(output.length > 1)
    }

    @Test
    fun lineOfEqualResistors() {
        var res = 10.0
        val v = 5.0

        val cl = Circuit()
        val l = Line()
        val vsl = VoltageSource().apply { potential = 5.0 }
        cl.add(vsl, l)
        vsl.negRef.connect(l.negRef)
        vsl.posRef.connect(l.posRef)
        vsl.negRef.ground()

        val parts = mutableListOf<Line.Part>()

        for(number in 1 .. 10) {
            val cr = Circuit()
            val vsr = VoltageSource().apply { potential = 5.0 }
            cr.add(vsr)
            vsr.negRef.ground()
            var lastr = vsr.posRef
            val resistors = (0 until number).map {
                Resistor().apply {
                    cr.add(this)
                    resistance = res
                    posRef.connect(lastr)
                    lastr = negRef
                }
            }
            lastr.connect(vsr.negRef)

            parts.add(l.add(l.size, res))

            cl.step(1.0)
            cr.step(1.0)

            parts.zip(resistors).forEach { (part, resistor) ->
                assert(within_tolerable_error(part.potential, resistor.potential, 1e-9))
                assert(within_tolerable_error(part.current, resistor.current, 1e-9))
                assert(within_tolerable_error(part.power, resistor.power, 1e-9))
            }
        }
    }

    @Test
    fun lineMutation() {
        val nomr = 10.0
        val c = Circuit()
        val vs = VoltageSource().apply { potential = 5.0 }
        val l = Line()
        c.add(vs, l)
        val parta = l.add(l.size, nomr)
        val partb = l.add(l.size, nomr)
        c.step(1.0)
        assertEquals(parta.power, partb.power)
        assertEquals(parta.current, partb.current)
        assertEquals(parta.potential, partb.potential)
        val factor = 2.0
        parta.resistance = factor * nomr
        c.step(1.0)
        assertEquals(parta.power * factor, partb.power)
        assertEquals(parta.current, partb.current)
        assertEquals(parta.potential, factor * partb.potential)
    }
}
