package org.eln2.libelectric.data

import org.ageseries.libage.data.LocatorDispatcher
import org.ageseries.libage.data.LocatorSerializationException
import org.ageseries.libage.data.put
import org.ageseries.libage.data.requireLocator
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.IllegalArgumentException

internal class LocatorTest {
    private object InvalidSerializers : LocatorDispatcher<InvalidSerializers>() {
        val TOO_LITTLE_PUT = register<Int>(
            { v, b -> b.put(ByteArray(3)) },
            { b -> b.getInt() },
            4
        )

        val TOO_LITTLE_GET = register<Int>(
            { v, b -> b.put(ByteArray(4)) },
            { b -> b.get(ByteArray(3)); 0 },
            4
        )

        val TOO_MUCH_PUT = register<Int>(
            { v, b -> b.put(ByteArray(5)) },
            { b -> b.getInt() },
            4
        )

        val TOO_MUCH_GET = register<Int>(
            { v, b -> b.put(ByteArray(4)) },
            { b -> b.get(ByteArray(5)); 0 },
            4
        )

        // So we don't get out of bounds instead of what we want

        val FILLER = register<Byte>(
            { v, b -> b.put(v) },
            { b -> b.get() },
            1
        )
    }

    @Test
    fun putTooLittle() {
        val locator = InvalidSerializers.buildLocator {
            it.put(TOO_LITTLE_PUT, 0)
        }

        assertThrows<LocatorSerializationException> {
            locator.toImage()
        }
    }

    @Test
    fun getTooLittle() {
        val locator = InvalidSerializers.buildLocator {
            it.put(TOO_LITTLE_GET, 0)
        }

        val image = locator.toImage()

        assertThrows<LocatorSerializationException> {
            InvalidSerializers.fromImage(image)
        }
    }

    @Test
    fun putTooMuch() {
        val locator = InvalidSerializers.buildLocator {
            it.put(TOO_MUCH_PUT, 0)
            it.put(FILLER, 0)
        }

        assertThrows<LocatorSerializationException> {
            locator.toImage()
        }
    }

    @Test
    fun getTooMuch() {
        val locator = InvalidSerializers.buildLocator {
            it.put(TOO_MUCH_GET, 0)
            it.put(FILLER, 0)
        }

        val image = locator.toImage()

        assertThrows<LocatorSerializationException> {
            InvalidSerializers.fromImage(image)
        }
    }

    private object Dispatcher : LocatorDispatcher<Dispatcher>() {
        val INT1 = register<Int>(
            { int, buffer -> buffer.putInt(int) },
            { buffer -> buffer.getInt() },
            4
        )

        val INT2 = defineFrom(INT1)
        val INT3 = defineFrom(INT1)

        val BYTE1 = register<Byte>(
            { byte, buffer -> buffer.put(byte) },
            { buffer -> buffer.get() },
            1
        )
    }

    @Test
    fun empty() {
        val builder = Dispatcher.builder()

        assertThrows<IllegalStateException> {
            builder.build()
        }
    }

    @Test
    fun duplicates() {
        assertThrows<IllegalArgumentException> {
            Dispatcher.buildLocator {
                it.put(INT1, 0)
                it.put(INT1, 1)
            }
        }
    }

    @Test
    fun archetypeCaching() {
        val a1 = byteArrayOf(1, 2, 3)
        val a2 = byteArrayOf(1, 2, 3)

        assertTrue(Dispatcher.getArchetype(a1) === Dispatcher.getArchetype(a2))
    }

    @Test
    fun archetypeOrdering() {
        val expected = byteArrayOf(Dispatcher.INT1.shardID, Dispatcher.INT2.shardID, Dispatcher.INT3.shardID).toList()

        assertEquals(expected, Dispatcher.buildLocator {
            it.put(INT1, 1)
            it.put(INT2, 2)
            it.put(INT3, 3)
        }.archetype.toList())

        assertEquals(expected, Dispatcher.buildLocator {
            it.put(INT1, 1)
            it.put(INT3, 3)
            it.put(INT2, 2)
        }.archetype.toList())

        assertEquals(expected, Dispatcher.buildLocator {
            it.put(INT3, 3)
            it.put(INT1, 1)
            it.put(INT2, 2)
        }.archetype.toList())

        assertEquals(expected, Dispatcher.buildLocator {
            it.put(INT3, 3)
            it.put(INT2, 2)
            it.put(INT1, 1)
        }.archetype.toList())
    }

    @Test
    fun getters() {
        run {
            val locator = Dispatcher.buildLocator {
                it.put(INT1, 1)
            }

            assertEquals(locator.get(Dispatcher.INT1), 1)
            assertEquals(locator.get(Dispatcher.INT2), null)
        }

        run {
            val locator = Dispatcher.buildLocator {
                it.put(INT1, 1)
                it.put(INT2, 2)
            }

            assertEquals(locator.get(Dispatcher.INT1), 1)
            assertEquals(locator.get(Dispatcher.INT2), 2)
            assertEquals(locator.get(Dispatcher.INT3), null)
        }

        run {
            val locator = Dispatcher.buildLocator {
                it.put(INT2, 2)
                it.put(INT1, 1)
            }

            assertEquals(locator.get(Dispatcher.INT1), 1)
            assertEquals(locator.get(Dispatcher.INT2), 2)
            assertEquals(locator.get(Dispatcher.INT3), null)
        }
    }

    @Test
    fun serializers() {
        run {
            val locator = Dispatcher.fromImage(
                Dispatcher.buildLocator {
                    it.put(INT1, 1)
                }.toImage()
            )

            assertEquals(locator.get(Dispatcher.INT1), 1)
            assertEquals(locator.get(Dispatcher.INT2), null)
        }

        run {
            val locator = Dispatcher.fromImage(
                Dispatcher.buildLocator {
                    it.put(INT1, 1)
                    it.put(INT2, 2)
                }.toImage()
            )

            assertEquals(locator.get(Dispatcher.INT1), 1)
            assertEquals(locator.get(Dispatcher.INT2), 2)
            assertEquals(locator.get(Dispatcher.INT3), null)
        }

        run {
            val locator = Dispatcher.fromImage(
                Dispatcher.buildLocator {
                    it.put(INT2, 2)
                    it.put(INT1, 1)
                }.toImage()
            )

            assertEquals(locator.get(Dispatcher.INT1), 1)
            assertEquals(locator.get(Dispatcher.INT2), 2)
            assertEquals(locator.get(Dispatcher.INT3), null)
        }
    }

    @Test
    fun has() {
        val locator = Dispatcher.buildLocator {
            it.put(INT1, 1)
            it.put(INT2, 2)
        }

        assertTrue(locator.has(Dispatcher.INT1))
        assertTrue(locator.has(Dispatcher.INT2))
        assertFalse(locator.has(Dispatcher.INT3))
    }

    @Test
    fun requireLocator() {
        val locator = Dispatcher.buildLocator {
            it.put(INT1, 1)
            it.put(INT2, 2)
        }

        assertDoesNotThrow {
            locator.requireLocator(Dispatcher.INT1)
            locator.requireLocator(Dispatcher.INT2)
        }

        assertThrows<IllegalArgumentException> {
            locator.requireLocator(Dispatcher.INT3)
        }
    }
}