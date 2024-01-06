@file:Suppress("UNUSED_VARIABLE")

package org.eln2.libelectric.sim.electrical.mna

import org.ageseries.libage.mathematics.approxEq
import org.ageseries.libage.sim.electrical.mna.*
import org.ageseries.libage.sim.electrical.mna.component.Line
import org.ageseries.libage.sim.electrical.mna.component.Resistor
import org.ageseries.libage.sim.electrical.mna.component.Term
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException

internal class CircuitBuilderTest {
    @Test
    fun testRealUnion() {
        val a = LineCompiler.Pin()

        assertFalse(a.hasRealsInternal)

        val b = LineCompiler.Pin()

        a.unite(b)

        assertFalse(a.hasRealsInternal)
        assertFalse(b.hasRealsInternal)

        val c = LineCompiler.Pin()
        val d = LineCompiler.Pin()

        d.markReal()
        d.unite(c)

        assertTrue(c.representative.hasRealsInternal)
        assertTrue(d.representative.hasRealsInternal)

        c.unite(a)

        listOf(a, b, c, d).forEach {
            assertTrue(it.representative.hasRealsInternal)
            assertEquals(it.representative, a.representative)
        }
    }

    @Test
    fun testBreakPoint() {
        let {
            val a = LineCompiler.Pin()
            assertTrue(a.isBreakPoint) // only 1

            val b = LineCompiler.Pin()

            a.unite(b)

            assertFalse(a.isBreakPoint)
            assertFalse(b.isBreakPoint)

            b.markReal()

            assertTrue(a.isBreakPoint && a.representative.hasRealsInternal)
            assertTrue(b.isBreakPoint && b.representative.hasRealsInternal)

            val c = LineCompiler.Pin()
            c.unite(b)

            assertTrue(c.isBreakPoint && c.representative.hasRealsInternal)
        }

        let {
            val a = LineCompiler.Pin()
            val b = LineCompiler.Pin()
            a.unite(b)

            assertFalse(a.isBreakPoint)
            assertFalse(b.isBreakPoint)

            val c = LineCompiler.Pin()
            c.unite(a)

            listOf(a, b, c).forEach {
                assertTrue(it.isBreakPoint) // 3
            }
        }

    }

    private fun runTest(action: (CircuitBuilder) -> Unit) {
        val circuit = Circuit()
        val builder = CircuitBuilder(circuit)
        action(builder)
    }

    @Test
    fun validation() {
        fun<T : Term> test(v1: T, v2: T) {
            runTest {
                assertThrows<IllegalArgumentException> { it.connect(v1, 0, v1, 0) }
                assertThrows<IllegalArgumentException> { it.connect(v1, 0, v2, 0) }

                assertTrue(it.add(v1))
                assertFalse(it.add(v1))

                assertThrows<IllegalArgumentException> { it.connect(v1, 0, v1, 0) }
                assertThrows<IllegalArgumentException> { it.connect(v1, 0, v2, 0) }

                assertTrue(it.add(v2))
                assertFalse(it.add(v2))

                assertThrows<IllegalArgumentException> { it.connect(v1, 0, v1, 0) }
                assertDoesNotThrow { it.connect(v1, 0, v2, 0) }

                it.build()

                assertThrows<IllegalStateException> { it.build() }
                assertThrows<IllegalStateException> { it.connect(v1, 1, v2, 1) }
                assertThrows<IllegalStateException> { it.add(v1) }

            }
        }

        test(VirtualResistor(), VirtualResistor())
        test(Resistor(), Resistor())
        test(VirtualResistor(), Resistor())
        test(Resistor(), VirtualResistor())
    }

    @Test
    fun simpleCases() {
        runTest {
            it.build()
            assertTrue(it.lineCompiler.lineGraphs.isEmpty())
            assertEquals(it.circuit.components.size, 0)
        }

        runTest {
            val r = Resistor()
            it.add(r)
            it.build()
            assertTrue(it.lineCompiler.lineGraphs.isEmpty())
            assertEquals(it.circuit.components.size, 1)
            assertEquals(it.circuit.components.first(), r)
        }

        runTest {
            val v = VirtualResistor()
            v.resistance = 3141.0

            it.add(v)
            it.build()

            assertEquals(it.lineCompiler.lineGraphs.size, 1)
            assertEquals(it.circuit.components.size, 1)

            val graph = it.lineCompiler.lineGraphs.first()
            assertTrue(graph.outers.contains(v) && graph.outers.size == 1)
            assertTrue(graph.inners.isEmpty())
            assertEquals(graph.anchor, null)

            it.lineCompiler.virtualResistors.values.forEach { (p, n) ->
                assertTrue(!p.representative.hasRealsInternal && !n.representative.hasRealsInternal)
            }

            val component = it.circuit.components.first() as Line

            assertTrue(component.isInCircuit && component.resistance == 3141.0 && component.parts.contains(v.part!!))
        }

        runTest {
            val v1 = VirtualResistor()
            val v2 = VirtualResistor()
            v1.resistance = 31.0
            v2.resistance = 41.0

            it.add(v1)
            it.add(v2)
            it.build()

            assertEquals(it.lineCompiler.lineGraphs.size, 2)
            assertEquals(it.circuit.components.size, 2)

            assertTrue(it.circuit.components.any { c -> c is Line && c.resistance == 31.0})
            assertTrue(it.circuit.components.any { c -> c is Line && c.resistance == 41.0})
        }

        runTest {
            val v1 = VirtualResistor()
            val v2 = VirtualResistor()
            v1.resistance = 31.0
            v2.resistance = 41.0

            it.add(v1)
            it.add(v2)
            it.connect(v1, POSITIVE, v2, POSITIVE)
            it.build()

            assertEquals(it.lineCompiler.lineGraphs.size, 1)
            assertEquals(it.circuit.components.size, 1)

            val graph = it.lineCompiler.lineGraphs.first()
            assertTrue(graph.outers.contains(v1) && graph.outers.contains(v2) && graph.outers.size == 2)
            assertTrue(graph.inners.isEmpty())
            assertEquals(graph.anchor, graph.outers.toList().last())

            it.lineCompiler.virtualResistors.values.forEach { (p, n) ->
                assertTrue(!p.representative.hasRealsInternal && !n.representative.hasRealsInternal)
            }

            val component = it.circuit.components.first() as Line
            assertEquals(component.resistance, 31.0 + 41.0)
        }

        runTest {
            val v1 = VirtualResistor()
            val v2 = VirtualResistor()
            val v3 = VirtualResistor()
            v1.resistance = 31.0
            v2.resistance = 41.0
            v3.resistance = 59.0

            it.add(v1)
            it.add(v2)
            it.add(v3)
            it.connect(v1, POSITIVE, v2, POSITIVE)
            it.connect(v2, NEGATIVE, v3, POSITIVE)
            it.build()

            assertEquals(it.lineCompiler.lineGraphs.size, 1)
            assertEquals(it.circuit.components.size, 1)

            val graph = it.lineCompiler.lineGraphs.first()
            assertTrue(graph.outers.contains(v1) && graph.outers.contains(v3) && graph.outers.size == 2)
            assertTrue(graph.inners.contains(v2) && graph.inners.size == 1)
            assertEquals(graph.anchor, v2)

            val component = it.circuit.components.first() as Line
            assertEquals(component.resistance, 31.0 + 41.0 + 59.0)
        }

        runTest {
            var last: VirtualResistor? = null

            for (resistance in 1..50) {
                val v = VirtualResistor()

                it.add(v)

                v.resistance = resistance.toDouble()

                if(last != null) {
                    it.connect(v, NEGATIVE, last, POSITIVE)
                }

                last = v
            }

            it.build()

            assertTrue(it.lineCompiler.lineGraphs.size == 1)
            assertTrue(it.circuit.components.size == 1)

            val component = it.circuit.components.first() as Line
            assertTrue(component.resistance approxEq (50 * (50 + 1) / 2).toDouble())
        }

        runTest {
            val v1 = VirtualResistor()
            val v2 = VirtualResistor()
            val v3 = VirtualResistor()
            val R = Resistor()
            v1.resistance = 31.0
            v2.resistance = 41.0
            v3.resistance = 59.0

            it.add(v1)
            it.add(v2)
            it.add(v3)
            it.connect(v1, POSITIVE, v2, POSITIVE)
            it.connect(v2, NEGATIVE, v3, POSITIVE)
            it.add(R)
            it.connect(v2, NEGATIVE, R, NEGATIVE)
            it.build()

            assertEquals(it.lineCompiler.lineGraphs.size, 2)
            assertEquals(it.circuit.components.size, 3)

            assertTrue(it.circuit.components.count { c -> c is Line } == 2)
            assertTrue(it.circuit.components.count { c -> c is Resistor && c !is Line } == 1)

            val (p, n) = it.lineCompiler.virtualResistors[v2]!!
            assertTrue(n.representative.hasRealsInternal && n.isBreakPoint)
        }
    }
}