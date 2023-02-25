package org.eln2.libelectric.sim.thermal

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
    ): Environment<Loc> {
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
        val sim = Simulator(TestEnv(0.0))
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
        sim.remove(a, b)
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
        val sim = Simulator(TestEnv(0.0))
        val a = TestBody(Loc(1), ThermalMass(Material.IRON))
        val b = TestBody(Loc(2), ThermalMass(Material.IRON))
        a.mass.temperature = STANDARD_TEMPERATURE
        b.mass.temperature = STANDARD_TEMPERATURE + A_BIT
        sim.add(a, b)
        val a_old = a.mass.temperature
        val b_old = b.mass.temperature
        sim.step(0.05)
        assert(a in sim.bodies)
        assert(b in sim.bodies)
        assertEquals(a.mass.temperature, a_old)
        assertEquals(b.mass.temperature, b_old)
    }

    @Test
    fun second_law() {
        val sim = Simulator(TestEnv(0.0))  // Don't conduct from/to env
        val a = TestBody(Loc(1), ThermalMass(Material.IRON))
        val b = TestBody(Loc(2), ThermalMass(Material.IRON))
        val lesser = STANDARD_TEMPERATURE
        val greater = STANDARD_TEMPERATURE + A_BIT
        for((a_temp, b_temp) in listOf(lesser to greater, greater to lesser)) {
            a.mass.temperature = a_temp
            b.mass.temperature = b_temp
            val a_old = a.mass.temperature
            val b_old = b.mass.temperature
            val conn = sim.connect(a, b)
            sim.step(0.05)
            assertEquals(a_temp.compareTo(b_temp), a_old.compareTo(a.mass.temperature)) {"A old $a_old, now ${a.mass.temperature}" }
            assertEquals(b_temp.compareTo(a_temp), b_old.compareTo(b.mass.temperature)) {"B old $b_old, now ${b.mass.temperature}" }
        }
    }

    @Test
    fun conductivity_proportional_to_connectivity() {
        var last_increase: Temperature? = null
        for(it in 1..10) {
            val sim = Simulator(TestEnv(0.0))
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
            val cen_old = cen.mass.temperature
            sim.step(0.05)
            val delta = cen.mass.temperature - cen_old
            if(last_increase != null) {
                assert(delta > last_increase!!) { "$delta !> $last_increase" }
            }
            last_increase = delta
        }
    }

    @Test
    fun environment() {
        val sim = Simulator(TestEnv(
            1.0,
            STANDARD_TEMPERATURE + A_BIT,
            STANDARD_TEMPERATURE - A_BIT,
            Loc(0),
        ))
        val a = TestBody(Loc(1), ThermalMass(Material.IRON))
        val b = TestBody(Loc(-1), ThermalMass(Material.IRON))
        sim.add(a, b)
        a.mass.temperature = STANDARD_TEMPERATURE
        b.mass.temperature = STANDARD_TEMPERATURE
        val a_old = a.mass.temperature
        val b_old = b.mass.temperature
        sim.step(0.05)
        assert(a.mass.temperature > a_old)
        assert(b.mass.temperature < b_old)
    }
}