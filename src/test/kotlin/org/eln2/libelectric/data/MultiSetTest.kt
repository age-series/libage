package org.eln2.libelectric.data

import org.ageseries.libage.data.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

internal class MultiSetTest {
    @Test
    fun emptySizes() {
        Assertions.assertEquals(multiSetOf<Int>().size, 0)
        Assertions.assertEquals(MutableMapMultiSet(emptyList<Pair<Int, Int>>().iterator()).size, 0)
        Assertions.assertEquals(MutableMapMultiSet(emptyList<Pair<Int, Int>>()).size, 0)
        Assertions.assertEquals(emptyMultiSet<Int>().size, 0)
        Assertions.assertTrue(multiSetOf<Int>().isEmpty())
        Assertions.assertTrue(mutableMultiSetOf<Int>().isEmpty())
        Assertions.assertTrue(MutableMapMultiSet(emptyList<Pair<Int, Int>>().iterator()).isEmpty())
        Assertions.assertTrue(MutableMapMultiSet(emptyList<Pair<Int, Int>>()).isEmpty())
        Assertions.assertTrue(emptyMultiSet<Int>().isEmpty())
    }

    @Test
    fun initializer() {
        val a = multiSetOf(1 to 1, 2 to 2, 3 to 3, 4 to 40)
        val b = MutableMapMultiSet(listOf(1 to 1, 2 to 2, 3 to 3, 4 to 40))
        val c = MutableMapMultiSet(listOf(1 to 1, 2 to 2, 3 to 3, 4 to 40).iterator())

        Assertions.assertEquals(a[1], 1)
        Assertions.assertEquals(a[2], 2)
        Assertions.assertEquals(a[3], 3)
        Assertions.assertEquals(a[4], 40)

        Assertions.assertEquals(b[1], 1)
        Assertions.assertEquals(b[2], 2)
        Assertions.assertEquals(b[3], 3)
        Assertions.assertEquals(b[4], 40)

        Assertions.assertEquals(c[1], 1)
        Assertions.assertEquals(c[2], 2)
        Assertions.assertEquals(c[3], 3)
        Assertions.assertEquals(c[4], 40)
    }

    @Test
    fun equality() {
        val a = multiSetOf(1 to 1, 2 to 2, 3 to 3, 4 to 40)
        val b = MutableMapMultiSet(listOf(4 to 40, 3 to 3, 2 to 2, 1 to 1))
        val c = MutableMapMultiSet(listOf(3 to 3, 1 to 1, 2 to 2, 4 to 40).iterator())

        Assertions.assertEquals(a, a)
        Assertions.assertEquals(b, b)
        Assertions.assertEquals(c, c)

        Assertions.assertEquals(a, b)
        Assertions.assertEquals(a, c)
        Assertions.assertEquals(b, a)
        Assertions.assertEquals(b, c)
        Assertions.assertEquals(c, a)
        Assertions.assertEquals(c, b)

        Assertions.assertNotEquals(a,  multiSetOf(1 to 1))
        Assertions.assertNotEquals(b,  multiSetOf(1 to 1))
        Assertions.assertNotEquals(c,  multiSetOf(1 to 1))

        Assertions.assertNotEquals(a, emptyMultiSet<Int>())
        Assertions.assertNotEquals(b, emptyMultiSet<Int>())
        Assertions.assertNotEquals(c, emptyMultiSet<Int>())

        Assertions.assertEquals(emptyMultiSet<Int>(), emptyMultiSet<Int>())
        Assertions.assertEquals(emptyMultiSet<Int>(), multiSetOf<Int>())
    }

    @Test
    fun multiplicity() {
        val set = mutableMultiSetOf<Int>()

        Assertions.assertFalse(set.contains(1))
        Assertions.assertEquals(set[1], 0)
        Assertions.assertEquals(set.remove(1), 0)
        Assertions.assertEquals(set, emptyMultiSet<Int>())

        set += 1
        set += 1
        set += 2

        Assertions.assertFalse(set.contains(3))
        Assertions.assertEquals(set[3], 0)
        Assertions.assertEquals(set.remove(3), 0)
        Assertions.assertNotEquals(set, emptyMultiSet<Int>())
        Assertions.assertTrue(set.isNotEmpty())

        Assertions.assertEquals(set[1], 2)
        Assertions.assertEquals(set[2], 1)

        set[1] = 10
        Assertions.assertEquals(set[1], 10)
        Assertions.assertEquals(set[2], 1)

        set.remove(1)
        Assertions.assertEquals(set[1], 0)
        Assertions.assertFalse(set.contains(1))

        set -= 2
        Assertions.assertEquals(set[2], 0)
        Assertions.assertFalse(set.contains(2))

        set.add(2, 5)
        Assertions.assertEquals(set[2], 5)
        Assertions.assertTrue(set.contains(2))

        set.clear()
        Assertions.assertEquals(set.size, 0)
        Assertions.assertTrue(set.isEmpty())

        repeat(10) {
            set += 3
        }

        Assertions.assertEquals(set.size, 1)
        Assertions.assertTrue(set.isNotEmpty())
        Assertions.assertEquals(set[3], 10)
        Assertions.assertTrue(set.contains(3))

        set.take(3, 5)
        Assertions.assertEquals(set[3], 5)
        Assertions.assertTrue(set.contains(3))

        set.take(3, 5)
        Assertions.assertEquals(set[3], 0)
        Assertions.assertFalse(set.contains(3))
        Assertions.assertTrue(set.isEmpty())
    }

    @Test
    fun multiplicityError() {
        val set = MutableMapMultiSet<Int>()

        assertThrows<IllegalArgumentException> {
            set -= 1
        }

        assertThrows<IllegalArgumentException> {
            set[1] = -1
        }

        assertThrows<IllegalArgumentException> {
            set.take(1, 5)
        }

        assertThrows<IllegalArgumentException> {
            set.add(1, 4)
            set.take(1, 5)
        }

        assertThrows<IllegalArgumentException> {
            set.put(1, -10)
        }

        assertDoesNotThrow {
            set.putAll(mapOf(10 to 1, 20 to 2))
        }

        assertThrows<IllegalArgumentException> {
            set.putAll(mapOf(10 to 1, 20 to -2))
        }
    }
}