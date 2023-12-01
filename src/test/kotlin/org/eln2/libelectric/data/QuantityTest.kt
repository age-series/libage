package org.eln2.libelectric.data

import org.ageseries.libage.data.*
import org.ageseries.libage.sim.thermal.STANDARD_TEMPERATURE
import org.eln2.libelectric.TestUtils.areEqual
import org.eln2.libelectric.TestUtils.areEqualAstronomic
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class QuantityTest {
    private fun<U> test(a1: Double, s1: QuantityScale<U>, a2: Double, s2: QuantityScale<U>) {
        areEqual(!Quantity(a1, s1), !Quantity(a2, s2))
    }

    private fun<U> testAstronomic(a1: Double, s1: QuantityScale<U>, a2: Double, s2: QuantityScale<U>) {
        areEqualAstronomic(!Quantity(a1, s1), !Quantity(a2, s2))
    }

    private fun<U> test(a1: Double, s1: QuantityScale<U>, q1: Quantity<U>) {
        areEqual(!Quantity(a1, s1), !q1)
    }

    @Test
    fun mass() {
        test(1.0, kg, 1000.0, g)
        test(1.0, g, 1000.0, mg)
    }

    @Test
    fun time() {
        test(1.0, s, 1e3, ms)
        test(1.0, s, 1e6, MICROSECONDS)
        test(1.0, s, 1e9, NANOSECONDS)
        test(1.0, MINUTES, 60.0, s)
        test(1.0, HOURS, 3600.0, s)
        test(1.0, DAYS, 24.0, HOURS)
    }

    @Test
    fun distance() {
        test(1.0, m, 100.0, cm)
        test(1.0, m, 1000.0, mm)
    }

    @Test
    fun energy() {
        test(1.0, kJ, 1e3, J)
        test(1.0, MJ, 1e6, J)
        test(1.0, GJ, 1e9, J)
        test(1.0, Ws, 1.0, J)
        test(1.0, Wmin, 60.0, J)
        test(1.0, Wh, 3.6e3, J)
        test(1.0, kWh, 3.6e6, J)
        test(1.0, MWh, 3.6e9, J)
        test(1.0, J, 1e3, MILLIJOULE)
        test(1.0, J, 1e6, MICROJOULE)
        test(1.0, J, 1e9, NANOJOULE)
        test(1.0, ERG, 100.0, NANOJOULE)
    }

    @Test
    fun power() {
        test(1.0, W, 1000.0, MILLIWATT)
        test(1.0, kW, 1e3, W)
        test(1.0, MW, 1e6, W)
        test(1.0, GW, 1e9, W)
    }

    @Test
    fun potential() {
        test(1.0, KV, 1000.0, V)
        test(1.0, V, 1000.0, MILLIVOLT)
    }

    @Test
    fun current() {
        test(1.0, A, 1000.0, mA)
    }

    @Test
    fun resistance() {
        test(1.0, KILOOHM, 1e3, OHM)
        test(1.0, MEGAOHM, 1e6, OHM)
        test(1.0, GIGAOHM, 1e9, OHM)
        test(1.0, OHM, 1000.0, MILLIOHM)
    }

    @Test
    fun electronVolt() {
        test(1.0, J, 6.2415090744609997e18, eV)
        test(1.0, J, 6.241509074461e15, keV)
        test(1.0, J, 6.241509074461e12, MeV)
        test(1.0, J, 6.2415090744609995e9, GeV)
        test(1.0, J, 6241509.074461, TeV)
    }

    @Test
    fun radioactivity() {
        test(1.0, nCi, 37.0, Bq)
        // Very large numerical errors occur at this scale.
        // Maybe choose a multiple of Bq as a standard unit so that we mitigate these issues?
        testAstronomic(1.0, kCi, 1e3, Ci)
        testAstronomic(1.0, MEGACURIES, 1e6, Ci)
        test(1.0, kBq, 1e3, Bq)
        test(1.0, MBq, 1e6, Bq)
        test(1.0, GBq, 1e9, Bq)
        testAstronomic(1.0, Ci, 1e3, mCi)
        testAstronomic(1.0, Ci, 1e6, uCi)
        testAstronomic(1.0, Ci, 1e9, nCi)
    }

    @Test
    fun radiationAbsorbedDose() {
        test(1.0, Gy, 100.0, RAD)
        test(1.0, Gy, 1000.0, mGy)
    }

    @Test
    fun radiationDoseEquivalent() {
        test(1.0, Sv, 1e3, mSv)
        test(1.0, Sv, 1e6, uSv)
        test(1.0, Sv, 1e9, nSv)
        test(100.0, REM, 1.0, Sv)
        test(1.0, REM, 1e3, MILLIREM)
        test(1.0, REM, 1e6, MICROREM)
    }

    @Test
    fun radiationExposure() {
        test(1.0, R, 2.58e-4, COULOMB_PER_KG)
        test(1.0, R, 1e3, mR)
        test(1.0, R, 1e6, uR)
        test(1.0, R, 1e9, nR)
    }

    @Test
    fun temperature() {
        test(0.0, CELSIUS, STANDARD_TEMPERATURE)
        test(0.0, CENTIGRADE, STANDARD_TEMPERATURE)
        test(0.01, CENTIGRADE, 0.1, MILLIGRADE)
        test(100.0, RANKINE, 55.5556, KELVIN)
    }

    @Test
    fun quantityArrayTest() {
        val array = QuantityArray<Time>(doubleArrayOf(0.0, 1.0, 2.0))

        areEqual(0.0, !array[0])
        areEqual(1.0, !array[1])
        areEqual(2.0, !array[2])

        assertThrows<IndexOutOfBoundsException> {
            array[-1]
        }

        assertThrows<IndexOutOfBoundsException> {
            array[3]
        }
    }
}