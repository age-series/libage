package org.eln2.libelectric.data

import org.ageseries.libage.data.biMapOf
import org.ageseries.libage.data.emptyBiMap
import org.ageseries.libage.data.mutableBiMapOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class BiMapTest {
    @Test
    fun emptySizes() {
        assertEquals(emptyBiMap<Int, Int>().size, 0)
        assertEquals(biMapOf<Int, Int>().size, 0)
        assertEquals(mutableBiMapOf<Int, Int>().size, 0)
    }

    @Test
    fun initializer() {
        val charPairs = arrayOf(1 to 'A', 2 to 'B', 3 to 'C')
        val bm = biMapOf(*charPairs)
        assertEquals(bm.size, charPairs.size)
        assertEquals(bm.forward.size, bm.size)
        assertEquals(bm.backward.size, bm.size)
        bm.forward.entries.forEach { ent ->
            assert(ent.toPair() in charPairs)
        }
        bm.backward.entries.forEach { (b, f) ->
            assert((f to b) in charPairs)
        }
    }

    @Test
    fun preservesBijection() {
        val bm = mutableBiMapOf(1 to 'A', 2 to 'B', 3 to 'C')
        assertEquals(bm.size, 3)
        assertEquals(bm.forward[1], 'A')
        assertEquals(bm.forward[2], 'B')
        assertEquals(bm.forward[3], 'C')
        assertEquals(bm.backward['A'], 1)
        assertEquals(bm.backward['B'], 2)
        assertEquals(bm.backward['C'], 3)
        bm.addOrReplace(1, 'B')
        assertEquals(bm.size, 2)
        assertEquals(bm.forward[1], 'B')
        assertEquals(bm.forward[3], 'C')
        assertEquals(bm.backward['B'], 1)
        assertEquals(bm.backward['C'], 3)
    }

    @Test
    fun replacementError() {
        val bm = mutableBiMapOf(1 to 'A', 2 to 'B')
        assertThrows<IllegalStateException> {
            bm.add(1, 'B')
        }
    }
}