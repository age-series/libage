package org.eln2.libelectric.sim.electrical

import org.ageseries.libage.sim.electrical.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.random.Random

fun main() {
    val b = ElectricalCircuitForestBuilder()

    val pow = PowerSource()
    val src = PotentialSource()

    b.add(pow, src)
    b.join(pow.positive, src.positive)
    b.join(pow.negative, src.negative)

    pow.maxPotential = 10.0
    pow.maxPower = 1.0
    pow.targetPower = 1.0

    src.potential = -100.0

    val c = b.build(1.0 / 100.0, true).solvers.first()
    while (true) {
        c.step()
        println()
    }
}

internal class ElectricalTests {
    private fun ElectricalCircuitForestBuilder.unique(optimization: Boolean = true) : ElectricalSimulation {
        val subSolvers = this.build(1.0 / 100.0, optimization).solvers
        assertEquals(subSolvers.size, 1)
        return subSolvers.first()
    }

    /**
     * Makes sure that the results are the same, regardless of the chosen reference node.
     * */
    @Test
    fun testGroundInvariance() {
        fun create(flag: Boolean): Triple<ElectricalSimulation, PotentialSource, Resistor> {
            val builder = ElectricalCircuitForestBuilder()
            val v1 = PotentialSource().also { it.potential = 10.0 }
            val r1 = Resistor()

            builder.add(v1, r1)
            builder.join(v1.positive, r1.positive)
            builder.join(v1.negative, r1.negative)

            if(flag) {
                builder.ground(v1.positive)
            }
            else {
                builder.ground(v1.negative)
            }

            return Triple(builder.unique(), v1, r1)
        }

        val (ca, va, ra) = create(true)
        val (cb, vb, rb) = create(false)

        repeat(10) {
            ca.step()
            cb.step()

            assertTrue(va.power > 0.0 && vb.power > 0.0)
            assertTrue(va.current == vb.current)
            assertTrue(va.potential == vb.potential)
            assertTrue(ra.current == rb.current)
        }
    }

    /**
     * Creates a singular system and makes sure the step errors.
     * */
    @Test
    fun testSingularSystem() {
        val builder = ElectricalCircuitForestBuilder()

        val v1 = PotentialSource()
        val v2 = PotentialSource()

        builder.add(v1, v2)

        builder.join(v1.positive, v2.positive)
        builder.join(v1.negative, v2.negative)

        v1.potential = 1.0
        v2.potential = 2.0

        val circuit = builder.unique()

        assertThrows<ElectricalSimulation.SingularSystemException> {
            circuit.step()
        }
    }

    /**
     * Makes sure the solver signals when invalid results are produced.
     * */
    @Test
    fun testInvalidResults() {
        val builder = ElectricalCircuitForestBuilder()

        val r = Resistor()
        val i = CurrentSource()

        builder.add(r, i)
        builder.join(r.positive, i.positive)
        builder.join(r.negative, i.negative)

        r.resistance = ElectricalSimulation.MAX_RESISTANCE
        i.current = Double.MAX_VALUE

        val circuit = builder.unique()

        assertThrows<ElectricalSimulation.InvalidResultsException> {
            circuit.step()
        }
    }

    /**
     * Tests a floating resistor (no loop) to make sure there is no current, potential over it.
     * */
    @Test
    fun floatingSystemTestResistor1() {
        val builder = ElectricalCircuitForestBuilder()

        val resistor = Resistor()

        builder.add(resistor)

        val circuit = builder.unique()

        repeat(10) {
            circuit.step()
            assertEquals(0.0, resistor.potential)
            assertEquals(0.0, resistor.current)
            assertEquals(0.0, resistor.power)
        }
    }

    /**
     * Tests a floating resistor system (no loop) to make sure there is no current, potential over it.
     * */
    @Test
    fun floatingSystemTestResistor2() {
        val builder = ElectricalCircuitForestBuilder()

        val resistor1 = Resistor()
        val resistor2 = Resistor()

        builder.add(resistor1, resistor2)

        builder.join(resistor1.positive, resistor2.positive)

        val circuit = builder.unique()

        assertEquals(circuit.components.size, 1)
        assertTrue(circuit.components.first() is ResistorSystem)

        repeat(10) {
            circuit.step()
            assertEquals(0.0, resistor1.potential)
            assertEquals(0.0, resistor1.current)
            assertEquals(0.0, resistor1.power)
            assertEquals(0.0, resistor2.potential)
            assertEquals(0.0, resistor2.current)
            assertEquals(0.0, resistor2.power)
        }
    }

    /**
     * Tests a floating inductor-capacitor-resistor system (no loop) to make sure there is no current, potential over it.
     * */
    @Test
    fun floatingSystemTestLCR() {
        val builder = ElectricalCircuitForestBuilder()

        val inductor = Inductor()
        val capacitor = Capacitor()
        val resistor = Resistor()

        builder.add(inductor, capacitor, resistor)

        builder.join(inductor.positive, capacitor.negative)
        builder.join(capacitor.positive, resistor.negative)

        val circuit = builder.unique()

        repeat(10) {
            circuit.step()
            assertEquals(0.0, inductor.potential, Double.MIN_VALUE)
            assertEquals(0.0, inductor.current, Double.MIN_VALUE)
            assertEquals(0.0, inductor.flux, Double.MIN_VALUE)
            assertEquals(0.0, capacitor.potential, Double.MIN_VALUE)
            assertEquals(0.0, capacitor.current, Double.MIN_VALUE)
            assertEquals(0.0, capacitor.charge, Double.MIN_VALUE)
            assertEquals(0.0, resistor.potential, Double.MIN_VALUE)
            assertEquals(0.0, resistor.current, Double.MIN_VALUE)
            assertEquals(0.0, resistor.power, Double.MIN_VALUE)
        }
    }

    /**
     * Tests a capacitor with incorrect initial conditions.
     * This is the [Capacitor.sanitizeState] at work.
     *
     * Without it, this test case would cause the charge and potential to grow indefinitely.
     * */
    @Test
    fun testCapacitorReconciliation() {
        val builder = ElectricalCircuitForestBuilder()

        val capacitor = Capacitor()

        builder.add(capacitor)

        val circuit = builder.unique()

        // Correct initial conditions:
        capacitor.charge = 1.0
        capacitor.lastPotential = 0.9 // Inconsistent state: should be 1.

        repeat(10) {
            circuit.step()

            // Reconciled by preserving charge.
            // This enforces the correct state V = 1:
            assertEquals(1.0, capacitor.potential)
            assertEquals(0.0, capacitor.current)
            assertEquals(1.0, capacitor.charge)
        }
    }

    /**
     * Tests an LC oscillator.
     */
    @Test
    fun testLC() {
        val builder = ElectricalCircuitForestBuilder()
        val inductor = Inductor()
        val capacitor = Capacitor()

        inductor.inductance = 1.0
        capacitor.capacitance = 1.0

        builder.add(inductor, capacitor)
        builder.join(inductor.positive, capacitor.negative)
        builder.join(inductor.negative, capacitor.positive)

        val circuit = builder.unique()

        capacitor.charge = 1.0
        capacitor.lastPotential = 1.0
        inductor.flux = 0.0

        val dt = 1.0 / 100.0
        val stepsPerPeriod = (6.28 / dt).toInt()

        val capacitorPotentials = ArrayList<Double>()
        val inductorCurrents = ArrayList<Double>()

        repeat(stepsPerPeriod * 3) {
            circuit.step()
            capacitorPotentials.add(capacitor.potential)
            inductorCurrents.add(inductor.current)
        }

        val maxCapacitorPotential = capacitorPotentials.maxOrNull()!! // Wtf?
        val minCapacitorPotential = capacitorPotentials.minOrNull()!!
        val maxInductorCurrent = inductorCurrents.maxOrNull()!!
        val minInductorCurrent = inductorCurrents.minOrNull()!!

        assertEquals(
            1.0,
            maxCapacitorPotential,
            0.1
        )

        assertEquals(
            -1.0,
            minCapacitorPotential,
            0.1
        )

        assertEquals(
            1.0,
            maxInductorCurrent,
            0.1
        )

        assertEquals(
            -1.0,
            minInductorCurrent,
            0.1,
        )

        val initialEnergy = 0.5 * capacitor.capacitance * 1.0 * 1.0
        val finalCapacitorEnergy = 0.5 * capacitor.capacitance * capacitor.potential * capacitor.potential
        val finalInductorEnergy = 0.5 * inductor.inductance * inductor.current * inductor.current
        val finalTotalEnergy = finalCapacitorEnergy + finalInductorEnergy

        assertEquals(
            initialEnergy,
            finalTotalEnergy,
            0.1
        )
    }

    /**
     * Tests line optimization is consistent with the unoptimized circuit for a normal case circuit.
     * "testResistorSystem reference image"
     * */
    @Test
    fun testResistorSystem() {
        class TestResistorSystem(
            val builder: ElectricalCircuitForestBuilder,
            val resistors: List<Resistor>,
            val optimizable: List<Resistor>,
            val independent: List<Resistor>,
            val potentials: List<PotentialSource>,
            val currents: List<CurrentSource>
        )

        fun create() : TestResistorSystem {
            val builder = ElectricalCircuitForestBuilder()
            val rng = Random((1.0 / 137.0).toBits())

            val resistors = ArrayList<Resistor>()
            val optimizable = ArrayList<Resistor>()
            val independent = ArrayList<Resistor>()

            fun resistor() : Resistor {
                val result = Resistor()
                result.resistance = rng.nextDouble(0.001, 100.0)
                builder.add(result)
                resistors.add(result)
                return result
            }

            val r1 = resistor()
            val r2 = resistor()
            val r3 = resistor()
            val r4 = resistor()
            val r5 = resistor()
            val r6 = resistor()
            val r7 = resistor()
            val r8 = resistor()
            val r9 = resistor()

            optimizable.addAll(listOf(r1, r2, r3, r4, r5, r6, r8, r9))
            independent.add(r7)

            val v1 = PotentialSource().also { it.potential = rng.nextDouble(-100.0, 100.0) }
            val i1 = CurrentSource().also { it.current = rng.nextDouble(-100.0, 100.0) }
            builder.add(v1, i1)

            // First line:
            builder.join(r1.negative, r2.negative)
            builder.join(r2.positive, r3.negative)

            // Second line:
            builder.join(r4.negative, r5.positive)
            builder.join(r5.negative, r6.negative)

            // Third line:
            builder.join(r8.positive, r9.negative)

            // Mid node:
            builder.join(r3.positive, r4.positive)
            builder.join(r3.positive, r8.negative)
            builder.join(r3.positive, r7.positive)

            // Potential to third line:
            builder.join(r9.positive, v1.positive)

            // Current to independent:
            builder.join(r7.negative, i1.positive)

            // Build N1:
            builder.join(v1.negative, r1.positive)
            builder.join(r1.positive, i1.negative)

            // Build N2:
            builder.join(v1.negative, r6.positive)
            builder.join(r6.positive, i1.negative)

            // All done
            return TestResistorSystem(
                builder,
                resistors, optimizable, independent,
                listOf(v1), listOf(i1)
            )
        }

        val optimizedData = create()
        val unoptimizedData = create()

        val optimizedCircuit = optimizedData.builder.unique(true)
        val unoptimizedCircuit = unoptimizedData.builder.unique(false)

        optimizedData.optimizable.forEach {
            assertTrue(it.isInOptimizedSystem)
        }

        optimizedData.independent.forEach {
            assertTrue(!it.isInOptimizedSystem)
        }

        unoptimizedData.resistors.forEach {
            assertTrue(!it.isInOptimizedSystem)
        }

        repeat(10) {
            optimizedCircuit.step()
            unoptimizedCircuit.step()

            for(i in 0 until optimizedData.resistors.size) {
                val a = optimizedData.resistors[i]
                val b = unoptimizedData.resistors[i]
                assertEquals(a.potential, b.potential, 1e-5)
                assertEquals(a.current, b.current, 1e-5)
                assertEquals(a.power, b.power, 1e-5)
            }

            for (i in 0 until optimizedData.potentials.size) {
                val a = optimizedData.potentials[i]
                val b = unoptimizedData.potentials[i]
                assertEquals(a.current, b.current, 1e-5)
                assertEquals(a.power, b.power, 1e-5)
            }

            for (i in 0 until optimizedData.currents.size) {
                val a = optimizedData.currents[i]
                val b = unoptimizedData.currents[i]
                assertEquals(a.potential, b.potential, 1e-5)
                assertEquals(a.power, b.power, 1e-5)
            }

            val resistanceFactors = optimizedData.resistors.map {
                Random.nextDouble(0.9, 1.1)
            }

            val potentialFactors = optimizedData.potentials.map {
                Random.nextDouble(0.9, 1.1)
            }

            val currentFactors = optimizedData.currents.map {
                Random.nextDouble(0.9, 1.1)
            }

            for(i in 0 until optimizedData.resistors.size) {
                optimizedData.resistors[i].resistance *= resistanceFactors[i]
                unoptimizedData.resistors[i].resistance *= resistanceFactors[i]
            }

            for(i in 0 until optimizedData.potentials.size) {
                optimizedData.potentials[i].potential *= potentialFactors[i]
                unoptimizedData.potentials[i].potential *= potentialFactors[i]
            }

            for(i in 0 until optimizedData.currents.size) {
                optimizedData.currents[i].current *= currentFactors[i]
                unoptimizedData.currents[i].current *= currentFactors[i]
            }
        }
    }

    /**
     * Ensures the line optimizer can handle a cycle made of 2 resistors in parallel.
     * */
    @Test
    fun testResistorSystemEdgeCase1() {
        val builder = ElectricalCircuitForestBuilder()

        val r1 = Resistor()
        val r2 = Resistor()

        builder.add(r1, r2)

        builder.join(r1.positive, r2.positive)
        builder.join(r1.negative, r2.negative)

        builder.unique(true)

        assertTrue(!r1.isInOptimizedSystem && !r2.isInOptimizedSystem)
    }

    /**
     * Ensures the line optimizer can handle a cycle made of 3 resistors in a loop.
     * */
    @Test
    fun testResistorSystemEdgeCase2() {
        val builder = ElectricalCircuitForestBuilder()

        val r1 = Resistor()
        val r2 = Resistor()
        val r3 = Resistor()

        builder.add(r1, r2, r3)

        builder.join(r1.positive, r2.negative)
        builder.join(r2.positive, r3.negative)
        builder.join(r3.positive, r1.negative)

        builder.unique(true)

        assertTrue(!r1.isInOptimizedSystem && !r2.isInOptimizedSystem && !r3.isInOptimizedSystem)
    }

    /**
     * Ensures the line optimizer can handle a more complicated cycle case.
     * */
    @Test
    fun testResistorSystemEdgeCase3() {
        val builder = ElectricalCircuitForestBuilder()

        val r1 = Resistor()
        val r2 = Resistor()
        val r3 = Resistor()
        val r4 = Resistor()
        val r5 = Resistor()

        builder.add(r1, r2, r3, r4, r5)

        builder.join(r1.positive, r3.negative)
        builder.join(r2.positive, r4.positive)

        builder.join(r1.negative, r2.negative)
        builder.join(r1.negative, r5.negative)

        builder.join(r3.positive, r5.positive)
        builder.join(r5.positive, r4.negative)

        builder.ground(r1.negative)

        builder.unique(true).also {
            assertTrue(it.components.size == 3)
        }

        assertTrue(r1.isInOptimizedSystem && r3.isInOptimizedSystem && r1.resistorSystem == r3.resistorSystem)
        assertTrue(r2.isInOptimizedSystem && r4.isInOptimizedSystem && r2.resistorSystem == r4.resistorSystem)
        assertTrue(!r5.isInOptimizedSystem)
    }

    /**
     * Ensures the line optimizer doesn't accidentally optimize a line with a ground in it.
     * */
    @Test
    fun testResistorSystemGrounding() {
        val builder = ElectricalCircuitForestBuilder()

        val r1 = Resistor()
        val r2 = Resistor()
        val r3 = Resistor()
        val r4 = Resistor()

        builder.add(r1, r2, r3, r4)

        builder.join(r1.negative, r2.positive)
        builder.join(r2.negative, r3.positive)
        builder.join(r3.negative, r4.positive)

        // Generate 2 lines:
        builder.ground(r2.negative)

        val circuit = builder.unique(true)

        assertTrue(circuit.components.size == 2 && circuit.components.all { it is ResistorSystem })
        assertTrue(r1.isInOptimizedSystem && r2.isInOptimizedSystem && r1.resistorSystem == r2.resistorSystem)
        assertTrue(r3.isInOptimizedSystem && r4.isInOptimizedSystem && r3.resistorSystem == r4.resistorSystem)
        assertTrue(r1.resistorSystem != r3.resistorSystem)
    }

    /**
     * Makes sure the power source respects the potential constraint.
     * */
    @Test
    fun testPowerSourcePotentialConstraint() {
        val builder = ElectricalCircuitForestBuilder()

        val ps = PowerSource()
        val load = Resistor()

        builder.add(ps, load)
        builder.join(ps.positive, load.positive)
        builder.join(ps.negative, load.negative)

        ps.maxPower = 1000.0
        ps.maxPotential = 100.0
        ps.targetPower = 1000.0
        load.resistance = 1e5 // So the max power isn't reached

        val circuit = builder.unique()

        repeat(10) {
            circuit.step()

            assertTrue(ps.power > 0.0 && ps.potential < ps.maxPotential) // blend region
            assertTrue(load.power > 0.0)
        }
    }

    @Test
    fun testCapacitorMoveSimulation() {
        val cap = Capacitor()

        var builder = ElectricalCircuitForestBuilder()
        builder.add(cap)

        cap.charge = 1.0

        val c1 = builder.unique()
        c1.step()

        assertEquals(cap.potential, 1.0)
        assertEquals(cap.charge, 1.0)

        c1.destroy()

        builder = ElectricalCircuitForestBuilder()
        builder.add(cap)

        val c2 = builder.unique()
        c2.step()

        assertEquals(cap.potential, 1.0)
        assertEquals(cap.charge, 1.0)
    }

    /**
     * Tests state reset on simulation change for power sources.
     * */
    @Test
    fun testPowerSourceMoveSimulation() {
        val src = PowerSource()

        var builder = ElectricalCircuitForestBuilder()
        builder.add(src)

        src.maxPower = 800.0
        src.maxPotential = 32.0
        src.targetPower = 800.0

        val c1 = builder.unique()
        c1.step()

        assertEquals(src.potential, src.potential)
        assertEquals(src.power, 0.0)

        c1.destroy()

        builder = ElectricalCircuitForestBuilder()
        builder.add(src)

        val c2 = builder.unique()
        c2.step()

        assertEquals(src.potential, src.potential)
        assertEquals(src.power, 0.0)
    }
}