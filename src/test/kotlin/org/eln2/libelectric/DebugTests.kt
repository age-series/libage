package org.eln2.libelectric

import org.ageseries.libage.debug.dprint
import org.ageseries.libage.debug.dprintln
import org.junit.jupiter.api.Test

internal class DebugTests {
    @Test
    fun dprintTest() {
        dprint("foo+", true)
        dprintln("bar=", false)
        dprintln("foobar")
    }
}
