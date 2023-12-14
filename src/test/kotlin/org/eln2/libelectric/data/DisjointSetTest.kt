package org.eln2.libelectric.data

import org.ageseries.libage.data.DisjointSet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class DisjointSetTest {
    @Test
    fun equalityAndInitialization() {
        val a = DisjointSet()
        val b = DisjointSet()

        assertNotEquals(a, b)
        assertNotEquals(a.representative, b.representative)
        assertEquals(a.parent, a)
        assertEquals(a.representative, a)
        assertEquals(a, a)
    }

    @Test
    fun unionBySize() {
        val a = DisjointSet()
        val b = DisjointSet()

        a.unite(b)

        assertEquals(a.representative, b.representative)
        val rep1 = a.representative

        assertEquals(rep1.size, 2)

        val c = DisjointSet()
        assertEquals(c.size, 1)
        c.unite(a)

        assertEquals(c.representative, rep1)
        assertEquals(rep1.size, 3)

        val d = DisjointSet()
        d.unite(c)

        listOf(a, b, c, d).forEach {
            assertEquals(it.representative, rep1)
        }
    }

    @Test
    fun unionByPriority() {
        val a = DisjointSet()
        val b = DisjointSet()

        a.unite(b)

        val c = DisjointSet(1)
        c.unite(a)

        listOf(a, b, c).forEach {
            assertEquals(it.representative, c)
        }
    }
}