package org.eln2.libelectric

import org.ageseries.libage.mathematics.Dual
import org.ageseries.libage.mathematics.approxEq
import org.junit.jupiter.api.Assertions

internal object TestUtils {
    private const val COMPARE_EPS = 1e-6
    private const val COMPARE_EPS_ASTRONOMIC = 10.0

    fun rangeScan(start: Double = 0.0, end: Double = 10.0, steps: Int = 10000, action: ((Double) -> Unit)) {
        require(start < end)

        val stepSize = (end - start) / steps
        var x = start

        while (x < end) {
            action(x)

            x += stepSize
        }
    }

    fun rangeScanKd(layers: Int, start: Double = 0.0, end: Double = 10.0, steps: Int = 10, action: ((DoubleArray) -> Unit)) {
        fun helper(depth: Int, vec: DoubleArray) {
            rangeScan(start = start, end = end, steps = steps) { v ->
                vec[depth] = v

                if (depth > 0) {
                    helper(depth - 1, vec);
                }
                else {
                    action(vec);
                }
            }
        }

        helper(layers - 1, DoubleArray(layers))
    }

    fun range(derivatives: Int = 3, start: Double = 0.0, end: Double = 10.0, steps: Int = 10000, action: ((Double, Dual) -> Unit)) {
        rangeScan(start = start, end = end, steps = steps) { x ->
            action(x, Dual.variable(x, derivatives + 1))
        }
    }

    fun areEqual(vararg reals : Double) {
        for (i in 1 until reals.size) {
            val a = reals[i - 1]
            val b = reals[i]

            Assertions.assertTrue(a.approxEq(b, COMPARE_EPS))
        }
    }

    fun areEqualAstronomic(vararg reals : Double) {
        for (i in 1 until reals.size) {
            val a = reals[i - 1]
            val b = reals[i]
            Assertions.assertTrue(a.approxEq(b, COMPARE_EPS_ASTRONOMIC))
        }
    }
    fun areEqual(vararg duals : Dual) {
        for (i in 1 until duals.size) {
            Assertions.assertTrue(duals[i - 1].approxEq(duals[i], COMPARE_EPS))
        }
    }
}