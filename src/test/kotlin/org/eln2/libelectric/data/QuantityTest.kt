package org.eln2.libelectric.data

import org.ageseries.libage.data.*
import org.ageseries.libage.sim.STANDARD_TEMPERATURE
import org.eln2.libelectric.TestUtils.areEqual
import org.eln2.libelectric.TestUtils.areEqualAstronomic
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class QuantityTest {
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