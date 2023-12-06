package org.eln2.libelectric.mathematics

import org.ageseries.libage.mathematics.*
import org.eln2.libelectric.TestUtils.areEqual
import org.eln2.libelectric.TestUtils.range
import org.eln2.libelectric.TestUtils.rangeScan
import org.eln2.libelectric.TestUtils.rangeScanKd
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.math.*
import kotlin.random.Random
import kotlin.time.times

internal class DualTest {
    private val random = Random(3141)

    private fun rngNz() = random.nextDouble(0.5, 10.0) * snz(random.nextDouble(-1.0, 1.0))

    @Test
    fun constructors() {
        assertEquals(Dual(listOf(1.0, 2.0, 3.0)), Dual.of(1.0, 2.0, 3.0))
        assertEquals(Dual(1.0, Dual.of(2.0, 3.0)), Dual.of(1.0, 2.0, 3.0))
        assertEquals(Dual(Dual.of(1.0, 2.0), 3.0), Dual.of(1.0, 2.0, 3.0))
        assertEquals(Dual.variable(10.0, 1), Dual.of(10.0))
        assertEquals(Dual.variable(10.0, 2), Dual.of(10.0, 1.0))
        assertEquals(Dual.variable(10.0, 3), Dual.of(10.0, 1.0, 0.0))
        assertEquals(Dual.const(10.0, 1), Dual.of(10.0))
        assertEquals(Dual.const(10.0, 2), Dual.of(10.0, 0.0))
        assertEquals(Dual.const(10.0, 3), Dual.of(10.0, 0.0, 0.0))
        assertEquals(Dual.castFromArray(doubleArrayOf(1.0, 2.0, 3.0)), Dual.of(1.0, 2.0, 3.0))
        assertEquals(Dual(Dual.of(1.0, 2.0), Dual.of(3.0, 4.0)), Dual.of(1.0, 2.0, 3.0, 4.0))
        assertEquals(Dual(Dual.empty, Dual.of(3.0, 4.0)), Dual.of(3.0, 4.0))
        assertEquals(Dual(Dual.of(1.0, 2.0), Dual.empty), Dual.of(1.0, 2.0))
        assertEquals(Dual(Dual.empty, Dual.empty), Dual.empty)
        assertEquals(Dual(listOf(1.0, 2.0), listOf(3.0, 4.0)), Dual.of(1.0, 2.0, 3.0, 4.0))
    }

    @Test
    fun unary() {
        range { _, xDual ->
            areEqual(xDual, +xDual, (1.0) * xDual, xDual * (1.0), Dual.const(1.0, xDual.size) * xDual)
            areEqual(-xDual, (-1.0) * xDual, xDual * (-1.0), Dual.const(-1.0, xDual.size) * xDual)
        }
    }

    @Test
    fun immutability() {
        let {
            val array = doubleArrayOf(1.0, 2.0, 3.0)
            val dual = Dual.create(array)

            assertTrue(dual.toList() == array.toList())
            array[0] = 10.0
            assertFalse(dual.toList() == array.toList())
        }

        let {
            val array = doubleArrayOf(1.0, 2.0, 3.0)
            val dual = Dual.castFromArray(array)

            assertTrue(dual.toList() == array.toList())
            array[0] = 10.0
            assertTrue(dual.toList() == array.toList())
        }

        let {
            val head = doubleArrayOf(1.0, 2.0)
            val tail = doubleArrayOf(3.0, 4.0)
            val dual = Dual(head, tail)

            assertTrue(dual.toList() == head.toList() + tail.toList())
            head[0] = 10.0
            assertFalse(dual.toList() == head.toList() + tail.toList())
            head[0] = 1.0
            assertTrue(dual.toList() == head.toList() + tail.toList())
            tail[0] = 30.0
            assertFalse(dual.toList() == head.toList() + tail.toList())
        }

        let {
            val head = mutableListOf(1.0, 2.0)
            val tail = mutableListOf(3.0, 4.0)
            val dual = Dual(head, tail)

            assertTrue(dual.toList() == head.toList() + tail.toList())
            head[0] = 10.0
            assertFalse(dual.toList() == head.toList() + tail.toList())
            head[0] = 1.0
            assertTrue(dual.toList() == head.toList() + tail.toList())
            tail[0] = 30.0
            assertFalse(dual.toList() == head.toList() + tail.toList())
        }
    }

    @Test
    fun dequeOperations() {
        assertEquals(Dual.of(1.0, 2.0, 3.0).head(0), Dual.of(1.0, 2.0, 3.0))
        assertEquals(Dual.of(1.0, 2.0, 3.0).head(), Dual.of(1.0, 2.0))
        assertEquals(Dual.of(1.0, 2.0, 3.0).head(1), Dual.of(1.0, 2.0))
        assertEquals(Dual.of(1.0, 2.0, 3.0).head(2), Dual.of(1.0))
        assertEquals(Dual.of(1.0, 2.0, 3.0).tail(0), Dual.of(1.0, 2.0, 3.0))
        assertEquals(Dual.of(1.0, 2.0, 3.0).tail(), Dual.of(2.0, 3.0))
        assertEquals(Dual.of(1.0, 2.0, 3.0).tail(1), Dual.of(2.0, 3.0))
        assertEquals(Dual.of(1.0, 2.0, 3.0).tail(2), Dual.of(3.0))
        assertEquals(Dual.castFromArray(doubleArrayOf(1.0, 2.0, 3.0)), Dual.of(1.0, 2.0, 3.0))
    }

    @Test
    fun accessOperations() {
        assertEquals(Dual.of(1.0, 2.0, 3.0).value, 1.0)
        assertEquals(Dual.of(1.0, 2.0, 3.0)[0], 1.0)
        assertEquals(Dual.of(1.0, 2.0, 3.0)[1], 2.0)
        assertEquals(Dual.of(1.0, 2.0, 3.0)[2], 3.0)
        assertTrue(Dual.of(1.0, 2.0, 3.0).bind().contentEquals(doubleArrayOf(1.0, 2.0, 3.0)))
        assertTrue(Dual.of(1.0, 2.0, 3.0).toDoubleArray().contentEquals(doubleArrayOf(1.0, 2.0, 3.0)))
    }

    @Test
    fun mapOperations() {
        assertEquals(Dual.of(1.0, 2.0, 3.0).mapValueAndTail({ it * 10.0 }) { it * 10.0 }, Dual.of(10.0, 20.0, 30.0))
        assertEquals(Dual.of(1.0, 2.0, 3.0).map { it * 10.0 }, Dual.of(10.0, 20.0, 30.0))
        assertEquals(Dual.of(1.0, 2.0, 3.0).mapValue { it * 10.0 }, Dual.of(10.0, 2.0, 3.0))
    }

    @Test
    fun equality() {
         assertEquals(Dual.of(1.0, 2.0, 3.0), Dual.of(1.0, 2.0, 3.0))
         assertNotEquals(Dual.of(1.0, 2.0, 3.0), Dual.of(2.0, 1.0, 3.0))
    }

    @Test
    fun sinTest() {
        range { x, xDual ->
            val v = sin(xDual)

            areEqual(v.value, sin(x))
            areEqual(v[1], cos(x))
            areEqual(v[2], -sin(x))
            areEqual(v[3], -cos(x))
        }
    }

    @Test
    fun cosTest() {
        range { x, xDual ->
            val v = cos(xDual)

            areEqual(v.value, cos(x))
            areEqual(v[1], -sin(x))
            areEqual(v[2], -cos(x))
            areEqual(v[3], sin(x))
        }
    }

    @Test
    fun tanTest() {
        range(start = 0.0, end = 1.0) { x, xDual ->
            val v = tan(xDual)

            areEqual(v.value, tan(x))
            areEqual(v[1], sec(x).pow(2))
            areEqual(v[2], 2.0 * tan(x) * sec(x).pow(2))
            areEqual(v[3], 2.0 * sec(x).pow(2) * (2.0 * tan(x).pow(2) + sec(x).pow(2)))
        }
    }

    @Test
    fun cotTest() {
        range(start = 0.1, end = 1.0) { x, xDual ->
            val v = cot(xDual)

            areEqual(v.value, cot(x))
            areEqual(v[1], -csc(x).pow(2))
            areEqual(v[2], 2.0 * cot(x) * csc(x).pow(2))
            areEqual(v[3], -2.0 * (csc(x).pow(4) + 2.0 * cot(x).pow(2) * csc(x).pow(2)))
        }
    }

    @Test
    fun secTest() {
        range(start = 0.0, end = 1.0) { x, xDual ->
            val v = sec(xDual)

            areEqual(v.value, sec(x))
            areEqual(v[1], tan(x) * sec(x))
            areEqual(v[2], sec(x) * (tan(x).pow(2) + sec(x).pow(2)))
            areEqual(v[3], tan(x) * sec(x) * (tan(x).pow(2) + 5.0 * sec(x).pow(2)))
        }
    }

    @Test
    fun cscTest() {
        range(start = 0.1, end = 1.0) { x, xDual ->
            val v = csc(xDual)

            areEqual(v.value, csc(x))
            areEqual(v[1], -cot(x) * csc(x))
            areEqual(v[2], csc(x) * (cot(x).pow(2) + csc(x).pow(2)))
            areEqual(v[3], -cot(x) * csc(x) * (cot(x).pow(2) + 5.0 * csc(x).pow(2)))
        }
    }

    @Test
    fun sinhTest() {
        range { x, xDual ->
            val v = sinh(xDual)

            areEqual(v.value, sinh(x))
            areEqual(v[1], cosh(x))
            areEqual(v[2], sinh(x))
            areEqual(v[3], cosh(x))
        }
    }

    @Test
    fun coshTest() {
        range { x, xDual ->
            val v = cosh(xDual)

            areEqual(v.value, cosh(x))
            areEqual(v[1], sinh(x))
            areEqual(v[2], cosh(x))
            areEqual(v[3], sinh(x))
        }
    }

    @Test
    fun tanhTest() {
        range { x, xDual ->
            val v = tanh(xDual)

            areEqual(v.value, tanh(x))
            areEqual(v[1], sech(x).pow(2))
            areEqual(v[2], -2.0 * tanh(x) * sech(x).pow(2))
            areEqual(v[3], 4.0 * tanh(x).pow(2) * sech(x).pow(2) - 2.0 * sech(x).pow(4))
        }
    }

    @Test
    fun cothTest() {
        range(start = 0.1) { x, xDual ->
            val v = coth(xDual)

            areEqual(v.value, coth(x))
            areEqual(v[1], -csch(x).pow(2))
            areEqual(v[2], 2.0 * coth(x) * csch(x).pow(2))
            areEqual(v[3], -2.0 * (csch(x).pow(4) + 2.0 * coth(x).pow(2) * csch(x).pow(2)))
        }
    }

    @Test
    fun sechTest() {
        range { x, xDual ->
            val v = sech(xDual)

            areEqual(v.value, sech(x))
            areEqual(v[1], tanh(x) * -sech(x))
            areEqual(v[2], tanh(x).pow(2) * sech(x) - sech(x).pow(3))
            areEqual(v[3], 5.0 * tanh(x) * sech(x).pow(3) - tanh(x).pow(3) * sech(x))
        }
    }

    @Test
    fun cschTest() {
        range(start = 0.1) { x, xDual ->
            val v = csch(xDual)

            areEqual(v.value, csch(x))
            areEqual(v[1], -coth(x) * csch(x))
            areEqual(v[2], csch(x) * (coth(x).pow(2) + csch(x).pow(2)))
            areEqual(v[3], -coth(x) * csch(x) * (coth(x).pow(2) + 5.0 * csch(x).pow(2)))
        }
    }

    @Test
    fun asinTest() {
        range(start = -0.9, end = 0.9) { x, xDual ->
            val v = asin(xDual)

            areEqual(v.value, asin(x))
            areEqual(v[1], 1.0 / sqrt(1.0 - x.pow(2)))
            areEqual(v[2], x / (1.0 - x.pow(2)).pow(3.0 / 2.0))
            areEqual(v[3], (2.0 * x.pow(2) + 1.0) / (1.0 - x.pow(2)).pow(5.0 / 2.0))
        }
    }

    @Test
    fun acosTest() {
        range(start = -0.9, end = 0.9) { x, xDual ->
            val v = acos(xDual)

            areEqual(v.value, acos(x))
            areEqual(v[1], -1.0 / sqrt(1.0 - x.pow(2)))
            areEqual(v[2], -x / (1.0 - x.pow(2)).pow(3.0 / 2.0))
            areEqual(v[3], (-2.0 * x.pow(2) - 1.0) / (1.0 - x.pow(2)).pow(5.0 / 2.0))
        }
    }

    @Test
    fun atanTest() {
        range(start = 0.0, end = 1e10) { x, xDual ->
            val v = atan(xDual)

            areEqual(v.value, atan(x))
            areEqual(v[1], 1.0 / (x.pow(2) + 1))
            areEqual(v[2], (2.0 * x) / (x.pow(2) + 1).pow(2))
            areEqual(v[3], (6.0 * x.pow(2) - 2.0) / (x.pow(2) + 1.0).pow(3))
        }
    }

    @Test
    fun asinhTest() {
        range(start = 0.0, end = 1e10) { x, xDual ->
            val v = asinh(xDual)

            areEqual(v.value, asinh(x))
            areEqual(v[1], 1.0 / sqrt(x.pow(2) + 1.0))
            areEqual(v[2], -x / (x.pow(2) + 1).pow(3.0 / 2.0))
            areEqual(v[3], (2.0 * x.pow(2) - 1.0) / (x.pow(2) + 1.0).pow(5.0 / 2.0))
        }
    }

    @Test
    fun acoshTest() {
        range(start = 1.1, end = 1e10) { x, xDual ->
            val v = acosh(xDual)

            areEqual(v.value, acosh(x))
            areEqual(v[1], 1.0 / (sqrt(x - 1.0) * sqrt(x + 1.0)))
            areEqual(v[2], -x / ((x - 1.0).pow(3.0 / 2.0) * (x + 1.0).pow(3.0 / 2.0)))
            areEqual(v[3], (2.0 * x.pow(2) + 1.0) / ((x - 1.0).pow(5.0 / 2.0) * (x + 1.0).pow(5.0 / 2.0)))
        }
    }

    @Test
    fun atanhTest() {
        range(start = -0.9, end = 0.9) { x, xDual ->
            val v = atanh(xDual)

            areEqual(v.value, atanh(x))
            areEqual(v[1], 1.0 / (1.0 - x.pow(2)))
            areEqual(v[2], 2.0 * x / (1.0 - x.pow(2)).pow(2))
            areEqual(v[3], -2.0 * (3.0 * x.pow(2) + 1.0) / (x.pow(2) - 1).pow(3))
        }
    }

    @Test
    fun atan2Test() {
        val n = 5

        repeat(200) {
            repeat(200) {
                val y = Dual((1..n).map { rngNz() })
                val x = Dual((1..n).map { rngNz() })

                val v = atan2(y, x)
                val test = atan(y / x)

                areEqual(v.value, atan2(y.value, x.value))

                repeat(n - 1) {
                    areEqual(test[it + 1], v[it + 1])
                }
            }
        }
    }

    @Test
    fun powTest() {
        rangeScan(start = 1.0, end = 4.0, steps = 100) { power ->
            range(start = 1.0, steps = 1000) { x, xDual ->
                val v = xDual.pow(power)

                areEqual(v.value, x.pow(power))
                areEqual(v[1], power * x.pow(power - 1))
                areEqual(v[2], (power - 1.0) * power * x.pow(power - 2))
                areEqual(v[3], (power - 2.0) * (power - 1.0) * power * x.pow(power - 3))
            }
        }
    }

    @Test
    fun sqrtTest() {
        range(start = 1.0) { x, xDual ->
            val v = sqrt(xDual)

            areEqual(v.value, sqrt(x))
            areEqual(v[1], 1.0 / (2.0 * sqrt(x)))
            areEqual(v[2], -1.0 / (4.0 * x.pow(3.0 / 2.0)))
            areEqual(v[3], 3.0 / (8.0 * x.pow(5.0 / 2.0)))
        }
    }

    @Test
    fun intPowTest() {
        repeat(5) { power ->
            range(start = 1.0, steps = 1000) { x, xDual ->
                val v = xDual.pow(power)

                areEqual(v.value, x.pow(power))
                areEqual(v[1], power * x.pow(power - 1))
                areEqual(v[2], (power - 1.0) * power * x.pow(power - 2))
                areEqual(v[3], (power - 2.0) * (power - 1.0) * power * x.pow(power - 3))
            }
        }
    }

    @Test
    fun lnTest() {
        range(start = 5.0, end = 10.0) { x, xDual ->
            val v = ln(xDual)

            areEqual(v.value, ln(x))
            areEqual(v[1], 1.0 / x)
            areEqual(v[2], -1.0 / x.pow(2))
            areEqual(v[3], 2.0 / x.pow(3))
        }
    }

    @Test
    fun ln1pTest() {
        range(start = 5.0, end = 10.0) { _, xDual ->
            areEqual(ln1p(xDual), ln(1.0 + xDual))
        }
    }

    @Test
    fun log2Test() {
        range(start = 5.0, end = 10.0) { x, xDual ->
            val v = log2(xDual)

            areEqual(v.value, log2(x))
            areEqual(v[1], 1.0 / (x * LN_2))
            areEqual(v[2], -1.0 / (x.pow(2) * LN_2))
            areEqual(v[3], 2.0 / (x.pow(3) * LN_2))
        }
    }

    @Test
    fun log10Test() {
        range(start = 5.0, end = 10.0) { x, xDual ->
            val v = log10(xDual)

            areEqual(v.value, log10(x))
            areEqual(v[1], 1.0 / (x * LN_10))
            areEqual(v[2], -1.0 / (x.pow(2) * LN_10))
            areEqual(v[3], 2.0 / (x.pow(3) * LN_10))
        }
    }

    @Test
    fun expTest() {
        range { x, xDual ->
            val v = exp(xDual)

            areEqual(v.value, exp(x))
            areEqual(v[1], exp(x))
            areEqual(v[2], exp(x))
            areEqual(v[3], exp(x))
        }
    }

    @Test
    fun expm1Test() {
        range { _, xDual ->
            areEqual(expm1(xDual), exp(xDual) - 1.0)
        }
    }

    @Test
    fun logBaseTest() {
        rangeScan(start = 1.1, end = 5.0, steps = 100) { base ->
            val y = ln(base)

            range(start = 2.0, end = 5.0, steps = 1000) { x, xDual ->
                val v = log(xDual, base)

                areEqual(v.value, log(x, base))
                areEqual(v[1], 1.0 / (x * y))
                areEqual(v[2], -1.0 / (x.pow(2) * y))
                areEqual(v[3], 2.0 / (x.pow(3) * y))
            }
        }

        range(start = 5.0, end = 10.0) { _, xDual ->
            areEqual(ln(xDual), log(xDual, E))
        }
    }

    @Test
    fun expBaseTest() {
        rangeScan(start = 1.1, end = 5.0, steps = 100) { base ->
            val y = ln(base)

            range(start = 2.0, end = 5.0, steps = 1000) { x, xDual ->
                val v = exp(xDual, base)

                areEqual(v.value, base.pow(x))
                areEqual(v[1], base.pow(x) * y)
                areEqual(v[2], base.pow(x) * y.pow(2))
                areEqual(v[3], base.pow(x) * y.pow(3))
            }
        }

        range { _, xDual ->
            areEqual(exp(xDual), exp(xDual, E))
        }
    }

    @Test
    fun constTest() {
        repeat(100000) {
            val x = Dual.of(rngNz(), rngNz(), rngNz(), rngNz())
            val c = rngNz()
            val cDual = Dual.const(c, x.size)

            areEqual(
                x + c,
                c + x,
                x + cDual,
                cDual + x
            )

            areEqual(
                x - c,
                x - cDual
            )

            areEqual(
                c - x,
                cDual - x
            )

            areEqual(
                x * c,
                c * x,
                x * cDual,
                cDual * x
            )

            areEqual(
                x / c,
                x / cDual
            )

            areEqual(
                c / x,
                cDual / x,
            )
        }
    }

    // Test both hermite and dual:
    @Test
    fun quinticHermiteTest() {
        range(steps = 10) { x, xDual ->
            rangeScanKd(layers = 6, steps = 5) { vec ->
                val p0 = vec[0]
                val v0 = vec[1]
                val a0 = vec[2]
                val a1 = vec[3]
                val v1 = vec[4]
                val p1 = vec[5]

                val v = hermiteQuinticDual(p0, v0, a0, a1, v1, p1, xDual)

                areEqual(v.value, hermiteQuintic(p0, v0, a0, a1, v1, p1, x))
                areEqual(v[1], hermiteQuinticDerivative1(p0, v0, a0, a1, v1, p1, x))
                areEqual(v[2], hermiteQuinticDerivative2(p0, v0, a0, a1, v1, p1, x))
            }
        }
    }

    @Test
    fun intersectTest() {
        fun test(a: Pair<Dual, Dual>, b: Pair<Dual, Dual>) = assertTrue(a == b)

        test(
            Dual.intersect(
                Dual.of(1.0),
                Dual.of(2.0)
            ),
            Pair(
                Dual.of(1.0),
                Dual.of(2.0)
            )
        )

        test(
            Dual.intersect(
                Dual.of(1.0, 2.0, 3.0),
                Dual.of(4.0, 5.0)
            ),
            Pair(
                Dual.of(1.0, 2.0),
                Dual.of(4.0, 5.0)
            )
        )

        test(
            Dual.intersect(
                Dual.of(1.0),
                Dual.of(4.0, 5.0, 6.0)
            ),
            Pair(
                Dual.of(1.0),
                Dual.of(4.0)
            )
        )

        test(
            Dual.intersect(
                Dual.of(1.0, 2.0, 3.0),
                Dual.of(4.0)
            ),
            Pair(
                Dual.of(1.0),
                Dual.of(4.0)
            )
        )
    }
}