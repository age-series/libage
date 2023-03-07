package org.eln2.libelectric.sim.thermal

import org.ageseries.libage.sim.Material
import org.ageseries.libage.sim.thermal.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.abs

internal class ThermalTests {
    companion object {
        val A_BIT = Temperature(10.0)
    }

    @Test
    fun consistent_data_model() {
        val sim = Simulator()
        val a = ThermalMass(Material.IRON)
        val b = ThermalMass(Material.IRON)
        val c = ThermalMass(Material.IRON)
        assert(a !in sim.masses)
        assert(b !in sim.masses)
        assert(c !in sim.masses)
        val conn = sim.connect(a, b)
        assert(conn in sim.connections)
        assertEquals(conn.masses[0], a)
        assertEquals(conn.masses[1], b)
        assert(a in sim.masses)
        assert(b in sim.masses)
        assert(c !in sim.masses)
        sim.remove(conn)
        assert(conn !in sim.connections)
        assert(a in sim.masses)
        assert(b in sim.masses)
        assert(c !in sim.masses)
        listOf(a, b).forEach { sim.remove(it) }
        assert(a !in sim.masses)
        assert(b !in sim.masses)
        assert(c !in sim.masses)
        val conn2 = sim.connect(b, c)
        assert(conn2 in sim.connections)
        assert(a !in sim.masses)
        assert(b in sim.masses)
        assert(c in sim.masses)
        sim.remove(b)
        assert(conn2 !in sim.connections)
        assert(a !in sim.masses)
        assert(b !in sim.masses)
        assert(c in sim.masses)
    }

    @Test
    fun perfectly_isolated() {
        val sim = Simulator()
        val a = ThermalMass(Material.IRON)
        val b = ThermalMass(Material.IRON)
        a.temperature = STANDARD_TEMPERATURE
        b.temperature = STANDARD_TEMPERATURE + A_BIT
        listOf(a, b).forEach { sim.add(it) }
        val aOld = a.temperature
        val bOld = b.temperature
        sim.step(0.05)
        assert(a in sim.masses)
        assert(b in sim.masses)
        assertEquals(a.temperature, aOld)
        assertEquals(b.temperature, bOld)
    }

    @Test
    fun second_law() {
        val sim = Simulator()  // Don't conduct from/to env
        val a = ThermalMass(Material.IRON)
        val b = ThermalMass(Material.IRON)
        val lesser = STANDARD_TEMPERATURE
        val greater = STANDARD_TEMPERATURE + A_BIT
        listOf(lesser to greater, greater to lesser).forEach { (a_temp, b_temp) ->
            a.temperature = a_temp
            b.temperature = b_temp
            val aOld = a.temperature
            val bOld = b.temperature
            sim.connect(a, b)
            sim.step(0.05)
            assertEquals(a_temp.compareTo(b_temp), aOld.compareTo(a.temperature)) {"A old $aOld, now ${a.temperature}" }
            assertEquals(b_temp.compareTo(a_temp), bOld.compareTo(b.temperature)) {"B old $bOld, now ${b.temperature}" }
        }
    }

    @Test
    fun conductivity_proportional_to_connectivity() {
        var lastIncrease: Temperature? = null
        for(it in 1..10) {
            val sim = Simulator()
            val cen = ThermalMass(Material.IRON)
            cen.temperature = STANDARD_TEMPERATURE
            val others = (1..it).map {
                ThermalMass(Material.IRON).also {
                    it.temperature = STANDARD_TEMPERATURE + A_BIT
                }
            }
            others.forEach {
                sim.connect(cen, it)
            }
            val cenOld = cen.temperature
            sim.step(0.05)
            val delta = cen.temperature - cenOld
            if(lastIncrease != null) {
                assert(delta > lastIncrease) { "$delta !> $lastIncrease" }
            }
            lastIncrease = delta
        }
    }

    @Test
    fun environment() {
        val sim = Simulator()
        val a = ThermalMass(Material.IRON)
        val b = ThermalMass(Material.IRON)
        listOf(a, b).forEach {
            sim.add(it)
        }
        sim.connect(a, STANDARD_TEMPERATURE + A_BIT)
        sim.connect(b, STANDARD_TEMPERATURE - A_BIT)
        a.temperature = STANDARD_TEMPERATURE
        b.temperature = STANDARD_TEMPERATURE
        val aOld = a.temperature
        val bOld = b.temperature
        sim.step(0.05)
        assert(a.temperature > aOld)
        assert(b.temperature < bOld)
    }

    @Test
    fun equilibrium() {
        val sim = Simulator()
        val a = ThermalMass(Material.IRON)
        val b = ThermalMass(Material.IRON)
        a.temperature = STANDARD_TEMPERATURE
        b.temperature = STANDARD_TEMPERATURE + A_BIT
        sim.connect(a, b)
        for(step in 0 until 1000) {
            sim.step(1.0)
            assert(a.temperature.kelvin >= 0.0) { "negative energy: $a" }
            assert(b.temperature.kelvin >= 0.0) { "negative energy: $b" }
            println("${b.temperature.kelvin - a.temperature.kelvin},${a.temperature},${b.temperature}")
            if(abs(a.temperature.kelvin - b.temperature.kelvin) < 1e-9) {
                println("equalized at $step: $a = $b")
                return
            }
        }
        error("failed to equalize: $a, $b")
    }

    @Test
    fun astronomical() {
        val sim = Simulator()
        val a = ThermalMass(Material.IRON)
        val b = ThermalMass(Material.IRON)
        a.temperature = STANDARD_TEMPERATURE * 1000.0
        b.temperature = Temperature(0.0)
        sim.connect(a, b)
        for(step in 0 until 1000) {
            sim.step(1.0)
            assert(a.temperature.kelvin >= 0.0) { "negative energy: $a" }
            assert(b.temperature.kelvin >= 0.0) { "negative energy: $b" }
            println("${b.temperature.kelvin - a.temperature.kelvin},${a.temperature},${b.temperature}")
            if(abs(a.temperature.kelvin - b.temperature.kelvin) < 1e-9) {
                println("equalized at $step: $a = $b")
                return
            }
        }
        error("failed to equalize: $a, $b")
    }
}
