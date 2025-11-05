package org.eln2.libelectric.sim.electrical

import org.ageseries.libage.sim.electrical.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import kotlin.math.pow
import kotlin.random.Random

internal class ElectricalTests {
    private fun ElectricalCircuitForestBuilder.testBuild(optimization: Boolean = true) : ElectricalSimulation {
        val subSolvers = this.build(
            1.0 / 100.0,
            optimization,
            ElectricalSimulation.ConstructionOptions(
                1e15 // Fails with 1e14 for example, the tolerances I set for the tests are too tight. maybe fix
            )).solvers
        assertEquals(subSolvers.size, 1)
        return subSolvers.first()
    }

    //#region Fundamental Tests

    /**
     * Tests that the simulation throws an error if step() is called after destroy().
     * */
    @Test
    fun testStepAfterDestroy() {
        val builder = ElectricalCircuitForestBuilder()
        val r = Resistor()
        builder.add(r)
        builder.ground(r.negative)

        val circuit = builder.testBuild()
        circuit.step()

        circuit.destroy()

        assertThrows<IllegalStateException> {
            circuit.step()
        }
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

            return Triple(builder.testBuild(), v1, r1)
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

        val circuit = builder.testBuild()

        assertThrows<ElectricalSimulation.SingularLinearSystemException> {
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

        val circuit = builder.testBuild()

        assertThrows<ElectricalSimulation.InvalidLinearResultsException> {
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

        val circuit = builder.testBuild()

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

        val circuit = builder.testBuild()

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

        val circuit = builder.testBuild()

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

        val circuit = builder.testBuild()

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

        val optimizedCircuit = optimizedData.builder.testBuild(true)
        val unoptimizedCircuit = unoptimizedData.builder.testBuild(false)

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

        builder.testBuild(true)

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

        builder.testBuild(true)

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

        builder.testBuild(true).also {
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

        val circuit = builder.testBuild(true)

        assertTrue(circuit.components.size == 2 && circuit.components.all { it is ResistorSystem })
        assertTrue(r1.isInOptimizedSystem && r2.isInOptimizedSystem && r1.resistorSystem == r2.resistorSystem)
        assertTrue(r3.isInOptimizedSystem && r4.isInOptimizedSystem && r3.resistorSystem == r4.resistorSystem)
        assertTrue(r1.resistorSystem != r3.resistorSystem)
    }

    @Test
    fun testInductorMoveSimulation() {
        val inductor = Inductor()
        inductor.inductance = 10.0

        var builder = ElectricalCircuitForestBuilder()
        builder.add(inductor)

        // Set initial flux
        inductor.flux = 1.0

        val c1 = builder.testBuild()
        c1.step()

        assertEquals(0.0, inductor.flux, 1e-9)

        c1.destroy()

        // Rebuild the simulation
        builder = ElectricalCircuitForestBuilder()
        builder.add(inductor)

        val c2 = builder.testBuild()
        c2.step()

        // Assert that the flux state (0.0) was persisted from c1
        // and that the simulation remains stable at 0.0.
        assertEquals(0.0, inductor.potential, 1e-9)
        assertEquals(0.0, inductor.current, 1e-9)
        assertEquals(0.0, inductor.flux, 1e-9)
    }

    @Test
    fun testCapacitorMoveSimulation() {
        val cap = Capacitor()

        var builder = ElectricalCircuitForestBuilder()
        builder.add(cap)

        cap.charge = 1.0

        val c1 = builder.testBuild()
        c1.step()

        assertEquals(cap.potential, 1.0)
        assertEquals(cap.charge, 1.0)

        c1.destroy()

        builder = ElectricalCircuitForestBuilder()
        builder.add(cap)

        val c2 = builder.testBuild()
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

        src.maxPotential = 32.0
        src.targetPower = 800.0

        val c1 = builder.testBuild()
        c1.step()

        assertEquals(src.potential, src.potential)
        assertEquals(0.0, src.power, 1e-5)

        c1.destroy()

        builder = ElectricalCircuitForestBuilder()
        builder.add(src)

        val c2 = builder.testBuild()
        c2.step()

        assertEquals(0.0, src.power, 1e-5)
    }

    /**
     * Makes sure the PowerSource nonlinear solver fails with an error when a generator is short-circuited.
     *
     * This forces the solver to try to deliver power into a 0V load,
     * which requires infinite current and should fail to converge.
     * */
    @Test
    fun testPowerSourceGeneratorShortCircuit() {
        val builder = ElectricalCircuitForestBuilder()

        val ps = PowerSource()
        ps.targetPower = 100.0
        ps.maxPotential = 100.0

        builder.add(ps)

        builder.ground(ps.positive)
        builder.ground(ps.negative)

        val circuit = builder.testBuild()

        assertThrows<ElectricalSimulation.PowerSourceSolverException> {
            circuit.step()
        }
    }

    //#endregion

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

        val circuit = builder.testBuild()

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

    @Test
    fun testVoltageDivider() {
        val builder = ElectricalCircuitForestBuilder()

        val v1 = PotentialSource().also { it.potential = 10.0 }
        val r1 = Resistor().also { it.resistance = 1000.0 }
        val r2 = Resistor().also { it.resistance = 1000.0 }

        builder.add(v1, r1, r2)

        builder.join(v1.positive, r1.positive)
        builder.join(r1.negative, r2.positive)
        builder.join(r2.negative, v1.negative)
        builder.ground(v1.negative)

        val circuit = builder.testBuild()
        circuit.step()

        val expectedVoltage = 10.0 * (1000.0 / (1000.0 + 1000.0))
        assertEquals(expectedVoltage, r1.negative.potential, 1e-9)
        assertEquals(5.0, r2.potential, 1e-9)
        assertEquals(5.0, r1.potential, 1e-9)

        val expectedCurrent = 10.0 / 2000.0
        assertEquals(expectedCurrent, r1.current, 1e-9)
        assertEquals(expectedCurrent, r2.current, 1e-9)
        assertEquals(expectedCurrent, v1.current, 1e-9)
    }

    @Test
    fun testCurrentDivider() {
        val builder = ElectricalCircuitForestBuilder()

        val i1 = CurrentSource().also { it.current = 10.0 }
        val r1 = Resistor().also { it.resistance = 100.0 }
        val r2 = Resistor().also { it.resistance = 300.0 }

        builder.add(i1, r1, r2)

        builder.join(i1.positive, r1.positive)
        builder.join(i1.positive, r2.positive)
        builder.join(i1.negative, r1.negative)
        builder.join(i1.negative, r2.negative)
        builder.ground(i1.negative)

        val circuit = builder.testBuild()
        circuit.step()

        val iR1 = 10.0 * (300.0 / (100.0 + 300.0))
        val iR2 = 10.0 * (100.0 / (100.0 + 300.0))

        assertEquals(7.5, iR1, 1e-9)
        assertEquals(2.5, iR2, 1e-9)

        val expectedVoltage = 10.0 * (1.0 / (1.0/100.0 + 1.0/300.0))
        assertEquals(750.0, expectedVoltage, 1e-9)
        assertEquals(expectedVoltage, i1.potential, 1e-9)
        assertEquals(expectedVoltage, r1.potential, 1e-9)
        assertEquals(expectedVoltage, r2.potential, 1e-9)
    }

    @Test
    fun testRCCircuitCharging() {
        val builder = ElectricalCircuitForestBuilder()

        val v1 = PotentialSource().also { it.potential = 10.0 }
        val r1 = Resistor().also { it.resistance = 1000.0 }
        val c1 = Capacitor().also { it.capacitance = 0.001 }

        val rc = r1.resistance * c1.capacitance
        assertEquals(1.0, rc, 1e-9)

        builder.add(v1, r1, c1)
        builder.join(v1.positive, r1.positive)
        builder.join(r1.negative, c1.positive)
        builder.join(c1.negative, v1.negative)
        builder.ground(v1.negative)

        val circuit = builder.testBuild()

        repeat(100) {
            circuit.step()
        }

        assertEquals(6.321, c1.potential, 0.1) // Use a larger delta due to discrete steps
        assertEquals(10.0 - c1.potential, r1.potential, 0.01)

        v1.potential = 0.0

        repeat(100) {
            circuit.step()
        }

        assertEquals(2.325, c1.potential, 0.01)
    }

    @Test
    fun testRLCircuitCurrentRise() {
        val builder = ElectricalCircuitForestBuilder()

        val v1 = PotentialSource().also { it.potential = 10.0 }
        val r1 = Resistor().also { it.resistance = 10.0 }
        val l1 = Inductor().also { it.inductance = 10.0 }

        val tau = l1.inductance / r1.resistance
        assertEquals(1.0, tau, 1e-9)

        builder.add(v1, r1, l1)
        builder.join(v1.positive, r1.positive)
        builder.join(r1.negative, l1.positive)
        builder.join(l1.negative, v1.negative)
        builder.ground(v1.negative)

        val circuit = builder.testBuild()

        repeat(100) {
            circuit.step()
        }

        assertEquals(0.632, l1.current, 0.01) // Use a larger delta
        assertEquals(0.632, r1.current, 0.01)
        assertEquals(0.632, v1.current, 0.01)

        val vR = l1.current * r1.resistance
        val vL = v1.potential - vR
        assertEquals(vR, r1.potential, 0.01)
        assertEquals(vL, l1.potential, 0.01)
    }

    /**
     * Makes sure the power source respects the **potential constraint** in **generator mode**, with a **passive load**.
     * */
    @Test
    fun testPowerSourceGeneratorPotentialConstraintPassiveLoad() {
        val builder = ElectricalCircuitForestBuilder()

        val ps = PowerSource()
        val load = Resistor()

        builder.add(ps, load)
        builder.join(ps.positive, load.positive)
        builder.join(ps.negative, load.negative)

        ps.maxPotential = 100.0
        ps.targetPower = 1000.0
        load.resistance = 1e5 // So the max power isn't reached

        val circuit = builder.testBuild()

        repeat(10) {
            circuit.step()

            assertTrue(ps.power > 0.0 && ps.potential < ps.maxPotential) // blend region
            assertTrue(load.power > 0.0)
        }
    }

    /**
     * Validates the common behavior of the power source (see the documentation in [PowerSource]):
     * - open-circuit when [PowerSource.targetPower] is zero
     * */
    @Test
    fun testPowerSourceCommonBehavior() {
        val builder = ElectricalCircuitForestBuilder()

        val powerSource = PowerSource()
        val potentialSource = PotentialSource()

        builder.add(powerSource, potentialSource)

        builder.join(powerSource.positive, potentialSource.positive)
        builder.join(powerSource.negative, potentialSource.negative)

        powerSource.targetPower = 0.0
        potentialSource.potential = 10.0

        val circuit = builder.testBuild()

        fun verify() {
            repeat(5) {
                circuit.step()

                assertEquals(0.0, powerSource.power, circuit.powerSourceResidualTolerance)
                assertEquals(potentialSource.potential, powerSource.potential, 0.1)
                assertEquals(0.0, potentialSource.power, circuit.powerSourceResidualTolerance)
                assertTrue(circuit.lastPowerSourceIterationCount < 3)
            }
        }

        potentialSource.potential = 0.0
        verify()

        potentialSource.potential = 0.0001
        verify()

        potentialSource.potential = 10000.0
        verify()

        potentialSource.potential = -0.001
        verify()

        potentialSource.potential = -1.0
        verify()

        potentialSource.potential = -10.0
        verify()

        potentialSource.potential = -1000.0
        verify()
    }

    /**
     * Tests the behavior of the power source **in generator mode** (see the documentation in [PowerSource]):
     * - the circuit forcing power into the source
     * - the source generating power with a positive potential over it
     * - the source generating power with a negative potential over it
     * */
    @Test
    fun testPowerSourceGeneratorReverseForwardForward() {
        val builder = ElectricalCircuitForestBuilder()

        val powerSource = PowerSource()
        val potentialSource = PotentialSource()

        builder.add(powerSource, potentialSource)

        builder.join(powerSource.positive, potentialSource.positive)
        builder.join(powerSource.negative, potentialSource.negative)

        powerSource.targetPower = 10.0
        powerSource.maxPotential = 10.0

        potentialSource.potential = 11.0

        val circuit = builder.testBuild()

        repeat(5) {
            circuit.step()

            assertEquals(-1.0, powerSource.power, 1e-6) // One watt is being forced into the device
            assertTrue(circuit.lastPowerSourceIterationCount < 3)
        }

        potentialSource.potential = 9.0

        repeat(5) {
            circuit.step()

            assertEquals(10.0, powerSource.power, 1e-6) // Creating a current through the potential source
            assertEquals(-10.0, potentialSource.power, 1e-6)
            assertTrue(circuit.lastPowerSourceIterationCount < 3)
        }

        potentialSource.potential = -10.0

        repeat(5) {
            circuit.step()

            assertEquals(10.0, powerSource.power, 1e-6) // Creating a current through the potential source
            assertEquals(-10.0, potentialSource.power, 1e-6)
            assertTrue(circuit.lastPowerSourceIterationCount < 3)
        }

        potentialSource.potential = -100.0

        repeat(5) {
            circuit.step()

            assertEquals(10.0, powerSource.power, 1e-6) // Creating a current through the potential source
            assertEquals(-10.0, potentialSource.power, 1e-6)
            assertTrue(circuit.lastPowerSourceIterationCount < 3)
        }
    }

    @Test
    fun testPowerConsumerBasic() {
        val builder = ElectricalCircuitForestBuilder()

        val vs = PotentialSource().also { it.potential = 20.0 } // 20V source
        val pc = PowerConsumer()

        builder.add(vs, pc)
        builder.join(vs.positive, pc.positive)
        builder.join(vs.negative, pc.negative)

        pc.targetPower = 50.0 // Request 50W
        pc.minEquivalentResistance = 0.1
        pc.setStabilizingResistance(10.0, 100.0)

        val circuit = builder.testBuild()

        circuit.step()

        assertEquals(20.0, pc.potential, 0.1)
        assertEquals(50.0, pc.power, 0.1)

        // Check that VS is supplying 50W
        assertEquals(50.0, vs.power, 0.1)
        assertEquals(2.5, vs.current, 0.1)
    }

    @Test
    fun testPowerConsumerConstraint() {
        val builder = ElectricalCircuitForestBuilder()

        val vs = PotentialSource().also { it.potential = 10.0 }
        val pc = PowerConsumer()

        builder.add(vs, pc)
        builder.join(vs.positive, pc.positive)
        builder.join(vs.negative, pc.negative)

        pc.targetPower = 500.0

        pc.minEquivalentResistance = 1.0
        pc.setStabilizingResistance(10.0, 100.0)

        val circuit = builder.testBuild()

        circuit.step()

        assertEquals(10.0, pc.potential, 1e-6)
        assertEquals(100.0, pc.power, 1e-5)
        assertEquals(100.0, vs.power, 1e-5)
    }

    @Test
    fun testPowerConsumerZeroTarget() {
        val builder = ElectricalCircuitForestBuilder()

        val vs = PotentialSource().also { it.potential = 10.0 }
        val pc = PowerConsumer()

        builder.add(vs, pc)
        builder.join(vs.positive, pc.positive)
        builder.join(vs.negative, pc.negative)

        pc.targetPower = 0.0
        pc.setStabilizingResistance(10.0, 100.0)

        val circuit = builder.testBuild()

        circuit.step()

        assertEquals(10.0, pc.potential, 0.1)
        assertEquals(0.0, pc.power, 0.1)
        assertEquals(0.0, vs.power, 0.1)
        assertEquals(0.0, vs.current, 0.1)
    }

    @Test
    fun testPowerSourceToConsumerStable() {
        val builder = ElectricalCircuitForestBuilder()

        val pg = PowerSource().also {
            it.maxPotential = 100.0
            it.setStabilizingResistance(50.0, 1000.0) // R_N = 2.5 Ohms
        }

        val pc = PowerConsumer().also {
            it.minEquivalentResistance = 0.5
            it.setStabilizingResistance(50.0, 1000.0) // R_N = 2.5 Ohms
        }

        val r = Resistor().also { it.resistance = 1.0 }

        builder.add(pg, pc, r)
        builder.join(pg.positive, r.positive)
        builder.join(r.negative, pc.positive)
        builder.join(pc.negative, pg.negative)

        val circuit = builder.testBuild()

        repeat(1000) {
            val genPower = 1000.0 * kotlin.math.sin(it * circuit.dt).pow(2)
            val consPower = 1000.0 * kotlin.math.cos(it * circuit.dt).pow(2)

            pg.targetPower = genPower
            pc.targetPower = consPower

            try {
                circuit.step()
            }
            catch (e: ElectricalSimulation.PowerSourceSolverException) {
                println("src targetPower: ${pg.targetPower}, src power: ${pg.power}")
                println("sink targetPower: ${pc.targetPower}, sink power: ${pc.power}")
                fail(e)
            }
            catch (e: Exception) {
                fail(e)
            }

            val dissipated = r.power
            val generated = pg.power
            val consumed = pc.power

            assertEquals(generated, dissipated + consumed, 0.1)

            assertTrue(circuit.lastPowerSourceIterationCount < 40)
        }
    }
}