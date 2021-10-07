package org.eln2.libelectric

import org.eln2.libelectric.debug.dprint
import org.eln2.libelectric.debug.dprintln
import org.junit.jupiter.api.Test

internal class DebugTests {
    @Test
    fun dprintTest() {
        dprint("foo+", true)
        dprintln("bar=", false)
        dprintln("foobar")
    }
}
