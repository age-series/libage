package org.eln2.libelectric.mathematics

import org.ageseries.libage.mathematics.*
import org.eln2.libelectric.TestUtils.areEqual
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.math.*

internal class MathTest {
    @Test
    fun fracTest() {
        assertEquals(frac(0.25), 0.25)
        assertEquals(frac(-0.25), 0.75)
        assertEquals(frac(0.25f), 0.25f)
        assertEquals(frac(-0.25f), 0.75f)
    }

    @Test
    fun mapTest() {
        assertEquals(map(1.0, 0.0, 1.0, 0.0, 10.0), 10.0)
        assertEquals(map(1.0f, 0.0f, 1.0f, 0.0f, 10.0f), 10.0f)
        assertEquals(map(0.0, 0.0, 1.0, 0.0, 10.0), 0.0)
        assertEquals(map(0.0f, 0.0f, 1.0f, 0.0f, 10.0f), 0.0f)
        assertEquals(map(0.5, 0.0, 1.0, 0.0, 10.0), 5.0)
        assertEquals(map(0.5f, 0.0f, 1.0f, 0.0f, 10.0f), 5.0f)
        assertEquals(map(1, 0, 1, 0, 10), 10)
        assertEquals(map(0, 0, 1, 0, 10), 0)
        assertEquals(map(5, 0, 10, 0, 100), 50)
    }

    @Test
    fun integralTest() {
        areEqual(integralScan(-1.0, +1.0) { cos(it) }, 1.682941970)
        areEqual(integralScan(-1.0, 1.0, tolerance = 1e-15) { (1.0.nz() / it.nz()) * sqrt((1.0 + it).nz() / (1.0 - it).nz()) * ln((2.0 * it * it + 2.0 * it + 1.0).nz() / (2.0 * it * it - 2.0 * it + 1.0).nz()) }, 8.372211626601276)
        areEqual(integralScan(0.0, PI) { sin(it - sqrt(PI * PI - it * it)).pow(2)}, PI / 2.0)
        areEqual(integralScan(0.0, 1.0) { sin(it * PI) / (it.pow(it) * (1.0 - it).pow(1.0 - it)) }, PI / E)
    }

    @Test
    fun intPowTest() {
        assertEquals((-1).pow(1), -1)
        assertEquals((-1).pow(2), 1)
        assertEquals((-1).pow(3), -1)
        assertEquals((-1).pow(4), 1)
        assertEquals((-1).pow(1), -1)

        repeat(10) { base ->
            repeat(5) { exponent ->
                assertEquals(base.pow(exponent + 1), base.toDouble().pow(exponent + 1).toInt())
            }
        }
        
        assertThrows<IllegalArgumentException> {
            1.pow(-1)
        }
    }

    @Test
    fun avgTest() {
        areEqual(avg(1.0, 2.0), 1.5)
        areEqual(avg(1.0, 2.0, 3.0), 2.0)
        areEqual(avg(1.0, 2.0, 3.0, 4.0), 2.5)
    }

    @Test
    fun testSignFunctions() {
        assertEquals(snz(0.0), 1.0)
        assertEquals(snz(1.0), 1.0)
        assertEquals(snz(-1.0), -1.0)
        assertEquals(nsnz(0.0), -1.0)
        assertEquals(nsnz(1.0), 1.0)
        assertEquals(nsnz(-1.0), -1.0)
        assertEquals(snzi(0.0), 1)
        assertEquals(snzi(1.0), 1)
        assertEquals(snzi(-1.0), -1)
        assertEquals(nsnzi(0.0), -1)
        assertEquals(nsnzi(1.0), 1)
        assertEquals(nsnzi(-1.0), -1)
        assertEquals(snzi(0), 1)
        assertEquals(snzi(1), 1)
        assertEquals(snzi(-1), -1)
        assertEquals(nsnzi(0), -1)
        assertEquals(nsnzi(1), 1)
        assertEquals(nsnzi(-1), -1)
    }

    @Test
    fun testSymforceSingularityRemoval() {
        assertEquals(snzE(0.0), SYMFORCE_EPS)
        assertEquals(snzE(0.0f), SYMFORCE_EPS_FLOAT)
        assertFalse((0.0.nz() / 0.0.nz()).isNaN())
        assertEquals(sin(0.0).nz() / 0.0.nz(), 1.0)
    }
}