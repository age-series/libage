package org.eln2.libelectric.mathematics

import org.ageseries.libage.mathematics.*
import org.eln2.libelectric.TestUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import kotlin.math.*
import kotlin.random.Random

internal class DualTest {
    private val random = Random(3141)

    private fun rngNz() = random.nextDouble(0.5, 10.0) * snz(random.nextDouble(-1.0, 1.0))

    @Test
    fun constructors() {
        Assertions.assertEquals(Dual(listOf(1.0, 2.0, 3.0)), Dual.of(1.0, 2.0, 3.0))
        Assertions.assertEquals(Dual(1.0, Dual.of(2.0, 3.0)), Dual.of(1.0, 2.0, 3.0))
        Assertions.assertEquals(Dual(Dual.of(1.0, 2.0), 3.0), Dual.of(1.0, 2.0, 3.0))
        Assertions.assertEquals(Dual.variable(10.0, 1), Dual.of(10.0))
        Assertions.assertEquals(Dual.variable(10.0, 2), Dual.of(10.0, 1.0))
        Assertions.assertEquals(Dual.variable(10.0, 3), Dual.of(10.0, 1.0, 0.0))
        Assertions.assertEquals(Dual.const(10.0, 1), Dual.of(10.0))
        Assertions.assertEquals(Dual.const(10.0, 2), Dual.of(10.0, 0.0))
        Assertions.assertEquals(Dual.const(10.0, 3), Dual.of(10.0, 0.0, 0.0))
        Assertions.assertEquals(Dual.castFromArray(doubleArrayOf(1.0, 2.0, 3.0)), Dual.of(1.0, 2.0, 3.0))
    }

    @Test
    fun dequeOperations() {
        Assertions.assertEquals(Dual.of(1.0, 2.0, 3.0).head(0), Dual.of(1.0, 2.0, 3.0))
        Assertions.assertEquals(Dual.of(1.0, 2.0, 3.0).head(), Dual.of(1.0, 2.0))
        Assertions.assertEquals(Dual.of(1.0, 2.0, 3.0).head(1), Dual.of(1.0, 2.0))
        Assertions.assertEquals(Dual.of(1.0, 2.0, 3.0).head(2), Dual.of(1.0))
        Assertions.assertEquals(Dual.of(1.0, 2.0, 3.0).tail(0), Dual.of(1.0, 2.0, 3.0))
        Assertions.assertEquals(Dual.of(1.0, 2.0, 3.0).tail(), Dual.of(2.0, 3.0))
        Assertions.assertEquals(Dual.of(1.0, 2.0, 3.0).tail(1), Dual.of(2.0, 3.0))
        Assertions.assertEquals(Dual.of(1.0, 2.0, 3.0).tail(2), Dual.of(3.0))
        Assertions.assertEquals(Dual.castFromArray(doubleArrayOf(1.0, 2.0, 3.0)), Dual.of(1.0, 2.0, 3.0))
    }

    @Test
    fun accessOperations() {
        Assertions.assertEquals(Dual.of(1.0, 2.0, 3.0).value, 1.0)
        Assertions.assertEquals(Dual.of(1.0, 2.0, 3.0)[0], 1.0)
        Assertions.assertEquals(Dual.of(1.0, 2.0, 3.0)[1], 2.0)
        Assertions.assertEquals(Dual.of(1.0, 2.0, 3.0)[2], 3.0)
        Assertions.assertTrue(Dual.of(1.0, 2.0, 3.0).bind().contentEquals(doubleArrayOf(1.0, 2.0, 3.0)))
        Assertions.assertTrue(Dual.of(1.0, 2.0, 3.0).toDoubleArray().contentEquals(doubleArrayOf(1.0, 2.0, 3.0)))
    }

    @Test
    fun mapOperations() {
        Assertions.assertEquals(Dual.of(1.0, 2.0, 3.0).mapValueAndTail({ it * 10.0 }) { it * 10.0 }, Dual.of(10.0, 20.0, 30.0))
        Assertions.assertEquals(Dual.of(1.0, 2.0, 3.0).map { it * 10.0 }, Dual.of(10.0, 20.0, 30.0))
        Assertions.assertEquals(Dual.of(1.0, 2.0, 3.0).mapValue { it * 10.0 }, Dual.of(10.0, 2.0, 3.0))
    }

    @Test
    fun equality() {
         Assertions.assertEquals(Dual.of(1.0, 2.0, 3.0), Dual.of(1.0, 2.0, 3.0))
         Assertions.assertNotEquals(Dual.of(1.0, 2.0, 3.0), Dual.of(2.0, 1.0, 3.0))
    }

    @Test
    fun sinTest() {
        TestUtils.range { x, xDual ->
            val v = sin(xDual)

            TestUtils.areEqual(v.value, sin(x))
            TestUtils.areEqual(v[1], cos(x))
            TestUtils.areEqual(v[2], -sin(x))
            TestUtils.areEqual(v[3], -cos(x))
        }
    }

    @Test
    fun cosTest() {
        TestUtils.range { x, xDual ->
            val v = cos(xDual)

            TestUtils.areEqual(v.value, cos(x))
            TestUtils.areEqual(v[1], -sin(x))
            TestUtils.areEqual(v[2], -cos(x))
            TestUtils.areEqual(v[3], sin(x))
        }
    }

    @Test
    fun tanTest() {
        // Scanning in the default 0-10 range causes numerical errors
        TestUtils.range(start = 0.0, end = 1.0) { x, xDual ->
            val v = tan(xDual)

            TestUtils.areEqual(v.value, tan(x))
            TestUtils.areEqual(v[1], sec(x).pow(2))
            TestUtils.areEqual(v[2], 2.0 * tan(x) * sec(x).pow(2))
            TestUtils.areEqual(v[3], 2.0 * sec(x).pow(2) * (2.0 * tan(x).pow(2) + sec(x).pow(2)))
        }
    }

    @Test
    fun cotTest() {
        TestUtils.range(start = 0.1, end = 1.0) { x, xDual ->
            val v = cot(xDual)

            TestUtils.areEqual(v.value, cot(x))
            TestUtils.areEqual(v[1], -csc(x).pow(2))
            TestUtils.areEqual(v[2], 2.0 * cot(x) * csc(x).pow(2))
            TestUtils.areEqual(v[3], -2.0 * (csc(x).pow(4) + 2.0 * cot(x).pow(2) * csc(x).pow(2)))
        }
    }

    @Test
    fun secTest() {
        TestUtils.range(start = 0.0, end = 1.0) { x, xDual ->
            val v = sec(xDual)

            TestUtils.areEqual(v.value, sec(x))
            TestUtils.areEqual(v[1], tan(x) * sec(x))
            TestUtils.areEqual(v[2], sec(x) * (tan(x).pow(2) + sec(x).pow(2)))
            TestUtils.areEqual(v[3], tan(x) * sec(x) * (tan(x).pow(2) + 5.0 * sec(x).pow(2)))
        }
    }

    @Test
    fun cscTest() {
        TestUtils.range(start = 0.1, end = 1.0) { x, xDual ->
            val v = csc(xDual)

            TestUtils.areEqual(v.value, csc(x))
            TestUtils.areEqual(v[1], -cot(x) * csc(x))
            TestUtils.areEqual(v[2], csc(x) * (cot(x).pow(2) + csc(x).pow(2)))
            TestUtils.areEqual(v[3], -cot(x) * csc(x) * (cot(x).pow(2) + 5.0 * csc(x).pow(2)))
        }
    }

    @Test
    fun sinhTest() {
        TestUtils.range { x, xDual ->
            val v = sinh(xDual)

            TestUtils.areEqual(v.value, sinh(x))
            TestUtils.areEqual(v[1], cosh(x))
            TestUtils.areEqual(v[2], sinh(x))
            TestUtils.areEqual(v[3], cosh(x))
        }
    }

    @Test
    fun coshTest() {
        TestUtils.range { x, xDual ->
            val v = cosh(xDual)

            TestUtils.areEqual(v.value, cosh(x))
            TestUtils.areEqual(v[1], sinh(x))
            TestUtils.areEqual(v[2], cosh(x))
            TestUtils.areEqual(v[3], sinh(x))
        }
    }

    @Test
    fun tanhTest() {
        TestUtils.range { x, xDual ->
            val v = tanh(xDual)

            TestUtils.areEqual(v.value, tanh(x))
            TestUtils.areEqual(v[1], sech(x).pow(2))
            TestUtils.areEqual(v[2], -2.0 * tanh(x) * sech(x).pow(2))
            TestUtils.areEqual(v[3], 4.0 * tanh(x).pow(2) * sech(x).pow(2) - 2.0 * sech(x).pow(4))
        }
    }

    @Test
    fun cothTest() {
        TestUtils.range(start = 0.1) { x, xDual ->
            val v = coth(xDual)

            TestUtils.areEqual(v.value, coth(x))
            TestUtils.areEqual(v[1], -csch(x).pow(2))
            TestUtils.areEqual(v[2], 2.0 * coth(x) * csch(x).pow(2))
            TestUtils.areEqual(v[3], -2.0 * (csch(x).pow(4) + 2.0 * coth(x).pow(2) * csch(x).pow(2)))
        }
    }

    @Test
    fun sechTest() {
        TestUtils.range { x, xDual ->
            val v = sech(xDual)

            TestUtils.areEqual(v.value, sech(x))
            TestUtils.areEqual(v[1], tanh(x) * -sech(x))
            TestUtils.areEqual(v[2], tanh(x).pow(2) * sech(x) - sech(x).pow(3))
            TestUtils.areEqual(v[3], 5.0 * tanh(x) * sech(x).pow(3) - tanh(x).pow(3) * sech(x))
        }
    }

    @Test
    fun cschTest() {
        TestUtils.range(start = 0.1) { x, xDual ->
            val v = csch(xDual)

            TestUtils.areEqual(v.value, csch(x))
            TestUtils.areEqual(v[1], -coth(x) * csch(x))
            TestUtils.areEqual(v[2], csch(x) * (coth(x).pow(2) + csch(x).pow(2)))
            TestUtils.areEqual(v[3], -coth(x) * csch(x) * (coth(x).pow(2) + 5.0 * csch(x).pow(2)))
        }
    }

    @Test
    fun powTest() {
        TestUtils.rangeScan(start = 1.0, end = 4.0, steps = 100) { power ->
            TestUtils.range(start = 1.0, steps = 1000) { x, xDual ->
                val v = pow(xDual, power)

                TestUtils.areEqual(v.value, x.pow(power))
                TestUtils.areEqual(v[1], power * x.pow(power - 1))
                TestUtils.areEqual(v[2], (power - 1.0) * power * x.pow(power - 2))
                TestUtils.areEqual(v[3], (power - 2.0) * (power - 1.0) * power * x.pow(power - 3))
            }
        }
    }

    @Test
    fun sqrtTest() {
        TestUtils.range(start = 1.0) { x, xDual ->
            val v = sqrt(xDual)

            TestUtils.areEqual(v.value, sqrt(x))
            TestUtils.areEqual(v[1], 1.0 / (2.0 * sqrt(x)))
            TestUtils.areEqual(v[2], -1.0 / (4.0 * x.pow(3.0 / 2.0)))
            TestUtils.areEqual(v[3], 3.0 / (8.0 * x.pow(5.0 / 2.0)))
        }
    }

    @Test
    fun intPowTest() {
        repeat(5) { power ->
            TestUtils.range(start = 1.0, steps = 1000) { x, xDual ->
                val v = pow(xDual, power)

                TestUtils.areEqual(v.value, x.pow(power))
                TestUtils.areEqual(v[1], power * x.pow(power - 1))
                TestUtils.areEqual(v[2], (power - 1.0) * power * x.pow(power - 2))
                TestUtils.areEqual(v[3], (power - 2.0) * (power - 1.0) * power * x.pow(power - 3))
            }
        }
    }

    @Test
    fun lnTest() {
        TestUtils.range(start = 5.0, end = 10.0) { x, xDual ->
            val v = ln(xDual)

            TestUtils.areEqual(v.value, ln(x))
            TestUtils.areEqual(v[1], 1.0 / x)
            TestUtils.areEqual(v[2], -1.0 / x.pow(2))
            TestUtils.areEqual(v[3], 2.0 / x.pow(3))
        }
    }

    @Test
    fun expTest() {
        TestUtils.range { x, xDual ->
            val v = exp(xDual)

            TestUtils.areEqual(v.value, exp(x))
            TestUtils.areEqual(v[1], exp(x))
            TestUtils.areEqual(v[2], exp(x))
            TestUtils.areEqual(v[3], exp(x))
        }
    }

    @Test
    fun constTest() {
        repeat(100000) {
            val x = Dual.of(rngNz(), rngNz(), rngNz(), rngNz())
            val c = rngNz()
            val cDual = Dual.const(c, x.size)

            TestUtils.areEqual(
                x + c,
                c + x,
                x + cDual,
                cDual + x
            )

            TestUtils.areEqual(
                x - c,
                x - cDual
            )

            TestUtils.areEqual(
                c - x,
                cDual - x
            )

            TestUtils.areEqual(
                x * c,
                c * x,
                x * cDual,
                cDual * x
            )

            TestUtils.areEqual(
                x / c,
                x / cDual
            )

            TestUtils.areEqual(
                c / x,
                cDual / x,
            )
        }
    }

    // Test both hermite and dual:
    @Test
    fun quinticHermiteTest() {
        TestUtils.range(steps = 10) { x, xDual ->
            TestUtils.rangeScanKd(layers = 6, steps = 5) { vec ->
                val p0 = vec[0]
                val v0 = vec[1]
                val a0 = vec[2]
                val a1 = vec[3]
                val v1 = vec[4]
                val p1 = vec[5]

                val v = hermiteQuinticDual(p0, v0, a0, a1, v1, p1, xDual)

                TestUtils.areEqual(v.value, hermiteQuintic(p0, v0, a0, a1, v1, p1, x))
                TestUtils.areEqual(v[1], hermiteQuinticDerivative1(p0, v0, a0, a1, v1, p1, x))
                TestUtils.areEqual(v[2], hermiteQuinticDerivative2(p0, v0, a0, a1, v1, p1, x))
            }
        }
    }
}