package org.eln2.libelectric.data

import org.ageseries.libage.data.Event
import org.ageseries.libage.data.EventBus
import org.ageseries.libage.data.registerHandler
import org.ageseries.libage.data.unregisterHandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.random.Random

class EventTest {
    private class MyEvent1 : Event {
        var received = false
    }

    private class MyEvent2 : Event {
        var received = false
    }

    private class MyTestEventParallel(val x: Int): Event

    @Test
    fun testScalarDispatch(){
        var eventsRegistered = true

        var receiveCount1 = 0
        var receiveCount2 = 0

        fun handler1(event: MyEvent1){
            if(!eventsRegistered){
                fail("Received event after handler was removed")
            }

            receiveCount1++

            event.received = true
        }

        fun handler2(event: MyEvent2){
            if(!eventsRegistered){
                fail("Received event after handler was removed")
            }

            receiveCount2++

            event.received = true
        }

        val manager = EventBus()

        manager.registerHandler(::handler1)
        manager.registerHandler(::handler2)

        repeat(100){
            val event = MyEvent1()
            manager.send(event)
            assertTrue(event.received)
        }

        assertEquals(receiveCount1, 100)
        assertEquals(receiveCount2, 0)

        repeat(50){
            val event = MyEvent2()
            manager.send(event)
            assertTrue(event.received)
        }

        assertEquals(receiveCount1, 100)
        assertEquals(receiveCount2, 50)

        manager.unregisterHandler(::handler1)
        manager.unregisterHandler(::handler2)

        eventsRegistered = false

        repeat(50){
            val a = MyEvent1()
            val b = MyEvent2()

            manager.send(a)
            manager.send(b)

            assertTrue(!a.received && !b.received)
        }

        assertEquals(receiveCount1, 100)
        assertEquals(receiveCount2, 50)
    }

    @Test
    fun testParallelDispatch(){
        val manager = EventBus()

        val toSend = (1..10000).map { MyTestEventParallel(it) }
        val received = ConcurrentLinkedQueue<MyTestEventParallel>()

        manager.registerHandler<MyTestEventParallel>{ received.add(it) }

        val sendQueue = ConcurrentLinkedQueue(toSend)
        val threads = (1..10).map{
            return@map Thread {
                while (true){
                    val event = sendQueue.poll()
                        ?: return@Thread

                    manager.send(event)

                    if(Random.nextInt(0, 10) == 5) {
                        Thread.sleep(1)
                    }
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals(received.sortedBy { it.x }, toSend)
    }
}