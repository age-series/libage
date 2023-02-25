package org.eln2.libelectric.sim.thermal

import org.ageseries.libage.sim.constant.Material
import org.ageseries.libage.sim.thermal.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ThermalTests {
    companion object {
        val A_BIT = Temperature(10.0)
    }
    @JvmInline
    value class Loc(val x: Int)

    class TestBody(override val locator: Loc, override val mass: ThermalMass): ThermalBody<Loc> {
        override val surfaceArea: Double
            get() = 1.0
    }

    class TestEnv(
        val conduct: Double = 1.0,
        val rightTemp: Temperature = STANDARD_TEMPERATURE,
        val leftTemp: Temperature = STANDARD_TEMPERATURE,
        val rightPoint: Loc = Loc(0),
    ): ThermalEnvironment<Loc> {
        override fun temperature(locator: Loc): Temperature =
            if(locator.x < rightPoint.x) {
                leftTemp
            } else {
                rightTemp
            }

        override fun conductance(locator: Loc): Double = conduct
    }

    @Test
    fun consistent_data_model() {
        val sim = ThermalSimulator(TestEnv(0.0))
        val a = TestBody(Loc(0), ThermalMass(Material.IRON))
        val b = TestBody(Loc(0), ThermalMass(Material.IRON))
        val c = TestBody(Loc(0), ThermalMass(Material.IRON))
        assert(a !in sim.bodies)
        assert(b !in sim.bodies)
        assert(c !in sim.bodies)
        val conn = sim.connect(a, b)
        assert(conn in sim.connections)
        assertEquals(conn.a, a)
        assertEquals(conn.b, b)
        assert(a in sim.bodies)
        assert(b in sim.bodies)
        assert(c !in sim.bodies)
        sim.remove(conn)
        assert(conn !in sim.connections)
        assert(a in sim.bodies)
        assert(b in sim.bodies)
        assert(c !in sim.bodies)
        listOf(a, b).forEach { sim.remove(it) }
        assert(a !in sim.bodies)
        assert(b !in sim.bodies)
        assert(c !in sim.bodies)
        val conn2 = sim.connect(b, c)
        assert(conn2 in sim.connections)
        assert(a !in sim.bodies)
        assert(b in sim.bodies)
        assert(c in sim.bodies)
        sim.remove(b)
        assert(conn2 !in sim.connections)
        assert(a !in sim.bodies)
        assert(b !in sim.bodies)
        assert(c in sim.bodies)
    }

    @Test
    fun perfectly_isolated() {
        val sim = ThermalSimulator(TestEnv(0.0))
        val a = TestBody(Loc(1), ThermalMass(Material.IRON))
        val b = TestBody(Loc(2), ThermalMass(Material.IRON))
        a.mass.temperature = STANDARD_TEMPERATURE
        b.mass.temperature = STANDARD_TEMPERATURE + A_BIT
        listOf(a, b).forEach { sim.add(it) }
        val aOld = a.mass.temperature
        val bOld = b.mass.temperature
        sim.step(0.05)
        assert(a in sim.bodies)
        assert(b in sim.bodies)
        assertEquals(a.mass.temperature, aOld)
        assertEquals(b.mass.temperature, bOld)
    }

    @Test
    fun second_law() {
        val sim = ThermalSimulator(TestEnv(0.0))  // Don't conduct from/to env
        val a = TestBody(Loc(1), ThermalMass(Material.IRON))
        val b = TestBody(Loc(2), ThermalMass(Material.IRON))
        val lesser = STANDARD_TEMPERATURE
        val greater = STANDARD_TEMPERATURE + A_BIT
        listOf(lesser to greater, greater to lesser).forEach { (a_temp, b_temp) ->
            a.mass.temperature = a_temp
            b.mass.temperature = b_temp
            val aOld = a.mass.temperature
            val bOld = b.mass.temperature
            val conn = sim.connect(a, b)
            sim.step(0.05)
            assertEquals(a_temp.compareTo(b_temp), aOld.compareTo(a.mass.temperature)) {"A old $aOld, now ${a.mass.temperature}" }
            assertEquals(b_temp.compareTo(a_temp), bOld.compareTo(b.mass.temperature)) {"B old $bOld, now ${b.mass.temperature}" }
        }
    }

    @Test
    fun conductivity_proportional_to_connectivity() {
        var lastIncrease: Temperature? = null
        for(it in 1..10) {
            val sim = ThermalSimulator(TestEnv(0.0))
            val cen = TestBody(Loc(0), ThermalMass(Material.IRON))
            cen.mass.temperature = STANDARD_TEMPERATURE
            val others = (1..it).map {
                TestBody(Loc(it), ThermalMass(Material.IRON)).also {
                    it.mass.temperature = STANDARD_TEMPERATURE + A_BIT
                }
            }
            others.forEach {
                sim.connect(cen, it)
            }
            val cenOld = cen.mass.temperature
            sim.step(0.05)
            val delta = cen.mass.temperature - cenOld
            if(lastIncrease != null) {
                assert(delta > lastIncrease!!) { "$delta !> $lastIncrease" }
            }
            lastIncrease = delta
        }
    }

    @Test
    fun environment() {
        val sim = ThermalSimulator(TestEnv(
            1.0,
            STANDARD_TEMPERATURE + A_BIT,
            STANDARD_TEMPERATURE - A_BIT,
            Loc(0),
        ))
        val a = TestBody(Loc(1), ThermalMass(Material.IRON))
        val b = TestBody(Loc(-1), ThermalMass(Material.IRON))
        listOf(a, b).forEach { sim.add(it) }
        a.mass.temperature = STANDARD_TEMPERATURE
        b.mass.temperature = STANDARD_TEMPERATURE
        val aOld = a.mass.temperature
        val bOld = b.mass.temperature
        sim.step(0.05)
        assert(a.mass.temperature > aOld)
        assert(b.mass.temperature < bOld)
    }
}
